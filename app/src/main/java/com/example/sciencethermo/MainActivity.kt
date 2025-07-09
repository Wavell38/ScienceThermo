package com.example.sciencethermo

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.sciencethermo.ui.DashboardScreen
import com.example.sciencethermo.ui.theme.TestAppTheme
import com.google.gson.Gson
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.*

private const val ACTION_USB_PERMISSION = "com.example.testapp.USB_PERMISSION"
private const val TAG = "PICO-USB"

/** Données envoyées par le Pico */
data class SensorData(
    val T: Double,
    val RH: Double,
    val Td: Double,
    val ES: Double,
    val E: Double,
    val AH: Double,
    val W: Double,
    val H: Double
)

class MainActivity : ComponentActivity() {

    /* ---------- État UI ---------- */
    private val temp = mutableStateOf("--")
    private val hum  = mutableStateOf("--")
    private val rose = mutableStateOf("--")
    private val vapSat = mutableStateOf("--")
    private val vapReal = mutableStateOf("--")
    private val humAbs = mutableStateOf("--")
    private val rapport = mutableStateOf("--")
    private val enthalpie = mutableStateOf("--")
    private val json = mutableStateOf("")
    private val tempVal = mutableDoubleStateOf(0.0)

    /* ---------- USB ---------- */
    private lateinit var usbManager: UsbManager
    private var serialPort: UsbSerialPort? = null
    private var readJob: Job? = null
    private val gson = Gson()

    /**
     * IMPORTANT : l'activité est déclarée en launchMode="singleTask" dans le Manifest.
     * Si elle est déjà visible, onNewIntent() est appelé au lieu d'ouvrir une nouvelle instance.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            Log.d(TAG, "onNewIntent → nouveau périphérique USB attaché, tentative d'ouverture")
            openFirstPortIfAny()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val exported = Context.RECEIVER_EXPORTED
        registerReceiver(receiver, IntentFilter(ACTION_USB_PERMISSION), exported)
        registerReceiver(receiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED), exported)
        registerReceiver(receiver, IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED), exported)

        setContent {
            TestAppTheme {
                DashboardScreen(
                    temperature = temp.value,
                    humidity    = hum.value,
                    rose        = rose.value,
                    vapSat      = vapSat.value,
                    vapReal     = vapReal.value,
                    humAbs      = humAbs.value,
                    rapport     = rapport.value,
                    enthalpie   = enthalpie.value,
                    rawJson     = json.value,
                    temperatureValueC = tempVal.doubleValue
                )
            }
        }

        openFirstPortIfAny()
    }

    /* ---------- Port série ---------- */
    private fun openFirstPortIfAny() {
        val devices = usbManager.deviceList.values
        Log.d(TAG, "Devices=${devices.map { it.deviceName }}")
        for (device in devices) {
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: continue
            val port = driver.ports.first()
            if (!usbManager.hasPermission(device)) {
                val pi = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
                usbManager.requestPermission(device, pi)
                return
            }
            openPort(port)
            break
        }
    }

    private fun openPort(port: UsbSerialPort) {
        val connection = usbManager.openDevice(port.driver.device) ?: return
        port.open(connection)
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        /* Certains firmwares n'envoient rien tant que DTR/RTS ne sont pas activés */
        try {
            port.dtr = true
            port.rts = true
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de régler DTR/RTS : ${e.message}")
        }
        serialPort = port
        Log.d(TAG, "Port ouvert, démarrage du reader…")
        startReader()
    }

    private fun startReader() {
        readJob?.cancel()
        readJob = lifecycleScope.launch(Dispatchers.IO) {
            val buf = ByteArray(256)
            val line = StringBuilder()
            try {
                while (isActive && serialPort != null) {
                    val len = try { serialPort?.read(buf, 1000) ?: 0 } catch (e: Exception) {
                        Log.e(TAG, "IO error on read: ${e.message}")
                        break
                    }
                    if (len > 0) {
                        val chunk = String(buf, 0, len, Charsets.UTF_8)
                        line.append(chunk)
                        val idx = line.indexOfAny(charArrayOf('\n', '\r'))
                        if (idx >= 0) {
                            val raw = line.substring(0, idx).trim()
                            line.delete(0, idx + 1)
                            Log.d(TAG, "RX=$raw")
                            try {
                                val data = gson.fromJson(raw, SensorData::class.java)
                                withContext(Dispatchers.Main) {
                                    temp.value = "%.2f °C".format(data.T)
                                    tempVal.doubleValue = data.T
                                    hum.value  = "%.2f  %%".format(data.RH)
                                    rose.value  = "%.2f °C".format(data.Td)
                                    vapSat.value  = "%.2f hPa".format(data.ES)
                                    vapReal.value  = "%.2f hPa".format(data.E)
                                    humAbs.value  = "%.2f g/m³".format(data.AH)
                                    rapport.value  = "%.2f g/kg".format(data.W)
                                    enthalpie.value  = "%.2f kJ/kg".format(data.H)
                                    json.value = raw
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "JSON parse error: ${e.message}")
                            }
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    temp.value = "--"
                    hum.value = "--"
                    json.value = ""
                    tempVal.doubleValue = 0.0
                    rose.value = "--"
                    vapSat.value = "--"
                    vapReal.value  = "--"
                    humAbs.value  = "--"
                    rapport.value  = "--"
                    enthalpie.value  = "--"
                }
            }
        }
    }

    /* ---------- BroadcastReceiver ---------- */
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION, UsbManager.ACTION_USB_DEVICE_ATTACHED -> openFirstPortIfAny()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    readJob?.cancel(); readJob = null
                    serialPort?.close(); serialPort = null
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        readJob?.cancel(); serialPort?.close()
    }
}
