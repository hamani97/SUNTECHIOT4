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
        editor.apply()
    }
    fun getBoolean(ctx: Context, key: String): Boolean {
        return ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getBoolean(key, false)
    }

    fun setInt(ctx: Context, key: String, data: Int) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putInt(key, data)
        editor.apply()
    }
    fun getInt(ctx: Context, key: String): Int {
        return ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getInt(key, 0)
    }

    fun setFloat(ctx: Context, key: String, data: Float) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putFloat(key, data)
        editor.apply()
    }
    fun getFloat(ctx: Context, key: String): Float {
        return ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getFloat(key, 0f)
    }

    fun setString(ctx: Context, key: String, data: String) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putString(key, data)
        editor.apply()
    }
    fun getString(ctx: Context, key: String): String {
        val data = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getString(key, "")
        return data ?: ""
    }

    fun setStringSet(ctx: Context, key: String, data: Set<String>) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putStringSet(key, data)
        editor.apply()
    }
    fun getStringSet(ctx: Context, key: String): Set<String> {
        return ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getStringSet(key, setOf())
    }

    fun setJSONArray(ctx: Context, key: String, data: JSONArray) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putString(key, data.toString())
        editor.apply()
    }
    fun getJSONArray(ctx: Context, key: String): JSONArray {
        val data = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getString(key, "[]")
        try {
            return JSONArray(data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return JSONArray()
    }

    fun setJSONObject(ctx: Context, key: String, data: JSONObject) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.putString(key, data.toString())
        editor.apply()
    }
    fun getJSONObject(ctx: Context, key: String): JSONObject? {
        val data = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).getString(key, "{}")
        try {
            return JSONObject(data)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    fun remove(ctx: Context, key: String) {
        val editor = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE).edit()
        editor.remove(key)
        editor.apply()
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

    // 모든 설정 내용 출력
    fun printPreferences(ctx: Context) {
        val prefs = ctx.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE)
        val keys = prefs.all
        for ((key, value) in keys) {
            Log.d("LocalStorage", key + ": " + value.toString())
        }
    }
}
