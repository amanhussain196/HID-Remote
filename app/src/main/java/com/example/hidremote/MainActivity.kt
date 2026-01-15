package com.example.hidremote

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var hidLink: HidLink
    private lateinit var tvStatus: TextView
    private lateinit var layoutMouse: LinearLayout
    private lateinit var layoutKeyboard: LinearLayout
    private lateinit var layoutDpad: GridLayout
    private lateinit var etInput: EditText

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            hidLink.init()
        } else {
            Toast.makeText(this, "Permissions required for Bluetooth HID", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hidLink = HidLink(this)
        tvStatus = findViewById(R.id.tvStatus)
        layoutMouse = findViewById(R.id.layoutMouse)
        layoutKeyboard = findViewById(R.id.layoutKeyboard)
        layoutDpad = findViewById(R.id.layoutDpad)
        etInput = findViewById(R.id.etInput)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        // Mode Switching
        findViewById<Button>(R.id.btnModeMouse).setOnClickListener { setMode(0) }
        findViewById<Button>(R.id.btnModeKeyboard).setOnClickListener { setMode(1) }
        findViewById<Button>(R.id.btnModeDpad).setOnClickListener { setMode(2) }

        // Pairing
        findViewById<Button>(R.id.btnPair).setOnClickListener {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        }

        // Mouse Listeners
        val touchpad = findViewById<TouchpadView>(R.id.touchpad)
        touchpad.listener = object : TouchpadView.MouseActionListener {
            override fun onMove(dx: Int, dy: Int) {
                hidLink.sendMouseReport(dx, dy, false, false)
            }
            override fun onLeftClick() {
                hidLink.sendMouseReport(0, 0, true, false) // Down
                hidLink.sendMouseReport(0, 0, false, false) // Up
            }
            override fun onRightClick() {
                hidLink.sendMouseReport(0, 0, false, true) // Down
                hidLink.sendMouseReport(0, 0, false, false) // Up
            }
        }

        // Keyboard Logic
        etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (count > before && s != null) {
                    val char = s[start + count - 1]
                    sendChar(char)
                } else if (count < before) {
                    // Backspace
                    hidLink.sendKeyboardReport(0, 0x2A) // Backspace DOWN
                    hidLink.sendKeyboardReport(0, 0)    // UP
                }
            }
            override fun afterTextChanged(s: Editable?) {
                 // Option: Clear to avoid long strings? 
                 // If we clear, we mess up backspace logic slightly unless we track nicely.
                 // For now, let it grow.
            }
        })
        
        findViewById<Button>(R.id.btnEnter).setOnClickListener {
             hidLink.sendKeyboardReport(0, 0x28) // Enter
             hidLink.sendKeyboardReport(0, 0)
        }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener {
             hidLink.sendKeyboardReport(0, 0x2A) // Backspace
             hidLink.sendKeyboardReport(0, 0)
        }

        // DPAD Logic
        setupDpadButton(R.id.btnUp, HidUtils.USAGE_DPAD_UP)
        setupDpadButton(R.id.btnDown, HidUtils.USAGE_DPAD_DOWN)
        setupDpadButton(R.id.btnLeft, HidUtils.USAGE_DPAD_LEFT)
        setupDpadButton(R.id.btnRight, HidUtils.USAGE_DPAD_RIGHT)
        setupDpadButton(R.id.btnOk, HidUtils.USAGE_DPAD_CENTER)
        setupDpadButton(R.id.btnBack, HidUtils.USAGE_BACK)
        setupDpadButton(R.id.btnHome, HidUtils.USAGE_HOME)

        // Hid Listener
        hidLink.listener = object : HidLink.HidStateListener {
            override fun onStateChanged(state: String) {
                runOnUiThread { tvStatus.text = "Status: $state" }
            }

            override fun onDeviceConnected(name: String) {
                runOnUiThread { tvStatus.text = "Connected to: $name" }
            }

            override fun onDeviceDisconnected() {
                runOnUiThread { tvStatus.text = "Status: Disconnected" }
            }
        }
    }

    private fun setupDpadButton(id: Int, usage: Short) {
        findViewById<Button>(id).setOnClickListener {
            hidLink.sendConsumerReport(usage, true) // Down
            hidLink.sendConsumerReport(usage, false) // Up
        }
    }

    private fun sendChar(c: Char) {
        val hidKey = KeyboardUtils.getHidKey(c)
        if (hidKey != null) {
            hidLink.sendKeyboardReport(hidKey.modifier, hidKey.keycode)
            hidLink.sendKeyboardReport(0, 0) // Key UP
        }
    }

    private fun setMode(mode: Int) {
        layoutMouse.visibility = if (mode == 0) View.VISIBLE else View.GONE
        layoutKeyboard.visibility = if (mode == 1) View.VISIBLE else View.GONE
        layoutDpad.visibility = if (mode == 2) View.VISIBLE else View.GONE
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missing = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            hidLink.init()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hidLink.close()
    }
}
