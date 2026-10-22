package com.example.restroomqueue

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity data class ClassRoom(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "1") val maxOut: Int = 1   // max students out at once (chosen by the user)
)

@Entity(
    foreignKeys = [ForeignKey(ClassRoom::class, ["id"], ["classId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("classId")]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val name: String,
    val queuedAt: Long? = null,   // not null = waiting in line (ordered by this time)
    val outSince: Long? = null    // not null = currently out of the room
)

// Privacy by design: a trip has NO student reference, only class + time.
@Entity data class Trip(@PrimaryKey(autoGenerate = true) val id: Long = 0, val classId: Long, val timestamp: Long)

@Dao interface AppDao {
    @Query("SELECT * FROM ClassRoom ORDER BY name") fun classes(): Flow<List<ClassRoom>>
    @Insert suspend fun insertClass(c: ClassRoom): Long
    @Delete suspend fun deleteClass(c: ClassRoom)
    @Query("UPDATE ClassRoom SET maxOut=:n WHERE id=:id") suspend fun setMaxOut(id: Long, n: Int)
    @Query("SELECT * FROM Student WHERE classId=:id ORDER BY name") fun students(id: Long): Flow<List<Student>>
    @Query("SELECT * FROM Student WHERE id=:id") suspend fun student(id: Long): Student?
    @Insert suspend fun insertStudents(s: List<Student>)
    @Update suspend fun update(s: Student)
    @Delete suspend fun deleteStudent(s: Student)
    @Insert suspend fun insertTrip(t: Trip)
    @Query("SELECT timestamp FROM Trip WHERE classId=:id") fun tripTimes(id: Long): Flow<List<Long>>

    // used by cloud backup / restore
    @Query("SELECT COUNT(*) FROM ClassRoom") suspend fun classCount(): Int
    @Query("SELECT * FROM ClassRoom") suspend fun allClasses(): List<ClassRoom>
    @Query("SELECT * FROM Student") suspend fun allStudents(): List<Student>
    @Query("SELECT * FROM Trip") suspend fun allTrips(): List<Trip>
    @Insert suspend fun insertClasses(c: List<ClassRoom>)
    @Insert suspend fun insertTrips(t: List<Trip>)
    @Query("DELETE FROM Trip") suspend fun clearTrips()
    @Query("DELETE FROM Student") suspend fun clearStudents()
    @Query("DELETE FROM ClassRoom") suspend fun clearClasses()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ClassRoom ADD COLUMN maxOut INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(entities = [ClassRoom::class, Student::class, Trip::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "restroom.db").addMigrations(MIGRATION_1_2).build().also { inst = it }
        }
    }
}
