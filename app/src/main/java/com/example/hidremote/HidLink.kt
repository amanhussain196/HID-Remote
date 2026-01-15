package com.example.hidremote

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.util.concurrent.Executor

@SuppressLint("MissingPermission")
class HidLink(private val context: Context) {

    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val btAdapter = btManager.adapter

    interface HidStateListener {
        fun onStateChanged(state: String)
        fun onDeviceConnected(name: String)
        fun onDeviceDisconnected()
    }

    var listener: HidStateListener? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = proxy as BluetoothHidDevice
                registerApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = null
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.d("HidLink", "onAppStatusChanged: $registered")
            listener?.onStateChanged(if (registered) "Registered as HID" else "Registration Failed")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            Log.d("HidLink", "onConnectionStateChanged: $state")
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                listener?.onDeviceConnected(device?.name ?: "Unknown")
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (connectedDevice == device) {
                    connectedDevice = null
                    listener?.onDeviceDisconnected()
                }
            }
        }
    }

    fun init() {
        if (btAdapter != null && btAdapter.isEnabled) {
            btAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
        } else {
            listener?.onStateChanged("Bluetooth disabled")
        }
    }

    fun close() {
        bluetoothHidDevice?.unregisterApp()
        btAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, bluetoothHidDevice)
    }

    private fun registerApp() {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "HID Remote",
            "Android Custom HID",
            "Android",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            concatDescriptors(
                HidUtils.KEYBOARD_REPORT_DESCRIPTOR,
                HidUtils.MOUSE_REPORT_DESCRIPTOR,
                HidUtils.CONSUMER_REPORT_DESCRIPTOR
            )
        )

        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800, 9, 0, 11250, -1
        )

        bluetoothHidDevice?.registerApp(sdp, null, qos, Executor { it.run() }, callback)
    }

    private fun concatDescriptors(vararg arrays: ByteArray): ByteArray {
        val totalLength = arrays.sumOf { it.size }
        val result = ByteArray(totalLength)
        var currentPos = 0
        for (a in arrays) {
            System.arraycopy(a, 0, result, currentPos, a.size)
            currentPos += a.size
        }
        return result
    }

    fun sendMouseReport(dx: Int, dy: Int, leftBtn: Boolean, rightBtn: Boolean) {
        val device = connectedDevice ?: return
        val host = bluetoothHidDevice ?: return

        var dX = dx
        var dY = dy
        if (dX > 127) dX = 127
        if (dX < -127) dX = -127
        if (dY > 127) dY = 127
        if (dY < -127) dY = -127

        var buttons: Int = 0
        if (leftBtn) buttons = buttons or 1
        if (rightBtn) buttons = buttons or 2

        val report = ByteArray(4)
        report[0] = buttons.toByte()
        report[1] = dX.toByte()
        report[2] = dY.toByte()
        
        host.sendReport(device, HidUtils.ID_MOUSE, report)
    }
    
    fun sendKeyboardReport(modifier: Byte, keyCode: Byte) {
        val device = connectedDevice ?: return
        val host = bluetoothHidDevice ?: return

        // 8 bytes: modifier, reserved, key1, key2, key3, key4, key5, key6
        val report = ByteArray(8)
        report[0] = modifier
        report[2] = keyCode
        
        host.sendReport(device, HidUtils.ID_KEYBOARD, report)
    }

    fun sendConsumerReport(usage: Short, pressed: Boolean) {
        val device = connectedDevice ?: return
        val host = bluetoothHidDevice ?: return

        // 2 bytes for usage value if pressed, 0 if released
        val report = ByteArray(2)
        if (pressed) {
            val uInt = usage.toInt()
            report[0] = (uInt and 0xFF).toByte()
            report[1] = ((uInt shr 8) and 0xFF).toByte()
        } else {
            report[0] = 0
            report[1] = 0
        }
        
        host.sendReport(device, HidUtils.ID_CONSUMER, report)
    }
}
