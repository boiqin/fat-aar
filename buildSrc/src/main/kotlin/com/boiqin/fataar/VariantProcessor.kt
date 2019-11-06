package com.boiqin.fataar

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.tasks.MergeFileTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskDependency
import java.io.File

/**
 * Processor for variant
 * Created by Vigi on 2017/2/24.
 * Modified by kezong on 2019/05/29
 */
class VariantProcessor(private val project: Project, private val variant: LibraryVariant) {


    private val resolvedArtifacts = ArrayList<ResolvedArtifact>()

    private val androidArchiveLibraries = ArrayList<AndroidArchiveLibrary>()

    private val jarFiles = ArrayList<File>()

    private val explodeTasks = ArrayList<Task>()

    private var gradlePluginVersion: String? = null

    private lateinit var versionAdapter: VersionAdapter

    private val android = project.extensions.getByName("android") as LibraryExtension

    init {
        // gradle version
        project.rootProject.buildscript.configurations.getByName("classpath").dependencies.forEach {
            if (it.group == "com.android.tools.build" && it.name == "gradle") {
                gradlePluginVersion = it.version
            }
        }
        checkNotNull(gradlePluginVersion) {
            "com.android.tools.build:gradle is no set in the root" +
                    " build.gradle file"
        }
        versionAdapter = VersionAdapter(project, variant, gradlePluginVersion!!)
    }

    fun addArtifacts(resolvedArtifacts: Set<ResolvedArtifact>) {
        this.resolvedArtifacts.addAll(resolvedArtifacts)
    }

    private fun addAndroidArchiveLibrary(library: AndroidArchiveLibrary) {
        androidArchiveLibraries.add(library)
    }

    fun addUnResolveArtifact(dependencies: Set<ResolvedDependency>) {
        if (dependencies != null) {
            dependencies.forEach {
                val artifact = FlavorArtifact.createFlavorArtifact(project, variant, it,
                        gradlePluginVersion!!)
                resolvedArtifacts.add(artifact)
            }
        }
    }

    private fun addJarFile(jar: File) {
        jarFiles.add(jar)
    }

    fun processVariant() {
        var taskPath = "pre" + variant.name.capitalize() + "Build"
        val prepareTask = project.tasks.findByPath(taskPath)
                ?: throw RuntimeException("Can not find task ${taskPath}!")
        taskPath = "bundle" + variant.name.capitalize()
        var bundleTask = project.tasks.findByPath(taskPath)
        if (bundleTask == null) {
            taskPath = "bundle" + variant.name.capitalize() + "Aar"
            bundleTask = project.tasks.findByPath(taskPath)
        }
        if (bundleTask == null) {
            throw RuntimeException("Can not find task ${taskPath}!")
        }
        processCache()
        processArtifacts(prepareTask, bundleTask)
        processClassesAndJars(bundleTask)
        if (androidArchiveLibraries.isEmpty()) {
            return
        }
        processManifest()
        processResourcesAndR()
        processAssets()
        processJniLibs()
        processProguardTxt(prepareTask)
        val rProcessor = RProcessor(project, variant, androidArchiveLibraries,
                gradlePluginVersion!!)
        rProcessor.inject(bundleTask)
    }

    private fun processCache() {
        if (Utils.compareVersion(gradlePluginVersion!!, "3.5.0") >= 0) {
            println(gradlePluginVersion)
            //TODO
            Utils.deleteDir(versionAdapter.libsDirFile)
            Utils.deleteDir(versionAdapter.classPathDirFiles.first())
        }
    }


    /**
     * exploded artifact files
     */
    private fun processArtifacts(prepareTask: Task, bundleTask: Task) {
        resolvedArtifacts.forEach {
            val artifact = it as DefaultResolvedArtifact
            if (FatLibraryPlugin.ARTIFACT_TYPE_JAR == artifact.type) {
                addJarFile(artifact.file)
            } else if (FatLibraryPlugin.ARTIFACT_TYPE_AAR == artifact.type) {
                val archiveLibrary = AndroidArchiveLibrary(project, artifact, variant.name)
                addAndroidArchiveLibrary(archiveLibrary)

//                val buildDependencies =
//                        artifact.getBuildDependencies().getDependencies(null)
                // 反射调用private属性
                val clazz = DefaultResolvedArtifact::class.java
                val buildDependenciesField = clazz.getDeclaredField("buildDependencies")
                buildDependenciesField.isAccessible = true
                val buildDependencies = (buildDependenciesField.get(artifact) as TaskDependency).getDependencies(null)
                //val buildDependencies = artifact.buildDependencies.getDependencies()
                Utils.deleteDir(archiveLibrary.rootFolder)
                val zipFolder = archiveLibrary.rootFolder
                zipFolder.mkdirs()
                val group = artifact.moduleVersion.id.group.capitalize()
                val name = artifact.name.capitalize()
                val taskName = "explode${group}${name}${variant.name.capitalize()}"
                val explodeTask = project.tasks.create<Copy>(taskName, Copy::class.java).run {
                    from(project.zipTree(artifact.file.absolutePath))
                    into(zipFolder)
                }

                if (buildDependencies.size == 0) {
                    explodeTask.dependsOn(prepareTask)
                } else {
                    explodeTask.dependsOn(buildDependencies.first())
                }
                val javacTask = versionAdapter.javaCompileTask
                javacTask.dependsOn(explodeTask)
                bundleTask.dependsOn(explodeTask)
                explodeTasks.add(explodeTask)
            }
        }
    }

