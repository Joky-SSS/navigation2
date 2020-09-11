package com.sunward.test

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.model.LatLngBounds
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.playlist_item.view.*
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private val mBluetoothService = BluetoothChatService.get()
    private val mDisposes = CompositeDisposable()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var adapter:SimpleAdapter
    private val gpsMap = mutableMapOf<Int, BestPosa>()
    private val voiceNameList = listOf(
        "1 - Dead And Gone",
        "2 - Numb Encore",
        "3 - push love away",
        "4 - That's All She Wrote",
        "1 - Dead And Gone",
        "2 - Numb Encore",
        "3 - push love away",
        "4 - That's All She Wrote"
    )
    private val voicePathList = listOf(
        "1 - Dead And Gone.mp3",
        "2 - Numb Encore.mp3",
        "3 - push love away.mp3",
        "4 - That's All She Wrote.mp3"
    )
    private var itemShow = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        RxBus.get().toFlowable(ReceiverEvent::class.java)
            .subscribeWith(object : SubscriberAdapter<ReceiverEvent>() {
                override fun onNext(t: ReceiverEvent) {
                    processEvent(t)
                }
            }).addTo(mDisposes)

//        bmapView.map.mapType = BaiduMap.MAP_TYPE_NONE

        adapter = SimpleAdapter { position ->
            Log.e("sss", "item $position clicked,${voiceNameList[position]}")
        }
        rvPlaylist.adapter = adapter
        rvPlaylist.setHasFixedSize(true)
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.list_item_divider_left_60)!!)
        rvPlaylist.addItemDecoration(divider)

        mBluetoothService.showMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }


        val myTileProvider = object : com.tencent.tencentmap.mapsdk.maps.model.UrlTileProvider() {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
                //1.过滤无效瓦片请求
                if (!checkTileExists(x, y, zoom)) {
                    return null
                }
                //2.配置瓦片请求链接
                val url = String.format("https://my.image.server/images/%d/%d/%d.png", zoom, x, y)
                return try {
                    //返回瓦片URL
                    URL(url)
                } catch (e: MalformedURLException) {
                    throw AssertionError(e)
                }
            }

            /**
             * 检查当前瓦片地址是否有数据
             */
            private fun checkTileExists(x: Int, y: Int, zoom: Int): Boolean {
                val minZoom = 12
                val maxZoom = 16
                return !(zoom < minZoom || zoom > maxZoom)
            }
        }

        val tileProvider = object : FileTileProvider() {
            override fun getTile(x: Int, y: Int, z: Int): Tile? {
                // 根据地图某一状态下x、y、z加载指定的瓦片图
                val filedir = "$z/${x}_$y.png"
                Log.e("ss", filedir)
                val fixdir = "tile/3/0/0.jpg"
                //将瓦片图资源解析为Bitmap
                val bm: Bitmap = getBitmapFromAsset(this@MainActivity, filedir) ?: return null

                // 通过瓦片图bitmap构造Tile示例
                val offlineTile = Tile(bm.width, bm.height, toRawData(bm))
                bm.recycle()
                return offlineTile
            }

            override fun getMaxDisLevel(): Int {
                return 20
            }

            override fun getMinDisLevel(): Int {
                return 16
            }
        }


        val options = TileOverlayOptions()
        val northeast = LatLng(28.260718,113.193378 )
        val southwest = LatLng(28.249534,113.186712)
