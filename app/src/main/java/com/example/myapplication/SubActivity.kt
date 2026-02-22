package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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

class SubActivity : ComponentActivity() {

    private var bluetoothStatus : MenuItem? =null

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
                // TODO: ボタン
            }) {
                Text(
                    text = "ボタン0",
                    modifier = Modifier
                )
            }
            Button(onClick = {
                // TODO: ボタン実装

            }) {
                Text(
                    text = "ボタン1",
                    modifier = Modifier
                )
            }
        }
    }

    fun actAdv() {
        val manager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter

        try {
            val gattServer = manager.openGattServer(
                this,
                gattServiceCallback()
            )

            // (iii)
            val gattService = BluetoothGattService(UUID_LIFF_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY)

            val chara1 = BluetoothGattCharacteristic(UUID_LIFF_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE)
            gattService.addCharacteristic(chara1)

            gattServer.addService(gattService)

            // (iv)

            val dataBuilder = AdvertiseData.Builder()
            dataBuilder.setIncludeTxPowerLevel(true)
            //dataBuilder.addServiceUuid(ParcelUuid.fromString(UUID_LIFF_SERVICE))

            val settingsBuilder = AdvertiseSettings.Builder()
            settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            settingsBuilder.setTimeout(0)
            settingsBuilder.setConnectable(true)

            val respBuilder = AdvertiseData.Builder()
            respBuilder.setIncludeDeviceName(true)

            // (v)
            //advertiser.s

        } catch (se: SecurityException) {
            Log.w("BT", "open", se)
        }
    }

    fun gattServiceCallback() {

    }

    fun getContext(): Context {
        return this
    }

    public override fun onStart() {
        super.onStart()

        // 表示を更新する
        bluetoothStatus?.tooltipText = "App not connected via bluetooth"

        MyBluetoothController.init(this)

        MyBluetoothController.getSender { hidd, device ->
            Log.i("SubActivity", "callback")
        }

        MyBluetoothController.getDisconnector {
            val mainHandler = Handler(getContext().mainLooper)

            mainHandler.post(object : Runnable {
                override fun run() {
                    //bluetoothStatus?.icon = getDrawable(R.drawable.ic_action_app_not_connected)
                    bluetoothStatus?.tooltipText = "App not connected via bluetooth"
                }
            })
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public override fun onStop() {
        super.onStop()

        MyBluetoothController.btHid?.unregisterApp()
        MyBluetoothController.hostDevice = null
        MyBluetoothController.btHid = null
    }

    public override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //bluetoothStatus = menu?.findItem(R.id.ble_app_connection_status)

        //val sharedPref = this.getPreferences(MODE_PRIVATE)

        return super.onCreateOptionsMenu(menu)
    }

}