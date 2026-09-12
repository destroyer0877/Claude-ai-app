package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("au_notes_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_API_KEY = "YOUR_API_KEY_HERE"
        const val DEFAULT_PIN = "1234"
        const val DEFAULT_MODEL = "gemini-2.0-flash"
        const val DEFAULT_SECURITY_QUESTION = "Who is the best person in your life?"
    }

    private val _blurApis = MutableStateFlow(prefs.getBoolean("blur_apis", true))
    val blurApis: StateFlow<Boolean> = _blurApis.asStateFlow()

    private val _syntaxHighlight = MutableStateFlow(prefs.getBoolean("syntax_highlight", true))
    val syntaxHighlight: StateFlow<Boolean> = _syntaxHighlight.asStateFlow()

    private val _lockPin = MutableStateFlow(prefs.getString("lock_pin", DEFAULT_PIN) ?: DEFAULT_PIN)
    val lockPin: StateFlow<String> = _lockPin.asStateFlow()

    private val _hasCustomPin = MutableStateFlow(prefs.getBoolean("has_custom_pin", false))
    val hasCustomPin: StateFlow<Boolean> = _hasCustomPin.asStateFlow()

    private val _securityQuestion = MutableStateFlow(prefs.getString("sec_question", DEFAULT_SECURITY_QUESTION) ?: DEFAULT_SECURITY_QUESTION)
    val securityQuestion: StateFlow<String> = _securityQuestion.asStateFlow()

    private val _securityAnswer = MutableStateFlow(prefs.getString("sec_answer", "") ?: "")
    val securityAnswer: StateFlow<String> = _securityAnswer.asStateFlow()

    private val _lockedFolders = MutableStateFlow(prefs.getStringSet("locked_folders", emptySet()) ?: emptySet())
    val lockedFolders: StateFlow<Set<String>> = _lockedFolders.asStateFlow()

    private val defaultFolders = listOf("All Notes", "Favorites", "APIs Keys", "Code", "Media", "Personal")
    private val _folderOrder = MutableStateFlow(
        prefs.getString("folder_order", null)?.split(",")?.filter { it.isNotBlank() } ?: defaultFolders
    )
    val folderOrder: StateFlow<List<String>> = _folderOrder.asStateFlow()

    private val _useInbuiltApi = MutableStateFlow(prefs.getBoolean("use_inbuilt_api", true))
    val useInbuiltApi: StateFlow<Boolean> = _useInbuiltApi.asStateFlow()

    private val _userApiKey = MutableStateFlow(prefs.getString("user_api_key", "") ?: "")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.getString("selected_model", DEFAULT_MODEL) ?: DEFAULT_MODEL)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setBlurApis(value: Boolean) {
        prefs.edit().putBoolean("blur_apis", value).apply()
        _blurApis.value = value
    }

    fun setSyntaxHighlight(value: Boolean) {
        prefs.edit().putBoolean("syntax_highlight", value).apply()
        _syntaxHighlight.value = value
    }

    fun setLockPin(pin: String) {
        prefs.edit().putString("lock_pin", pin).putBoolean("has_custom_pin", true).apply()
        _lockPin.value = pin
        _hasCustomPin.value = true
    }

    fun setSecurityDetails(pin: String, question: String, answer: String) {
        prefs.edit()
            .putString("lock_pin", pin)
            .putBoolean("has_custom_pin", true)
            .putString("sec_question", question)
            .putString("sec_answer", answer.trim().lowercase())
            .apply()
        _lockPin.value = pin
        _hasCustomPin.value = true
        _securityQuestion.value = question
        _securityAnswer.value = answer.trim().lowercase()
    }

    fun verifySecurityAnswer(enteredAnswer: String): Boolean {
        val stored = _securityAnswer.value.trim().lowercase()
        return stored.isNotEmpty() && stored == enteredAnswer.trim().lowercase()
    }

    fun toggleFolderLock(folderName: String) {
        val current = _lockedFolders.value.toMutableSet()
        if (current.contains(folderName)) {
            current.remove(folderName)
        } else {
            current.add(folderName)
        }
        prefs.edit().putStringSet("locked_folders", current).apply()
        _lockedFolders.value = current
    }

    fun isFolderLocked(folderName: String): Boolean {
        return _lockedFolders.value.contains(folderName)
    }

    fun setFolderOrder(newOrder: List<String>) {
        val joined = newOrder.joinToString(",")
        prefs.edit().putString("folder_order", joined).apply()
        _folderOrder.value = newOrder
    }

    fun removeFolder(folderName: String) {
        val current = _folderOrder.value.toMutableList()
        current.remove(folderName)
        setFolderOrder(current)
    }

    fun setUseInbuiltApi(value: Boolean) {
        prefs.edit().putBoolean("use_inbuilt_api", value).apply()
        _useInbuiltApi.value = value
    }

    fun setUserApiKey(key: String) {
        prefs.edit().putString("user_api_key", key).apply()
        _userApiKey.value = key
    }

    fun setSelectedModel(model: String) {
        prefs.edit().putString("selected_model", model).apply()
        _selectedModel.value = model
    }

    fun setDarkMode(value: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", value).apply()
        _isDarkMode.value = value
    }

    fun getEffectiveApiKey(): String {
        return if (_useInbuiltApi.value) {
            DEFAULT_API_KEY
        } else {
            _userApiKey.value.ifBlank { DEFAULT_API_KEY }
        }
    }
}
