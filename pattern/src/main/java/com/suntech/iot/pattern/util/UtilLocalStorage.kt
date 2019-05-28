package com.suntech.iot.pattern.util

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * Created by rightsna on 2016. 5. 9..
 */
object UtilLocalStorage {

    private val APP_KEY = "app"

    fun setBoolean(ctx: Context, key: String, data: Boolean) {
//        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putBoolean(key, data)
        editor.commit()
    }
    fun getBoolean(ctx: Context, key: String): Boolean {
        return ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getBoolean(key, false)
    }

    fun setInt(ctx: Context, key: String, data: Int) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putInt(key, data)
        editor.commit()
    }
    fun getInt(ctx: Context, key: String): Int {
        return ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getInt(key, 0)
    }

    fun setFloat(ctx: Context, key: String, data: Float) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putFloat(key, data)
        editor.commit()
    }
    fun getFloat(ctx: Context, key: String): Float {
        return ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getFloat(key, 0f)
    }

    fun setString(ctx: Context, key: String, data: String) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putString(key, data)
        editor.commit()
    }
    fun getString(ctx: Context, key: String): String {
        var data = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getString(key, "")
        if (data == null) data = ""
        return data
    }

    fun setStringSet(ctx: Context, key: String, data: Set<String>) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putStringSet(key, data)
        editor.commit()
    }
    fun getStringSet(ctx: Context, key: String): Set<String> {
        return ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getStringSet(key, setOf())
    }

    fun setJSONArray(ctx: Context, key: String, data: JSONArray) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putString(key, data.toString())
        editor.commit()
    }
    fun getJSONArray(ctx: Context, key: String): JSONArray {
        val data = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getString(key, "[]")
        try {
            val jsons = JSONArray(data)
            return jsons
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return JSONArray()
    }

    fun setJSONObject(ctx: Context, key: String, data: JSONObject) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putString(key, data.toString())
        editor.commit()
    }
    fun getJSONObject(ctx: Context, key: String): JSONObject? {
        val data = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getString(key, "{}")
        try {
            val json = JSONObject(data)
            return json
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    fun remove(ctx: Context, key: String) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.remove(key)
        editor.commit()
    }

    // 설정된 모든 내용을 출력해봄
    fun printPreferences(ctx: Context) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val keys = prefs.all
        for ((key, value) in keys) {
            Log.d("LocalStorage", key + ": " + value.toString())
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        return dir!!.delete()
    }
}
