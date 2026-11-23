package com.example.qahwamanager.ui.loyalty

/**
 * Self-contained ISO/IEC 18004 QR Code matrix and SVG generator (Versions 1..6, ECC Level L, Byte Mode).
 * Supports URLs up to 134 UTF-8 bytes without external library dependencies.
 */
object QrCodeGenerator {

    private data class VersionInfo(
        val version: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocks: Int,
        val alignmentCenters: IntArray,
    )

    private val VERSIONS = listOf(
        VersionInfo(1, 19, 7, 1, intArrayOf()),
        VersionInfo(2, 34, 10, 1, intArrayOf(6, 18)),
        VersionInfo(3, 55, 15, 1, intArrayOf(6, 22)),
        VersionInfo(4, 80, 20, 1, intArrayOf(6, 26)),
        VersionInfo(5, 108, 26, 1, intArrayOf(6, 30)),
        VersionInfo(6, 136, 18, 2, intArrayOf(6, 34)),
    )

    // Precomputed 15-bit format strings for ECC Level L (01) and Mask 0..7 (with BCH & 0x5412 XOR mask)
    private val FORMAT_BITS_LEVEL_L = intArrayOf(
        0x77C4, // Mask 0
        0x72F3, // Mask 1
        0x7DAA, // Mask 2
        0x789D, // Mask 3
        0x662F, // Mask 4
        0x6318, // Mask 5
        0x6C41, // Mask 6
        0x6976, // Mask 7
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val root = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], root)
            }
            poly = next
        }
        return poly
    }

    private fun rsComputeRemainder(data: IntArray, degree: Int): IntArray {
        val gen = rsGeneratorPoly(degree)
        val rem = IntArray(degree)
        for (byte in data) {
            val factor = byte xor rem[0]
            System.arraycopy(rem, 1, rem, 0, degree - 1)
            rem[degree - 1] = 0
            for (i in 0 until degree) {
                rem[i] = rem[i] xor gfMul(gen[i + 1], factor)
            }
        }
        return rem
    }

    /**
     * Encodes [content] into a square boolean matrix where `true` represents a dark QR module.
     */
    fun encode(content: String): Array<BooleanArray> {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val vInfo = VERSIONS.firstOrNull { bytes.size + 2 <= it.totalDataCodewords }
            ?: VERSIONS.last()
        val usableBytes = if (bytes.size + 2 > vInfo.totalDataCodewords) {
            bytes.copyOf(vInfo.totalDataCodewords - 2)
        } else {
            bytes
        }

        // 1. Build data bit stream: 4-bit mode (0100) + 8-bit length + payload + terminator + pad bytes
        val dataCodewords = IntArray(vInfo.totalDataCodewords)
        var bitCapacity = vInfo.totalDataCodewords * 8
        val bits = ArrayList<Int>(bitCapacity)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }
        appendBits(0b0100, 4)
        appendBits(usableBytes.size, 8)
        for (b in usableBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        val terminatorLen = minOf(4, bitCapacity - bits.size)
        appendBits(0, terminatorLen)
        while (bits.size % 8 != 0) {
            bits.add(0)
        }
        for (i in 0 until bits.size / 8) {
            var cw = 0
            for (b in 0 until 8) {
                cw = (cw shl 1) or bits[i * 8 + b]
            }
            dataCodewords[i] = cw
        }
        var padToggle = true
        for (i in (bits.size / 8) until vInfo.totalDataCodewords) {
            dataCodewords[i] = if (padToggle) 0xEC else 0x11
            padToggle = !padToggle
        }

        // 2. Split into blocks and compute Reed-Solomon EC codewords
        val blockSize = vInfo.totalDataCodewords / vInfo.numBlocks
        val dataBlocks = Array(vInfo.numBlocks) { b ->
            IntArray(blockSize) { idx -> dataCodewords[b * blockSize + idx] }
        }
        val ecBlocks = Array(vInfo.numBlocks) { b ->
            rsComputeRemainder(dataBlocks[b], vInfo.ecCodewordsPerBlock)
        }

        val finalCodewords = ArrayList<Int>(
            vInfo.totalDataCodewords + vInfo.numBlocks * vInfo.ecCodewordsPerBlock
        )
        for (i in 0 until blockSize) {
            for (b in 0 until vInfo.numBlocks) {
                finalCodewords.add(dataBlocks[b][i])
            }
        }
        for (i in 0 until vInfo.ecCodewordsPerBlock) {
            for (b in 0 until vInfo.numBlocks) {
                finalCodewords.add(ecBlocks[b][i])
            }
        }

        // 3. Initialize module grid
        val size = 17 + 4 * vInfo.version
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunctionModule(r: Int, c: Int, dark: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                modules[r][c] = dark
                isFunction[r][c] = true
            }
        }

        fun placeFinderPattern(top: Int, left: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = top + dr
                    val c = left + dc
                    if (r !in 0 until size || c !in 0 until size) continue
                    val inOuter = dr in 0..6 && dc in 0..6 && (dr == 0 || dr == 6 || dc == 0 || dc == 6)
                    val inInner = dr in 2..4 && dc in 2..4
                    setFunctionModule(r, c, inOuter || inInner)
                }
            }
        }

        placeFinderPattern(0, 0)
        placeFinderPattern(0, size - 7)
        placeFinderPattern(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            setFunctionModule(6, i, i % 2 == 0)
            setFunctionModule(i, 6, i % 2 == 0)
        }

        // Alignment patterns
        val centers = vInfo.alignmentCenters
        for (cr in centers) {
            for (cc in centers) {
                val overlapsFinder = (cr <= 8 && cc <= 8) ||
                    (cr <= 8 && cc >= size - 9) ||
                    (cr >= size - 9 && cc <= 8)
                if (overlapsFinder) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val dark = dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0)
                        setFunctionModule(cr + dr, cc + dc, dark)
                    }
                }
            }
        }

        // Dark module & reserved format info areas
        setFunctionModule(4 * vInfo.version + 9, 8, true)
        for (i in 0..8) {
            if (!isFunction[8][i]) setFunctionModule(8, i, false)
            if (!isFunction[i][8]) setFunctionModule(i, 8, false)
        }
        for (i in 0..7) {
            if (!isFunction[8][size - 1 - i]) setFunctionModule(8, size - 1 - i, false)
            if (!isFunction[size - 1 - i][8]) setFunctionModule(size - 1 - i, 8, false)
        }

        // 4. Zig-zag data placement
        var bitIndex = 0
        val totalBits = finalCodewords.size * 8
        var upward = true
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (step in 0 until size) {
                val r = if (upward) size - 1 - step else step
                for (j in 0..1) {
                    val c = right - j
                    if (!isFunction[r][c]) {
                        val dark = if (bitIndex < totalBits) {
                            val cw = finalCodewords[bitIndex ushr 3]
                            ((cw ushr (7 - (bitIndex and 7))) and 1) == 1
                        } else {
                            false
                        }
                        // Mask 0: (r + c) % 2 == 0
                        val masked = if ((r + c) % 2 == 0) !dark else dark
                        modules[r][c] = masked
                        bitIndex++
                    }
                }
            }
            upward = !upward
            right -= 2
        }

        // 5. Place format bits for Level L, Mask 0
        val formatBits = FORMAT_BITS_LEVEL_L[0]
        fun formatBit(i: Int): Boolean = ((formatBits ushr i) and 1) == 1

        // Around top-left
        val tlCoords = arrayOf(
            8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5,
            8 to 7, 8 to 8, 7 to 8, 5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8,
        )
        for (i in 0..14) {
            val (r, c) = tlCoords[i]
            modules[r][c] = formatBit(14 - i)
        }
        // Bottom-left & top-right
        for (i in 0..6) {
            modules[size - 1 - i][8] = formatBit(14 - i)
        }
        for (i in 7..14) {
            modules[8][size - 15 + i] = formatBit(14 - i)
        }

        return modules
    }

    /**
     * Renders [content] into a standalone inline `<svg>` string with a 4-module quiet zone.
     */
    fun toSvg(content: String, darkHex: String = "#143023", lightHex: String = "#FFFFFF"): String {
        val matrix = encode(content)
        val n = matrix.size
        val quiet = 2
        val total = n + quiet * 2
        val sb = StringBuilder(total * total * 12)
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $total $total" shape-rendering="crispEdges">""")
        sb.append("""<rect width="$total" height="$total" fill="$lightHex"/>""")
        sb.append("""<path fill="$darkHex" d="""")
        for (r in 0 until n) {
            for (c in 0 until n) {
                if (matrix[r][c]) {
                    val x = c + quiet
                    val y = r + quiet
                    sb.append("M").append(x).append(" ").append(y).append("h1v1h-1z")
                }
            }
        }
        sb.append(""""/></svg>""")
        return sb.toString()
    }
}
