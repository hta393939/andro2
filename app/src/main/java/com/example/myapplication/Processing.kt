package com.example.myapplication

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.util.Log

object BluetoothController: BluetoothHidDevice.Callback(), BluetoothProfile.ServiceListener {
    override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
        super.onSetReport(device, type, id, data)
    }

    override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
        super.onGetReport(device, type, id, bufferSize)
    }

    override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        super.onConnectionStateChanged(device, state)
    }

    override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
        super.onAppStatusChanged(pluggedDevice, registered)
    }




    ////
    override fun onServiceDisconnected(profile: Int) {
        //TODO("Not yet implemented")
        Log.i("disconnected", "onServiceDisconnected")
    }

    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
        //TODO("Not yet implemented")
        Log.i("connected", "onServiceConnected")
    }


}




class Processing {
    public fun act1() {
        //val manager = BluetoothManager()
        //val apaptor = manager.getAdapter()
        return
    }
}

