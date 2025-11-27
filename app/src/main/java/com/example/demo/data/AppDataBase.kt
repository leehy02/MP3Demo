package com.example.demo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FileEntity::class],
    version = 2,                 // ← 엔티티 바뀌었으니 버전 올린 상태 유지
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "demo_ble.db"
                )
                    // ✅ 개발 단계: 스키마 바뀌면 기존 DB 드롭 후 재생성 (크래시 방지)
                    .fallbackToDestructiveMigration()
                    // (옵션) 여러 프로세스/인스턴스에서 변경 알림 동기화
                    //.enableMultiInstanceInvalidation()
                    .build().also { INSTANCE = it }
            }
    }
}
