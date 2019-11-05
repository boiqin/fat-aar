package com.boiqin.fataar

import org.gradle.api.Project
import java.io.File

import java.lang.ref.WeakReference
import kotlin.math.min

/**
 * Utils
 * @author kezong @since 2018-12-10 17:28
 * modify by alexbchen on 2019/10/31
 */
object Utils {
        private var projectRef: WeakReference<Project>? = null

        fun setProject(project: Project) {
            projectRef = WeakReference(project)
        }

        fun logError(msg: String) {
            val project = projectRef?.get()
            project?.logger?.error("fataar-${msg}")
        }

        fun logInfo(msg: String) {
            val p = projectRef?.get()
            p?.logger?.info("fataar-${msg}")
        }

        fun logAnytime(msg: String) {
            val p = projectRef?.get()
            p?.logger?.debug("[fat-aar]${msg}")
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

        /**
         * 删除文件夹
         * dirPath 文件路径
         */
        fun deleteDir(dirpath: String) {
            val dir = File(dirpath)
            deleteDir(dir)
        }

        fun deleteDir(dir: File) {
            if (dir.checkFile())
                return
            for (file in dir.listFiles()) {
                if (file.isFile)
                    file.delete() // 删除所有文件
                else if (file.isDirectory)
                    deleteDir(file) // 递规的方式删除文件夹
            }
            dir.delete()// 删除目录本身
        }

        fun File.checkFile(): Boolean {
            return !this.exists() || !this.isDirectory
        }

    fun main() {
        println(compareVersion("3.5.0","3.4.0"))
        println(compareVersion("3.5.0","3.4"))
        println(compareVersion("3.3.0","3.4"))
        println(compareVersion("3.3","3.4"))
        println(compareVersion("2.1.4","3.4"))
    }
}