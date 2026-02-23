package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.util.UUID


class SubActivity : ComponentActivity() {

    /** 2A4D input report */
    private val UUID_INPUT = uuidFrom16bit(0x2A4D)
    /** 1812 */
    private val UUID_SERVICE_HID = uuidFrom16bit(0x1812)
    /** 180A */
    private val UUID_SERVICE_DIS = uuidFrom16bit(0x180A)
    /** 2A50 */
    private val UUID_CHAR_PNP = uuidFrom16bit(0x2A50)

    /** 180F */
    private val UUID_SERVICE_BAS = uuidFrom16bit(0x180F)
    /** 2A19 */
    private val UUID_CHAR_BAS = uuidFrom16bit(0x2A19)

    /** 入力リポートキャラ */
    private var inputChara: BluetoothGattCharacteristic? = null

    //private var bluetoothStatus : MenuItem? =null

    /** 接続成功時に保持する */
    private var remoteDevice: BluetoothDevice? = null

    private var serverCallback: BluetoothGattServerCallback? = null

    /** サーバー */
    private var gattSrv: BluetoothGattServer? = null

    /** アドバタイザー */
    private var adv: BluetoothLeAdvertiser? = null
    /** 格納用 */
    private var advertiseCallback: AdvertiseCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ActivityじゃなくてComponentActivityにすると出てきた
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme() {
                PageComponent()
            }
        }
    }

    @Composable
    fun PageComponent() {
        // Unitを返す@Composableは大文字スタートらしい
        Column(
            modifier = Modifier
        ) {
            Text(
                text = "サブアクティビティ"
            )
            Text(
                text = "サブだよ"
            )
            Button(onClick = {
                actAdv()
            }) {
                Text(
                    text = "ボタン0 アドバータイズ開始",
                    modifier = Modifier
                )
            }
            Button(onClick = {
                sendReport()
            }) {
                Text(
                    text = "ボタン1 キーリポート送信",
                    modifier = Modifier
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

    fun actAdv() {
        val manager: BluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
/*
        if (adapter.isEnabled) {
            val success = adapter.setName("andro2")
            if (success) {
                Log.i("BT", "setName")
            } else {
                Log.w("BT", "setName")
            }
        }*/


        /** 2A4B REPORTMAP */
        val UUID_REPORTMAP = uuidFrom16bit(0x2A4B)
        val UUID_INFO = uuidFrom16bit(0x2A4A)

        val UUID_OUTPUT = uuidFrom16bit(0x2A4D)
        val UUID_REFDESC = uuidFrom16bit(0x2908)
        val UUID_CCCD = uuidFrom16bit(0x2902)


        try {
            short("before server")

            serverCallback = object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                    super.onConnectionStateChange(device, status, newState)

                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            remoteDevice = device
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {

                        }
                    }
                }

                override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
                    super.onServiceAdded(status, service)

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        short("サービスが追加された")
                    }
                }

                override fun onCharacteristicReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

                    if (characteristic == null) {
                        gattSrv?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null)
                        return
                    }
                    when (characteristic.uuid) {
                        UUID_REPORTMAP.uuid -> {
                            val data = 100
                            val value = byteArrayOf(data.toByte())
                            gattSrv?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                value
                            )
                        }
                        else -> {
                            gattSrv?.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_FAILURE,
                                0,
                                null
                            )
                        }
                    }
                }
            }


            val gattServer = manager.openGattServer(
                this,
                serverCallback
            )
            this.gattSrv = gattServer

            /** デバイス情報 */
            val disService = BluetoothGattService(UUID_SERVICE_DIS.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val charPnP = BluetoothGattCharacteristic(UUID_CHAR_PNP.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ)
            charPnP.value = byteArrayOf(0x02.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                0x00.toByte(), 0x00.toByte(), 0x00.toByte())
            disService.addCharacteristic(charPnP)
            gattServer.addService(disService)

            /** バッテリー */
            val basService = BluetoothGattService(UUID_SERVICE_BAS.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val charBas = BluetoothGattCharacteristic(UUID_CHAR_BAS.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_READ)
            charBas.setValue(byteArrayOf(99.toByte()))
            basService.addCharacteristic(charBas)
            gattServer.addService(basService)


            /** HIDサービス */
            val gattService = BluetoothGattService(UUID_SERVICE_HID.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val input1 = BluetoothGattCharacteristic(UUID_INPUT.uuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ)
            inputChara = input1
            gattService.addCharacteristic(input1)

            val output1 = BluetoothGattCharacteristic(UUID_OUTPUT.uuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE)
            gattService.addCharacteristic(output1)


            val refDesc1 = BluetoothGattDescriptor(UUID_REFDESC.uuid,
                BluetoothGattDescriptor.PERMISSION_READ
            )
            @Suppress("DEPRECATION")
            refDesc1.value = byteArrayOf(0x01.toByte(), 0x01.toByte())

            val cccd1 = BluetoothGattDescriptor(UUID_CCCD.uuid,
                BluetoothGattDescriptor.PERMISSION_READ or
                        BluetoothGattDescriptor.PERMISSION_WRITE
            )
            input1.addDescriptor(cccd1)

            val reportMap1 = BluetoothGattCharacteristic(UUID_REPORTMAP.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattDescriptor.PERMISSION_READ)
            // ペリフェラルはこの書き方しか無いらしい
            @Suppress("DEPRECATION")
            reportMap1.value = DescriptorCollection.KEYBOARD
            gattService.addCharacteristic(reportMap1)

            gattServer.addService(gattService)

            // アドバータイズの作文と開始
            short("after add service")

            val dataBuilder = AdvertiseData.Builder()
            /** gamepad */
            //val appearValue: Short = 0x03C4.toShort()

            /*
            val appear = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN).apply {
                    put(3.toByte()) // Bytes
                    put(0x19.toByte()) // Type
                    putShort(appearValue)
                }.array()
            dataBuilder.addManufacturerData(0xFFFF, appear)
             */
            dataBuilder.setIncludeDeviceName(true)
            dataBuilder.setIncludeTxPowerLevel(true)
            dataBuilder.addServiceUuid(UUID_SERVICE_HID)
                .addServiceUuid(UUID_SERVICE_DIS)
                .addServiceUuid(UUID_SERVICE_BAS)

            val settingsBuilder = AdvertiseSettings.Builder()
            settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            settingsBuilder.setTimeout(0) // タイムアウト無し
            settingsBuilder.setConnectable(true)

            val respBuilder = AdvertiseData.Builder()
            val pseudoAppear = byteArrayOf(
                0xC4.toByte(), 0x03.toByte())
            respBuilder.setIncludeDeviceName(true)
                .addServiceUuid(UUID_SERVICE_HID)
                .addManufacturerData(0xffff, pseudoAppear)
                //.addServiceData()
            // (v)
            val advertiser = adapter.bluetoothLeAdvertiser
            this.adv = advertiser

            advertiseCallback = object : AdvertiseCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    super.onStartSuccess(settingsInEffect)

                    short("開始成功")
                }

                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)

                    short("開始に失敗 $errorCode")
                }
            }

            short("before startAdvertising")

            advertiser.startAdvertising(
                settingsBuilder.build(),
                dataBuilder.build(),
                respBuilder.build(),
                advertiseCallback
            )

        } catch (se: SecurityException) {
            Log.w("BT", "open", se)

            short("catch $se")
        }
    }

    fun getContext(): Context {
        return this
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


    fun sendReport() {
        val buf = byteArrayOf(
            0x00.toByte(),
            0x00.toByte(), // reserved
            0x00.toByte(), // LED
            0x61.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
        )
        try {
            val success = gattSrv?.notifyCharacteristicChanged(
                remoteDevice!!,
                inputChara!!,
                false,
                buf
            ) ?: false
            //if (!success) {
            //    short("送信成功")
            //}
        } catch (se: SecurityException) {
            //short("送信catch $se")
            short("送信catch")
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
    }

}
