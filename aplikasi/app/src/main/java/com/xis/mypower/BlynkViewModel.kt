package com.xis.mypower

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class AppState(
    val voltage: Float = 0f,
    val current: Float = 0f,
    val powerP1: Float = 0f,
    val powerP2: Float = 0f,
    val powerP3: Float = 0f,
    val totalPower: Float = 0f,
    val isAutoMode: Boolean = true,
    val isOnline: Boolean = false,
    val relayP1On: Boolean = true,
    val relayP2On: Boolean = true,
    val relayP3On: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null,
    val historyPower: List<Float> = emptyList()
)

class BlynkViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("BlynkPrefs", Context.MODE_PRIVATE)
    
    var authToken = prefs.getString("auth_token", "") ?: ""
        private set
    var templateId = prefs.getString("template_id", "") ?: ""
        private set
    var templateName = prefs.getString("template_name", "") ?: ""
        private set

    private val baseUrl = "https://blynk.cloud/external/api"
    private val client = OkHttpClient()

    private val _uiState = MutableStateFlow(AppState())
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    init {
        startPolling()
    }

    fun hasValidToken(): Boolean {
        return authToken.isNotEmpty()
    }

    fun logout() {
        prefs.edit().clear().apply()
        authToken = ""
        templateId = ""
        templateName = ""
        _uiState.update { AppState() }
    }

    fun verifyAndSaveCredentials(token: String, tId: String, tName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (token.isBlank()) {
                withContext(Dispatchers.Main) { onResult(false, "Token tidak boleh kosong") }
                return@launch
            }
            val url = "$baseUrl/isHardwareConnected?token=$token"
            val request = Request.Builder().url(url).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        prefs.edit()
                            .putString("auth_token", token)
                            .putString("template_id", tId)
                            .putString("template_name", tName)
                            .apply()
                        authToken = token
                        templateId = tId
                        templateName = tName
                        withContext(Dispatchers.Main) { onResult(true, null) }
                    } else {
                        withContext(Dispatchers.Main) { onResult(false, "Token tidak valid (HTTP ${response.code})") }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Koneksi gagal: ${e.message}") }
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                fetchData()
                delay(5000) // Polling diubah menjadi 5 detik untuk menghemat kuota message
            }
        }
    }

    private fun isHardwareConnected(): Boolean {
        if (authToken.isBlank()) return false
        val url = "$baseUrl/isHardwareConnected?token=$authToken"
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                response.body?.string()?.toBoolean() ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun fetchPin(pin: String): String {
        if (authToken.isBlank()) return "0"
        val url = "$baseUrl/get?token=$authToken&$pin"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            return response.body?.string()?.replace("\"", "")?.replace("[", "")?.replace("]", "") ?: "0"
        }
    }

    private fun fetchData() {
        try {
            val online = isHardwareConnected()
            if (!online) {
                _uiState.update { it.copy(isOnline = false, isLoading = false) }
                return // Hentikan fetching data sensor jika ESP offline untuk menghemat kuota (10 messages saved/poll)
            }

            val v1 = fetchPin("v1").toFloatOrNull() ?: _uiState.value.voltage
            val v2 = fetchPin("v2").toFloatOrNull() ?: _uiState.value.current
            val v3 = fetchPin("v3").toFloatOrNull() ?: _uiState.value.powerP1
            val v4 = fetchPin("v4").toFloatOrNull() ?: _uiState.value.powerP2
            val v5 = fetchPin("v5").toFloatOrNull() ?: _uiState.value.powerP3
            val v6 = fetchPin("v6").toFloatOrNull() ?: _uiState.value.totalPower
            
            val v7 = fetchPin("v7") != "0"
            val v8 = fetchPin("v8") != "0"
            val v9 = fetchPin("v9") != "0"
            val v10 = fetchPin("v10") == "0" 

            _uiState.update {
                val newHistory = (it.historyPower + v6).takeLast(30)
                it.copy(
                    isOnline = true,
                    voltage = v1, current = v2, powerP1 = v3, powerP2 = v4, powerP3 = v5, totalPower = v6,
                    relayP1On = v7, relayP2On = v8, relayP3On = v9, isAutoMode = v10,
                    historyPower = newHistory,
                    isLoading = false, error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Gagal mengambil data dari server") }
        }
    }

    fun setRelay(pin: String, isOn: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Update local state terlebih dahulu agar UI terasa responsif (Optimistic Update)
                _uiState.update { 
                    when(pin) {
                        "v7" -> it.copy(relayP1On = isOn)
                        "v8" -> it.copy(relayP2On = isOn)
                        "v9" -> it.copy(relayP3On = isOn)
                        else -> it
                    }
                }
                
                val value = if (isOn) "1" else "0"
                val url = "$baseUrl/update?token=$authToken&$pin=$value"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun setMode(isAuto: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.update { it.copy(isAutoMode = isAuto) }
                
                val value = if (isAuto) "0" else "1"
                val url = "$baseUrl/update?token=$authToken&v10=$value"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
