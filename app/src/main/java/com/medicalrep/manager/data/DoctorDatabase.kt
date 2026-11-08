package com.medicalrep.manager.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "medical_centers")
data class MedicalCenterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "specialties")
data class SpecialtyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "doctors", indices = [Index("centerId"), Index("specialtyId")])
data class DoctorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val specialty: String = "",
    val phoneNumbers: String = "",
    // Kept for compatibility with records created by app version 0.1.0.
    val medicalCenter: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val centerId: Long? = null,
    val specialtyId: Long? = null
)


@Entity(
    tableName = "doctor_attachments",
    foreignKeys = [ForeignKey(
        entity = DoctorEntity::class,
        parentColumns = ["id"],
        childColumns = ["doctorId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("doctorId")]
)
data class DoctorAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doctorId: Long,
    val displayName: String,
    val mimeType: String,
    val localPath: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DoctorAttachmentDao {
    @Query("SELECT * FROM doctor_attachments WHERE doctorId = :doctorId ORDER BY createdAt DESC")
    fun observeForDoctor(doctorId: Long): Flow<List<DoctorAttachmentEntity>>

    @Query("SELECT * FROM doctor_attachments WHERE doctorId = :doctorId")
    suspend fun getForDoctor(doctorId: Long): List<DoctorAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: DoctorAttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: DoctorAttachmentEntity)
}

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

    @Query("UPDATE doctors SET centerId = NULL WHERE centerId = :centerId")
    suspend fun detachCenter(centerId: Long)

    @Query("UPDATE doctors SET specialtyId = NULL WHERE specialtyId = :specialtyId")
    suspend fun detachSpecialty(specialtyId: Long)

    @Query("SELECT COUNT(*) FROM doctors")
    fun observeCount(): Flow<Int>
}

@Dao
interface MedicalCenterDao {
    @Query("SELECT * FROM medical_centers ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MedicalCenterEntity>>

    @Query("SELECT COUNT(*) FROM doctors WHERE centerId = :centerId")
    suspend fun doctorCount(centerId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(center: MedicalCenterEntity): Long

    @Update
    suspend fun update(center: MedicalCenterEntity)

    @Delete
    suspend fun delete(center: MedicalCenterEntity)
}


@Dao
interface SpecialtyDao {
    @Query("SELECT * FROM specialties ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SpecialtyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(specialty: SpecialtyEntity): Long

    @Update
    suspend fun update(specialty: SpecialtyEntity)

    @Delete
    suspend fun delete(specialty: SpecialtyEntity)

    @Query("UPDATE doctors SET specialtyId = NULL WHERE specialtyId = :specialtyId")
    suspend fun detachDoctors(specialtyId: Long)
}

@Database(
    entities = [DoctorEntity::class, MedicalCenterEntity::class, SpecialtyEntity::class, DoctorAttachmentEntity::class],
    version = 4,
    exportSchema = false
)
abstract class MedicalRepDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun medicalCenterDao(): MedicalCenterDao
    abstract fun specialtyDao(): SpecialtyDao
    abstract fun doctorAttachmentDao(): DoctorAttachmentDao

    companion object {
        @Volatile private var INSTANCE: MedicalRepDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS medical_centers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        address TEXT NOT NULL DEFAULT '',
                        phone TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE doctors ADD COLUMN centerId INTEGER")
                // Preserve existing free-text center names by converting each unique value.
                db.execSQL("""
                    INSERT INTO medical_centers (name, address, phone, notes, createdAt)
                    SELECT DISTINCT TRIM(medicalCenter), '', '', '', CAST(strftime('%s','now') AS INTEGER) * 1000
                    FROM doctors
                    WHERE TRIM(medicalCenter) != ''
                """.trimIndent())
                db.execSQL("""
                    UPDATE doctors
                    SET centerId = (
                        SELECT medical_centers.id FROM medical_centers
                        WHERE medical_centers.name = TRIM(doctors.medicalCenter)
                        LIMIT 1
                    )
                    WHERE TRIM(medicalCenter) != ''
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctors_centerId ON doctors(centerId)")
            }
        }


        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS specialties (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("ALTER TABLE doctors ADD COLUMN specialtyId INTEGER")
                db.execSQL("""
                    INSERT INTO specialties (name, createdAt)
                    SELECT DISTINCT TRIM(specialty), CAST(strftime('%s','now') AS INTEGER) * 1000
                    FROM doctors
                    WHERE TRIM(specialty) != ''
                """.trimIndent())
                db.execSQL("""
                    UPDATE doctors
                    SET specialtyId = (
                        SELECT specialties.id FROM specialties
                        WHERE specialties.name = TRIM(doctors.specialty)
                        LIMIT 1
                    )
                    WHERE TRIM(specialty) != ''
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctors_specialtyId ON doctors(specialtyId)")
            }
        }



        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS doctor_attachments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        doctorId INTEGER NOT NULL,
                        displayName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        localPath TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(doctorId) REFERENCES doctors(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_attachments_doctorId ON doctor_attachments(doctorId)")
            }
        }

        fun getInstance(context: Context): MedicalRepDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicalRepDatabase::class.java,
                    "medical_rep_manager.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }
    }
}
