package com.boiqin.fataar

import java.io.File

/**
 * Utils
 * @author kezong @since 2018-12-10 17:28
 * modify by alexbchen on 2019/10/31
 */
object Utils {
    private const val TAG = "fataar-"
    private const val ANSI_RESET = "\u001B[0m"
    private const val ANSI_RED = "\u001B[31m"
    private const val ANSI_GREEN = "\u001B[32m"
    private const val ANSI_YELLOW = "\u001B[33m"
    private const val ANSI_BLUE = "\u001B[34m"

    fun logError(msg: String) {
        println(ANSI_RED + TAG + msg + ANSI_RESET)
    }

    fun logInfo(msg: String) {
        println(ANSI_GREEN + TAG + msg + ANSI_RESET)
    }

    fun logDebug(msg: String) {
        println(ANSI_YELLOW + TAG + msg + ANSI_RESET)
    }

    fun compareVersion(v1: String, v2: String): Int {
        val s1 = v1.split(".")
        val s2 = v2.split(".")
        val len1 = s1.size
        val len2 = s2.size
        var i = 0
        var j = 0
        while (i < len1 && j < len2) {
            if (s1[i].toInt() > s2[j].toInt()) {
                return 1
            } else if (s1[i].toInt() < s2[j].toInt()) {
                return -1
            }
            i++
            j++
        }
        while (i < len1) {
            if (s1[i].toInt() != 0) {
                return 1
            }
            i++
        }
        while (j < len2) {
            if (s2[j].toInt() != 0) {
                return -1
            }
            j++
        }
        return 0

    }

    fun deleteDir(dir: File) {
        if (!dir.exists()) {
            return
        }
        if(dir.isDirectory) {
            val fileList = dir.listFiles()
            fileList?.forEach { file ->
                if (file.isFile) {
                    file.delete() // 删除所有文件
                } else if (file.isDirectory) {
                    deleteDir(file) // 递规的方式删除文件夹
                }
            }
        }
        dir.delete()// 删除目录本身
    }
}