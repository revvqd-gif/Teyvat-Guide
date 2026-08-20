package com.teyvatmap.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

sealed class UiState<out T> {
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    object Idle : UiState<Nothing>()
}

class MapViewModel(
    private val repository: MapRepository,
    private val cookieManager: CookieManager
) : androidx.lifecycle.ViewModel() {

    // UI State using sealed class
    private val _mapInfo = MutableStateFlow<UiState<MapInfoDetail>>(UiState.Idle)
    val mapInfo: Flow<UiState<MapInfoDetail>> = _mapInfo

    private val _labelTree = MutableStateFlow<UiState<List<LabelNode>>>(UiState.Idle)
    val labelTree: Flow<UiState<List<LabelNode>>> = _labelTree

    private val _areas = MutableStateFlow<UiState<List<MapArea>>>(UiState.Idle)
    val areas: Flow<UiState<List<MapArea>>> = _areas

    private val _points = MutableStateFlow<UiState<List<MapPoint>>>(UiState.Idle)
    val points: Flow<UiState<List<MapPoint>>> = _points

    private val _markedPoints = MutableStateFlow<UiState<Set<Int>>>(UiState.Idle)
    val markedPoints: Flow<UiState<Set<Int>>> = _markedPoints

    private val _selectedLabelIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedLabelIds: Flow<Set<Int>> = _selectedLabelIds.distinctUntilChanged()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: Flow<Boolean> = _isLoading.distinctUntilChanged()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: Flow<String?> = _errorMessage.distinctUntilChanged()

    private val _cookieStatus = MutableStateFlow<String>("Checking cookie...")
    val cookieStatus: Flow<String> = _cookieStatus.distinctUntilChanged()

    private val _hasValidCookie = MutableStateFlow<Boolean>(false)
    val hasValidCookie: Flow<Boolean> = _hasValidCookie.distinctUntilChanged()

    private val _showZones = MutableStateFlow<Boolean>(true)
    val showZones: Flow<Boolean> = _showZones.distinctUntilChanged()

    private val _onlyUncollected = MutableStateFlow<Boolean>(true)
    val onlyUncollected: Flow<Boolean> = _onlyUncollected.distinctUntilChanged()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        androidx.lifecycle.viewModelScope.launch {
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
                onFailure = { UiState.Error(it.message, it) }
            )
            _labelTree.value = labelsResult.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message, it) }
            )
            _areas.value = areasResult.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message, it) }
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
        androidx.lifecycle.viewModelScope.launch {
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
        androidx.lifecycle.viewModelScope.launch {
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
        androidx.lifecycle.viewModelScope.launch {
            cookieManager.clearCookie()
            _hasValidCookie.value = false
            _cookieStatus.value = "No cookie"
            _markedPoints.value = UiState.Success(emptySet())
        }
    }

    fun refreshMarks() {
        androidx.lifecycle.viewModelScope.launch {
            _markedPoints.value = UiState.Loading
            val result = repository.getMarkedPoints()
            _markedPoints.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message, it) }
            )
            if (result is Result.Success) {
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

        androidx.lifecycle.viewModelScope.launch {
            val labelIds = _selectedLabelIds.value.toList()
            val result = repository.getPoints(labelIds)
            _points.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message, it) }
            )
            _isLoading.value = false

            if (result is Result.Failure) {
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
        return points.value?.fold(
            onSuccess = { it.count { it.labelId == labelId } },
            onFailure = { 0 },
            onLoading = { 0 },
            onIdle = { 0 }
        ) ?: 0
    }

    // Get child labels for a category
    fun getChildLabels(categoryId: Int): List<LabelNode> {
        return labelTree.value?.fold(
            onSuccess = { it.firstOrNull { it.id == categoryId }?.children ?: emptyList() },
            onFailure = { emptyList() },
            onLoading = { emptyList() },
            onIdle = { emptyList() }
        ) ?: emptyList()
    }
}

// Extension functions for Result
inline fun <T> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R
): R = when (this) {
    is Result.Success -> onSuccess(value)
    is Result.Failure -> onFailure(exception)
}

inline fun <T> UiState<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (String, Throwable?) -> R,
    onLoading: () -> R,
    onIdle: () -> R
): R = when (this) {
    is UiState.Success -> onSuccess(data)
    is UiState.Error -> onFailure(message, throwable)
    UiState.Loading -> onLoading()
    UiState.Idle -> onIdle()
}