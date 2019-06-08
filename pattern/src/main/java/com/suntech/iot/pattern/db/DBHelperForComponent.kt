package com.suntech.iot.pattern.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.joda.time.DateTime
import java.util.*

class DBHelperForComponent
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
            _openHelper = DBHelperForComponent(context)
        }

        /**
         * This is an internal class that handles the creation of all database tables
         */
        internal inner class DBHelperForComponent(context: Context) : SQLiteOpenHelper(context, "main_3.db", null, 1) {

            override fun onCreate(db: SQLiteDatabase) {
                val sql = "create table component (_id integer primary key autoincrement, " +
                        "wosno text, " +
                        "shift_id text, " +
                        "shift_name text, " +
                        "styleno text, " +
                        "model text, " +
                        "size text, " +
                        "target int, " +
                        "actual int, " +
                        "defective int, " +
                        "seq int, " +
                        "start_dt DATE default CURRENT_TIMESTAMP, " +
                        "end_dt DATE)"

                db.execSQL(sql)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

    operator fun get(work_idx: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select _id, wosno, shift_id, shift_name, styleno, model, size, target, actual, defective, seq,  start_dt, end_dt " +
                "from component where _id = ?"
        val cur = db.rawQuery(sql, arrayOf(work_idx.toString()))
        if (cur.moveToNext()) {
            row.put("work_idx", cur.getString(0))
            row.put("wosno", cur.getString(1))
            row.put("shift_id", cur.getString(2))
            row.put("shift_name", cur.getString(3))
            row.put("styleno", cur.getString(4))
            row.put("model", cur.getString(5))
            row.put("size", cur.getInt(6))
            row.put("target", cur.getInt(7))
            row.put("actual", cur.getInt(8))
            row.put("defective", cur.getInt(9))
            row.put("seq", cur.getInt(10))
            row.put("start_dt", cur.getString(11))
            row.put("end_dt", cur.getInt(12))
            cur.close()
            db.close()
            return row
        } else {
            cur.close()
            db.close()
            return null
        }
    }

    operator fun get(wosno: String, size: String): ContentValues? {
        val db = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val sql = "select _id, wosno, shift_id, shift_name, styleno, model, size, target, actual, defective, seq, start_dt, end_dt " +
                "from component where wosno = ? and size = ?"
        val cur = db.rawQuery(sql, arrayOf(wosno.toString(), size.toString()))
        if (cur.moveToNext()) {
            row.put("work_idx", cur.getString(0))
            row.put("wosno", cur.getString(1))
            row.put("shift_id", cur.getString(2))
            row.put("shift_name", cur.getString(3))
            row.put("styleno", cur.getString(4))
            row.put("model", cur.getString(5))
            row.put("size", cur.getInt(6))
            row.put("target", cur.getInt(7))
            row.put("actual", cur.getInt(8))
            row.put("defective", cur.getInt(9))
            row.put("seq", cur.getInt(10))
            row.put("start_dt", cur.getString(11))
            row.put("end_dt", cur.getInt(12))
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
        val sql = "select _id, wosno, shift_id, shift_name, styleno, model, size, target, actual, defective, seq, start_dt, end_dt " +
                    "from component where actual > 0 order by (target-actual) desc"
        val cur = db.rawQuery(sql, arrayOf())
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("work_idx", cur.getString(0))
            row.put("wosno", cur.getString(1))
            row.put("shift_id", cur.getString(2))
            row.put("shift_name", cur.getString(3))
            row.put("styleno", cur.getString(4))
            row.put("model", cur.getString(5))
            row.put("size", cur.getString(6))
            row.put("target", cur.getString(7))
            row.put("actual", cur.getString(8))
            row.put("defective", cur.getString(9))
            row.put("seq", cur.getString(10))
            row.put("start_dt", cur.getString(11))
            row.put("end_dt", cur.getString(12))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun gets_all_wos():  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null
        val sql = "select _id, wosno from component"
        val cur = db.rawQuery(sql, arrayOf())
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("work_idx", cur.getString(0))
            row.put("wosno", cur.getString(1))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun gets(wosno: String, size: String):  ArrayList<HashMap<String, String>>? {
        var arr = ArrayList<HashMap<String, String>>()
        val db = _openHelper.readableDatabase ?: return null
        val sql = "select _id, wosno, shift_id, shift_name, styleno, model, size, target, actual, defective, seq, start_dt, end_dt " +
                "from component where actual > 0 or (wosno = ? and size = ?) order by (target-actual) desc"
        val cur = db.rawQuery(sql, arrayOf(wosno.toString(), size.toString()))
        while (cur.moveToNext()) {
            val row = HashMap<String, String>()
            row.put("work_idx", cur.getString(0))
            row.put("wosno", cur.getString(1))
            row.put("shift_id", cur.getString(2))
            row.put("shift_name", cur.getString(3))
            row.put("styleno", cur.getString(4))
            row.put("model", cur.getString(5))
            row.put("size", cur.getString(6))
            row.put("target", cur.getString(7))
            row.put("actual", cur.getString(8))
            row.put("defective", cur.getString(9))
            row.put("seq", cur.getString(10))
            row.put("start_dt", cur.getString(11))
            row.put("end_dt", cur.getString(12))
            arr.add(row)
        }
        cur.close()
        db.close()
        return arr
    }

    fun add(wosno: String, shift_id:String, shift_name:String, styleno: String, model:String, size:String, target: Int, actual: Int, defective: Int, seq: Int): Long {
        val db = _openHelper.writableDatabase ?: return 0
        val row = ContentValues()
        row.put("wosno", wosno)
        row.put("shift_id", shift_id)
        row.put("shift_name", shift_name)
        row.put("styleno", styleno)
        row.put("model", model)
        row.put("size", size)
        row.put("target", target)
        row.put("actual", actual)
        row.put("defective", defective)
        row.put("seq", seq)
        row.put("start_dt", DateTime().toString("yyyy-MM-dd HH:mm:ss"))
        val id = db.insert("component", null, row)
        db.close()
        return id
    }

    fun updateWorkActual(work_idx: String, actual: Int) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("actual", actual)
        db.update("component", row, "_id = ?", arrayOf(work_idx.toString()))
        db.close()
    }

    fun updateDefective(work_idx: String, defective: Int) {
        val db = _openHelper.writableDatabase ?: return
        val row = ContentValues()
        row.put("defective", defective)
        db.update("component", row, "_id = ?", arrayOf(work_idx.toString()))
        db.close()
    }

    fun delete(_idx: String) {
        val db = _openHelper.writableDatabase ?: return
        db.delete("component", "_id = ?", arrayOf(_idx))
        db.close()
    }

    fun delete() {
        val db = _openHelper.writableDatabase ?: return
        db.delete("component", "", arrayOf())
        db.close()
    }

}