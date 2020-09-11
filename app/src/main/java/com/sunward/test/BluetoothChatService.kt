package com.sunward.test

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class BluetoothChatService private constructor() {

    private val mAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mCommunicateThread: CommunicateThread? = null
    private var mCombineThread: CombineThread? = null
    private val mListens = WeakHashMap<Int, IConnectStateChange>()
    val showMessage = MutableLiveData<String>()
    private var mState = STATE_NONE
    var mSerialNo = ""

    @get:Synchronized
    @set:Synchronized
    var state: Int
        get() = mState
        private set(state) {
            Log.d(TAG, "setState() $mState -> $state")
            mState = state
            notifyStateChange(state)
        }

    private val cacheList = LinkedBlockingQueue<String>()

    @Synchronized
    fun start() {
        Log.d(TAG, "start")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mCommunicateThread != null) {
            mCommunicateThread!!.cancel()
            mCommunicateThread = null
        }
        //if (mAcceptThread == null) {
        //    mAcceptThread = AcceptThread()
        //    mAcceptThread!!.start()
        //}
        state = STATE_LISTEN
    }

    @Synchronized
    fun connect(address: String) {
        val device = mAdapter.getRemoteDevice(address)
        Log.d(TAG, "connect to: $device")
        if (mState == STATE_CONNECTED) {
            Log.e(TAG, "Bluetooth already connected")
            return
        }
        if (mState == STATE_CONNECTING && mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mCommunicateThread != null) {
            mCommunicateThread!!.cancel()
            mCommunicateThread = null
        }
        state = STATE_CONNECTING
        mSerialNo = ""
        mConnectThread = ConnectThread(device)
        mConnectThread!!.start()
    }

    @Synchronized
    private fun connected(socket: BluetoothSocket?, device: BluetoothDevice) {
        Log.d(TAG, "connected")
        if (mConnectThread != null) {
//            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mCommunicateThread != null) {
            mCommunicateThread!!.cancel()
            mCommunicateThread = null
        }
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }

        mCommunicateThread = CommunicateThread(socket)
        mCommunicateThread!!.start()
        if (mCombineThread == null) {
            mCombineThread = CombineThread()
            mCombineThread!!.start()
        }
        state = STATE_CONNECTED
        showMessage.postValue("连接成功")
        write("log versiona\r\n".toByteArray())
    }

    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mCommunicateThread != null) {
            mCommunicateThread!!.cancel()
            mCommunicateThread = null
        }
        if (mAcceptThread != null) {
            mAcceptThread!!.cancel()
            mAcceptThread = null
        }
        if (mCombineThread != null) {
            mCombineThread!!.cancel()

        }
        mSerialNo = ""
        state = STATE_NONE
        cacheList.clear()
    }

    fun write(out: ByteArray) {
        synchronized(this) {
            if (mState == STATE_CONNECTED) {
                val r = mCommunicateThread
                r!!.write(out)
            }
        }
    }

    private fun connectionFailed() {
        mSerialNo = ""
        state = STATE_LISTEN
        showMessage.postValue("连接失败")
    }

    private fun connectionLost() {
        mSerialNo = ""
        state = STATE_LISTEN
        showMessage.postValue("蓝牙已断开连接")
    }

    private inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket?
        override fun run() {
            Log.d(TAG, "BEGIN mAcceptThread$this")
            while (mState != STATE_CONNECTED) {
                try {
                    val socket = mmServerSocket!!.accept()
                    if (socket != null) {
                        synchronized(this@BluetoothChatService) {
                            when (mState) {
                                STATE_NONE, STATE_CONNECTED -> {
                                    try {
                                        socket.close()
                                    } catch (e: IOException) {
                                        Log.e(TAG, "Could not close unwanted socket", e)
                                    }
                                }
                                STATE_LISTEN, STATE_CONNECTING -> connected(
                                    socket,
                                    socket.remoteDevice
                                )
                                else -> Log.e("", "")
                            }
                        }
                    }
                } catch (e2: IOException) {
                    Log.e(TAG, "accept() failed", e2)
                }
            }
            Log.i(TAG, "END mAcceptThread")
            return
        }

        fun cancel() {
            Log.d(TAG, "cancel $this")
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "close() of server failed",
                    e
                )
            }
        }

        init {
            name = "AcceptThread"
            var tmp: BluetoothServerSocket? = null
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(
                    NAME,
                    SERVICE_UUID
                )
            } catch (e: IOException) {
                Log.e(TAG, "listen() failed", e)
            }
            mmServerSocket = tmp
        }
    }

    private inner class ConnectThread(private val mmDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?

        init {
            name = "ConnectThread"
            var tmp: BluetoothSocket? = null
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(SERVICE_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "create() failed", e)
            }
            mmSocket = tmp
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread")
            mAdapter.cancelDiscovery()
            try {
                mmSocket!!.connect()
//                synchronized(
//                    this@BluetoothChatService
//                ) { mConnectThread = null }
                connected(mmSocket, mmDevice)
            } catch (e: IOException) {
                connectionFailed()
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(
                        TAG,
                        "unable to close() socket during connection failure",
                        e2
                    )
                }
            }
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }

    private inner class CommunicateThread(socket: BluetoothSocket?) : Thread() {

        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        private val mmSocket: BluetoothSocket?
        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val bytes = mmInStream!!.read(buffer)
                    if (bytes > 0) {
                        val frame = String(buffer, 0, bytes)
                        cacheList.offer(frame)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    return
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                mmOutStream!!.write(buffer)
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "close() of connect socket failed",
                    e
                )
            }
        }

        init {
            Log.d(TAG, "create ConnectedThread")
            name = "CommunicateThread"
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket!!.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "temp sockets not created",
                    e
                )
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    private inner class CombineThread : Thread() {
        private val buffer = StringBuilder()

        init {
            name = "CombineThread"
        }

        @Volatile
        private var mRunFlag = true
        override fun run() {
            while (true) {
                var frame = cacheList.take()
                var index = frame.indexOf("\n")
                while (index != -1) {
                    buffer.append(frame.substring(0, index))
                    val message = buffer.toString()
                    buffer.clear()
                    frame = if (index != (frame.length - 1)) {
                        frame.substring(index + 1)
                    } else ""
                    processMessage(message)
                    index = frame.indexOf("\n")
                }
                buffer.append(frame)
            }
        }

        private fun processMessage(message: String) {
            var event: ReceiverEvent? = null
            if (message.startsWith("#")) {
                val headAndBody = message.split(";")
                if (headAndBody.size < 2) return
                val headStr = headAndBody[0]
                val heads = headStr.split(",")
                val command = heads[0]
                val bodys = headAndBody[1].split(",")
                when (command) {
                    "#HEADINGA" -> {
                        val headinga = Headinga().apply {
                            serialNo = mSerialNo
                            solStat = bodys[0]
                            posType = bodys[1]
                            length = bodys[2].toDouble()
                            heading = bodys[3].toDouble()
                            pitch = bodys[4].toDouble()
                            stnId = bodys[8]
                            solnSVs = bodys[10].toInt()
                        }
                        event = ReceiverEvent(Command.HEADINGA, headinga)
                    }
                    "#BESTPOSA" -> {
                        val bestposa = BestPosa().apply {
                            serialNo = mSerialNo
                            solStat = bodys[0]
                            posType = bodys[1]
                            lat = bodys[2].toDouble()
                            lon = bodys[3].toDouble()
                            hgt = bodys[4].toDouble()
                            stnId = bodys[10]
                            solnSVs = bodys[14].toInt()
                        }
                        event = ReceiverEvent(Command.BESTPOSA, bestposa)
                    }
                }
            } else if (message.startsWith("$")) {
                val bodys = message.split(",")
                val command = bodys[0]
                if (command == "\$GPYBM") {
                    val gpybm = Gpybm().apply {
                        serialNo = bodys[1]
                        lat = bodys[3].toDouble()
                        lon = bodys[4].toDouble()
                        hgt = bodys[5].toDouble()
                        pitch = bodys[7].toDouble()
                        locationType = bodys[16].toInt()
                        satNoUsed = bodys[18].toInt()
                    }
                    event = ReceiverEvent(Command.GPYBM, gpybm)
                }
            } else if (message.startsWith("OK!")) {
                event = ReceiverEvent(Command.SETTING_OK)
            } else if (message.startsWith("Error")) {
                event = ReceiverEvent(Command.SETTING_ERROR)
            }
            event?.let { RxBus.get().post(it) }
        }

        fun cancel() {
            mRunFlag = false
            buffer.clear()
        }
    }

    fun addStateChangeListener(listener: IConnectStateChange) {
        mListens[listener.hashCode()] = listener
    }

    fun removeStateChangeListener(listener: IConnectStateChange) {
        mListens.remove(listener.hashCode())
    }

    private fun notifyStateChange(state: Int) {
        for ((_, value) in mListens) {
            value.onBLEConnectStateChange(state)
        }
    }

    private object Holder {
        val service = BluetoothChatService()
    }

    companion object {
        private val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val NAME = "BluetoothCom"
        const val STATE_CONNECTED = 3
        const val STATE_CONNECTING = 2
        const val STATE_LISTEN = 1
        const val STATE_NONE = 0
        private const val TAG = "BluetoothChatService"

        fun get(): BluetoothChatService {
            return Holder.service
        }
    }
}

interface IConnectStateChange {
    fun onBLEConnectStateChange(state: Int)
}