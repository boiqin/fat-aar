package com.boiqin.fataar

import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.tasks.ManifestProcessorTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import java.io.File

/**
 * @author kezong on 2019/7/16.
 */
class VersionAdapter(private val project: Project, private val variant: LibraryVariant,
                     private val gradlePluginVersion: String) {

    val classPathDirFiles: ConfigurableFileCollection
        get() {
            return when {
                Utils.compareVersion(gradlePluginVersion, "3.5.0") >= 0 -> project.files("${project
                        .buildDir.path}/intermediates/" +
                        "javac/${variant.name}/classes")
                Utils.compareVersion(gradlePluginVersion, "3.2.0") >= 0 -> // >= Versions 3.2.X
                    project.files("${project.buildDir.path}/intermediates/" +
                            "javac/${variant.name}/compile${variant.name.capitalize()
                            }JavaWithJavac/classes")
                else -> // Versions 3.0.x and 3.1.x
                    project.files("${project.buildDir.path}/intermediates/classes/${variant.dirName}")
            }
        }

    val rClassPath: ConfigurableFileCollection
        get() {
            return when {
                Utils.compareVersion(gradlePluginVersion, "3.5.0") >= 0 -> project.files("${project
                        .buildDir.path}/intermediates/" + "compile_only_not_namespaced_r_class_jar/"
                        + variant.name)
                Utils.compareVersion(gradlePluginVersion, "3.3.0") >= 0 -> project.files("${project
                        .buildDir.path}/intermediates/" + "compile_only_not_namespaced_r_class_jar/"
                        + "${variant.name}/generate${variant.name.capitalize()}RFile")
                else -> classPathDirFiles
            }
        }

    val libsDirFile: File
        get() {
            return if (Utils.compareVersion(gradlePluginVersion, "3.1.0") >= 0) {
                project.file(project.buildDir.path + "/intermediates/packaged-classes/" + variant
                        .dirName + "/libs")
            } else {
                project.file(project.buildDir.path + "/intermediates/bundles/" + variant.dirName +
                        "/libs")
            }
        }

    val javaCompileTask: Task
        get() {
            return if (Utils.compareVersion(gradlePluginVersion, "3.3.0") >= 0) {
                variant.javaCompileProvider.get()
            } else {
                variant.javaCompiler
            }
        }

    val processManifest: ManifestProcessorTask
        get() {
            return if (Utils.compareVersion(gradlePluginVersion, "3.3.0") >= 0) {
                variant.outputs.first().processManifestProvider.get()
            } else {
                variant.outputs.first().processManifest
            }
        }

    val mergeAssets: Task
        get() {
            return if (Utils.compareVersion(gradlePluginVersion, "3.3.0") >= 0) {
                variant.mergeAssetsProvider.get()
            } else {
                variant.mergeAssets
            }
        }

    val symbolFile: File
        get() {
            return if (Utils.compareVersion(gradlePluginVersion, "3.1.0") >= 0) {
                project.file(project.buildDir.path + "/intermediates/symbols/" + variant.dirName +
                        "/R.txt")
            } else {
                project.file(project.buildDir.path + "/intermediates/bundles/" + variant.name + "/R" +
                        ".txt")
            }
        }
}