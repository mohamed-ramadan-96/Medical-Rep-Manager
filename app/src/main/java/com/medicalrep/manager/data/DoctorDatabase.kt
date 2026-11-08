package com.medicalrep.manager.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val specialty: String = "",
    val phoneNumbers: String = "",
    val medicalCenter: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DoctorDao {
    @Query("SELECT * FROM doctors ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<DoctorEntity>>

    @Query("""
        SELECT * FROM doctors
        WHERE name LIKE '%' || :query || '%'
           OR specialty LIKE '%' || :query || '%'
           OR phoneNumbers LIKE '%' || :query || '%'
           OR medicalCenter LIKE '%' || :query || '%'
        ORDER BY name COLLATE NOCASE ASC
    """)
    fun search(query: String): Flow<List<DoctorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doctor: DoctorEntity): Long

    @Update
    suspend fun update(doctor: DoctorEntity)

    @Delete
    suspend fun delete(doctor: DoctorEntity)

    @Query("SELECT COUNT(*) FROM doctors")
    fun observeCount(): Flow<Int>
}

@Database(entities = [DoctorEntity::class], version = 1, exportSchema = false)
abstract class MedicalRepDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao

    companion object {
        @Volatile private var INSTANCE: MedicalRepDatabase? = null

        fun getInstance(context: Context): MedicalRepDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicalRepDatabase::class.java,
                    "medical_rep_manager.db"
                ).build().also { INSTANCE = it }
            }
    }
}
