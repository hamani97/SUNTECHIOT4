package com.suntech.iot.pattern.util

import android.content.Context
import java.io.File

/**
 * Created by rightsna on 2016. 5. 9..
 */
object UtilFile {

    /**
     * @author wyddn
     * *
     * @param getExternalStorageDirectory
     * *
     * @return 킷캣 이후 앱에 할당된 통일된 파일 경로
     */
    private fun appDefaultPathFile(context: Context, path: String): File {

        val sb = StringBuilder()
        sb.append(path)
        sb.append(File.separator)
        sb.append("Android")
        sb.append(File.separator)
        sb.append("data")
        sb.append(File.separator)
        sb.append(context.packageName)
        sb.append(File.separator)
        sb.append("files")

        return File(sb.toString())
    }

    fun getFileExt(fileName: String): String {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
    }
}
