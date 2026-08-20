package com.teyvatmap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teyvatmap.data.CookieManager
import com.teyvatmap.data.MapRepository
import com.teyvatmap.data.CookieParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    object Idle : UiState<Nothing>()
}

class MapViewModel(
    private val repository: MapRepository,
    private val cookieManager: CookieManager
) : ViewModel() {

    // UI State using sealed class
    private val _mapInfo = MutableStateFlow<UiState<com.teyvatmap.data.MapInfoDetail>>(UiState.Idle)
    val mapInfo = _mapInfo

    private val _labelTree = MutableStateFlow<UiState<List<com.teyvatmap.data.LabelNode>>>(UiState.Idle)
    val labelTree = _labelTree

    private val _areas = MutableStateFlow<UiState<List<com.teyvatmap.data.MapArea>>>(UiState.Idle)
    val areas = _areas

    private val _points = MutableStateFlow<UiState<List<com.teyvatmap.data.MapPoint>>>(UiState.Idle)
    val points = _points

    private val _markedPoints = MutableStateFlow<UiState<Set<Int>>>(UiState.Idle)
    val markedPoints = _markedPoints

    private val _selectedLabelIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedLabelIds = _selectedLabelIds

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage

    private val _cookieStatus = MutableStateFlow<String>("Checking cookie...")
    val cookieStatus = _cookieStatus

    private val _hasValidCookie = MutableStateFlow<Boolean>(false)
    val hasValidCookie = _hasValidCookie

    private val _showZones = MutableStateFlow<Boolean>(true)
    val showZones = _showZones

    private val _onlyUncollected = MutableStateFlow<Boolean>(true)
    val onlyUncollected = _onlyUncollected

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            loadStaticData()
        }
    }

    private suspend fun loadStaticData() {
        _isLoading.value = true
        _errorMessage.value = null

        try {
            // Load map info, labels, and areas in parallel
            val mapInfoResult = repository.getMapInfo()
            val labelsResult = repository.getLabelTree()
            val areasResult = repository.getAreas()

            _mapInfo.value = mapInfoResult.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { e -> UiState.Error(e.message ?: "Unknown error", e) }
            )
            _labelTree.value = labelsResult.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { e -> UiState.Error(e.message ?: "Unknown error", e) }
            )
            _areas.value = areasResult.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { e -> UiState.Error(e.message ?: "Unknown error", e) }
            )

            // Set default selected labels (all top-level categories)
            val labels = labelsResult.getOrNull() ?: emptyList()
            if (labels.isNotEmpty()) {
                _selectedLabelIds.value = labels.map { it.id }.toSet()
            }

            // Check for saved cookie
            checkCookie()
        } catch (e: Exception) {
            _errorMessage.value = e.message
        } finally {
            _isLoading.value = false
        }
    }

    fun checkCookie() {
        viewModelScope.launch {
            val cookie = cookieManager.getCookieSync()
            val hasCookie = cookie.isNotBlank() && CookieParser.hasValidTokens(cookie)

            _hasValidCookie.value = hasCookie
            _cookieStatus.value = if (hasCookie) "Cookie saved ✓" else "No cookie"

            if (hasCookie) {
                refreshMarks()
            }
        }
    }

    fun saveCookie(rawCookie: String) {
        viewModelScope.launch {
            val parsed = CookieParser.parseCookie(rawCookie)
            if (parsed.isBlank()) {
                _errorMessage.value = "Invalid cookie format"
                return@launch
            }

            if (!CookieParser.hasValidTokens(parsed)) {
                _errorMessage.value = "Invalid cookie: missing required tokens (ltoken_v2, cookie_token_v2)"
                return@launch
            }

            cookieManager.saveCookie(rawCookie)
            checkCookie()
            _errorMessage.value = null
        }
    }

    fun clearCookie() {
        viewModelScope.launch {
            cookieManager.clearCookie()
            _hasValidCookie.value = false
            _cookieStatus.value = "No cookie"
            _markedPoints.value = UiState.Success(emptySet())
        }
    }

    fun refreshMarks() {
        viewModelScope.launch {
            _markedPoints.value = UiState.Loading
            val result = repository.getMarkedPoints()
            _markedPoints.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { e -> UiState.Error(e.message ?: "Unknown error", e) }
            )
            if (result.isSuccess) {
                _cookieStatus.value = "Synced: ${result.getOrNull()?.size ?: 0} marks"
                refreshPoints()
            } else {
                _cookieStatus.value = "Sync failed"
            }
        }
    }

    fun refreshPoints() {
        if (_selectedLabelIds.value.isEmpty()) {
            _points.value = UiState.Success(emptyList())
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val labelIds = _selectedLabelIds.value.toList()
            val result = repository.getPoints(labelIds)
            _points.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { e -> UiState.Error(e.message ?: "Unknown error", e) }
            )
            _isLoading.value = false

            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load points"
            }
        }
    }

    fun toggleLabel(labelId: Int) {
        val current = _selectedLabelIds.value.toMutableSet()
        if (current.contains(labelId)) {
            current.remove(labelId)
        } else {
            current.add(labelId)
        }
        _selectedLabelIds.value = current
        refreshPoints()
    }

    fun selectCategory(categoryId: Int?) {
        // Handle category selection if needed
    }

    fun toggleZones() {
        _showZones.value = !_showZones.value
    }

    fun toggleUncollected() {
        _onlyUncollected.value = !_onlyUncollected.value
    }

    fun getLabelCount(labelId: Int): Int {
        return points.value?.let {
            when (it) {
                is UiState.Success -> it.data.count { it.labelId == labelId }
                else -> 0
            }
        } ?: 0
    }

    // Get child labels for a category
    fun getChildLabels(categoryId: Int): List<com.teyvatmap.data.LabelNode> {
        return labelTree.value?.let {
            when (it) {
                is UiState.Success -> it.data.firstOrNull { it.id == categoryId }?.children ?: emptyList()
                else -> emptyList()
            }
        } ?: emptyList()
    }
}