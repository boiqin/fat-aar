package com.boiqin.fataar

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.io.FileOutputStream
import java.util.*


/**
 * R file processor
 * @author kezong on 2019/7/16.
 * Modify by alexbchen on 2019/11/05.
 */
class RProcessor(private val project: Project, private val variant: LibraryVariant,
                 private val libraries: Collection<AndroidArchiveLibrary>, private val gradlePluginVersion: String) {

    private var javaDir: File = project.file("${project.buildDir}/intermediates/fat-R/r/${variant.dirName}")
    private var classDir: File = project.file("${project.buildDir}/intermediates/fat-R/r-class/${variant.dirName}")
    // aar zip file
    private var jarDir: File = project.file("${project.buildDir}/outputs/aar-R/${variant.dirName}/libs")
    private var aarUnZipDir: File = jarDir.parentFile
    private var aarOutputDir: File = project.file("${project.buildDir}/outputs/aar/")
    private var aarOutputPath: String = variant.outputs.first().outputFile.absolutePath
    private var versionAdapter: VersionAdapter = VersionAdapter(project, variant, gradlePluginVersion)


    private val symbolsMap: Map<String, HashMap<String, String>>
        get() {
            val file = versionAdapter.symbolFile
            Utils.logInfo("get R file and deal: ${file.absolutePath}")
            if (!file.exists()) {
                throw IllegalAccessException("{$file.absolutePath} not found")
            }

            val map = hashMapOf<String, HashMap<String, String>>()
            file.forEachLine { line ->
                val (intNum, resType, resName, resValue) = line.split(' ')
                if (!map.containsKey(resType)) {
                    map[resType] = hashMapOf(Pair(resName, intNum))
                } else {
                    map[resType]?.put(resName, intNum)
                }
            }
            return map
        }

    fun inject(bundleTask: Task) {
        val rFileTask = createRFileTask(javaDir)
        val rClassTask = createRClassTask(javaDir, classDir)
        val rJarTask = createRJarTask(classDir, jarDir)
        val reBundleAar = createBundleAarTask(aarUnZipDir, aarOutputDir, aarOutputPath)

        reBundleAar.doFirst {
            project.copy {
                it.from(project.zipTree(aarOutputPath))
                it.into(aarUnZipDir)
            }
            deleteEmptyDir(aarUnZipDir)
        }

        reBundleAar.doLast {
            Utils.logInfo("target: $aarOutputPath")
        }

        bundleTask.doFirst {
            val f = File(aarOutputPath)
            if (f.exists()) {
                f.delete()
            }
            jarDir.parentFile?.apply {
                Utils.deleteDir(this)
            }
            jarDir.mkdirs()
        }

        bundleTask.doLast {
            // support gradle 5.1 && gradle plugin 3.4 before, the outputName is changed
            val file = File(aarOutputPath)
            if (!file.exists()) {
                aarOutputPath = aarOutputDir?.absolutePath + "/" + project.name + ".aar"
                reBundleAar.archiveName = File(aarOutputPath).name
            }
        }

        bundleTask.finalizedBy(rFileTask)
        rFileTask.finalizedBy(rClassTask)
        rClassTask.finalizedBy(rJarTask)
        rJarTask.finalizedBy(reBundleAar)
    }

    private fun createRFile(library: AndroidArchiveLibrary, rFolder: File, symbolsMap:
    Map<String, HashMap<String, String>>) {
        val libPackageName = variant.applicationId
        val aarPackageName = library.packageName

        val packagePath = aarPackageName!!.replace(".", "/")

        val rTxt = library.rFile
        val rMap: HashMap<String, HashMap<String, String>> = hashMapOf()

        if (rTxt.exists()) {
            rTxt.forEachLine { line ->
                val (intString, resType, resName, resValue) = line.split(' ')
                if (symbolsMap.containsKey(resType) && symbolsMap[resType]?.get(resName) == intString) {
                    if (!rMap.containsKey(resType)) {
                        rMap[resType] = hashMapOf(Pair(resName, intString))
                    } else {
                        rMap[resType]?.put(resName, intString)
                    }
                }
            }
        }

        val sb = StringBuilder("package $aarPackageName;\n\n")
        sb.append("public final class R {\n")

        rMap.forEach { (resType, values) ->
            sb.append("  public static final class $resType {\n")
            values.forEach { (resName, intString) ->
                sb.append(
                        "    public static final $intString $resName = $libPackageName.R.$resType.$resName;\n")
            }
            sb.append("    }\n")
        }
        sb.append("    }\n")



        File("${rFolder?.path}/$packagePath").mkdirs()
        val outputStream = FileOutputStream("${rFolder?.path}/$packagePath/R.java")
        outputStream.write(sb.toString().toByteArray())
        outputStream.close()
    }

    /**
     * 在主工程中创建一个r-classes.jar文件，重定向每个embed模块的资源引用
     *
     * 因为将每个模块中的res文件全部都打包到了主工程中,最后生成的资源引用全都存在于主工程包名下的的R.txt中，
     * 导致原有被embed的模块中的资源引用无法找到，需要一个重定向的机制
     *
     * 如embed的模块包名为com.xx.xx
     * 主工程包名为com.aa.aa
     * 那么r-classes.jar中包含com.xx.xx.R:
     * 而且每个资源的引用重定向为类似格式:
     * public static final class resType {
     *   public static final int resName = com.aa.aa.R.resType.resName;
     * }
     *
     * 另一种思路是直接生成R.java文件到各工程目录
     */
    private fun createRFileTask(destFolder: File): Task {
        val task = project.tasks.create("createRsFile" + variant.name)
        task.doLast {
            if (destFolder.exists()) {
                Utils.deleteDir(destFolder)
            }
            if (libraries.isNotEmpty()) {
                libraries.forEach {
                    Utils.logInfo("Generate R File, Library:${it.name}")
                    createRFile(it, destFolder, symbolsMap)
                }
            }
        }

        return task
    }

    private fun createRClassTask(sourceDir: File, destinationDir: File): Task {
        project.mkdir(destinationDir)

        val classpath = versionAdapter.rClassPath
        val taskName = "compileRs${variant.name.capitalize()}"
        val task = project.tasks.create(taskName, JavaCompile::class.java) {
            it.setSource(sourceDir.path)
            val android = project.extensions.getByName("android") as LibraryExtension
            it.sourceCompatibility = android.compileOptions.sourceCompatibility.toString()
            it.targetCompatibility = android.compileOptions.targetCompatibility.toString()
            it.classpath = classpath
            it.destinationDir = destinationDir
            Utils.logDebug(it.sourceCompatibility)
            Utils.logDebug(it.targetCompatibility)
        }

        task.doFirst {
            Utils.logInfo("Compile R.class, Dir:${sourceDir.path}")
            Utils.logInfo("Compile R.class, classpath:${classpath.first().absolutePath}")

            if (Utils.compareVersion(gradlePluginVersion, "3.3.0") >= 0) {
                project.copy {
                    it.from(project.zipTree(versionAdapter.rClassPath.first()
                            .absolutePath + "/R.jar"))
                    it.into(versionAdapter.rClassPath.first().absolutePath)
                }
            }
        }
        return task
    }

    private fun createRJarTask(fromDir: File, desFile: File): Task {
        val taskName = "createRsJar${variant.name.capitalize()}"
        val task = project.tasks.create(taskName, Jar::class.java) {
            it.from(fromDir.path)
            it.archiveName = "r-classes.jar"
            it.destinationDir = desFile
        }
        task.doFirst {
            Utils.logInfo("Generate R.jar, Dir：$fromDir")
        }
        return task
    }

    private fun createBundleAarTask(from: File, destDir: File, filePath: String)
            : AbstractArchiveTask {
        val taskName = "reBundleAar${variant.name?.capitalize()}"

        return project.tasks.create(taskName, Zip::class.java) {
            it.from(from)
            it.include("**")
            it.archiveName = File(filePath).name
            it.destinationDir = destDir
        }
    }

    private fun deleteEmptyDir(file: File) {
        file.listFiles()?.forEach { x ->
            if (x.isDirectory) {
                val listFiles = x.listFiles()
                if ((null != listFiles) && listFiles.isEmpty()) {
                    x.delete()
                } else {
                    deleteEmptyDir(x)
                    if ((null != listFiles) && listFiles.isEmpty()) {
                        x.delete()
                    }
                }
            }
        }
    }
}
