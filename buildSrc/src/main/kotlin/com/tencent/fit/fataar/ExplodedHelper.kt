package com.tencent.fit.fataar

import org.gradle.api.Project
import java.io.File

/**
 * process jars and classes
 * Created by Vigi on 2017/1/20.
 * Modified by kezong on 2018/12/18
 * Add by alexbchen on 2019/10/31
 */
/**
 * process jars and classes
 */
object ExplodedHelper {

    /**
     *  将androidLibraries中的jar文件 复制到主工程的libs目录下
     *
     *  将jarFiles(java工程生成的jar包) 复制到主工程的libs目录下
     */
        fun processLibsIntoLibs(project: Project,
                                androidLibraries: Collection<AndroidArchiveLibrary>,
                                jarFiles: Collection<File>, folderOut: File) {
            for (androidLibrary in androidLibraries) {
                if (!androidLibrary.rootFolder.exists()) {
                    Utils.logInfo("[warning]" + androidLibrary.rootFolder + " not found!")
                    continue
                }
                if (androidLibrary.localJars.isEmpty()) {
                    Utils.logInfo("Not found jar file, Library:${androidLibrary.name}")
                } else {
                    Utils.logInfo("Merge ${androidLibrary.name} jar file, Library:${androidLibrary.name}")
                }
                androidLibrary.localJars.forEach {
                    Utils.logInfo(it.path)
                }
                project.copy {
                    it.from(androidLibrary.localJars)
                    it.into(folderOut)
                }
            }
            for (jarFile in jarFiles) {
                if (!jarFile.exists()) {
                    Utils.logInfo("[warning]$jarFile not found!")
                    continue
                }
                Utils.logInfo("copy jar from: " + jarFile + " to " + folderOut.absolutePath)
                project.copy {
                    it.from(jarFile)
                    it.into(folderOut)
                }
            }
        }

    /**
     * 将embed工程的class.jar 解压为class文件之后 整体复制到壳工程buildInterClassPathDir目录下
     * 如:xxx/build/intermediates/javac/WKDevDebug/classes
     */
        fun processClassesJarInfoClasses(project: Project,
                                         androidLibraries: Collection<AndroidArchiveLibrary>,
                                         folderOut: File) {
            Utils.logInfo("Merge ClassesJar")
            val allJarFiles = ArrayList<File>()
            for (androidLibrary in androidLibraries) {
                if (!androidLibrary.rootFolder.exists()) {
                    Utils.logInfo("[warning]" + androidLibrary.rootFolder + " not found!")
                    continue
                }
                allJarFiles.add(androidLibrary.classesJarFile)
            }
            for (jarFile in allJarFiles) {
                project.copy {
                    it.from(project.zipTree(jarFile))
                    it.into(folderOut)
                    it.exclude("META-INF/")
                }
            }
        }


        fun processLibsIntoClasses(project: Project,
                                   androidLibraries: Collection<AndroidArchiveLibrary>, jarFiles: Collection<File>,
                                   folderOut: File) {
            Utils.logInfo("Merge Libs")
            val allJarFiles = ArrayList<File>()
            for (androidLibrary in androidLibraries) {
                if (!androidLibrary.rootFolder.exists()) {
                    Utils.logInfo("[warning]" + androidLibrary.rootFolder + " not found!")
                    continue
                }
                Utils.logInfo("[androidLibrary]" + androidLibrary.name)
                allJarFiles.addAll(androidLibrary.localJars)
            }
            for (jarFile in jarFiles) {
                if (!jarFile.exists()) {
                    continue
                }
                allJarFiles.add(jarFile)
            }
            for (jarFile in allJarFiles) {
                project.copy {
                    it.from(project.zipTree(jarFile))
                    it.into(folderOut)
                    it.exclude("META-INF/")
                }
            }
        }

}