    /**
     * merge manifest
     */
    private fun processManifest() {
        val processManifestTask = versionAdapter.processManifest
        val manifestOutputBackup = if (gradlePluginVersion != null && Utils.compareVersion
                (gradlePluginVersion!!, "3.3.0") >= 0) {
            project.file("${project.buildDir.path}/intermediates/library_manifest/${variant
                    .name}/AndroidManifest.xml")
        } else {
            // TODO
            //processManifestTask.manifestOutputDirectory.get().asFile.absolutePath + " +
            //""/AndroidManifest.xml")

            project.file(processManifestTask.manifestOutputDirectory.asFile.get().absolutePath + "/AndroidManifest.xml")

        }
        val manifestsMergeTask = project.tasks.create("merge${variant.name.capitalize()
        }Manifest", LibraryManifestMerger::class.java)
        manifestsMergeTask.gradleVersion = project.gradle.gradleVersion
        manifestsMergeTask.gradlePluginVersion = gradlePluginVersion!!
        manifestsMergeTask.variantName = variant.name
        manifestsMergeTask.mainManifestFile = manifestOutputBackup
        val list = ArrayList<File>()
        for (archiveLibrary in androidArchiveLibraries) {
            list.add(archiveLibrary.manifest)
        }
        manifestsMergeTask.secondaryManifestFiles = list
        manifestsMergeTask.outputFile = manifestOutputBackup
        manifestsMergeTask.dependsOn(processManifestTask)
        manifestsMergeTask.doFirst {
            val existFiles = ArrayList<File>()
            manifestsMergeTask.secondaryManifestFiles?.forEach {
                if (it.exists()) {
                    existFiles.add(it)
                }
            }
            manifestsMergeTask.secondaryManifestFiles = existFiles
        }

        explodeTasks.forEach {
            manifestsMergeTask.dependsOn(it)
        }

        processManifestTask.finalizedBy(manifestsMergeTask)
    }

    private fun handleClassesMergeTask(isMinifyEnabled: Boolean): Task {
        val task = project.tasks.create("mergeClasses" + variant.name.capitalize())
        task.doFirst {
            val dustDir = versionAdapter.classPathDirFiles.first()
            if (isMinifyEnabled) {
                ExplodedHelper.processClassesJarInfoClasses(project, androidArchiveLibraries,
                        dustDir)
                ExplodedHelper.processLibsIntoClasses(project, androidArchiveLibraries,
                        jarFiles, dustDir)
            } else {
                ExplodedHelper.processClassesJarInfoClasses(project, androidArchiveLibraries,
                        dustDir)
            }
        }
        return task
    }

    private fun handleJarMergeTask(): Task {
        val task = project.tasks.create("mergeJars" + variant.name.capitalize())
        task.doFirst {
            ExplodedHelper.processLibsIntoLibs(project, androidArchiveLibraries, jarFiles,
                    versionAdapter.libsDirFile)
        }
        return task
    }

    /**
     * merge classes and jars
     */
    private fun processClassesAndJars(bundleTask: Task) {
        val isMinifyEnabled = variant.buildType.isMinifyEnabled
        if (isMinifyEnabled) {
            //merge proguard file
            for (archiveLibrary in androidArchiveLibraries) {
                val thirdProguardFiles = archiveLibrary.proguardRules
                for (file in thirdProguardFiles) {
                    if (file.exists()) {
                        Utils.logInfo("add proguard file: " + file.absolutePath)
                        android.defaultConfig.proguardFile(file)
                    }
                }
            }
        }

        val taskPath = "transformClassesAndResourcesWithSyncLibJarsFor" + variant.name.capitalize()
        val syncLibTask = project.tasks.findByPath(taskPath)
                ?: throw RuntimeException("Can not find task ${taskPath}!")

        val javacTask = versionAdapter.javaCompileTask
        val mergeClasses = handleClassesMergeTask(isMinifyEnabled)
        syncLibTask.dependsOn(mergeClasses)
        explodeTasks.forEach {
            mergeClasses.dependsOn(it)
        }
        mergeClasses.dependsOn(javacTask)
        bundleTask.dependsOn(mergeClasses)//修复多个embed丢失的代码问题

        if (!isMinifyEnabled) {
            val mergeJars = handleJarMergeTask()
            mergeJars.mustRunAfter(syncLibTask)
            bundleTask.dependsOn(mergeJars)
            explodeTasks.forEach {
                mergeJars.dependsOn(it)
            }
            mergeJars.dependsOn(javacTask)
        }
    }

