package com.suntech.iot.pattern.util

import android.util.Log
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by rightsna on 2016. 5. 9..
 */
object OEEUtil {
    fun parseTime(dt_txt:String?) : DateTime {
        if (dt_txt==null || dt_txt=="") return DateTime()
        return DateTimeFormat.forPattern("HH:mm:ss").parseDateTime(dt_txt)
    }

    fun parseTime2(dt_txt:String?) : DateTime {
        if (dt_txt==null || dt_txt=="") return DateTime()
        return DateTimeFormat.forPattern("HH:mm").parseDateTime(dt_txt)
    }

    fun parseDate(dt_txt:String?) : DateTime {
        if (dt_txt==null || dt_txt=="") return DateTime()
        return DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(dt_txt)
    }

    fun parseDateTime(dt_txt:String?) : DateTime {
        if (dt_txt==null || dt_txt=="") return DateTime()
        return DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(dt_txt)
    }

    fun LogWrite(txt: String="No values", title:String="Watching Data") {
        Log.e(title, "--[ " + title + " ]-------------------------------------")
        Log.e(title, "" + txt.toString())
        Log.e(title, "--------------------------------------------------------")
    }
}