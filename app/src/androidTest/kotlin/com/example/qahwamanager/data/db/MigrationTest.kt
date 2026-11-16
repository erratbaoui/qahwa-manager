package com.example.qahwamanager.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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

    private companion object {
        const val DB_NAME = "migration-test"
    }
}
