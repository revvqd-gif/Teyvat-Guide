package com.teyvatmap.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.teyvatmap.data.db.MapEntities.*

@Database(
    entities = [
        LabelEntity::class,
        AreaEntity::class,
        PointEntity::class,
        MarkedPointEntity::class,
        MapInfoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TeyvatMapDatabase : RoomDatabase() {

    abstract fun labelDao(): LabelDao
    abstract fun areaDao(): AreaDao
    abstract fun pointDao(): PointDao
    abstract fun markedPointDao(): MarkedPointDao
    abstract fun mapInfoDao(): MapInfoDao

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @Volatile
        private var INSTANCE: TeyvatMapDatabase? = null

        fun getInstance(context: Context): TeyvatMapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeyvatMapDatabase::class.java,
                    "teyvat_map_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}