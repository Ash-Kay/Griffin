package com.ashishkumars.griffin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ashishkumars.griffin.databinding.ActivityMainBinding
import com.ashishkumars.griffin.utils.SettingsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        settingsManager = SettingsManager(this)

        checkOverlayPermission()
        checkPhoneStatePermission()
        startForegroundService()
        setupDebugSwitch()
        setupTimePicker()

        binding.btnOverlayGrantPermission.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionResultLauncher.launch(intent)
        }

        binding.btnPhoneGrantPermission.setOnClickListener {
            phonePermissionResultLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
    }

    private fun setupTimePicker() {
        with(binding.timePicker) {
            minValue = 1
            maxValue = 2 * 60
            val currentDelayInSec = settingsManager.getDelay()

            if (currentDelayInSec == 0) {
                value = 10
                settingsManager.setDelay(10)
            } else {
                value = currentDelayInSec
            }

            setOnValueChangedListener { _, _, newVal ->
                settingsManager.setDelay(newVal)
            }
        }
    }

    private fun setupDebugSwitch() {
        if (BuildConfig.DEBUG) {
            val currentMode = settingsManager.getIsDebugMode()
            binding.switchDebugMode.isChecked = currentMode
            binding.switchDebugMode.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setIsDebugMode(isChecked)
            }
        } else {
            binding.sectionDebugSwitch.visibility = View.GONE
        }
    }

    private fun startForegroundService() {
        val foregroundServiceIntent = Intent(this, ForegroundService::class.java)
        ContextCompat.startForegroundService(this, foregroundServiceIntent)
    }

    private var phonePermissionResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                checkPhoneStatePermission()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_PHONE_STATE
                    )
                ) {
                    Toast.makeText(this, "Phone permission Not Given!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Allow Phone permission form settings!", Toast.LENGTH_LONG)
                        .show()

                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.parse("package:$packageName")
                    intent.data = uri
                    startActivity(intent)
                }
            }
        }

    private var overlayPermissionResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private fun checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            binding.sectionOverlayPermission.visibility = View.GONE
        } else {
            binding.sectionOverlayPermission.visibility = View.VISIBLE
        }
    }

    private fun checkPhoneStatePermission() {
        if (applicationContext.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            binding.sectionPhonePermission.visibility = View.VISIBLE
        } else {
            binding.sectionPhonePermission.visibility = View.GONE
        }
    }
}