package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context

import android.os.BatteryManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.ParcelUuid
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID


fun ltime(): String {
    val lt = LocalDateTime.now(ZoneId.of("Asia/Tokyo"))
    val format = DateTimeFormatter.ofPattern("HH:mm:ss")
    return lt.format(format)
}

data class UIState(
    val state: String = "進捗",
    val latest: String = "開始",
    val remoteName: String = "",
    val counter: String = "",
    val console: String = ""
)

class SubVM : ViewModel() {
    private val _uiState = MutableStateFlow(UIState())
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    fun setState(arg: String) {
        _uiState.update { current -> current.copy(state = arg) }
    }
    fun setLatest(arg: String) {
        _uiState.update { it.copy(latest = arg) }
    }
    fun setCounter(arg: String) {
        _uiState.update { it.copy(counter = arg) }
    }
    fun setRemoteName(arg: String) {
        _uiState.update { it.copy(remoteName = arg) }
    }
    fun addConsole(arg: String) {
        _uiState.update { current ->
            current.copy(console = "${ltime()}, ${arg}\n" + current.console)
        }
    }
}


/** 通知キュー用 */
class NotificationData {
    var device: BluetoothDevice? = null
    var char: BluetoothGattCharacteristic? = null
    var byteSeq: ByteArray = ByteArray(0)
    //var responseNeeded: Boolean = false
}

class SubActivity : ComponentActivity() {

    /** 1812 */
    private val UUID_SERVICE_HID = uuidFrom16bit(0x1812)
    /** 180A */
    private val UUID_SERVICE_DIS = uuidFrom16bit(0x180A)
    /** 2A29 */
    private val UUID_CHAR_MANUFACTURER = uuidFrom16bit(0x2A29)
    /** 2A50 */
    private val UUID_CHAR_PNP = uuidFrom16bit(0x2A50)

    /** 1800 GAP */
    private val UUID_SERVICE_GAP = uuidFrom16bit(0x1800)
    /** 2A00 デバイス名 */
    private val UUID_CHAR_DN = uuidFrom16bit(0x2A00)
    /** 2A01 2バイト **/
    private val UUID_CHAR_APPEAR = uuidFrom16bit(0x2A01)

    /** 180F */
    private val UUID_SERVICE_BAS = uuidFrom16bit(0x180F)
    /** 2A19 */
    private val UUID_CHAR_BAS = uuidFrom16bit(0x2A19)

    /** 2A4A */
    val UUID_CHAR_INFO = uuidFrom16bit(0x2A4A)
    /** 2A4B REPORTMAP */
    val UUID_CHAR_REPORTMAP = uuidFrom16bit(0x2A4B)
    /** 2A4C */
    val UUID_CHAR_CONTROLPOINT = uuidFrom16bit(0x2A4C)
    /** 2A4E */
    val UUID_CHAR_PROTOCOLMODE = uuidFrom16bit(0x2A4E)
    /** 2A4D REPORT */
    val UUID_CHAR_INPUT = uuidFrom16bit(0x2A4D)
    /** 2908  */
    val UUID_DESC_REPORTREF = uuidFrom16bit(0x2908)
    /** 2902 */
    val UUID_DESC_CCCD = uuidFrom16bit(0x2902)

    /** 入力リポートキャラ */
    private var inputChara: BluetoothGattCharacteristic? = null


    /** 接続成功時に保持する */
    private var remoteDevice: BluetoothDevice? = null

    private var serverCallback: BluetoothGattServerCallback? = null

    /** サーバー */
    private var mGattSrv: BluetoothGattServer? = null

    /** アドバタイザー */
    private var mAdv: BluetoothLeAdvertiser? = null

    /** 格納用 */
    private var advertiseCallback: AdvertiseCallback? = null

    private var mQueue: MutableList<NotificationData> = mutableListOf<NotificationData>()

    private var mMtu = 23

    /** UI更新用 */
    private var viewModel1: SubVM? = null

    private var counter1: Int = 1

    private lateinit var timer1: CountDownTimer

