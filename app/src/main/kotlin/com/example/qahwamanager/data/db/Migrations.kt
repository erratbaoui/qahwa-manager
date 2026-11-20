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
