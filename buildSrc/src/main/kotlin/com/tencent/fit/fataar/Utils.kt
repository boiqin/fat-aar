package com.tencent.fit.fataar

import org.gradle.api.Project
import java.io.File

import java.lang.ref.WeakReference
import kotlin.math.min

/**
 * Utils
 * @author kezong @since 2018-12-10 17:28
 * modify by alexbchen on 2019/10/31
 */
class Utils {
    companion object {
        private var projectRef: WeakReference<Project>? = null

        fun setProject(project: Project) {
            projectRef = WeakReference(project)
        }

        fun logError(msg: String) {
            val p = projectRef?.get()
            p?.logger?.error("[fat-aar]${msg}")
        }

        fun logInfo(msg: String) {
            val p = projectRef?.get()
            p?.logger?.info("[fat-aar]${msg}")
        }

        fun logAnytime(msg: String) {
            //TODO
            val p = projectRef?.get()
            p?.logger?.debug("[fat-aar]${msg}")
        }

        fun showDir(indent: Int, file: File) {
            for (i in 0..indent.minus(1)) {
                print('-')
            }
            println(file.name + " " + file.length())
            if (file.isDirectory) {
                val files = file.listFiles()
                files.forEach {
                    showDir(indent + 4, it)
                }
            }
        }

        fun compareVersion(v1: String, v2: String): Int {

            val versionArray1 = v1.split("\\.")
            val versionArray2 = v2.split("\\.")
            var idx = 0
            val minLength = min(versionArray1.size, versionArray2.size)
            var diff = 0
            while (idx < minLength) {
                diff = versionArray1[idx].length - versionArray2[idx].length//先比较长度
                if (diff != 0) {
                    break
                }
                diff = versionArray1[idx].compareTo(versionArray2[idx])//再比较字符
                if (diff != 0) {
                    break
                }
                ++idx
            }
            //如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
            diff = if (diff != 0) diff else (versionArray1.size - versionArray2.size)
            return diff

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


    }

}