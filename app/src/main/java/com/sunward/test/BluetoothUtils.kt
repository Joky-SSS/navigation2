package com.sunward.test

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings

object BluetoothUtils {

    const val REQUEST_CODE_SELECT_DEVICE = 1
    const val REQUEST_CODE_ENABLE_BLUETOOTH = 2

    fun toSelectDevice(activity: Activity) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!btAdapter.isEnabled) {
            enableBluetooth(activity)
        } else {
            DeviceListActivity.startForResult(activity)
        }
    }

    fun enableBluetooth(activity: Activity) {
        activity.startActivityForResult(
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
            REQUEST_CODE_ENABLE_BLUETOOTH
        )
    }

    fun isBluetoothEnable(): Boolean {
        return BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false
    }
}