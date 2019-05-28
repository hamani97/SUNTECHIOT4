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
 * Modified by hamani on 2019. 5. 1..
 */
class DBHelperForReport
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
            return db.rawQuery("select * from report", null)
        }

    init {
        _openHelper = DBHelperForReport(context)
    }

    /**
     * This is an internal class that handles the creation of all database tables
     */
    internal inner class DBHelperForReport(context: Context) : SQLiteOpenHelper(context, "report8.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            val sql = "create table report (" +
                    "_id integer primary key autoincrement, " +
                    "date text, " +
                    "houly text, " +
                    "shift_idx text, " +
                    "actual int default 0, " +
                    "dt DATE default CURRENT_TIMESTAMP)"

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
        val sql = "select date, houly, shift_idx, actual, dt " +
                "from report where _id = ?"
        val cur = db.rawQuery(sql, arrayOf(idx))
        if (cur.moveToNext()) {
            row.put("date", cur.getString(0))
            row.put("houly", cur.getString(1))
            row.put("shift_idx", cur.getString(2))
            row.put("actual", cur.getInt(3))
            row.put("dt", cur.getString(4))
        }
        cur.close()
        db.close()
        return row
    }

    operator fun get(date: String, houly:String, shift_idx: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select _id, houly, actual " +
                "from report where date = ? and houly = ? and shift_idx = ?"
        val cur = db.rawQuery(sql, arrayOf(date, houly, shift_idx))
        if (cur.moveToNext()) {
            row.put("idx", cur.getString(0))
            row.put("actual", cur.getInt(2))
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

        val sql = "select _id, date, houly, shift_idx, actual, dt from report order by dt desc"
        val cur = db.rawQuery(sql, arrayOf())
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("idx", cur.getString(0))
            row.put("date", cur.getString(1))
            row.put("houly", cur.getString(2))
            row.put("shift_idx", cur.getString(3))
            row.put("actual", "" + cur.getInt(4))
            row.put("dt", cur.getString(5))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun gets(date:String, shift_idx:String):  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null

        val sql = "select _id, houly, actual, dt from report where date = ? and shift_idx = ? order by houly asc "
        val cur = db.rawQuery(sql, arrayOf(date, shift_idx))
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("idx", cur.getString(0))
            row.put("houly", cur.getString(1))
            row.put("actual", "" + cur.getInt(2))
            row.put("dt", cur.getString(3))
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
    fun add(date:String, houly:String, shift_idx:String, actual:Int): Long {
        val db = _openHelper.writableDatabase ?: return 0
        val row = ContentValues()
        row.put("date", date)
        row.put("houly", houly)
        row.put("shift_idx", shift_idx)
        row.put("actual", actual)
        row.put("dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
        val id = db.insert("report", null, row)
        db.close()
        return id
    }

    fun updateActual(_idx: String, actual:Int) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("actual", actual)
        db.update("report", row, "_id = ?", arrayOf(_idx))
        db.close()
    }

    fun deleteByDT(dt:String) {
        val db = _openHelper.writableDatabase ?: return
        db.delete("report", "date = ?", arrayOf(dt))
        db.close()
    }

    fun delete() {
        val db = _openHelper.writableDatabase ?: return
        db.delete("report", "", arrayOf())
        db.close()
    }
}