package com.sunward.test

object SolStat {
    const val SOL_COMPUTED = "SOL_COMPUTED"
    const val INSUFFICIENT_OBS = "INSUFFICIENT_OBS"
    const val COLD_START = "COLD_START"
}

object PosType {
    const val NONE = "NONE"
    const val FIXEDPOS = "FIXEDPOS"
    const val SINGLE = "SINGLE"
    const val PSRDIFF = "PSRDIFF"
    const val NARROW_FLOAT = "NARROW_FLOAT"
    const val WIDE_INT = "WIDE_INT"
    const val NARROW_INT = "NARROW_INT"
    const val SUPER_WIDE_LANE = "SUPER WIDE-LANE"
    const val INVALID_FIX = "INVALID_FIX"
}

object LocationType {
    const val INVALID = 0       //定位无效
    const val FIXED_GPS = 1     //GPS固定
    const val DIFF = 2          //码差分
    const val FIXED = 4         //RTK模糊度固定解解算
    const val FLOAT = 5         //RTK模糊度浮点解解算
    const val ESTIMATING = 6    //正在估算
    const val INPUT_FIXED = 7   //人工输入固定值
    const val SUPER_WIDE = 8    //超宽巷模式
    const val SBAS = 9          //SBAS
}

data class Headinga(
    var serialNo: String = "",
    var solStat: String = SolStat.COLD_START,
    var posType: String = PosType.NONE,
    var length: Double = 0.0,
    var heading: Double = 0.0,
    var pitch: Double = 0.0,
    var solnSVs: Int = 0,
    var stnId: String = ""
)

data class BestPosa(
    var serialNo: String = "",
    var solStat: String = SolStat.COLD_START,
    var posType: String = PosType.NONE,
    var lat: Double = 0.0,//纬度 B
    var lon: Double = 0.0,//经度 l
    var hgt: Double = 0.0, //高度 h
    var solnSVs: Int = 0,
    var stnId: String = ""
)


data class PSRDOP(var hDop: Double = 0.0, var htDop: Double = 0.0)

data class Gpybm(
    var serialNo: String = "",
    var lat: Double = 0.0,//纬度 B
    var lon: Double = 0.0,//经度 l
    var hgt: Double = 0.0, //高度 h
    var heading: Double = 0.0,
    var pitch: Double = 0.0,
    var satNoUsed: Int = 0,
    var locationType: Int = 0
)

data class Point(var x: Double = 0.0, var y: Double = 0.0)