package com.suntech.iot.pattern.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.joda.time.DateTime
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
    internal inner class DBHelperForDownTime(context: Context) : SQLiteOpenHelper(context, "downtime.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            val sql = "create table downtime (_id integer primary key autoincrement, " +
                    "work_idx text, " +
                    "design_idx text, " +
                    "idx text, " +
                    "shift_id text, " +
                    "shift_name text, " +
                    "completed text, " +
                    "list text, " +
                    "start_dt DATE default CURRENT_TIMESTAMP, " +
                    "end_dt DATE)"

            db.execSQL(sql)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    /**
     * Return values for a single row with the specified id
     * @param id The unique id for the row o fetch
     * @return All column values are stored as properties in the ContentValues object
     */
    operator fun get(idx: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select idx, work_idx, design_idx, shift_id, shift_name, completed, list, start_dt, end_dt " +
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
        }
        cur.close()
        db.close()
        return row
    }

    fun gets():  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null

        val sql = "select _id, idx, work_idx, design_idx, shift_id, shift_name, completed, list, start_dt, end_dt " +
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
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun counts_for_notcompleted():  Int {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return -1

        val sql = "select _id from downtime where completed = ?"
        val cur = db.rawQuery(sql, arrayOf("N"))
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("_id", cur.getString(0))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr.size
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

    fun updateEnd(idx: String, list:String) {

        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("completed", "Y")
        row.put("list", list)
        row.put("end_dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
        db.update("downtime", row, "idx = ?", arrayOf(idx))
        db.close()
    }

    fun delete(idx: String) {
        val db = _openHelper.writableDatabase ?: return
        db.delete("downtime", "idx = ?", arrayOf(idx))
        db.close()
    }

    fun delete() {
        val db = _openHelper.writableDatabase ?: return
        db.delete("downtime", "", arrayOf())
        db.close()
    }
}