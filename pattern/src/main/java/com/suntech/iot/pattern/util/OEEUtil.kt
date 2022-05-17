package com.suntech.iot.pattern.util

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.json.JSONArray
import kotlin.math.floor

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

    fun computeTarget(millis: Float, ct: Int, pieces: Int, pairs: Float) : Float {
        val total_cycle_time = ct * pieces           // 몇초에 한번의 제품을 만드는지
        return if (total_cycle_time != 0) floor(floor(millis / total_cycle_time) * pairs * 100) / 100 else 0f   // 소숫점을 버릴때 사용
//        return if (total_cycle_time != 0) floor((millis / total_cycle_time) * pairs * 100) / 100 else 0f      // 소숫점까지 구할때 사용
    }

    /*  handleWorkData()
     *  작업 시간을 검사한다.
     *  첫 작업 시간보다 작은 시간이 보일경우 하루가 지난것이므로 1일을 더한다.
     *
     *  {
            "idx": "1860",
            "date": "2020-04-06",
            "available_stime": "08:25",
            "available_etime": "20:25",
            "planned1_stime": "10:25",
            "planned1_etime": "11:25",
            "planned2_stime": "15:25",
            "planned2_etime": "16:25",
            "planned3_stime": "",
            "planned3_etime": "",
            "over_time": "0",
            "line_idx": "1",
            "line_name": "CELL 41",
            "shift_idx": "1",
            "shift_name": "SHIFT 1",
            "target": "600"
        }
     */
    fun handleWorkData(list: JSONArray) : JSONArray {
        var shift_stime = DateTime()
        for (i in 0..(list.length() - 1)) {
            val item = list.getJSONObject(i)

            val date = item["date"].toString()  // 2019-04-05
            if (i==0) { // 첫시간 기준
                shift_stime = parseDateTime(date + " " + item["available_stime"] + ":00")   // 2019-04-05 06:01:00  (available_stime = 06:01)
            }

            var work_stime = parseDateTime(date + " " + item["available_stime"] + ":00")    // 2019-04-05 06:01:00
            var work_etime = parseDateTime(date + " " + item["available_etime"] + ":00")    // 2019-04-05 14:00:00

            if (item["over_time"] != "0") work_etime = work_etime.plusHours(item["over_time"].toString().toInt())

            val planned1_stime_txt = date + " " + if (item["planned1_stime"] == "") "00:00:00" else item["planned1_stime"].toString() + ":00"   // 2019-04-05 11:30:00
            val planned1_etime_txt = date + " " + if (item["planned1_etime"] == "") "00:00:00" else item["planned1_etime"].toString() + ":00"   // 2019-04-05 13:00:00
            val planned2_stime_txt = date + " " + if (item["planned2_stime"] == "") "00:00:00" else item["planned2_stime"].toString() + ":00"   // 2019-04-05 00:00:00
            val planned2_etime_txt = date + " " + if (item["planned2_etime"] == "") "00:00:00" else item["planned2_etime"].toString() + ":00"   // 2019-04-05 00:00:00

            var planned1_stime_dt = parseDateTime(planned1_stime_txt)
            var planned1_etime_dt = parseDateTime(planned1_etime_txt)
            var planned2_stime_dt = parseDateTime(planned2_stime_txt)
            var planned2_etime_dt = parseDateTime(planned2_etime_txt)

            // 첫 시작시간 보다 작은 값이면 하루가 지난 날짜임
            // 종료 시간이 시작 시간보다 작은 경우도 하루가 지난 날짜로 처리
            if (shift_stime.secondOfDay > work_stime.secondOfDay) work_stime = work_stime.plusDays(1)
            if (shift_stime.secondOfDay > work_etime.secondOfDay || work_stime.secondOfDay >= work_etime.secondOfDay) work_etime = work_etime.plusDays(1)
            if (shift_stime.secondOfDay > planned1_stime_dt.secondOfDay) planned1_stime_dt = planned1_stime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned1_etime_dt.secondOfDay || planned1_stime_dt.secondOfDay >= planned1_etime_dt.secondOfDay) planned1_etime_dt = planned1_etime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned2_stime_dt.secondOfDay) planned2_stime_dt = planned2_stime_dt.plusDays(1)
            if (shift_stime.secondOfDay > planned2_etime_dt.secondOfDay || planned2_stime_dt.secondOfDay >= planned2_etime_dt.secondOfDay) planned2_etime_dt = planned2_etime_dt.plusDays(1)

            item.put("work_stime", work_stime.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("work_etime", work_etime.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned1_stime_dt", planned1_stime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned1_etime_dt", planned1_etime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned2_stime_dt", planned2_stime_dt.toString("yyyy-MM-dd HH:mm:ss"))
            item.put("planned2_etime_dt", planned2_etime_dt.toString("yyyy-MM-dd HH:mm:ss"))
//            Log.e("new list", ""+item.toString())
        }
        return list
    }
}