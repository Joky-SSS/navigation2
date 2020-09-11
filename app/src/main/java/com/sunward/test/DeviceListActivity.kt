package com.sunward.test

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.devicelist.*

class DeviceListActivity : AppCompatActivity() {
    private var mBtAdapter: BluetoothAdapter? = null
    private val mDeviceClickListener = OnItemClickListener { _, v, _, _ ->
        val info = (v as TextView).text.toString()
        val address = info.substring(info.length - 17)
        val intent = Intent()
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address)
        this@DeviceListActivity.setResult(RESULT_OK, intent)
        finish()
    }
    private val mPairedDevicesArrayAdapter by lazy {
        ArrayAdapter<String>(
            this,
            R.layout.device_name
        )
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.devicelist)
        setResult(RESULT_CANCELED)
        paired_devices.adapter = mPairedDevicesArrayAdapter
        paired_devices.onItemClickListener = mDeviceClickListener

        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = mBtAdapter?.bondedDevices
        pairedDevices?.let {
            if (it.size > 0) {
                title_paired_devices.show()
                for (device in it) {
                    mPairedDevicesArrayAdapter.add(
                        """
                        ${device.name}
                        ${device.address}
                        """.trimIndent()
                    )
                }
            }
        } ?: mPairedDevicesArrayAdapter!!.add("没有已配对的设备")
    }

    companion object {
        var EXTRA_DEVICE_ADDRESS = "device_address"
        private const val TAG = "DeviceListActivity"

        fun startForResult(activity: Activity) {
            activity.startActivityForResult(
                Intent(activity, DeviceListActivity::class.java),
                BluetoothUtils.REQUEST_CODE_SELECT_DEVICE
            )
        }
    }
}