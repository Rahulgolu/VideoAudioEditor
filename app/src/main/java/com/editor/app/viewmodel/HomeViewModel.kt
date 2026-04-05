package com.editor.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.editor.app.repository.ProjectRepository
import com.editor.app.repository.ProjectSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(context: Context) : ViewModel() {

    private val repository = ProjectRepository(context)

    private val _projects = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val projects: StateFlow<List<ProjectSummary>> = _projects.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { refresh() }

    /** Reload projects from DB — call on resume or after save. */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _projects.value = repository.getAllProjects()
            _isLoading.value = false
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            refresh()
        }
    }

    fun renameProject(id: String, newName: String) {
        viewModelScope.launch {
            repository.renameProject(id, newName)
            refresh()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HomeViewModel(context) as T
            }
    }
}