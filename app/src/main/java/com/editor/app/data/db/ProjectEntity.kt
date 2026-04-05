package com.editor.app.data.db

import androidx.room.*

// ─── Entity ───────────────────────────────────────────────────────────────────

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val durationMs: Long,
    val tracksJson: String,        // serialized List<TimelineTrack>
    val effectsJson: String,       // serialized List<EditEffect>
    val textOverlaysJson: String,  // serialized List<TextOverlay>
    val exportConfigJson: String,  // serialized ExportConfig
    val thumbnailUri: String? = null
)

// ─── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("UPDATE projects SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameProject(id: String, name: String, updatedAt: Long)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vibe_edit_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}