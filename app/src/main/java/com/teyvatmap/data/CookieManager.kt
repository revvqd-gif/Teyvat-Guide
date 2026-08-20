package com.teyvatmap.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesKeys
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.rx.preferencesDataStore
import com.teyvatmap.CookieParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import io.reactivex.rxjava3.core.Completable

class CookieManager(private val context: Context) {

    private val Context.dataStore by preferencesDataStore("cookie_prefs")

    private val COOKIE_KEY = preferencesKey<String>("hoyolab_cookie")
    private val LAST_SYNC_KEY = preferencesKey<Long>("last_sync_time")

    private val _cookieFlow = MutableStateFlow<String>("")
    val cookieFlow = _cookieFlow.asStateFlow()

    private val _hasCookieFlow = MutableStateFlow<Boolean>(false)
    val hasCookieFlow = _hasCookieFlow.asStateFlow()

    init {
        loadCookie()
    }

    private fun loadCookie() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val cookie = context.dataStore.data
                .map { it[COOKIE_KEY] ?: "" }
                .firstOrNull() ?: ""
            _cookieFlow.value = cookie
            _hasCookieFlow.value = cookie.isNotBlank() && CookieParser.hasValidTokens(cookie)
        }
    }

    suspend fun saveCookie(cookie: String) {
        context.dataStore.edit { it[COOKIE_KEY] = cookie }
        _cookieFlow.value = cookie
        _hasCookieFlow.value = cookie.isNotBlank() && CookieParser.hasValidTokens(cookie)
    }

    suspend fun clearCookie() {
        context.dataStore.edit { it.remove(COOKIE_KEY) }
        _cookieFlow.value = ""
        _hasCookieFlow.value = false
    }

    fun getCookieSync(): String {
        return _cookieFlow.value
    }

    fun hasCookieSync(): Boolean {
        return _hasCookieFlow.value
    }

    suspend fun updateLastSync() {
        context.dataStore.edit { it[LAST_SYNC_KEY] = System.currentTimeMillis() }
    }

    suspend fun getLastSyncTime(): Long {
        return context.dataStore.data
            .map { it[LAST_SYNC_KEY] ?: 0L }
            .firstOrNull() ?: 0L
    }
}