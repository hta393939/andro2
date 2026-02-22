package com.example.myapplication

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Handler
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