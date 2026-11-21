package com.medicalrep.manager.data

import android.content.Context
import androidx.room.ColumnInfo
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
    val createdAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapUrl: String? = null
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
    val specialtyId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mapUrl: String? = null,
    @ColumnInfo(defaultValue = "''") val region: String = ""
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

@Entity(
    tableName = "center_attachments",
    foreignKeys = [ForeignKey(
        entity = MedicalCenterEntity::class,
        parentColumns = ["id"],
        childColumns = ["centerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("centerId")]
)
data class CenterAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val centerId: Long,
    val displayName: String,
    val mimeType: String,
    val localPath: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "doctor_visits",
    foreignKeys = [ForeignKey(entity = DoctorEntity::class, parentColumns = ["id"], childColumns = ["doctorId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("doctorId"), Index("visitedAt"), Index("nextFollowUpAt")]
)
data class DoctorVisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doctorId: Long,
    val visitedAt: Long,
    val outcome: String = "",
    val notes: String = "",
    val nextFollowUpAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weekly_plan_entries",
    foreignKeys = [
        ForeignKey(entity = DoctorEntity::class, parentColumns = ["id"], childColumns = ["doctorId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MedicalCenterEntity::class, parentColumns = ["id"], childColumns = ["centerId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [
        Index("weekStartDate"),
        Index("visitDate"),
        Index("doctorId"),
        Index("centerId"),
        Index(value = ["weekStartDate", "doctorId"], unique = true)
    ]
)
data class WeeklyPlanEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStartDate: String,
    val visitDate: String,
    val doctorId: Long,
    val centerId: Long?,
    val regionSnapshot: String,
    val centerNameSnapshot: String,
    val doctorNameSnapshot: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface DoctorVisitDao {
    @Query("SELECT * FROM doctor_visits ORDER BY visitedAt DESC")
    fun observeAll(): Flow<List<DoctorVisitEntity>>

    @Query("SELECT * FROM doctor_visits WHERE doctorId = :doctorId ORDER BY visitedAt DESC")
    fun observeForDoctor(doctorId: Long): Flow<List<DoctorVisitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: DoctorVisitEntity): Long

    @Update
    suspend fun update(visit: DoctorVisitEntity)

    @Delete
    suspend fun delete(visit: DoctorVisitEntity)
}

@Entity(
    tableName = "doctor_center_links",
    primaryKeys = ["doctorId", "centerId"],
    foreignKeys = [
        ForeignKey(entity = DoctorEntity::class, parentColumns = ["id"], childColumns = ["doctorId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = MedicalCenterEntity::class, parentColumns = ["id"], childColumns = ["centerId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("centerId")]
)
data class DoctorCenterLinkEntity(
    val doctorId: Long,
    val centerId: Long
)

@Entity(
    tableName = "doctor_availability",
    foreignKeys = [ForeignKey(
        entity = DoctorEntity::class,
        parentColumns = ["id"],
        childColumns = ["doctorId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = MedicalCenterEntity::class,
        parentColumns = ["id"],
        childColumns = ["centerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("doctorId"), Index(value = ["doctorId", "dayOfWeek"]), Index("centerId")]
)
data class DoctorAvailabilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doctorId: Long,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val slotGroupId: String = "",
    val centerId: Long? = null
)

@Dao
interface WeeklyPlanDao {
    @Query("SELECT * FROM weekly_plan_entries ORDER BY visitDate ASC, centerNameSnapshot COLLATE NOCASE ASC, doctorNameSnapshot COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<WeeklyPlanEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: WeeklyPlanEntryEntity): Long

    @Delete
    suspend fun delete(entry: WeeklyPlanEntryEntity)

    @Query("DELETE FROM weekly_plan_entries WHERE weekStartDate = :weekStartDate")
    suspend fun deleteWeek(weekStartDate: String)
}

@Dao
interface DoctorCenterLinkDao {
    @Query("SELECT * FROM doctor_center_links")
    fun observeAll(): Flow<List<DoctorCenterLinkEntity>>

    @Query("SELECT centerId FROM doctor_center_links WHERE doctorId = :doctorId")
    suspend fun getCenterIds(doctorId: Long): List<Long>

    @Query("DELETE FROM doctor_center_links WHERE doctorId = :doctorId")
    suspend fun deleteForDoctor(doctorId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<DoctorCenterLinkEntity>)
}

@Dao
interface DoctorAvailabilityDao {
    @Query("SELECT * FROM doctor_availability ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeAll(): Flow<List<DoctorAvailabilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(slot: DoctorAvailabilityEntity): Long

    @Update
    suspend fun update(slot: DoctorAvailabilityEntity)

    @Delete
    suspend fun delete(slot: DoctorAvailabilityEntity)
}

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
interface CenterAttachmentDao {
    @Query("SELECT * FROM center_attachments WHERE centerId = :centerId ORDER BY createdAt DESC")
    fun observeForCenter(centerId: Long): Flow<List<CenterAttachmentEntity>>

    @Query("SELECT * FROM center_attachments WHERE centerId = :centerId")
    suspend fun getForCenter(centerId: Long): List<CenterAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: CenterAttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: CenterAttachmentEntity)
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

    @Query("SELECT * FROM doctors")
    suspend fun getAllForImport(): List<DoctorEntity>
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
    entities = [DoctorEntity::class, MedicalCenterEntity::class, SpecialtyEntity::class, DoctorAttachmentEntity::class, CenterAttachmentEntity::class, DoctorAvailabilityEntity::class, DoctorVisitEntity::class, DoctorCenterLinkEntity::class, WeeklyPlanEntryEntity::class],
    version = 13,
    exportSchema = false
)
abstract class MedicalRepDatabase : RoomDatabase() {
    abstract fun doctorDao(): DoctorDao
    abstract fun medicalCenterDao(): MedicalCenterDao
    abstract fun specialtyDao(): SpecialtyDao
    abstract fun doctorAttachmentDao(): DoctorAttachmentDao
    abstract fun centerAttachmentDao(): CenterAttachmentDao
    abstract fun doctorCenterLinkDao(): DoctorCenterLinkDao
    abstract fun doctorAvailabilityDao(): DoctorAvailabilityDao
    abstract fun doctorVisitDao(): DoctorVisitDao
    abstract fun weeklyPlanDao(): WeeklyPlanDao

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



        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS doctor_availability (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        doctorId INTEGER NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(doctorId) REFERENCES doctors(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_availability_doctorId ON doctor_availability(doctorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_availability_doctorId_dayOfWeek ON doctor_availability(doctorId, dayOfWeek)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS doctor_visits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        doctorId INTEGER NOT NULL,
                        visitedAt INTEGER NOT NULL,
                        outcome TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        nextFollowUpAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(doctorId) REFERENCES doctors(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_visits_doctorId ON doctor_visits(doctorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_visits_visitedAt ON doctor_visits(visitedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_visits_nextFollowUpAt ON doctor_visits(nextFollowUpAt)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE doctor_availability ADD COLUMN slotGroupId TEXT NOT NULL DEFAULT ''")
                // Preserve the grouping used by previous app versions for existing records.
                db.execSQL("""
                    UPDATE doctor_availability
                    SET slotGroupId = 'legacy-' || (
                        SELECT MIN(other.id)
                        FROM doctor_availability AS other
                        WHERE other.doctorId = doctor_availability.doctorId
                          AND other.startTime = doctor_availability.startTime
                          AND other.endTime = doctor_availability.endTime
                          AND other.notes = doctor_availability.notes
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS center_attachments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        centerId INTEGER NOT NULL,
                        displayName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        localPath TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(centerId) REFERENCES medical_centers(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_center_attachments_centerId ON center_attachments(centerId)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medical_centers ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE medical_centers ADD COLUMN longitude REAL")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE doctors ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE doctors ADD COLUMN longitude REAL")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medical_centers ADD COLUMN mapUrl TEXT")
                db.execSQL("ALTER TABLE doctors ADD COLUMN mapUrl TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS doctor_center_links (
                        doctorId INTEGER NOT NULL,
                        centerId INTEGER NOT NULL,
                        PRIMARY KEY(doctorId, centerId),
                        FOREIGN KEY(doctorId) REFERENCES doctors(id) ON DELETE CASCADE,
                        FOREIGN KEY(centerId) REFERENCES medical_centers(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_center_links_centerId ON doctor_center_links(centerId)")
                db.execSQL("""
                    INSERT OR IGNORE INTO doctor_center_links (doctorId, centerId)
                    SELECT id, centerId FROM doctors WHERE centerId IS NOT NULL
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE doctor_availability_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        doctorId INTEGER NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startTime TEXT NOT NULL,
                        endTime TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        slotGroupId TEXT NOT NULL DEFAULT '',
                        centerId INTEGER,
                        FOREIGN KEY(doctorId) REFERENCES doctors(id) ON DELETE CASCADE,
                        FOREIGN KEY(centerId) REFERENCES medical_centers(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO doctor_availability_new
                    (id, doctorId, dayOfWeek, startTime, endTime, notes, createdAt, slotGroupId, centerId)
                    SELECT a.id, a.doctorId, a.dayOfWeek, a.startTime, a.endTime, a.notes, a.createdAt, a.slotGroupId, d.centerId
                    FROM doctor_availability a
                    LEFT JOIN doctors d ON d.id = a.doctorId
                """.trimIndent())
                db.execSQL("DROP TABLE doctor_availability")
                db.execSQL("ALTER TABLE doctor_availability_new RENAME TO doctor_availability")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_availability_doctorId ON doctor_availability(doctorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_availability_doctorId_dayOfWeek ON doctor_availability(doctorId, dayOfWeek)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_doctor_availability_centerId ON doctor_availability(centerId)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE doctors ADD COLUMN region TEXT NOT NULL DEFAULT ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weekly_plan_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        weekStartDate TEXT NOT NULL,
                        visitDate TEXT NOT NULL,
                        doctorId INTEGER NOT NULL,
                        centerId INTEGER,
                        regionSnapshot TEXT NOT NULL,
                        centerNameSnapshot TEXT NOT NULL,
                        doctorNameSnapshot TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(doctorId) REFERENCES doctors(id) ON DELETE CASCADE,
                        FOREIGN KEY(centerId) REFERENCES medical_centers(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_plan_entries_weekStartDate ON weekly_plan_entries(weekStartDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_plan_entries_visitDate ON weekly_plan_entries(visitDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_plan_entries_doctorId ON weekly_plan_entries(doctorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_plan_entries_centerId ON weekly_plan_entries(centerId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_weekly_plan_entries_weekStartDate_doctorId ON weekly_plan_entries(weekStartDate, doctorId)")
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        fun getInstance(context: Context): MedicalRepDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicalRepDatabase::class.java,
                    "medical_rep_manager.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                    .build().also { INSTANCE = it }
            }
    }
}
