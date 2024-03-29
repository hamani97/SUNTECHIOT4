package com.suntech.iot.pattern.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

/**
 * Created by rightsna on 2018. 1. 2..
 */
class DBHelperForDownTime
/**
 * Construct a new database helper object
 * @param context The current context for the application or activity
 */
    (context: Context) {
    private val _openHelper: SQLiteOpenHelper

    /**
     * Return a cursor object with all rows in the table.
     * @return A cursor suitable for use in a SimpleCursorAdapter
     */
    val all: Cursor?
        get() {
            val db = _openHelper.readableDatabase ?: return null
            return db.rawQuery("select * from downtime", null)
        }

    init {
        _openHelper = DBHelperForDownTime(context)
    }

    /**
     * This is an internal class that handles the creation of all database tables
     */
    internal inner class DBHelperForDownTime(context: Context) : SQLiteOpenHelper(context, "downtime.db", null, 5) {

        override fun onCreate(db: SQLiteDatabase) {
            val sql="create table downtime (_id integer primary key autoincrement, " +
                    "work_idx text, " +
                    "design_idx text, " +
                    "idx text, " +          // 서버 DB의 index
                    "shift_id text, " +
                    "shift_name text, " +
                    "completed text, " +
                    "list text, " +
                    "millis int default 0, " +          // 초
                    "real_millis int default 0, " +     // 휴식시간을 뺀 초
                    "target float default 0.0, " +       // 현디자인의 ct로 계산된 타겟수
                    "start_dt DATE default CURRENT_TIMESTAMP, " +
                    "end_dt DATE)"

            db.execSQL(sql)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("drop table if exists downtime")
            onCreate(db)
        }
    }

    /**
     * Return values for a single row with the specified id
     * @param id The unique id for the row o fetch
     * @return All column values are stored as properties in the ContentValues object
     */
    operator fun get(idx: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select idx, work_idx, design_idx, shift_id, shift_name, completed, list, start_dt, end_dt, millis, real_millis, target " +
                "from downtime where idx = ?"
        val cur = db.rawQuery(sql, arrayOf(idx))
        if (cur.moveToNext()) {
            row.put("idx", cur.getString(0))
            row.put("work_idx", cur.getString(1))
            row.put("design_idx", cur.getString(2))
            row.put("shift_id", cur.getString(3))
            row.put("shift_name", cur.getString(4))
            row.put("completed", cur.getString(5))
            row.put("list", cur.getString(6))
            row.put("start_dt", cur.getString(7))
            row.put("end_dt", cur.getString(8))
            row.put("millis", cur.getInt(9))
            row.put("real_millis", cur.getInt(10))
            row.put("target", cur.getFloat(11))
        }
        cur.close()
        db.close()
        return row
    }

    fun getLocalIdx(idx: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select idx, work_idx, design_idx, shift_id, shift_name, completed, list, start_dt, end_dt, millis, real_millis, target " +
                "from downtime where _id = ?"
        val cur = db.rawQuery(sql, arrayOf(idx))
        if (cur.moveToNext()) {
            row.put("idx", cur.getString(0))
            row.put("work_idx", cur.getString(1))
            row.put("design_idx", cur.getString(2))
            row.put("shift_id", cur.getString(3))
            row.put("shift_name", cur.getString(4))
            row.put("completed", cur.getString(5))
            row.put("list", cur.getString(6))
            row.put("start_dt", cur.getString(7))
            row.put("end_dt", cur.getString(8))
            row.put("millis", cur.getInt(9))
            row.put("real_millis", cur.getInt(10))
            row.put("target", cur.getFloat(11))
        }
        cur.close()
        db.close()
        return row
    }

    fun gets():  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null
        val sql = "select _id, idx, work_idx, design_idx, shift_id, shift_name, completed, list, start_dt, end_dt, millis, real_millis, target " +
                "from downtime order by start_dt desc"
        val cur = db.rawQuery(sql, arrayOf())
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("_id", cur.getString(0))
            row.put("idx", cur.getString(1))
            row.put("work_idx", cur.getString(2))
            row.put("design_idx", cur.getString(3))
            row.put("shift_id", cur.getString(4))
            row.put("shift_name", cur.getString(5))
            row.put("completed", cur.getString(6))
            row.put("list", cur.getString(7))
            row.put("start_dt", cur.getString(8))
            row.put("end_dt", cur.getString(9))
            row.put("millis", cur.getString(10))
            row.put("real_millis", cur.getString(11))
            row.put("target", "" + cur.getFloat(12))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun gets(work_idx: String):  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null
        val sql = "select _id, idx, work_idx, design_idx, shift_id, shift_name, completed, list, start_dt, end_dt, millis, real_millis, target " +
                "from downtime where work_idx = ? order by start_dt desc"
        val cur = db.rawQuery(sql, arrayOf(work_idx))
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("_id", cur.getString(0))
            row.put("idx", cur.getString(1))
            row.put("work_idx", cur.getString(2))
            row.put("design_idx", cur.getString(3))
            row.put("shift_id", cur.getString(4))
            row.put("shift_name", cur.getString(5))
            row.put("completed", cur.getString(6))
            row.put("list", cur.getString(7))
            row.put("start_dt", cur.getString(8))
            row.put("end_dt", cur.getString(9))
            row.put("millis", cur.getString(10))
            row.put("real_millis", cur.getString(11))
            row.put("target", "" + cur.getFloat(12))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun count_start_dt(start_dt: String): Int {
        val db = _openHelper.readableDatabase ?: return -1
        val sql = "select count(*) from downtime where start_dt = ?"
        val cur = db.rawQuery(sql, arrayOf(start_dt))
        var cnt = -1
        if (cur.moveToNext()) {
            cnt = cur.getInt(0)
        }
        cur.close()
        db.close()
        return cnt
    }

//    fun counts_for_notcompleted():  Int {
//        var arr = ArrayList<HashMap<String, String>>()
//        val db = _openHelper.readableDatabase ?: return -1
//
//        val sql = "select _id from downtime where completed = ?"
//        val cur = db.rawQuery(sql, arrayOf("N"))
//        while (cur.moveToNext()) {
//            val row = HashMap<String, String>()
//            row.put("_id", cur.getString(0))
//            arr.add(row)
//        }
//        cur.close()
//        db.close()
//        return arr.size
//    }

    fun count_for_notcompleted(): Int {
        val db = _openHelper.readableDatabase ?: return -1
        val sql = "select count(*) from downtime where completed = ?"
        val cur = db.rawQuery(sql, arrayOf("N"))
        val cnt = if (cur.moveToNext()) cur.getInt(0) else 0
        cur.close()
        db.close()
        return cnt
    }

    fun count_all():  Int {
        val db = _openHelper.readableDatabase ?: return -1
        val sql = "select count(*) from downtime"
        val cur = db.rawQuery(sql, arrayOf())
        val cnt = if (cur.moveToNext()) cur.getInt(0) else 0
        cur.close()
        db.close()
        return cnt
    }

    fun sum_target_count(): Float {
        val db = _openHelper.readableDatabase ?: return 0f
        val sql = "select sum(target) as cnt from downtime"
        val cur = db.rawQuery(sql, arrayOf())
        val cnt =
            if (cur.moveToNext()) cur.getFloat(0)
            else 0f
        cur.close()
        db.close()
        return cnt
    }

    fun sum_real_millis_count(): Int {
        val db = _openHelper.readableDatabase ?: return 0
        val sql = "select sum(real_millis) as cnt from downtime"
        val cur = db.rawQuery(sql, arrayOf())
        val cnt =
            if (cur.moveToNext()) cur.getInt(0)
            else 0
        cur.close()
        db.close()
        return cnt
    }

    /**
     * Add a new row to the database table
     * @param title The title value for the new row
     * @param priority The priority value for the new row
     * @return The unique id of the newly added row
     */
    fun add(idx: String, work_idx: String, design_idx: String, shift_id:String, shift_name:String, start_dt:String): Long {
        val db = _openHelper.writableDatabase ?: return 0
        val row = ContentValues()
        row.put("idx", idx)
        row.put("work_idx", work_idx)
        row.put("design_idx", design_idx)
        row.put("shift_id", shift_id)
        row.put("shift_name", shift_name)
        row.put("completed", "N")
        row.put("start_dt", start_dt)
        val id = db.insert("downtime", null, row)
        db.close()
        return id
    }

    fun updateLastId(idx: String, last_id: String) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("idx", last_id)
        db.update("downtime", row, "_id = ?", arrayOf(idx))
        db.close()
    }

    fun updateEnd(idx: String, list:String, end_dt:String, millis:Int, real_millis:Int, target:Float) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("completed", "Y")
        row.put("list", list)
        row.put("end_dt", end_dt)
        row.put("millis", millis)
        row.put("real_millis", real_millis)
        row.put("target", target)
        db.update("downtime", row, "idx = ?", arrayOf(idx))
        db.close()
    }

    fun updateEndLocalIdx(idx: String, list:String, end_dt:String, millis:Int, real_millis:Int, target:Float) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("completed", "Y")
        row.put("list", list)
        row.put("end_dt", end_dt)
        row.put("millis", millis)
        row.put("real_millis", real_millis)
        row.put("target", target)
        db.update("downtime", row, "_id = ?", arrayOf(idx))
        db.close()
    }

    fun updateDidxTarget(idx: String, design_idx:String, target:Float) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("design_idx", design_idx)
        row.put("target", target)
        db.update("downtime", row, "idx = ?", arrayOf(idx))
        db.close()
    }

    fun delete(idx: String) {
        val db = _openHelper.writableDatabase ?: return
        db.delete("downtime", "idx = ?", arrayOf(idx))
        db.close()
    }

    fun deleteLocalIdx(idx: String) {
        val db = _openHelper.writableDatabase ?: return
        db.delete("downtime", "_id = ?", arrayOf(idx))
        db.close()
    }

    fun delete() {
        val db = _openHelper.writableDatabase ?: return
        db.delete("downtime", "", arrayOf())
        db.close()
    }

    fun delete_for_notcompleted() {
        val db = _openHelper.readableDatabase ?: return
        db.delete("downtime", "completed = ?", arrayOf("N"))
        db.close()
    }

    fun deleteOldData(date: String) {
        val db = _openHelper.writableDatabase ?: return
        db.delete("downtime", "start_dt < ?", arrayOf(date))
        db.close()
    }
}