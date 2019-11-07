package com.boiqin.fataar

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Created by Vigi on 2017/2/16.
 * Modify by kezong on 2019/7/16.
 * Modify by alexbchen on 2019/11/05.
 */
class AndroidArchiveLibrary(private val project: Project, private val artifact: ResolvedArtifact, private val variantName: String) {

    val group: String
        get() = artifact.moduleVersion.id.group

    val name: String
        get() = artifact.moduleVersion.id.name

    val version: String
        get() = artifact.moduleVersion.id.version

    init {
        require("aar" == artifact.type) { "artifact must be aar type!" }
    }

    /**
     * 解压Aar之后放置aar内文件的位置
     */
    val rootFolder: File
        get() {
            val explodedRootDir = project.file(
                    project.buildDir.toString() + "/intermediates" + "/exploded-aar/")
            val id = artifact.moduleVersion.id
            return project.file(explodedRootDir.toString()
                    + "/" + id.group
                    + "/" + id.name
                    + "/" + id.version
                    + "/" + variantName)
        }

    val aidlFolder: File
        get() = File(rootFolder, "aidl")

    val assetsFolder: File
        get() = File(rootFolder, "assets")

    val classesJarFile: File
        get() = File(rootFolder, "classes.jar")

    val localJars: Collection<File>
        get() {
            val localJars = ArrayList<File>()
            val jarList = File(rootFolder, "libs").listFiles()
            if (jarList != null) {
                for (jars in jarList) {
                    if (jars.isFile && jars.name.endsWith(".jar")) {
                        localJars.add(jars)
                    }
                }
            }

            return localJars
        }

    val jniFolder: File
        get() = File(rootFolder, "jni")

    val resFolder: File
        get() = File(rootFolder, "res")

    val manifest: File
        get() = File(rootFolder, "AndroidManifest.xml")

    val lintJar: File
        get() = File(rootFolder, "lint.jar")

    val proguardRules: List<File>
        get() {
            val list = ArrayList<File>()
            list.add(File(rootFolder, "proguard-rules.pro"))
            list.add(File(rootFolder, "proguard-project.txt"))
            return list
        }

    val rFile: File
        get() = File(rootFolder, "R.txt")

    val packageName: String?
        get() {
            var packageName: String? = null
            val manifestFile = manifest
            if (manifestFile.exists()) {
                try {
                    val dbf = DocumentBuilderFactory.newInstance()
                    val doc = dbf.newDocumentBuilder().parse(manifestFile)
                    val element = doc.documentElement
                    packageName = element.getAttribute("package")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else {
                throw RuntimeException("$name module's AndroidManifest not found")
            }
            return packageName
        }
}
