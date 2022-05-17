package com.suntech.iot.pattern.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.joda.time.DateTime

/**
 * Created by rightsna on 2018. 1. 2..
 * Modified by hamani on 2019. 5. 1..
 */
class DBHelperForTarget
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
            return db.rawQuery("select * from target", null)
        }

    init {
        _openHelper = DBHelperForTarget(context)
    }

    /**
     * This is an internal class that handles the creation of all database tables
     */
    internal inner class DBHelperForTarget(context: Context) : SQLiteOpenHelper(context, "target.db", null, 2) {

        override fun onCreate(db: SQLiteDatabase) {
            val sql="create table target (" +
                    "_id integer primary key autoincrement, " +
                    "date text, " +
                    "shift_idx text, " +
                    "shift_name text, " +
                    "target float, " +
                    "work_stime text, " +
                    "work_etime text, " +
                    "dt DATE default CURRENT_TIMESTAMP)"

            db.execSQL(sql)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("drop table if exists target")
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
        val sql = "select date, shift_idx, shift_name, target, work_stime, work_etime, dt from target where _id = ?"
        val cur = db.rawQuery(sql, arrayOf(idx))
        if (cur.moveToNext()) {
            row.put("date", cur.getString(0))
            row.put("shift_idx", cur.getString(1))
            row.put("shift_name", cur.getString(2))
            row.put("target", cur.getFloat(3))
            row.put("work_stime", cur.getString(4))
            row.put("work_etime", cur.getString(5))
            row.put("dt", cur.getString(6))
        }
        cur.close()
        db.close()
        return row
    }
    operator fun get(date: String, shift_idx: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select _id, shift_name, target, work_stime, work_etime from target where date = ? and shift_idx = ?"
        val cur = db.rawQuery(sql, arrayOf(date, shift_idx))
        if (cur.moveToNext()) {
            row.put("idx", cur.getString(0))
            row.put("shift_name", cur.getString(1))
            row.put("target", cur.getFloat(2))
            row.put("work_stime", cur.getString(3))
            row.put("work_etime", cur.getString(4))
            cur.close()
            db.close()
            return row
        } else {
            cur.close()
            db.close()
            return null
        }
    }

    fun gets():  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null
        val sql = "select _id, date, shift_idx, shift_name, target, work_stime, work_etime, dt from target order by shift_idx"
        val cur = db.rawQuery(sql, arrayOf())
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("idx", cur.getString(0))
            row.put("date", cur.getString(1))
            row.put("shift_idx", cur.getString(2))
            row.put("shift_name", cur.getString(3))
            row.put("target", "" + cur.getFloat(4))
            row.put("work_stime", cur.getString(5))
            row.put("work_etime", cur.getString(6))
            row.put("dt", cur.getString(7))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun gets(date: String):  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null
        val sql = "select _id, date, shift_idx, shift_name, target, work_stime, work_etime, dt from target where date = ? order by shift_idx"
        val cur = db.rawQuery(sql, arrayOf(date.toString()))
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("idx", cur.getString(0))
            row.put("date", cur.getString(1))
            row.put("shift_idx", cur.getString(2))
            row.put("shift_name", cur.getString(3))
            row.put("target", "" + cur.getFloat(4))
            row.put("work_stime", cur.getString(5))
            row.put("work_etime", cur.getString(6))
            row.put("dt", cur.getString(7))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    /**
     * Add a new row to the database table
     * @param title The title value for the new row
     * @param priority The priority value for the new row
     * @return The unique id of the newly added row
     */
    fun add(date:String, shift_idx:String, shift_name:String, target:Float, work_stime:String, work_etime:String): Long {
        val db = _openHelper.writableDatabase ?: return 0
        val row = ContentValues()
        row.put("date", date)
        row.put("shift_idx", shift_idx)
        row.put("shift_name", shift_name)
        row.put("target", target)
        row.put("work_stime", work_stime)
        row.put("work_etime", work_etime)
        row.put("dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
        val id = db.insert("target", null, row)
        db.close()
        return id
    }
    fun update(_idx: String, shift_name:String, target:Float, work_stime:String, work_etime:String) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("shift_name", shift_name)
        row.put("target", target)
        row.put("work_stime", work_stime)
        row.put("work_etime", work_etime)
        db.update("target", row, "_id = ?", arrayOf(_idx))
        db.close()
    }
    fun replace(date: String, shift_idx: String, shift_name:String, target:Float, work_stime:String, work_etime:String) {
        val db = _openHelper.readableDatabase ?: return
        val sql = "select _id from target where date = ? and shift_idx = ?"
        val cur = db.rawQuery(sql, arrayOf(date, shift_idx))
        if (cur.moveToNext()) {
            val idx = cur.getString(0).toLong()
            val row = ContentValues()
            row.put("shift_name", shift_name)
            row.put("target", target)
            row.put("work_stime", work_stime)
            row.put("work_etime", work_etime)
            db.update("target", row, "_id = ?", arrayOf(idx.toString()))
        } else {
            val row = ContentValues()
            row.put("date", date)
            row.put("shift_idx", shift_idx)
            row.put("shift_name", shift_name)
            row.put("target", target)
            row.put("work_stime", work_stime)
            row.put("work_etime", work_etime)
            row.put("dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
            db.insert("target", null, row)
        }
        cur.close()
        db.close()
    }

    fun deleteByDT(dt:String) {
        val db = _openHelper.writableDatabase ?: return
        db.delete("target", "date = ?", arrayOf(dt))
        db.close()
    }
    fun delete() {
        val db = _openHelper.writableDatabase ?: return
        db.delete("target", "", arrayOf())
        db.close()
    }
}