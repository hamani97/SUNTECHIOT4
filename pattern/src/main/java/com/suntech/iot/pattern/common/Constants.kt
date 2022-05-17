package com.suntech.iot.pattern.common

/**
 * Created by rightsna on 2016. 11. 17..
 */
object Constants {

    // 1.5.8 버전까지 사용
//    val CONTEXT_PATH = "/"
//    val HOST_URL = "http://1.255.57.123"          // both -> distribute server
//    val API_URL = "/api/query.php"
//
//    val API_CHECK_DEVICE = "check_device"
//    val API_SET_START_TIME = "set_start_time"
//    val API_GET_COLOR_CODE = "get_color_code"
//    val API_GET_PUSH_TEXT = "get_push_text"
//    val API_GET_DOWN_CHECK_TIME = "get_down_check_time"
//    val API_GET_COMPONENT = "get_component"
//    val API_GET_DESIGN = "get_design"

    // 2.0.1 버전부터 사용
    val HOST_URL = "http://155.68.178.250"
    val CONTEXT_PATH = "/hwi/query.php"

    val API_SERVER_URL = HOST_URL + CONTEXT_PATH
    val DOWNTIME_FIRST = 10*60000           /// 현재 shift의 첫생산인데 지각인경우 downtime
    val BR_ADD_COUNT = "br.add.count"
    val DEMO_VERSION = false           /// 데모용 앱

    val arr_pieces = arrayListOf (
        "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"
    )
    val arr_pairs = arrayListOf (
        "¼", "½", "¾",
        "1", "1 ¼", "1 ½", "1 ¾",
        "2", "2 ½",
        "3", "3 ½",
        "4", "4 ½",
        "5", "5 ½",
        "6", "6 ½",
        "7", "7 ½",
        "8", "8 ½",
        "9", "9 ½",
        "10")
    val arr_pairs_value = arrayListOf (
        0.25f, 0.5f, 0.75f,
        1f, 1.25f, 1.5f, 1.75f,
        2f, 2.5f,
        3f, 3.5f,
        4f, 4.5f,
        5f, 5.5f,
        6f, 6.5f,
        7f, 7.5f,
        8f, 8.5f,
        9f, 9.5f,
        10f
    )
}