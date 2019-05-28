package com.suntech.iot.pattern.util

import java.util.*

/**
 * Created by rightsna on 2016. 5. 9..
 */
object UtilString {

    fun getRandomString(length: Int): String {
        val buffer = StringBuffer()
        val random = Random()
        val chars = "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z".split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in 0..length - 1) {
            buffer.append(chars[random.nextInt(chars.size)])
        }
        return buffer.toString()
    }
}