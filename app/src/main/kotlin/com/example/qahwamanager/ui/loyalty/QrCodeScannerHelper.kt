package com.example.qahwamanager.ui.loyalty

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Launches Google Play Services' zero-permission QR Code Scanner sheet (`GmsBarcodeScanning`).
 * Falls back gracefully via [onFallbackRequired] if Play Services code scanner module is unavailable.
 */
object QrCodeScannerHelper {

    fun startQrScan(
        context: Context,
        onScanned: (String) -> Unit,
        onFallbackRequired: () -> Unit = {},
    ) {
        runCatching {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build()
            val scanner = GmsBarcodeScanning.getClient(context, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val raw = barcode.rawValue?.trim().orEmpty()
                    if (raw.isNotEmpty()) {
                        onScanned(raw)
                    }
                }
                .addOnFailureListener {
                    onFallbackRequired()
                }
        }.onFailure {
            onFallbackRequired()
        }
    }
}
