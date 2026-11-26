package com.aysha.assistant

import android.app.Application
import com.aysha.assistant.memory.MemoryRepository
import com.aysha.assistant.phonecontrol.PhoneControlExecutor
import com.aysha.assistant.settings.SettingsRepository
import com.aysha.assistant.voice.VoiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AyshaApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var memoryRepository: MemoryRepository
        private set

    lateinit var phoneControlExecutor: PhoneControlExecutor
        private set

    lateinit var voiceController: VoiceController
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        memoryRepository = MemoryRepository(this)
        phoneControlExecutor = PhoneControlExecutor(this)
        voiceController = VoiceController(this)

        appScope.launch {
            memoryRepository.ensureBuiltInMacrosSeeded()
        }
    }
}
