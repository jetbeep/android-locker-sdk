package com.jetbeep.app.lockersdkdemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.jetbeep.lockersdk.LockerSdk
import com.jetbeep.lockersdk.config.DeviceInfoValidationType
import com.jetbeep.lockersdk.locker.DeviceListener
import com.jetbeep.lockersdk.locker.LockerDevice
import com.jetbeep.lockersdk.locker.OnConnectionDeviceListener
import com.jetbeep.lockersdk.utils.Log
import com.jetbeep.lockersdk.utils.decodeHex
import com.jetbeep.lockersdk.utils.getListRequiredPermissions
import com.jetbeep.lockersdk.utils.isBluetoothEnabled
import com.jetbeep.lockersdk.utils.isPermissionsGranted
import com.jetbeep.lockersdk.utils.requestBluetoothIntent
import com.jetbeep.lockersdk.utils.toHex
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "LOCKER_DEMO"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { isGranted ->
        log(TAG, "Permissions: $isGranted")
        changePermissionsUi(isPermissionsGranted(this))
    }

    private var connectedDevice: LockerDevice? = null

    private val deviceAdapter = DeviceAdapter(::onItemDeviceClick)
    private var deviceRecyclerView: RecyclerView? = null
    private var permissionStatus: TextView? = null
    private var bluetoothStatus: TextView? = null
    private var requestPermission: Button? = null
    private var enableBt: Button? = null
    private var nearbyDeviceLabel: TextView? = null
    private var startScan: Button? = null
    private var stopScan: Button? = null
    private var scannerStatus: TextView? = null
    private var functionsLayout: View? = null
    private var disconnect: Button? = null
    private var searchProgressBar: ProgressBar? = null
    private var done1: View? = null
    private var done2: View? = null
    private var nearbyGroup: View? = null
    private var connectionGroup: View? = null
    private var functionProgressBar: ProgressBar? = null
    private var connectedDeviceLabel: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        initUi()

        LockerSdk.locker.addDeviceListener(deviceListener)
        LockerSdk.locker.addConnectionListener(connectionDeviceListener)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.logEvents.collect {
                    log(it.tag, it.message)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                LockerSdk.locker.nearbyDevices.collect { devices ->
                    nearbyGroup?.isVisible = devices.isNotEmpty()
                    deviceAdapter.submitList(devices)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LockerSdk.locker.removeDeviceListener(deviceListener)
        LockerSdk.locker.removeConnectionListener(connectionDeviceListener)
    }

    override fun onResume() {
        super.onResume()
        changePermissionsUi(isPermissionsGranted(this))
        changeBtUi(isBluetoothEnabled(this))
    }

    private fun initUi() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        permissionStatus = findViewById(R.id.permissions_status)
        bluetoothStatus = findViewById(R.id.bluetooth_status)

        requestPermission = findViewById(R.id.request_permissions)
        requestPermission?.setOnClickListener {
            requestPermissions()
        }

        enableBt = findViewById(R.id.request_bluetooth)
        enableBt?.setOnClickListener {
            startActivity(requestBluetoothIntent)
        }

        startScan = findViewById(R.id.start_scan)
        startScan?.setOnClickListener {
            startScan()
        }

        stopScan = findViewById(R.id.stop_scan)
        stopScan?.setOnClickListener {
            stopScan()
        }

        searchProgressBar = findViewById(R.id.search_progress)
        searchProgressBar?.isVisible = false

        scannerStatus = findViewById(R.id.scanner_status)

        nearbyGroup = findViewById(R.id.nearby)
        nearbyDeviceLabel = findViewById(R.id.nearby_devices)

        functionsLayout = findViewById(R.id.functions_layout)
        functionsLayout?.isVisible = false

        disconnect = findViewById(R.id.disconnect)
        disconnect?.setOnClickListener {
            disconnect()
        }

        deviceRecyclerView = findViewById(R.id.device)
        deviceRecyclerView?.adapter = deviceAdapter

        connectionGroup = findViewById(R.id.connection_group)

        functionProgressBar = findViewById(R.id.func_progress)

        connectedDeviceLabel = findViewById(R.id.functions_label)

        val getDeviceInfo: Button = findViewById(R.id.getDeviceInfo)
        getDeviceInfo.setOnClickListener {
            lifecycleScope.launch {
                functionProgressBar?.isVisible = true
                val result = LockerSdk.locker.getDeviceInfo(
                    DeviceInfoValidationType.DEVICE_KEY,
                    publicDeviceKey
                )
                functionProgressBar?.isVisible = false
                result.onSuccess {
                    val message = StringBuilder()
                        .append("Device id: ")
                        .append(it.deviceId)
                        .append("\n")
                        .append("Project id: ")
                        .append(it.projectId)
                        .append("\n")
                        .append("Signature:")
                        .append(it.signature.toHex())

                    showInfoDialog("Device info", message.toString())
                }.onFailure {
                    showInfoDialog("Device info", it.toString())
                }
                log(TAG, "getDeviceInfo: $result")

            }
        }

        val encryptConnection: Button = findViewById(R.id.encrypt_connection)
        encryptConnection.setOnClickListener {
            lifecycleScope.launch {
                functionProgressBar?.isVisible = true
                val result = LockerSdk.locker.enableEncryption()
                functionProgressBar?.isVisible = false
                result.onSuccess {
                    showInfoDialog(
                        "Encryption",
                        "An encrypted connection has been established; all subsequent data during transmission will be encrypted"
                    )
                }.onFailure {
                    showInfoDialog("Encryption", it.toString())
                }
                log(TAG, "establishEncryptedConnection: $result")
            }
        }

//        val lockCode: EditText = findViewById(R.id.lock_code)
        val openLock: Button = findViewById(R.id.open_lock)
        openLock.setOnClickListener {
            lifecycleScope.launch {
                functionProgressBar?.isVisible = true
                val password = 12345L // lockCode.text.toString().toLong()
                val result = LockerSdk.locker.openLock(password)
                functionProgressBar?.isVisible = false
                result.onSuccess {
                    showInfoDialog("Open lock", "Open result: ${result.isSuccess}")
                }
                result.onFailure {
                    showInfoDialog("Open lock", it.toString())
                }
                log(TAG, "Open lock result: $result")

            }
        }

        val shareLog: Button = findViewById(R.id.share)
        shareLog.setOnClickListener {
            shareLogs()
        }

        done1 = findViewById(R.id.done1)
        done2 = findViewById(R.id.done2)
    }

    private fun onItemDeviceClick(device: LockerDevice) {
        if (connectedDevice != null) {
            disconnect()
        }
        connect(device)
    }

    private fun changePermissionsUi(isGranted: Boolean) {
        permissionStatus?.text =
            if (isGranted) getString(R.string.granted) else getString(R.string.not_granted)
        requestPermission?.isVisible = !isGranted
        done2?.isVisible = isGranted
    }

    private fun changeBtUi(enable: Boolean) {
        bluetoothStatus?.text =
            if (enable) getString(R.string.bluetooth_on) else getString(R.string.bluetooth_off)
        enableBt?.isVisible = !enable
        done1?.isVisible = enable
    }

    private fun requestPermissions() {
        val list = getListRequiredPermissions()
        log(TAG, "requestPermissions: ${list.joinToString(", ")}")
        requestPermissionLauncher.launch(list)
    }

    private fun startScan() {
        log(TAG, "startScan")
        val startResult = LockerSdk.locker.startSearching()
        if (startResult.isSuccess) {
            scannerStatus?.text = getString(R.string.scanning)
            startScan?.isVisible = false
            stopScan?.isVisible = true
            searchProgressBar?.isVisible = true
        } else {
            log(TAG, "startResult: $startResult")
            Toast.makeText(this@MainActivity, startResult.toString(), Toast.LENGTH_SHORT).show()
            searchProgressBar?.isVisible = false
        }
    }

    private fun stopScan() {
        log(TAG, "stopScan")
        LockerSdk.locker.stopSearching()
        scannerStatus?.text = getString(R.string.device_search)
        startScan?.isVisible = true
        stopScan?.isVisible = false
        searchProgressBar?.isVisible = false
    }

    private fun connect(lockerDevice: LockerDevice) {
        lifecycleScope.launch {
            log(TAG, "try connect, connectable = ${lockerDevice.isConnectable}")
            connectionGroup?.isVisible = true
            val connectResult = LockerSdk.locker.connect(lockerDevice)
            connectionGroup?.isVisible = false
            log(TAG, "$connectResult")
            if (connectResult.isFailure) {
                Toast.makeText(this@MainActivity, connectResult.toString(), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun disconnect() {
        LockerSdk.locker.disconnect()
    }

    private val deviceListener = object : DeviceListener {
        override fun onFound(device: LockerDevice) {
            log(TAG, "onFound: $device")
        }

        override fun onLost(device: LockerDevice) {
            log(TAG, "onLost: $device")
        }

        override fun onChanged(device: LockerDevice) {
            log(TAG, "onChanged: $device")
        }

        override fun onError(e: Exception) {
            log(TAG, "Device listener error: $e")
        }

    }

    private val connectionDeviceListener = object : OnConnectionDeviceListener {

        override fun onConnected(lockerDevice: LockerDevice) {
            connectedDevice = lockerDevice
            functionsLayout?.isVisible = true
            connectedDeviceLabel?.text = getString(R.string.connected_device, lockerDevice.deviceId)
            disconnect?.isVisible = true
        }

        override fun onDisconnected() {
            connectedDevice = null
            functionsLayout?.isVisible = false
            disconnect?.isVisible = false
        }

    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .apply {
                setTitle(title)
                setMessage(message)
                setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            }
            .show()
    }

    private val logs: MutableList<String> = mutableListOf()
    private fun shareLogs() {
        val logsString = StringBuilder()
        logs.forEach { logsString.append(it).append("\n") }

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, logsString.toString())
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun log(tag: String, message: String) {
        val msg = "$tag: $message"
        android.util.Log.d(TAG, msg)
        logs.add(msg)
    }

    // Use your device's public key
    private val publicDeviceKey =
        "04 88 20 d6 33 e6 07 ac  bc 2b c7 2c 8b 35 1a 22 6a 20 a2 8e a6 f9 aa a2  88 d4 85 80 c0 81 4f 8e ea db 35 9a 0e 00 74 41  63 ca 11 52 53 c4 57 5a  c2 d7 b8 43 9f e3 ff 22  81 c3 ca 2f ff d1 bd 9e  00"
            .replace(" ", "")
            .decodeHex()
}