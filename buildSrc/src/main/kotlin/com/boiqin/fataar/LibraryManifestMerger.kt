package com.boiqin.fataar

import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.tasks.InvokeManifestMerger
import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.ManifestProvider
import com.android.manifmerger.MergingReport
import org.apache.tools.ant.BuildException
import org.gradle.api.tasks.TaskAction
import java.io.*
import java.util.*

/**
 * ManifestMerger for Library
 * @author kezong on 2019/7/8.
 */
open class LibraryManifestMerger : InvokeManifestMerger() {

    var gradlePluginVersion = "0"
    var gradleVersion = "0"

    override fun doTaskAction() {
        try {
            doFullTaskAction()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Gradle Plugin Version:$gradlePluginVersion")
            println("Gradle Version:$gradleVersion")
            println("If you see this error message, please submit issue to ")
        }

    }

    @TaskAction
    @Throws(ManifestMerger2.MergeFailureException::class, IOException::class)
    protected fun doFullTaskAction() {
        val iLogger = LoggerWrapper(logger)
        val mergerInvoker = ManifestMerger2.newMerger(mainManifestFile, iLogger, ManifestMerger2.MergeType.LIBRARY)
        val secondaryManifestFiles = secondaryManifestFiles
        val manifestProviders = ArrayList<ManifestProvider>()
        if (secondaryManifestFiles != null) {
            for (file in secondaryManifestFiles) {
                manifestProviders.add(object : ManifestProvider {
                    override fun getManifest(): File {
                        return file.absoluteFile
                    }

                    override fun getName(): String {
                        return file.name
                    }
                })
            }
        }
        mergerInvoker.addManifestProviders(manifestProviders)
        val mergingReport = mergerInvoker.merge()
        if (mergingReport.result.isError) {
            logger.error(mergingReport.reportString)
            mergingReport.log(iLogger)
            throw BuildException(mergingReport.reportString)
        }

        // fix utf-8 problem in windows
        val writer = BufferedWriter(OutputStreamWriter(
                FileOutputStream(outputFile), "UTF-8")
        )
        writer.append(mergingReport
                .getMergedDocument(MergingReport.MergedManifestKind.MERGED))
        writer.flush()
        writer.close()
    }
}
