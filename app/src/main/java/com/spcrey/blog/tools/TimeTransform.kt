package com.spcrey.blog.tools

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimeTransform {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    fun getNow(): String {
        val currentDateTime = LocalDateTime.now()
        return currentDateTime.format(formatter)
    }

    fun transform(dataTimeString: String): String {
        val newDataTimeString = dataTimeString.substring(0, 10) + "T" + dataTimeString.substring(11)
        val currentDateTime = LocalDateTime.now()
        val dataTime = LocalDateTime.parse(newDataTimeString, formatter)
        return if (currentDateTime.year == dataTime.year) {
            if (currentDateTime.month == dataTime.month && currentDateTime.dayOfMonth == dataTime.dayOfMonth) {
                "${dataTime.hour}点${dataTime.minute}分"
            } else {
                if (currentDateTime.month.compareTo(dataTime.month) > 1) {
                    "${dataTime.monthValue}月${dataTime.dayOfMonth}日"
                } else {
                    "昨天"
                }
            }
        } else {
            "${dataTime.year}年"
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(transform("2022-07-29 12:22:09"))

    }
}