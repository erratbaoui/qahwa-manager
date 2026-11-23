package com.example.qahwamanager.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: money moves from REAL dirhams to INTEGER centimes, and order items gain
 * Arabic/French name snapshots (filled from the current menu where the item still exists).
 *
 * SQLite can't change a column's type, so each table is rebuilt. The old order tables are
 * renamed (child first) and dropped (child first), so dropping the old parent can never
 * cascade into the new order_items rows, whether or not foreign keys are enforced.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `menu_items` RENAME TO `menu_items_v1`")
        db.execSQL("ALTER TABLE `order_items` RENAME TO `order_items_v1`")
        db.execSQL("ALTER TABLE `orders` RENAME TO `orders_v1`")

        db.execSQL("CREATE TABLE IF NOT EXISTS `menu_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `nameAr` TEXT NOT NULL, `nameFr` TEXT NOT NULL, `category` TEXT NOT NULL, `priceCentimes` INTEGER NOT NULL, `description` TEXT NOT NULL, `isAvailable` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `orders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tableNumber` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `totalCentimes` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `order_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `orderId` INTEGER NOT NULL, `menuItemId` INTEGER NOT NULL, `menuItemName` TEXT NOT NULL, `menuItemNameAr` TEXT NOT NULL, `menuItemNameFr` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `unitPriceCentimes` INTEGER NOT NULL, FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")

        db.execSQL(
            """
            INSERT INTO `menu_items` (id, name, nameAr, nameFr, category, priceCentimes, description, isAvailable)
            SELECT id, name, nameAr, nameFr, category, CAST(ROUND(price * 100) AS INTEGER), description, isAvailable
            FROM `menu_items_v1`
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `orders` (id, tableNumber, status, createdAt, totalCentimes)
            SELECT id, tableNumber, status, createdAt, CAST(ROUND(totalAmount * 100) AS INTEGER)
            FROM `orders_v1`
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `order_items` (id, orderId, menuItemId, menuItemName, menuItemNameAr, menuItemNameFr, quantity, unitPriceCentimes)
            SELECT oi.id, oi.orderId, oi.menuItemId, oi.menuItemName,
                   COALESCE(m.nameAr, ''), COALESCE(m.nameFr, ''),
                   oi.quantity, CAST(ROUND(oi.unitPrice * 100) AS INTEGER)
            FROM `order_items_v1` AS oi
            LEFT JOIN `menu_items_v1` AS m ON m.id = oi.menuItemId
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `order_items_v1`")
        db.execSQL("DROP TABLE `orders_v1`")
        db.execSQL("DROP TABLE `menu_items_v1`")

        // The old index went away with order_items_v1.
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_order_items_orderId` ON `order_items` (`orderId`)")
    }
}

/**
 * v2 -> v3: staff, shifts and owner credentials. Purely additive: four new tables, and five
 * nullable shift columns on `orders` (added with ALTER TABLE, so they carry no foreign keys).
 * Existing rows are untouched.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `staff` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isOwner` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `shifts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `staffId` INTEGER NOT NULL, `status` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `endedByTakeover` INTEGER NOT NULL, `settledAt` INTEGER, `expectedCentimes` INTEGER, `countedCentimes` INTEGER, `failedOwnerPinAttempts` INTEGER NOT NULL, FOREIGN KEY(`staffId`) REFERENCES `staff`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shifts_staffId` ON `shifts` (`staffId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shifts_status` ON `shifts` (`status`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `shift_left_open` (`shiftId` INTEGER NOT NULL, `orderId` INTEGER NOT NULL, `totalCentimes` INTEGER NOT NULL, PRIMARY KEY(`shiftId`, `orderId`), FOREIGN KEY(`shiftId`) REFERENCES `shifts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`orderId`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_left_open_orderId` ON `shift_left_open` (`orderId`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `owner_credentials` (`id` INTEGER NOT NULL, `pinHash` TEXT NOT NULL, `pinSalt` TEXT NOT NULL, `recoveryHash` TEXT NOT NULL, `recoverySalt` TEXT NOT NULL, `failedAttemptsInRow` INTEGER NOT NULL, `lockedUntil` INTEGER NOT NULL, PRIMARY KEY(`id`))")

        db.execSQL("ALTER TABLE `orders` ADD COLUMN `openedShiftId` INTEGER")
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `paidShiftId` INTEGER")
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `paidAt` INTEGER")
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `cancelledAt` INTEGER")
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `cancelledShiftId` INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_openedShiftId` ON `orders` (`openedShiftId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_orders_paidShiftId` ON `orders` (`paidShiftId`)")
    }
}

/**
 * v3 -> v4: adds a `role` column to `staff` (OWNER, WAITER, BARMAN, SECURITY).
 * Existing rows default to WAITER, with `isOwner = 1` rows set to OWNER.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `staff` ADD COLUMN `role` TEXT NOT NULL DEFAULT 'WAITER'")
        db.execSQL("UPDATE `staff` SET `role` = 'OWNER' WHERE `isOwner` = 1")
    }
}

/**
 * v4 -> v5: adds cloud sync metadata (`remoteId`, `updatedAt`, and `isDeleted` on menu/inventory)
 * so multi-owner devices can sync safely via Cloud Firestore without primary-key collisions.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `remoteId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `menu_items` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `menu_items` SET `remoteId` = 'menu-' || `id` || '-' || lower(hex(randomblob(8))) WHERE `remoteId` = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_menu_items_remoteId` ON `menu_items` (`remoteId`)")

        db.execSQL("ALTER TABLE `inventory_items` ADD COLUMN `remoteId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `inventory_items` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `inventory_items` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `inventory_items` SET `remoteId` = 'inv-' || `id` || '-' || lower(hex(randomblob(8))) WHERE `remoteId` = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_inventory_items_remoteId` ON `inventory_items` (`remoteId`)")

        db.execSQL("ALTER TABLE `staff` ADD COLUMN `remoteId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `staff` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `staff` SET `remoteId` = 'staff-' || `id` || '-' || lower(hex(randomblob(8))) WHERE `remoteId` = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_staff_remoteId` ON `staff` (`remoteId`)")

        db.execSQL("ALTER TABLE `shifts` ADD COLUMN `remoteId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `shifts` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `shifts` SET `remoteId` = 'shift-' || `id` || '-' || lower(hex(randomblob(8))) WHERE `remoteId` = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shifts_remoteId` ON `shifts` (`remoteId`)")

        db.execSQL("ALTER TABLE `orders` ADD COLUMN `remoteId` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `orders` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `orders` SET `remoteId` = 'order-' || `id` || '-' || lower(hex(randomblob(8))) WHERE `remoteId` = ''")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_orders_remoteId` ON `orders` (`remoteId`)")
    }
}

/**
 * v5 -> v6: adds the `spendings` table for tracking daily/shift expenses.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `spendings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `shiftId` INTEGER,
                `title` TEXT NOT NULL,
                `amountCentimes` INTEGER NOT NULL,
                `spentAt` INTEGER NOT NULL,
                `remoteId` TEXT NOT NULL DEFAULT '',
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `isDeleted` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_spendings_shiftId` ON `spendings` (`shiftId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_spendings_spentAt` ON `spendings` (`spentAt`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_spendings_remoteId` ON `spendings` (`remoteId`)")
    }
}

/**
 * v6 -> v7: adds the `loyalty_customers` table for the Tappy-style NFC/QR digital stamp card system.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `loyalty_customers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `phone` TEXT NOT NULL,
                `currentStamps` INTEGER NOT NULL,
                `lifetimeStamps` INTEGER NOT NULL,
                `rewardsRedeemed` INTEGER NOT NULL,
                `lastVisitAt` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `remoteId` TEXT NOT NULL DEFAULT '',
                `updatedAt` INTEGER NOT NULL DEFAULT 0,
                `isDeleted` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loyalty_customers_phone` ON `loyalty_customers` (`phone`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loyalty_customers_lastVisitAt` ON `loyalty_customers` (`lastVisitAt`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_loyalty_customers_remoteId` ON `loyalty_customers` (`remoteId`)")
    }
}
