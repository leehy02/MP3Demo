package com.example.demo.data

// FileEntity.kt
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val fileNumber: Int,                 // ✅ 고유키
    val fileName: String,
    val savedAt: Long = System.currentTimeMillis()
)
