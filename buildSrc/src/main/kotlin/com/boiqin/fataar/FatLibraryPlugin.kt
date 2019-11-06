package com.boiqin.fataar

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.*
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact
import java.util.*
import kotlin.collections.HashSet

/**
 * plugin entry
 *
 * Created by Vigi on 2017/1/14.
 * Modified by kezong on 2018/12/18
 * Modified by kezong on 2019/11/05
 */
class FatLibraryPlugin : Plugin<Project> {
    companion object {
        const val ARTIFACT_TYPE_AAR = "aar"
        const val ARTIFACT_TYPE_JAR = "jar"
    }

    private lateinit var project: Project

    private lateinit var embedConf: Configuration

    private var artifacts: Set<DefaultResolvedArtifact> = HashSet()

    private var unResolveArtifact: Set<ResolvedDependency> = HashSet()


    override fun apply(project: Project) {
        Utils.logInfo("this is fataar ,dealing with " + project
                .name)
        val taskList = project.gradle.startParameter.taskNames
        for (task in taskList) {
            Utils.logInfo("execute gradle task: $task")
        }
        this.project = project
        checkAndroidPlugin()
        createConfiguration()
        project.afterEvaluate {
            resolveArtifacts()
            dealUnResolveArtifacts()

            val android = project.extensions.getByName("android") as LibraryExtension
            var taskFounded = false

            android.libraryVariants.filter {
                // 过滤掉不需要的task
                val currentFlavor = it.flavorName + it.buildType.name.capitalize()
                taskList.isNotEmpty() && taskList.first().contains(currentFlavor, true)
            }.forEach {
                Utils.logInfo("start process: ${it.flavorName}${it.buildType.name.capitalize()}")
                taskFounded = true
                // 开始处理
                processVariant(it)
            }

            if (!taskFounded && taskList.isNotEmpty()) {
                Utils.logInfo(
                        "CombineAarPlugin has no talk with current task: ${taskList.first()}")
            }
        }

    }


    private fun checkAndroidPlugin() {
        if (!project.plugins.hasPlugin("com.android.library")) {
            throw ProjectConfigurationException("fat-aar-plugin must be applied in project that has android library plugin!", Throwable())
        }
    }

    private fun createConfiguration() {
        embedConf = project.configurations.create("embed").extendsFrom()
        embedConf.isVisible = true
        embedConf.isTransitive = false

        project.gradle.addListener(object : DependencyResolutionListener {

            override fun beforeResolve(resolvableDependencies: ResolvableDependencies) {
                embedConf.dependencies.forEach {
                    project.dependencies.add("compileOnly", it)
                    Utils.logInfo("beforeResolve, add dependencies:$it")
                }
                project.gradle.removeListener(this)
            }

            override fun afterResolve(resolvableDependencies: ResolvableDependencies) {
                Utils.logInfo("afterResolve resolvableDependencies:$resolvableDependencies")
            }
        })
    }

    /**
     * 处理本地embed aar和jar包
     */
    private fun resolveArtifacts() {
        val set = HashSet<DefaultResolvedArtifact>()
        embedConf.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
            Utils.logInfo("embedConf.resolvedConfiguration.resolveLocalArtifacts is $artifact")
            // jar file wouldn't be here
            if (ARTIFACT_TYPE_AAR == artifact.type || ARTIFACT_TYPE_JAR == artifact.type) {
                Utils.logInfo("[embed detected][${artifact.type}] ${artifact.moduleVersion.id}")
            } else {
                throw ProjectConfigurationException("Only support embed aar and jar dependencies!", Throwable())
            }
            set.add(artifact as DefaultResolvedArtifact)
        }
        if (set.isEmpty()) {
            Utils.logInfo("没有embed配置的resolvedConfiguration")
        } else {
            artifacts = Collections.unmodifiableSet(set)
        }
    }

    private fun processVariant(variant: LibraryVariant) {
        Utils.logInfo("processVariant ${variant.flavorName}")
        val processor = VariantProcessor(project, variant)
        if (artifacts.isNotEmpty()) {
            processor.addArtifacts(artifacts)
            Utils.logInfo("processor.addArtifacts $artifacts")
        } else {
            Utils.logInfo("processor.addArtifact,but resolvedArtifacts is empty")
        }
        processor.addUnResolveArtifact(unResolveArtifact)
        processor.processVariant()
    }

    private fun dealUnResolveArtifacts() {
        val dependencies = Collections.unmodifiableSet(embedConf.resolvedConfiguration
                .firstLevelModuleDependencies)
        val dependencySet = HashSet<ResolvedDependency>()
        dependencies.forEach { dependency ->
            var match = false
            artifacts.forEach { artifact ->
                if (dependency.moduleName == artifact.name) {
                    match = true
                }
            }
            if (!match) {
                Utils.logInfo("[unResolve dependency detected][${dependency.name}]")
                dependencySet.add(dependency)
            }
        }

        if (dependencySet.isEmpty()) {
            Utils.logInfo("没有embedConf配置的unResolve dependency")
        } else {
            unResolveArtifact = Collections.unmodifiableSet(dependencySet)
        }
    }
}
