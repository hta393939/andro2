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
import android.os.Parcel
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
import java.nio.ByteBuffer
import java.util.UUID


class SubActivity : ComponentActivity() {

    /** 2A4D input report */
    private val UUID_INPUT = uuidFrom16bit(0x2A4D)
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

        try {
            short("before server")

            serverCallback = object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(
                    device: BluetoothDevice?,
                    status: Int,
                    newState: Int
                ) {
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
                            null
                        )
                        return
                    }
                    when (characteristic.uuid) {
                        UUID_CHAR_REPORTMAP.uuid -> {
                            val data = 66
                            val value = byteArrayOf(
                                0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
                                data.toByte(), 0x00.toByte(),
                                0x00.toByte(), 0x00.toByte(),
                                0x00.toByte(), 0x00.toByte()
                            )
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
            val disService = BluetoothGattService(
                UUID_SERVICE_DIS.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val charManufacturer = BluetoothGattCharacteristic(
                UUID_CHAR_MANUFACTURER.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            charManufacturer.value = byteArrayOf(
                0x62.toByte(), 0x31.toByte(), 0x30.toByte()
            )
            disService.addCharacteristic(charManufacturer)
            val charPnP = BluetoothGattCharacteristic(
                UUID_CHAR_PNP.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or
                        BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            charPnP.value = byteArrayOf(
                0x06.toByte(),
                0x04.toByte(), 94.toByte(),
                0x07.toByte(), 165.toByte(),
                0x00.toByte(), 0x03.toByte()
            )
            disService.addCharacteristic(charPnP)
            gattServer.addService(disService)

            /** バッテリー。moddableに寄せてみる */
            val basService = BluetoothGattService(
                UUID_SERVICE_BAS.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val charBas = BluetoothGattCharacteristic(
                UUID_CHAR_BAS.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
            )
            charBas.setValue(byteArrayOf(99.toByte()))
            basService.addCharacteristic(charBas)
            gattServer.addService(basService)

            /** GAP */
            val gapService = BluetoothGattService(
                UUID_SERVICE_GAP.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val charDN = BluetoothGattCharacteristic(
                UUID_CHAR_DN.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            charDN.value = byteArrayOf(0x61.toByte(), 0x32.toByte(), 0x33.toByte())
            gapService.addCharacteristic(charDN)
            val charAppear = BluetoothGattCharacteristic(
                UUID_CHAR_APPEAR.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            charAppear.value = byteArrayOf(0xC3.toByte(), 0x03.toByte())
            gapService.addCharacteristic(charAppear)
            gattServer.addService(gapService)

            /** HIDサービス。ローカル変数 */
            val gattService = BluetoothGattService(
                UUID_SERVICE_HID.uuid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            val info1 = BluetoothGattCharacteristic(
                UUID_CHAR_INFO.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or
                        BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            info1.value = byteArrayOf(
                0x0b.toByte(), 0x01.toByte(), 0x00.toByte(), 0x15.toByte()
            )
            gattService.addCharacteristic(info1)

            val cp1 = BluetoothGattCharacteristic(
                UUID_CHAR_CONTROLPOINT.uuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or
                        BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            cp1.value = byteArrayOf(0x01.toByte()) // TODO: ここは実装する
            gattService.addCharacteristic(cp1)

            val pm1 = BluetoothGattCharacteristic(
                UUID_CHAR_PROTOCOLMODE.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            )
            pm1.value = byteArrayOf(0x01.toByte()) // TODO: ここは実装する
            gattService.addCharacteristic(pm1)


            val input1 = BluetoothGattCharacteristic(
                UUID_CHAR_INPUT.uuid,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            inputChara = input1
            /** デスクリプションの追加 */
            val refDesc1 = BluetoothGattDescriptor(
                UUID_DESC_REPORTREF.uuid,
                //BluetoothGattDescriptor.PROPERTY_READ or
                //        BluetoothGattDescriptor.PROPERTY_WRITE,
                BluetoothGattDescriptor.PERMISSION_READ
            )
            @Suppress("DEPRECATION")
            refDesc1.value = byteArrayOf(0x01.toByte(), 0x01.toByte())
            input1.addDescriptor(refDesc1)

            gattService.addCharacteristic(input1)

            /** リポートマップ */
            val reportMap1 = BluetoothGattCharacteristic(
                UUID_CHAR_REPORTMAP.uuid,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or
                        BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
            )
            // ペリフェラルはこの書き方しか無いらしい
            @Suppress("DEPRECATION")
            reportMap1.value = DescriptorCollection.KEYBOARD
            gattService.addCharacteristic(reportMap1)

            gattServer.addService(gattService)

            // アドバータイズの作文と開始
            short("after add service")

            val dataBuilder = AdvertiseData.Builder().apply {
                //setIncludeDeviceName(true)
                setIncludeTxPowerLevel(true)
                addServiceUuid(UUID_SERVICE_HID)
                //addServiceUuid(UUID_SERVICE_BAS)
            }
            /** gamepad */
            //val appearValue: Short = 0x03C4.toShort()

            // apply this
            val settingsBuilder = AdvertiseSettings.Builder().apply {
                setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                setTimeout(0) // タイムアウト無し
                setConnectable(true)
                setDiscoverable(true)
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
                addServiceUuid(UUID_SERVICE_BAS)
            }
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

                    // NOTE: 1 は31byte超えてデータ多すぎエラーらしい;;
                    short("開始に失敗 $errorCode")
                }
            }

            short("before startAdvertising")

            advertiser.startAdvertising(
                settingsBuilder.build(),
                dataParcel,
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
