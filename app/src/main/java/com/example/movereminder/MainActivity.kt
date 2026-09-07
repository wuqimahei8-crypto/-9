package com.example.movereminder

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.movereminder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var startHour = 9
    private var startMinute = 0
    private var endHour = 18
    private var endMinute = 0

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                binding.tvStatus.text = "未授予通知权限，提醒可能无法显示"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = Prefs.load(this)
        startHour = prefs.startHour
        startMinute = prefs.startMinute
        endHour = prefs.endHour
        endMinute = prefs.endMinute
        binding.etInterval.setText(prefs.intervalMinutes.toString())
        binding.switchEnabled.isChecked = prefs.enabled

        updateTimeButtons()
        updateStatus(prefs.enabled)

        binding.btnStartTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                startHour = h; startMinute = m; updateTimeButtons()
            }, startHour, startMinute, true).show()
        }

        binding.btnEndTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                endHour = h; endMinute = m; updateTimeButtons()
            }, endHour, endMinute, true).show()
        }

        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ensurePermissions()
                val interval = binding.etInterval.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 40
                val newPrefs = Prefs(true, startHour, startMinute, endHour, endMinute, interval)
                Prefs.save(this, newPrefs)
                ReminderScheduler.scheduleNext(this)
                updateStatus(true)
            } else {
                val newPrefs = Prefs.load(this).copy(enabled = false)
                Prefs.save(this, newPrefs)
                ReminderScheduler.cancel(this)
                updateStatus(false)
            }
        }

        binding.btnTest.setOnClickListener {
            sendBroadcast(Intent(this, ReminderReceiver::class.java))
        }
    }

    private fun updateTimeButtons() {
        binding.btnStartTime.text = "开始时间: %02d:%02d".format(startHour, startMinute)
        binding.btnEndTime.text = "结束时间: %02d:%02d".format(endHour, endMinute)
    }

    private fun updateStatus(enabled: Boolean) {
        binding.tvStatus.text = if (enabled) "提醒已开启" else "提醒已关闭"
    }

    private fun ensurePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}
