package com.tdc.subvectio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tdc.subvectio.entities.Delivery
import com.tdc.subvectio.util.Converter

@Database(entities = [Delivery::class], version = 1, exportSchema = false)
@TypeConverters(Converter::class)
public abstract class SubvectioDatabase : RoomDatabase() {

    abstract fun deliveryDao(): DeliveryDao

    companion object {
        @Volatile
        private var INSTANCE: SubvectioDatabase? = null

        fun getDatabase(context: Context): SubvectioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SubvectioDatabase::class.java,
                    "subvectio_database.db")
                .allowMainThreadQueries()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}