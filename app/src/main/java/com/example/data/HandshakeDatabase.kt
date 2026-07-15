package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "handshakes")
data class Handshake(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ssid: String,
    val bssid: String,
    val timestamp: Long = System.currentTimeMillis(),
    val encryption: String,
    val crackedKey: String? = null,
    val notes: String = ""
)

@Dao
interface HandshakeDao {
    @Query("SELECT * FROM handshakes ORDER BY timestamp DESC")
    fun getAllHandshakes(): Flow<List<Handshake>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHandshake(handshake: Handshake)

    @Query("DELETE FROM handshakes WHERE id = :id")
    suspend fun deleteHandshake(id: Int)

    @Query("UPDATE handshakes SET crackedKey = :key WHERE id = :id")
    suspend fun updateCrackedKey(id: Int, key: String)

    @Query("DELETE FROM handshakes")
    suspend fun clearAll()
}

@Database(entities = [Handshake::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun handshakeDao(): HandshakeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kali_handshakes_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
