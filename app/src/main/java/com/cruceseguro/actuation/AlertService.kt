package com.cruceseguro.actuation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.cruceseguro.decision.ProximityLevel
import java.util.Locale

class AlertService(private val context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context, this)
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS &&
            tts.setLanguage(Locale.forLanguageTag("es-ES")) >= TextToSpeech.LANG_AVAILABLE
    }

    fun alert(level: ProximityLevel) {
        if (level !in ALERT_LEVELS) return
        runCatching { vibrate(level) }
        runCatching { speak(level) }
        if (level == ProximityLevel.CRITICAL) runCatching { flashBriefly() }
    }

    fun close() {
        handler.removeCallbacksAndMessages(null)
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
        runCatching { setTorch(false) }
    }

    private fun speak(level: ProximityLevel) {
        if (!ttsReady) return
        val message = when (level) {
            ProximityLevel.PREVENTIVE -> "Cruce peatonal próximo, continúe con precaución."
            ProximityLevel.NEAR -> "Se está acercando al cruce peatonal."
            ProximityLevel.CRITICAL -> "Atención. Cruce peatonal inmediato. Deténgase y verifique antes de cruzar."
            else -> return
        }
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "crossing-${level.name}")
    }

    @Suppress("DEPRECATION")
    private fun vibrate(level: ProximityLevel) {
        val (timings, amplitudes) = when (level) {
            ProximityLevel.PREVENTIVE -> longArrayOf(0, 180, 140, 180) to intArrayOf(0, 90, 0, 90)
            ProximityLevel.NEAR -> longArrayOf(0, 260, 120, 260, 120, 260) to intArrayOf(0, 170, 0, 170, 0, 170)
            ProximityLevel.CRITICAL -> longArrayOf(0, 500, 100, 500, 100, 700) to intArrayOf(0, 255, 0, 255, 0, 255)
            else -> return
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        if (vibrator.hasAmplitudeControl()) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }

    private fun flashBriefly() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        val cameraId = findFlashCamera() ?: return
        cameraManager.setTorchMode(cameraId, true)
        handler.postDelayed({ runCatching { cameraManager.setTorchMode(cameraId, false) } }, 1_200L)
    }

    private fun setTorch(enabled: Boolean) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        findFlashCamera()?.let { cameraManager.setTorchMode(it, enabled) }
    }

    private fun findFlashCamera(): String? = cameraManager.cameraIdList.firstOrNull { id ->
        cameraManager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }

    private companion object {
        val ALERT_LEVELS = setOf(ProximityLevel.PREVENTIVE, ProximityLevel.NEAR, ProximityLevel.CRITICAL)
    }
}
