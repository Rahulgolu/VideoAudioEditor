package com.editor.app.repository

import android.content.Context
import com.editor.app.data.*
import com.editor.app.data.db.AppDatabase
import com.editor.app.data.db.ProjectEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class ProjectRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).projectDao()
    private val gson = Gson()

    // ─── Save ─────────────────────────────────────────────────────────────────

    suspend fun saveProject(state: EditorUiState): String {
        val id = if (state.projectId.isBlank()) UUID.randomUUID().toString()
        else state.projectId
        val now = System.currentTimeMillis()

        val entity = ProjectEntity(
            id = id,
            name = state.projectName,
            createdAt = state.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now,
            durationMs = state.durationMs,
            tracksJson = gson.toJson(state.tracks),
            effectsJson = gson.toJson(state.effects),
            textOverlaysJson = gson.toJson(state.textOverlays),
            exportConfigJson = gson.toJson(state.exportConfig)
        )
        dao.insertProject(entity)
        return id
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    suspend fun loadProject(id: String): EditorUiState? {
        val entity = dao.getProjectById(id) ?: return null
        return entityToState(entity)
    }

    // ─── List all ─────────────────────────────────────────────────────────────

    suspend fun getAllProjects(): List<ProjectSummary> {
        return dao.getAllProjects().map { entity ->
            ProjectSummary(
                id = entity.id,
                name = entity.name,
                durationMs = entity.durationMs,
                updatedAt = entity.updatedAt,
                createdAt = entity.createdAt
            )
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    suspend fun deleteProject(id: String) {
        dao.deleteProjectById(id)
    }

    // ─── Rename ───────────────────────────────────────────────────────────────

    suspend fun renameProject(id: String, newName: String) {
        dao.renameProject(id, newName, System.currentTimeMillis())
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun entityToState(entity: ProjectEntity): EditorUiState {
        val trackType = object : TypeToken<List<TimelineTrack>>() {}.type
        val effectType = object : TypeToken<List<EditEffect>>() {}.type
        val overlayType = object : TypeToken<List<TextOverlay>>() {}.type

        return EditorUiState(
            projectId = entity.id,
            projectName = entity.name,
            createdAt = entity.createdAt,
            durationMs = entity.durationMs,
            tracks = runCatching {
                gson.fromJson<List<TimelineTrack>>(entity.tracksJson, trackType)
            }.getOrDefault(defaultTracks()),
            effects = runCatching {
                gson.fromJson<List<EditEffect>>(entity.effectsJson, effectType)
            }.getOrDefault(defaultEffects()),
            textOverlays = runCatching {
                gson.fromJson<List<TextOverlay>>(entity.textOverlaysJson, overlayType)
            }.getOrDefault(emptyList()),
            exportConfig = runCatching {
                gson.fromJson(entity.exportConfigJson, ExportConfig::class.java)
            }.getOrDefault(ExportConfig())
        )
    }
}

// Summary shown in Home screen project list
data class ProjectSummary(
    val id: String,
    val name: String,
    val durationMs: Long,
    val updatedAt: Long,
    val createdAt: Long
)