//        val northeast = LatLng(80.0, 180.0)
//        val southwest = LatLng(-80.0, -180.0)
        options.tileProvider(tileProvider)
            .setPositionFromBounds(
                LatLngBounds.Builder().include(northeast).include(southwest).build()
            )
        val tileOverlay = bmapView.map.addTileLayer(options)
        Log.e("ss", "ss")


        bmapView.map.setMapStatus(
            MapStatusUpdateFactory.newLatLngBounds(
                LatLngBounds.Builder().include(
                    northeast
                ).include(southwest).build()
            )
        );
        val builder = MapStatus.Builder()
        builder.target(LatLng(28.249534,113.193378)).zoom(16.0f)
        bmapView.map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()))
        bmapView.map.setMaxAndMinZoomLevel(20F, 16F)
        bmapView.map.isMyLocationEnabled = true;

        val locationConfig = MyLocationConfiguration(
            MyLocationConfiguration.LocationMode.FOLLOWING,
            true,
            null,
            0,
            0
        )
        bmapView.map.setMyLocationConfiguration(locationConfig)

        val locData = MyLocationData.Builder()
            // 此处设置开发者获取到的方向信息，顺时针0-360
            .direction(90F).latitude(28.249534)
            .longitude(113.193378).build()

        Thread {
            Thread.sleep(1000)
            bmapView.map.setMyLocationData(locData)
        }.start()

        val listener: BaiduMap.OnMapClickListener = object : BaiduMap.OnMapClickListener {
            override fun onMapClick(point: LatLng?) {
                if (itemShow) {
                    toolbar.hide()
                    rvPlaylist.hide()
                } else {
                    toolbar.show()
                    rvPlaylist.show()
                }
                itemShow = !itemShow
            }
            override fun onMapPoiClick(mapPoi: MapPoi?) {}
        }
        bmapView.map.setOnMapClickListener(listener)

        btnPlay.setOnClickListener {
            playSound(adapter.selectedPosition)
        }
        btnStop.setOnClickListener { stopPlay() }
    }

    private fun processEvent(event: ReceiverEvent) {
        Log.e("ss", "data : ${event.data}")
        when (event.command) {
            Command.BESTPOSA -> {
                val bestPosa = event.data as BestPosa
                if (bestPosa.solStat != SolStat.SOL_COMPUTED || bestPosa.posType != PosType.NARROW_INT) return
                val trans = CoordinateTransformUtil.wgs84tobd09(bestPosa.lon, bestPosa.lat)
                processLocation(lat = trans[1], lon = trans[0])
            }
            Command.HEADINGA -> {
                val headinga = event.data as Headinga
                if (headinga.solStat != SolStat.SOL_COMPUTED || headinga.posType != PosType.NARROW_INT) return
                processLocation(headinga = headinga.heading.toFloat())
            }
            Command.GPYBM -> {
                val gpybm = event.data as Gpybm
                if (gpybm.locationType != LocationType.FIXED) return
                val trans = CoordinateTransformUtil.wgs84tobd09(gpybm.lon, gpybm.lat)
                processLocation(trans[1], trans[0], gpybm.heading.toFloat())
            }
        }
    }

    private fun processLocation(lat: Double = 0.0, lon: Double = 0.0, headinga: Float = 0.0F) {
        val location = bmapView.map.locationData
        val locData = MyLocationData.Builder()
            // 此处设置开发者获取到的方向信息，顺时针0-360
            .direction(if (headinga == 0.0F) location.direction else headinga)
            .latitude(if (lat == 0.0) location.latitude else lat)
            .longitude(if (lon == 0.0) location.longitude else lon)
            .build()
        bmapView.map.setMyLocationData(locData)

        executor.submit(CheckPointTask(lat, lon))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_connections, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect_bluetooth -> BluetoothUtils.toSelectDevice(this)
            R.id.menu_disconnect_bluetooth -> mBluetoothService.stop()
            //R.id.menu_connect_network -> RxBus.get().post(SendEvent(Command.DO_CONNECT))
            //R.id.menu_disconnect_network -> RxBus.get().post(SendEvent(Command.DO_DISCONNECT))
        }
        return true
    }

    private fun playSound(index: Int) {

        val path = "voice/${voicePathList[index]}"
        val am: AssetManager = resources.assets
        var afd: AssetFileDescriptor? = null
        try {
            afd = am.openFd(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        player.setAssetFileDescriptor(afd)
        player.start()
    }

    private fun stopPlay(){
        player.pause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == BluetoothUtils.REQUEST_CODE_SELECT_DEVICE && resultCode == Activity.RESULT_OK) {
            mBluetoothService.connect(data!!.extras!!.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)!!);
        }
        if (requestCode == BluetoothUtils.REQUEST_CODE_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "请打开蓝牙", Toast.LENGTH_SHORT).show()
            } else if (resultCode == RESULT_OK) {
                DeviceListActivity.startForResult(this)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        mDisposes.dispose()
        mBluetoothService.stop()
        super.onDestroy()
    }

    inner class CheckPointTask(val lat: Double, val lon: Double) : Runnable {
        override fun run() {
            val gsRT = PilingUtils.bl2gsXY(lat, lon)
            for ((index, bestposa) in gpsMap) {
                val gsFix = PilingUtils.bl2gsXY(bestposa.lat, bestposa.lon)
                if (PilingUtils.distance(gsRT, gsFix) <= 10) {
                    adapter.selectedPosition = index
                    playSound(index)
                    break
                }
            }
        }
    }

    inner class SimpleAdapter(private val clickListener: (position: Int) -> Unit) :
        RecyclerView.Adapter<ViewHolder>(), View.OnClickListener {
        var selectedPosition = -1
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.playlist_item, parent, false)
            view.setOnClickListener(this)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(holder){
                itemView.title.text = voiceNameList[position]
                itemView.setTag(R.id.position_tag, position)
                if(position == selectedPosition){
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.rippleMask
                        )
                    )
                }else{
                    itemView.setBackgroundResource(R.drawable.ripple_transparent_white)
                }
            }
        }

        override fun getItemCount() = voiceNameList.size

        override fun onClick(v: View?) {
            val position = v!!.getTag(R.id.position_tag) as Int
            selectedPosition = position
            notifyDataSetChanged()
            clickListener(position)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun toRawData(bm: Bitmap): ByteArray? {
        val size = bm.rowBytes * bm.height
        val b: ByteBuffer = ByteBuffer.allocate(size)

        bm.copyPixelsToBuffer(b)

        return try {
            b.array()
            //            b.get(bytes, 0, bytes.size)
        } catch (e: BufferUnderflowException) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
        val istr: InputStream
        var bitmap: Bitmap? = null
        try {
            istr = assets.open(filePath!!)
            bitmap = BitmapFactory.decodeStream(istr)
        } catch (e: IOException) {
            // handle exception
        }
        return bitmap
    }
}

