package com.example.myapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.getSystemService

// これでいいのか?
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult

object MyBluetoothController: BluetoothHidDevice.Callback(), BluetoothProfile.ServiceListener {
    /** 保持する */
    private lateinit var appContext: Context

    val featureReport = FeatureReport()


    override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
        Log.i("setfirst","setfirst")
        super.onSetReport(device, type, id, data)
        Log.i("setreport","this $device and $type and $id and $data")

    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {

        Log.i("getbefore", "first")
        super.onGetReport(device, type, id, bufferSize)

        Log.i("get", "second")
        if (type == BluetoothHidDevice.REPORT_TYPE_FEATURE) {
            featureReport.wheelResolutionMultiplier = true
            featureReport.acPanResolutionMultiplier = true
            Log.i("getbthid","$btHid")

            val id: Byte = 0
            //val id = FeatureReport.ID
            val wasrs=btHid?.replyReport(device, type,
                id, featureReport.bytes)
            Log.i("replysuccess flag ",wasrs.toString())
        }


    }

    /** アダプター */
    val btAdapter by lazy {
        // TODO: 新しい書き方
        //val manager = getSystemService(this, Context.BLUETOOTH_SERVICE)


        BluetoothAdapter.getDefaultAdapter()!!
    }
    /** HIDデバイス */
    var btHid: BluetoothHidDevice? = null
    /** ホストデバイス */
    var hostDevice: BluetoothDevice? = null
    var autoPairFlag = false

    var mpluggedDevice :BluetoothDevice? = null



    private var deviceListener: ((BluetoothHidDevice, BluetoothDevice)->Unit)? = null
    private var disconnectListener: (()->Unit)? = null

    /** 初期化する */
    fun init(ctx: Context) {
        appContext = ctx.applicationContext

        if (btHid != null)
            return
        btAdapter.getProfileProxy(ctx, this, BluetoothProfile.HID_DEVICE)
    }

    fun getSender(callback: (BluetoothHidDevice, BluetoothDevice)->Unit) {
        btHid?.let { hidd ->
            hostDevice?.let { host ->
                callback(hidd, host)
                return
            }
        }
        deviceListener = callback
    }


    fun getDisconnector(callback: ()->Unit) {

        disconnectListener = callback
    }

    /*****************************************************/
    /** BluetoothProfile.ServiceListener implementation **/
    /*****************************************************/

    override fun onServiceDisconnected(profile: Int) {
        Log.e(TAG, "Service disconnected!")
        if (profile == BluetoothProfile.HID_DEVICE)
            btHid = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
        Log.i(TAG, "Connected to service")

        Toast.makeText(
            appContext,
            "サービストースト",
            Toast.LENGTH_SHORT
        ).show()


        if (profile != BluetoothProfile.HID_DEVICE) {
            Log.wtf(TAG, "WTF? $profile")
            return
        }

        val btHid = proxy as? BluetoothHidDevice
        if (btHid == null) {
            Log.wtf(TAG, "WTF? Proxy received but it's not BluetoothHidDevice")

            return
        }
        this.btHid = btHid
        // TODO: 登録
        btHid.registerApp(sdpRecord,
            null,
            qosOut,
            {it.run()}, this)

        // TODO: これActivityに通知するにはどうしたらいい???

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                // 最大300秒
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            //startActivityForResult(intent, REQUEST_CODE_DISCOVERABLE)
        } else {
            Log.w("BT", "No calling")
            /*
            try {
                // TODO: なぜセットできない Android 12で無くなった
                val success = btAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 300000)
                if (success) {
                    Log.d("BT", "Discoverable mode set successfully")
                } else {
                    Log.w("BT", "Failed to the")
                }
            } catch (e: SecurityException) {
                Log.e("BT", "Permission error", e)
            } */
        }
    }



    /************************************************/
    /** BluetoothHidDevice.Callback implementation **/
    /************************************************/



    override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        super.onConnectionStateChanged(device, state)
        Log.i(TAG, "Connection state ${when(state) {
            BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
            BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"

            else -> state.toString()
        }}")
        if (state == BluetoothProfile.STATE_CONNECTED) {
            if (device != null) {
                hostDevice = device

                deviceListener?.invoke(btHid!!, device)

                //deviceListener = null
            } else {
                Log.e(TAG, "Device not connected")
            }
        } else {
            hostDevice = null
            if(state == BluetoothProfile.STATE_DISCONNECTED)
            {
                disconnectListener?.invoke()
            }

        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
        super.onAppStatusChanged(pluggedDevice, registered)
        if(registered)
        {
            val pairedDevices = btHid?.getDevicesMatchingConnectionStates(intArrayOf(BluetoothProfile.STATE_CONNECTING,BluetoothProfile.STATE_CONNECTED,BluetoothProfile.STATE_DISCONNECTED,BluetoothProfile.STATE_DISCONNECTING))
            Log.d("paired d", "paired devices are : $pairedDevices")
            Log.d("paired d","${btHid?.getConnectionState(pairedDevices?.get(0))}")
            mpluggedDevice = pluggedDevice
            if(btHid?.getConnectionState(pluggedDevice)== BluetoothProfile.STATE_DISCONNECTED && pluggedDevice!= null && autoPairFlag)
            {
                btHid?.connect(pluggedDevice)
                //hostDevice.toString()


            }


            else if(btHid?.getConnectionState(pairedDevices?.get(0))== BluetoothProfile.STATE_DISCONNECTED && autoPairFlag)
            {
                Log.i("ddaaqq","sssS"
                )
                btHid?.connect(pairedDevices?.get(0))
            }

//            val intent = Intent("CUSTOM_ACTION")
//            intent.putExtra("DATE", Date().toString())
//            Log.d("j", "sending broadcast")
//
//            // send local broadcast
//            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)





        }


    }





    /*************/
    /** Garbage **/
    /*************/

    const val TAG = "MyBluetoothController"


    private val sdpRecord by lazy {
        BluetoothHidDeviceAppSdpSettings(
            "Pixel HID1",
            "Mobile BController",
            "bla",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            DescriptorCollection.MOUSE_KEYBOARD_COMBO
        )
    }


    private val qosOut by lazy {
        BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
    }

}

