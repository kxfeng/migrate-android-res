package com.github.kxfeng.migrateres

import groovy.io.FileType
import groovy.xml.XmlUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.regex.Pattern

class MigrateResPlugin implements Plugin<Project> {

    Pattern VALUES_PATTERN = ~/values\S*/
    String EXT_CONFIG_NAME = "migrateAndroidRes"

    @Override
    void apply(Project project) {

        project.extensions.create(EXT_CONFIG_NAME, MigrateResExtension)

        project.afterEvaluate {

            project.android.applicationVariants.each { variant ->

                def migrateTaskName = "migrate${variant.name.capitalize()}Resource"

                project.task("${migrateTaskName}") {
                    doLast {
                        project.extensions["${EXT_CONFIG_NAME}"].subTasks.each { subTask ->

                            if (!project.extensions["${EXT_CONFIG_NAME}"].enable) {
                                project.logger.info("migrateTask: migrate is disabled")
                                return
                            }

                            project.logger.info("migrateTask: subTask ${subTask.name}")

                            project.android.sourceSets.each { sourceSet ->
                                sourceSet.res.srcDirs.each { resDir ->
                                    switch (subTask.from) {
                                        case VALUES_PATTERN:
                                            mergeTask(project, resDir, subTask)
                                            break
                                        default:
                                            copyTask(project, resDir, subTask)
                                            break
                                    }
                                }
                            }
                        }
                    }
                }

                project.tasks[migrateTaskName].dependsOn project.tasks["preBuild"].taskDependencies.getDependencies()
                project.tasks["preBuild"].dependsOn project.tasks[migrateTaskName]
            }
        }
    }

    static void copyTask(project, resDir, subTask) {
        def srcFolder = new File("${resDir}/${subTask.from}")

        if (!srcFolder.exists()) {
            project.logger.warn("migrateTask: ${srcFolder.canonicalPath} not exist")
            return
        }

        if (!srcFolder.isDirectory()) {
            throw new GradleException("migrateTask: ${srcFolder.canonicalPath} is not directory")
        }

        srcFolder.eachFileRecurse(FileType.FILES) { file ->
            project.logger.info("migrateTask: from:${subTask.from}/${file.name} to:${subTask.to}")
        }

        subTask.to.each { dest ->
            project.copy {
                from "${resDir}/${subTask.from}"
                into "${resDir}/${dest}"
            }
        }
        project.delete "${resDir}/${subTask.from}"
    }

    static void mergeTask(project, resDir, subTask) {
        def srcFolder = new File("${resDir}/${subTask.from}")

        if (!srcFolder.exists()) {
            project.logger.info("migrateTask: ${srcFolder.canonicalPath} not exist")
            return
        }

        if (!srcFolder.isDirectory()) {
            throw new GradleException("migrateTask: ${srcFolder.canonicalPath} is not directory")
        }

        srcFolder.listFiles().each { srcFile ->

            if (!srcFile.exists()) {
                project.logger.warn("migrateTask: ${srcFile.canonicalPath} not exist")
                return
            }

            if (!srcFile.isFile()) {
                throw new GradleException("migrateTask: ${srcFile.canonicalPath} is not file")
            }

            def srcXml = new XmlParser().parse(srcFile)

            subTask.to.each { dest ->
                def toFile = new File("${resDir}/${dest}/${srcFile.name}")

                project.logger.info("migrateTask: from:${srcFile.canonicalPath} to:${toFile.canonicalPath}")

                if (!toFile.exists()) {
                    project.copy {
                        from "${srcFile.absolutePath}"
                        into "${resDir}/${dest}"
                    }
                    return
                }

                if (!toFile.isFile()) {
                    throw new GradleException("migrateTask: ${toFile.canonicalPath} is not file")
                }

                def toXml = new XmlParser().parse(toFile)

                srcXml.children().each { child ->
                    def findName = toXml.findAll { it.attribute("name") == child.attribute("name") }

                    if (findName.size() > 0)
                        throw new GradleException("migrateTask: ${child.attribute('name')} already exist in ${toFile.canonicalPath}")

                    toXml.append(child)
                }

                toXml.children().sort { it.attribute('name') }

                toFile.withWriter('UTF-8') { outWriter ->
                    XmlUtil.serialize(toXml, outWriter)
                }
            }
        }

        project.delete "${resDir}/${subTask.from}"
    }
}