    /**
     * merge R.txt(actually is to fix issue caused by provided configuration) and res
     *
     * Here I have to inject res into "main" instead of "variant.name".
     * To avoid the res from embed dependencies being used, once they have the same res Id with main res.
     *
     * Now the same res Id will cause a build exception: Duplicate resources, to encourage you to change res Id.
     * Adding "android.disableResourceValidation=true" to "gradle.properties" can do a trick to skip the exception, but is not recommended.
     */
    private fun processResourcesAndR() {

        val taskPath = "generate" + variant.name.capitalize() + "Resources"
        val resourceGenTask = project.tasks.findByPath(taskPath)
                ?: throw RuntimeException("Can not find task ${taskPath}!")

        resourceGenTask.doFirst {
            for (archiveLibrary in androidArchiveLibraries) {
                android.sourceSets.forEach {
                    println(it.name)
                    println(variant.name)
                    if (it.name == variant.name) {
                        Utils.logInfo("Merge resource，Library res：${archiveLibrary.resFolder}")
                        it.res.srcDir(archiveLibrary.resFolder)
                    }
                }
            }
        }

        explodeTasks.forEach {
            resourceGenTask.dependsOn(it)
        }
    }

    /**
     * merge assets
     *
     * AaptOptions.setIgnoreAssets and AaptOptions.setIgnoreAssetsPattern will work as normal
     */
    private fun processAssets() {

        val assetsTask = versionAdapter.mergeAssets
        assetsTask.doFirst {
            for (archiveLibrary in androidArchiveLibraries) {
                if (archiveLibrary.assetsFolder.exists()) {
                    android.sourceSets.forEach {
                        println("processAssets" + variant.name)
                        if (it.name == variant.name) {
                            it.assets.srcDir(archiveLibrary.assetsFolder)

                        }
                    }
                }
            }
        }

        explodeTasks.forEach {
            assetsTask.dependsOn(it)
        }
    }

    /**
     * merge jniLibs
     */
    private fun processJniLibs() {
        val taskPath = "merge" + variant.name.capitalize() + "JniLibFolders"
        val mergeJniLibsTask = project.tasks.findByPath(taskPath)
                ?: throw RuntimeException("Can not find task ${taskPath}!")
        mergeJniLibsTask.doFirst {
            for (archiveLibrary in androidArchiveLibraries) {
                if (archiveLibrary.jniFolder.exists()) {
                    android.sourceSets.forEach {
                        if (it.name == variant.name) {
                            it.jniLibs.srcDir(archiveLibrary.jniFolder)
                        }
                    }
                }
            }
        }



        explodeTasks.forEach {
            mergeJniLibsTask.dependsOn(it)
        }
    }

    /**
     * fixme
     * merge proguard.txt
     */
    private fun processProguardTxt(prepareTask: Task) {
        val taskPath = "merge" + variant.name.capitalize() + "ConsumerProguardFiles"
        val mergeFileTask = project.tasks.findByPath(taskPath) as? MergeFileTask
                ?: throw RuntimeException("Can not find task ${taskPath}!")
        for (archiveLibrary in androidArchiveLibraries) {
            val thirdProguardFiles = archiveLibrary.proguardRules
            for (file in thirdProguardFiles) {
                if (file.exists()) {
                    Utils.logInfo("add proguard file: " + file.absolutePath)
                    mergeFileTask.inputs.file(file)
                }
            }
        }
        mergeFileTask.doFirst {
            val proguardFiles = mergeFileTask.inputFiles
            for (archiveLibrary in androidArchiveLibraries) {
                val thirdProguardFiles = archiveLibrary.proguardRules
                for (file in thirdProguardFiles) {
                    if (file.exists()) {
                        Utils.logInfo("add proguard file: " + file.absolutePath)
                        // TODO
                        proguardFiles.files.add(file)
                    }
                }
            }
        }
        mergeFileTask.dependsOn(prepareTask)
    }
}
