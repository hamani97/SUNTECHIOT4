package com.suntech.iot.pattern.common

/**
 * Created by rightsna on 2016. 11. 17..
 */
object Constants {
    val CONTEXT_PATH = "/"
    val HOST_URL = "http://1.255.57.123"          // both -> distribute server

    val API_SERVER_URL = HOST_URL + CONTEXT_PATH

    val DOWNTIME_FIRST = 10*60000           /// 현재 shift의 첫생산인데 지각인경우 downtime

    val BR_ADD_COUNT = "br.add.count"

    val DEMO_VERSION = false           /// 데모용 앱
}