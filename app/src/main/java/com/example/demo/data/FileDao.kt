package com.example.demo.data

// FileDao.kt
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Upsert
    suspend fun upsert(file: FileEntity)          // ✅ 단건 업서트

    @Upsert
    suspend fun upsertAll(files: List<FileEntity>)// ✅ 다건 업서트도 가능(옵션)

    @Query("SELECT * FROM files ORDER BY fileNumber ASC")
    fun getAll(): Flow<List<FileEntity>>          // ✅ UI 자동 갱신

    @Query("DELETE FROM files")
    suspend fun clear()

    @Query("DELETE FROM files WHERE fileNumber = :num")
    suspend fun deleteByNumber(num: Int)
}

