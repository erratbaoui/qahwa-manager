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

@Database(
    entities = [MenuItem::class, Order::class, OrderItem::class, InventoryItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QahwaDatabase : RoomDatabase() {
    abstract fun menuItemDao(): MenuItemDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun inventoryDao(): InventoryDao

    companion object {
        fun create(context: Context): QahwaDatabase {
            return Room.databaseBuilder(
                context,
                QahwaDatabase::class.java,
                "qahwa_database"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with typical Moroccan coffee shop items
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Café Cassé', 'كافيه كاسيه', 'Café Cassé', 'coffee', 8.0, 'Half coffee, half milk — Moroccan style', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Café au Lait', 'قهوة بالحليب', 'Café au Lait', 'coffee', 10.0, 'Coffee with steamed milk', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Espresso', 'إسبريسو', 'Espresso', 'coffee', 7.0, 'Short, strong espresso shot', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Café Noir', 'قهوة سوداء', 'Café Noir', 'coffee', 6.0, 'Black filter coffee', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Atay Naa''naa', 'أتاي بالنعناع', 'Thé à la Menthe', 'tea', 8.0, 'Traditional Moroccan mint tea', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Atay Chiba', 'أتاي بالشيبة', 'Thé à l''Absinthe', 'tea', 8.0, 'Moroccan tea with wormwood', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Jus d''Orange', 'عصير البرتقال', 'Jus d''Orange', 'juice', 12.0, 'Fresh-squeezed orange juice', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Jus d''Avocat', 'عصير الأفوكادو', 'Jus d''Avocat', 'juice', 18.0, 'Creamy avocado shake', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Makroud', 'مقروض', 'Makroud', 'food', 5.0, 'Semolina and date pastry', 1)")
                        db.execSQL("INSERT INTO menu_items (name, nameAr, nameFr, category, price, description, isAvailable) VALUES ('Msemen', 'مسمن', 'Msemen', 'food', 6.0, 'Moroccan layered flatbread', 1)")

                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold) VALUES ('Coffee Beans', 5.0, 'kg', 1.0)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold) VALUES ('Milk', 10.0, 'L', 2.0)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold) VALUES ('Mint', 2.0, 'kg', 0.5)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold) VALUES ('Green Tea', 1.0, 'kg', 0.3)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold) VALUES ('Sugar', 8.0, 'kg', 1.0)")
                        db.execSQL("INSERT INTO inventory_items (name, quantity, unit, lowStockThreshold) VALUES ('Oranges', 20.0, 'pcs', 10.0)")
                    }
                })
                .build()
        }
    }
}
