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
class DBHelperForCount
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
            return db.rawQuery("select * from stitch", null)
        }

    init {
        _openHelper = DBHelperForCount(context)
    }

    /**
     * This is an internal class that handles the creation of all database tables
     */
    internal inner class DBHelperForCount(context: Context) : SQLiteOpenHelper(context, "stitch.db", null, 1) {

        override fun onCreate(db: SQLiteDatabase) {
            val sql = "create table stitch (_id integer primary key autoincrement, " +
                    "work_idx text, value text, " +
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
        val sql = "select work_idx, value, dt " +
                "from stitch where idx = ?"
        val cur = db.rawQuery(sql, arrayOf(idx))
        if (cur.moveToNext()) {
            row.put("work_idx", cur.getString(0))
            row.put("value", cur.getString(1))
            row.put("dt", cur.getString(2))
        }
        cur.close()
        db.close()
        return row
    }

    fun gets():  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null

        val sql = "select _id, work_idx, value, dt " +
                "from stitch order by dt desc"
        val cur = db.rawQuery(sql, arrayOf())
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("_id", cur.getString(0))
            row.put("work_idx", cur.getString(1))
            row.put("value", cur.getString(2))
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
    fun add(work_idx: String, value:String): Long {
        val db = _openHelper.writableDatabase ?: return 0
        val row = ContentValues()
        row.put("work_idx", work_idx)
        row.put("value", value)
        row.put("dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
        val id = db.insert("stitch", null, row)
        db.close()
        return id
    }

    fun delete() {
        val db = _openHelper.writableDatabase ?: return
        db.delete("stitch", "", arrayOf())
        db.close()
    }
}