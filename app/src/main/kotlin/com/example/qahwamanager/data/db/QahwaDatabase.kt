package com.example.qahwamanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OwnerCredentials
import com.example.qahwamanager.data.model.Shift
import com.example.qahwamanager.data.model.ShiftLeftOpen
import com.example.qahwamanager.data.model.Staff

@Database(
    entities = [
        MenuItem::class, Order::class, OrderItem::class, InventoryItem::class,
        Staff::class, Shift::class, ShiftLeftOpen::class, OwnerCredentials::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class QahwaDatabase : RoomDatabase() {
    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun staffDao(): StaffDao
    abstract fun shiftDao(): ShiftDao
    abstract fun ownerDao(): OwnerDao

    companion object {
        fun create(context: Context): QahwaDatabase {
            return Room.databaseBuilder(
                context,
                QahwaDatabase::class.java,
                "qahwa_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with typical Moroccan coffee shop items (prices in centimes)
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Café Cassé', 'كافيه كاسيه', 'Café Cassé', 'coffee', 800, 'Half coffee, half milk — Moroccan style', 1, 'seed-menu-1', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Café au Lait', 'قهوة بالحليب', 'Café au Lait', 'coffee', 1000, 'Coffee with steamed milk', 1, 'seed-menu-2', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Espresso', 'إسبريسو', 'Espresso', 'coffee', 700, 'Short, strong espresso shot', 1, 'seed-menu-3', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Café Noir', 'قهوة سوداء', 'Café Noir', 'coffee', 600, 'Black filter coffee', 1, 'seed-menu-4', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Atay Naa''naa', 'أتاي بالنعناع', 'Thé à la Menthe', 'tea', 800, 'Traditional Moroccan mint tea', 1, 'seed-menu-5', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Atay Chiba', 'أتاي بالشيبة', 'Thé à l''Absinthe', 'tea', 800, 'Moroccan tea with wormwood', 1, 'seed-menu-6', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Jus d''Orange', 'عصير البرتقال', 'Jus d''Orange', 'juice', 1200, 'Fresh-squeezed orange juice', 1, 'seed-menu-7', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Jus d''Avocat', 'عصير الأفوكادو', 'Jus d''Avocat', 'juice', 1800, 'Creamy avocado shake', 1, 'seed-menu-8', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Makroud', 'مقروض', 'Makroud', 'food', 500, 'Semolina and date pastry', 1, 'seed-menu-9', 1, 0)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, priceCentimes, description, isAvailable, remoteId, updatedAt, isDeleted) VALUES ('Msemen', 'مسمن', 'Msemen', 'food', 600, 'Moroccan layered flatbread', 1, 'seed-menu-10', 1, 0)")

                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold, remoteId, updatedAt, isDeleted) VALUES ('Coffee Beans', 5.0, 'kg', 1.0, 'seed-inv-1', 1, 0)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold, remoteId, updatedAt, isDeleted) VALUES ('Milk', 10.0, 'L', 2.0, 'seed-inv-2', 1, 0)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold, remoteId, updatedAt, isDeleted) VALUES ('Mint', 2.0, 'kg', 0.5, 'seed-inv-3', 1, 0)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold, remoteId, updatedAt, isDeleted) VALUES ('Green Tea', 1.0, 'kg', 0.3, 'seed-inv-4', 1, 0)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold, remoteId, updatedAt, isDeleted) VALUES ('Sugar', 8.0, 'kg', 1.0, 'seed-inv-5', 1, 0)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold, remoteId, updatedAt, isDeleted) VALUES ('Oranges', 20.0, 'pcs', 10.0, 'seed-inv-6', 1, 0)")
                    }
                })
                .build()
        }
    }
}
