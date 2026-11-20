package com.example.qahwamanager.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        QahwaDatabase::class.java,
    )

    @Test
    fun migrate1To2_convertsMoneyToCentimesAndKeepsOrderHistory() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL("INSERT INTO menu_items (id, name, nameAr, nameFr, category, price, description, isAvailable) VALUES (1, 'Mint tea', 'أتاي', 'Thé', 'tea', 8.5, '', 1)")
            execSQL("INSERT INTO menu_items (id, name, nameAr, nameFr, category, price, description, isAvailable) VALUES (2, 'Cheap', '', '', 'food', 0.29, '', 0)")
            execSQL("INSERT INTO orders (id, tableNumber, status, createdAt, totalAmount) VALUES (10, '4', 'PAID', 1000, 17.29)")
            execSQL("INSERT INTO order_items (id, orderId, menuItemId, menuItemName, quantity, unitPrice) VALUES (100, 10, 1, 'Mint tea', 2, 8.5)")
            // Menu item 99 was deleted after the sale; its snapshot must survive.
            execSQL("INSERT INTO order_items (id, orderId, menuItemId, menuItemName, quantity, unitPrice) VALUES (101, 10, 99, 'Gone', 1, 0.29)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, MIGRATION_1_2)

        db.query("SELECT id, priceCentimes, isAvailable FROM menu_items ORDER BY id").use { c ->
            c.moveToNext(); assertEquals(listOf(1L, 850L, 1L), listOf(c.getLong(0), c.getLong(1), c.getLong(2)))
            c.moveToNext(); assertEquals(listOf(2L, 29L, 0L), listOf(c.getLong(0), c.getLong(1), c.getLong(2)))
        }
        db.query("SELECT tableNumber, status, createdAt, totalCentimes FROM orders WHERE id = 10").use { c ->
            c.moveToFirst()
            assertEquals("4", c.getString(0))
            assertEquals("PAID", c.getString(1))
            assertEquals(1000L, c.getLong(2))
            assertEquals(1729L, c.getLong(3))
        }
        db.query("SELECT id, orderId, menuItemName, menuItemNameAr, menuItemNameFr, quantity, unitPriceCentimes FROM order_items ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToNext()
            assertEquals(listOf<Any>(100L, 10L, "Mint tea", "أتاي", "Thé", 2L, 850L),
                listOf<Any>(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getLong(5), c.getLong(6)))
            c.moveToNext()
            assertEquals(listOf<Any>(101L, 10L, "Gone", "", "", 1L, 29L),
                listOf<Any>(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getLong(5), c.getLong(6)))
        }
    }

    @Test
    fun migrate2To3_keepsExistingDataAndAddsShiftTables() {
        helper.createDatabase(DB_NAME, 2).apply {
            execSQL("INSERT INTO menu_items (id, name, nameAr, nameFr, category, priceCentimes, description, isAvailable) VALUES (1, 'Mint tea', 'أتاي', 'Thé', 'tea', 850, '', 1)")
            execSQL("INSERT INTO inventory_items (id, name, quantity, unit, lowStockThreshold) VALUES (1, 'Mint', 2.5, 'kg', 0.5)")
            execSQL("INSERT INTO orders (id, tableNumber, status, createdAt, totalCentimes) VALUES (10, '4', 'PAID', 1000, 1729)")
            execSQL("INSERT INTO orders (id, tableNumber, status, createdAt, totalCentimes) VALUES (11, '7', 'OPEN', 2000, 500)")
            execSQL("INSERT INTO order_items (id, orderId, menuItemId, menuItemName, menuItemNameAr, menuItemNameFr, quantity, unitPriceCentimes) VALUES (100, 10, 1, 'Mint tea', 'أتاي', 'Thé', 2, 850)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 3, true, MIGRATION_2_3)

        db.query("SELECT id, name, nameAr, nameFr, category, priceCentimes, description, isAvailable FROM menu_items").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals(listOf<Any>(1L, "Mint tea", "أتاي", "Thé", "tea", 850L, "", 1L),
                listOf<Any>(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getLong(5), c.getString(6), c.getLong(7)))
        }
        db.query("SELECT id, name, quantity, unit, lowStockThreshold FROM inventory_items").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals(listOf<Any>(1L, "Mint", 2.5, "kg", 0.5),
                listOf<Any>(c.getLong(0), c.getString(1), c.getDouble(2), c.getString(3), c.getDouble(4)))
        }
        db.query("SELECT id, tableNumber, status, createdAt, totalCentimes, openedShiftId, paidShiftId, paidAt, cancelledAt, cancelledShiftId FROM orders ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToNext()
            assertEquals(listOf<Any>(10L, "4", "PAID", 1000L, 1729L),
                listOf<Any>(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4)))
            for (col in 5..9) assertTrue("column $col of order 10 should be NULL", c.isNull(col))
            c.moveToNext()
            assertEquals(listOf<Any>(11L, "7", "OPEN", 2000L, 500L),
                listOf<Any>(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4)))
            for (col in 5..9) assertTrue("column $col of order 11 should be NULL", c.isNull(col))
        }
        db.query("SELECT id, orderId, menuItemId, menuItemName, menuItemNameAr, menuItemNameFr, quantity, unitPriceCentimes FROM order_items").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals(listOf<Any>(100L, 10L, 1L, "Mint tea", "أتاي", "Thé", 2L, 850L),
                listOf<Any>(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), c.getString(4), c.getString(5), c.getLong(6), c.getLong(7)))
        }
        for (table in listOf("staff", "shifts", "shift_left_open", "owner_credentials")) {
            db.query("SELECT COUNT(*) FROM $table").use { c ->
                c.moveToFirst()
                assertEquals("$table should start empty", 0L, c.getLong(0))
            }
        }
    }

    @Test
    fun migrate3To4_addsStaffRoleFromIsOwner() {
        helper.createDatabase(DB_NAME, 3).apply {
            execSQL("INSERT INTO staff (id, name, isOwner, isActive, createdAt) VALUES (1, 'Amina', 1, 1, 1000)")
            execSQL("INSERT INTO staff (id, name, isOwner, isActive, createdAt) VALUES (2, 'Karim', 0, 1, 2000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)

        db.query("SELECT id, name, role, isOwner FROM staff ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToNext()
            assertEquals(listOf<Any>(1L, "Amina", "OWNER", 1L), listOf<Any>(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
            c.moveToNext()
            assertEquals(listOf<Any>(2L, "Karim", "WAITER", 0L), listOf<Any>(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
        }
    }

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