    private lateinit var mIntervalTimer2: CountDownTimer

    private var mWithAdr = true

    init {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel1 = ViewModelProvider(this).get(SubVM::class)

        // ActivityじゃなくてComponentActivityにすると出てきた
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme() {
                PageComponent(viewModel1!!)
            }
        }

        mIntervalTimer2 = object : CountDownTimer(Long.MAX_VALUE, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                sendNotification()
                counter1 ++
                viewModel1?.setCounter("${counter1}")
            }
            override fun onFinish() { // 0を指定するとfinishしてしまうので大きい値にする
                short("interval timer onFinish")
            }
        }.start()
    }

    /** GUI */
    @Composable
    fun PageComponent(viewModel: SubVM) {
        var isOn by remember { mutableStateOf(true) }

        val uiState by viewModel.uiState.collectAsState()
        // Unitを返す@Composableは大文字スタートらしい
        Column(modifier = Modifier
            .background(color = Color(128, 128, 128))) {
            Text(text = "sub activity")
            Text(text = "here is hidden")
            Text(text = "${uiState.state} ${uiState.remoteName}")
            Row(modifier = Modifier) {
                Button(onClick = { readyServer() }) {
                    Text(text = "ready server")
                }
                Spacer(modifier = Modifier.weight(0.125f))
                Switch(checked = isOn,
                    onCheckedChange = {
                        isOn = it
                        mWithAdr = it
                    })
            }
            Button(onClick = { startAdv() }) {
                Text(text = "start advertising")
            }
            Button(onClick = {
                sendReport()
                viewModel1?.addConsole("send key report")
            }) {
                Text(text = "send key report")
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    counter1++
                    viewModel1?.setLatest("latest $counter1")
                    viewModel1?.addConsole("counter $counter1")
                }, shape = RoundedCornerShape(8.dp)) {
                    Text(text = "update ${uiState.latest}")
                }
                Text(text = "counter ${uiState.counter}")
            }
            Button(onClick = {
                inputChara?.let {
                    @Suppress("DEPRECATION")
                    it.value[3] = 0x62
                }
                sendReport()

                timer1 = object : CountDownTimer(1_000, 0) {
                    override fun onTick(millisUntilFinished: Long) {
                        short("onTick $millisUntilFinished")
                    }

                    override fun onFinish() {
                        short("onFinish")

                        inputChara?.let {
                            @Suppress("DEPRECATION")
                            it.value[3] = 0
                            sendReport()
                        }
                    }
                }.start()

            }) {
                Text(text = "key report down, up timer")
            }
            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()) {
                Text(
                    text = uiState.console,
                    modifier = Modifier
                        .fillMaxWidth()
                        //.heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }

    fun short(message: String) {
        Toast.makeText(
            getContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    /** BondState はbroadcastで受け取る必要があるらしい */
    fun readyServer() {
        val manager: BluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

        try {
            //short("before server")

            serverCallback = object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(
                    device: BluetoothDevice?,
                    status: Int,
                    newState: Int
                ) {
                    super.onConnectionStateChange(device, status, newState)

                    // 発火する
                    viewModel1?.addConsole("onConnectionStateChange $newState ${device?.bondState} ${device?.name}")

                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            remoteDevice = device

                            viewModel1?.addConsole("接続された")
                            viewModel1?.setState(ucode(0x1F4F6))
                            val remoteName = device?.name
                            viewModel1?.setRemoteName("$remoteName")

                            mAdv?.stopAdvertising(advertiseCallback)
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            viewModel1?.addConsole("切断された")
                            viewModel1?.setState(ucode(0x1f6ab))
                        }
                    }
                }

                override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                    super.onServiceAdded(status, service)

                    short("onServiceAdded $status")
                }

                override fun onPhyRead(
                    device: BluetoothDevice?,
                    txPhy: Int,
                    rxPhy: Int,
                    status: Int
                ) {
                    super.onPhyRead(device, txPhy, rxPhy, status)
                    // 発火する
                    viewModel1?.addConsole("onPhyRead ${device?.name}")
                }

                override fun onPhyUpdate(
                    device: BluetoothDevice?,
                    txPhy: Int,
                    rxPhy: Int,
                    status: Int
                ) {
                    super.onPhyUpdate(device, txPhy, rxPhy, status)
                    viewModel1?.addConsole("onPhyUpdate $status ${device?.bondState}")
                }
                override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
                    super.onMtuChanged(device, mtu)
                    viewModel1?.addConsole("onMtuChanged $mtu")
                    mMtu = mtu
                }

                override fun onCharacteristicReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

                    viewModel1?.addConsole("charread, $requestId, ${characteristic?.instanceId} $offset ${characteristic?.uuid}")

                    if (characteristic == null) {
                        viewModel1?.addConsole("null char")

                        mGattSrv?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null
                        )
                        return
                    }

                    @Suppress("DEPRECATION")
                    mGattSrv?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        characteristic.value
                    )

                }

                override fun onCharacteristicWriteRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    characteristic: BluetoothGattCharacteristic?,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    super.onCharacteristicWriteRequest(
                        device,
                        requestId,
                        characteristic,
                        preparedWrite,
                        responseNeeded,
                        offset,
                        value
                    )

                    viewModel1?.addConsole("charwreq, $requestId")
                }

                override fun onDescriptorReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    descriptor: BluetoothGattDescriptor?
                ) {
                    super.onDescriptorReadRequest(device, requestId, offset, descriptor)

                    viewModel1?.addConsole("descread")

                    mGattSrv?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        descriptor?.value
                    )
                }

                override fun onDescriptorWriteRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    descriptor: BluetoothGattDescriptor?,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    super.onDescriptorWriteRequest(
                        device,
                        requestId,
                        descriptor,
                        preparedWrite,
                        responseNeeded,
                        offset,
                        value
                    )

                    viewModel1?.addConsole("descwrite")
                }

            }

            // 開く
            val gattServer = manager.openGattServer(
                this,
                serverCallback
            )
            this.mGattSrv = gattServer

            // NOTICE: クリア呼んだら減るかな
            gattServer.clearServices()

            val useDeviceNameInDIS = false


            /** デバイス情報 */
            val disService = BluetoothGattService(
                UUID_SERVICE_DIS.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val charManufacturer = BluetoothGattCharacteristic(
                UUID_CHAR_MANUFACTURER.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            @Suppress("DEPRECATION")
            charManufacturer.value = byteArrayOf(
                0x62.toByte(), 0x31.toByte(), 0x30.toByte()
            )
            if (useDeviceNameInDIS) {
                disService.addCharacteristic(charManufacturer)
            }

            val charPnP = BluetoothGattCharacteristic(
                UUID_CHAR_PNP.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            @Suppress("DEPRECATION")
            charPnP.value = byteArrayOf(
                0x02.toByte(), // flags
                0x00.toByte(), 0x00.toByte(), // VID
                0x00.toByte(), 0x00.toByte(), // PID
                0x00.toByte(), 0x00.toByte() // version
            )
            /*
            charPnP.value = byteArrayOf(
                0x06.toByte(), // flags
                0x04.toByte(), 94.toByte(), // VID
                0x07.toByte(), 165.toByte(), // PID
                0x00.toByte(), 0x03.toByte() // version
            ) */
            disService.addCharacteristic(charPnP)
            gattServer.addService(disService)


            /** バッテリー。moddableに寄せてみる */
            val basService = BluetoothGattService(
                UUID_SERVICE_BAS.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val charBas = BluetoothGattCharacteristic(
                UUID_CHAR_BAS.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val battery = getBatteryPercentage()
            viewModel1?.addConsole("battery, $battery")
            @Suppress("DEPRECATION")
            charBas.setValue(byteArrayOf(battery.toByte()))
            basService.addCharacteristic(charBas)
            gattServer.addService(basService)


            val useProtocolMode = false
            /** 無くすとappearanceアイコン変わらないと思うけどどうしよう;; */
            val useGAP = false
            /** GAPサービス */
            val gapService = BluetoothGattService(
                UUID_SERVICE_GAP.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val charDN = BluetoothGattCharacteristic(
                UUID_CHAR_DN.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            @Suppress("DEPRECATION")
            charDN.value = byteArrayOf(0x61.toByte(), 0x32.toByte(), 0x33.toByte())
            gapService.addCharacteristic(charDN)
            val charAppear = BluetoothGattCharacteristic(
                UUID_CHAR_APPEAR.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            @Suppress("DEPRECATION")
            charAppear.value = byteArrayOf(0xC3.toByte(), 0x03.toByte())
            gapService.addCharacteristic(charAppear)
            if (useGAP) {
                gattServer.addService(gapService)
            }


            /** 本命。HIDサービス。ローカル変数 */
            val hidService = BluetoothGattService(
                UUID_SERVICE_HID.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            /** TODO: HID Information だがここチェックするのか?? */
            val info1 = BluetoothGattCharacteristic(
                UUID_CHAR_INFO.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            @Suppress("DEPRECATION")
            info1.value = byteArrayOf(
                0x11.toByte(), 0x01.toByte(), 0x00.toByte(),
                0x02.toByte()
                //01 11 00 03 // RemoteWake有りは3
            )
            hidService.addCharacteristic(info1)

            val cp1 = BluetoothGattCharacteristic(
                UUID_CHAR_CONTROLPOINT.uuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            @Suppress("DEPRECATION")
            cp1.value = byteArrayOf(0x00.toByte()) // TODO: ここは実装する
            hidService.addCharacteristic(cp1)

            val pm1 = BluetoothGattCharacteristic(
                UUID_CHAR_PROTOCOLMODE.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            @Suppress("DEPRECATION")
            pm1.value = byteArrayOf(0x01.toByte()) // TODO: ここは実装する
            if (useProtocolMode) {
                hidService.addCharacteristic(pm1)
            }


            val input1 = BluetoothGattCharacteristic(
                UUID_CHAR_INPUT.uuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            inputChara = input1
            /** デスクリプションの追加 */
            val refDesc1 = BluetoothGattDescriptor(
                UUID_DESC_REPORTREF.uuid,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
            )
            @Suppress("DEPRECATION")
            refDesc1.value = byteArrayOf(0x00.toByte(), 0x01.toByte()) // ID無し、input
            input1.addDescriptor(refDesc1)

            val cccd1 = BluetoothGattDescriptor(
                UUID_DESC_CCCD.uuid,
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED or
                        BluetoothGattDescriptor.PERMISSION_READ
            )
            @Suppress("DEPRECATION")
            cccd1.value = byteArrayOf(0x00.toByte(), 0x00.toByte())
            input1.addDescriptor(cccd1)

            hidService.addCharacteristic(input1)


            /** TODO: リポートマップ。まさか追加する順番とかじゃないよね;; */
            val reportMap1 = BluetoothGattCharacteristic(
                UUID_CHAR_REPORTMAP.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED
            )
            // ペリフェラルはこの書き方しか無いらしい
            @Suppress("DEPRECATION")
            reportMap1.value = DescriptorCollection.KEYBOARD
            hidService.addCharacteristic(reportMap1)

            gattServer.addService(hidService)

            short("add service done")
            viewModel1?.addConsole("gattServer ready")

            if (mWithAdr) {
                startAdv()
            }

        } catch (se: SecurityException) {
            Log.w("BT", "open", se)

            short("catch $se")
        }
    }

    fun startAdv() {
        viewModel1?.addConsole("startAdv")

        val manager: BluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        //mAdapter = adapter
        /*
                if (adapter.isEnabled) {
                    val success = adapter.setName("andro2")
                    if (success) {
                        Log.i("BT", "setName")
                    } else {
                        Log.w("BT", "setName")
                    }
                }*/

        val dataBuilder = AdvertiseData.Builder().apply {
            //setIncludeDeviceName(true)
            setIncludeTxPowerLevel(true)
            addServiceUuid(UUID_SERVICE_HID)
            addServiceUuid(UUID_SERVICE_BAS)
            addServiceUuid(UUID_SERVICE_DIS)
            addServiceUuid(UUID_SERVICE_GAP)
        }
        /** gamepad */
        //val appearValue: Short = 0x03C4.toShort()

        // apply this
        val settingsBuilder = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            setTimeout(0) // タイムアウト無し
            setConnectable(true)
            //setDiscoverable(true) // この行は無くても見えた
        }

        /*
        fun AdvertiseData.writeToParcel(dest: Parcel, flags: Int): Unit {
            val mid = Parcel.obtain()
            this.writeToParcel(mid, flags)
            //val baseBuf = ByteBuffer.allocate(31)
            //mid.marshall(baseBuf, )
            // ByteArray
            val baseBuf = mid.marshall()
            val edit = baseBuf + 0x03.toByte() + 0x19.toByte() + 0xC4.toByte() + 0x03.toByte()
            dest.unmarshall(edit, 0, 31)
        } */
        // TODO: 上書きチェック↑
        val dataParcel = dataBuilder.build()
        /*
        fun dataParcel.writeToParcel(dest: Parcel, flags: Int): Unit {
            val mid = Parcel.obtain()
            this.writeToParcel(mid, flags)
        } */

        val respBuilder = AdvertiseData.Builder().apply {
            setIncludeDeviceName(true)
            //addServiceUuid(UUID_SERVICE_BAS)
            //addServiceUuid(UUID_SERVICE_GAP)
            //addServiceUuid(UUID_SERVICE_DIS)
        }

        val advertiser = adapter.bluetoothLeAdvertiser
        this.mAdv = advertiser

        advertiseCallback = object : AdvertiseCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)

                short("開始成功")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)

                // NOTE: 1 は31byte超えてデータ多すぎエラーらしい;;
                short("開始に失敗 $errorCode")
            }
        }

        short("before startAdvertising")
        viewModel1?.setState(ucode(0x1F4AC))

        mAdv?.startAdvertising(
            settingsBuilder.build(),
            dataParcel,
            respBuilder.build(),
            advertiseCallback
        )
    }

    fun getContext(): Context {
        return this
    }

    fun getBatteryPercentage(): Int {
        val batteryManager = getContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }


    public override fun onStart() {
        super.onStart()
    }

    public override fun onPause() {
        super.onPause()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public override fun onStop() {
        super.onStop()
    }

    public override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //bluetoothStatus = menu?.findItem(R.id.ble_app_connection_status)

        //val sharedPref = this.getPreferences(MODE_PRIVATE)

        return super.onCreateOptionsMenu(menu)
    }

    /** queueから取り出す場合 */
    fun sendNotification() {
        if (mQueue.isEmpty()) {
            return
        }
        val noty = mQueue.removeFirst()
        try {
            mGattSrv?.notifyCharacteristicChanged(
                noty.device!!,
                noty.char!!,
                false, // falseはnoti
                noty.byteSeq
            )
        } catch (se: SecurityException) {
            short("sendNoti $se")
        }

    }

    fun sendReport() {
        try {
            @Suppress("DEPRECATION")
            val success = inputChara?.value?.let {
                mGattSrv?.notifyCharacteristicChanged(
                    remoteDevice!!,
                    inputChara!!,
                    false,
                    it
                )
            } ?: false
            //if (!success) {
            short("送信成功 $success")
            //}
        } catch (se: SecurityException) {
            short("送信catch $se")
            //short("送信catch")
        } catch (e: Exception) {
            short("send catch $e")
        }
    }

    companion object {
        // 16-bit UUIDを指定してParcelUuidを作る（一番よく使われる方法）
        fun uuidFrom16bit(shortUuid: Int): ParcelUuid {
            // 16bit値を下位に配置
            return ParcelUuid(
                UUID.fromString(
                    String.format("0000%04X-0000-1000-8000-00805F9B34FB", shortUuid and 0xFFFF)
                )
            )
        }

        fun ucode(code: Int): String {
            return Character.toChars(code).concatToString()
        }


    }

}
