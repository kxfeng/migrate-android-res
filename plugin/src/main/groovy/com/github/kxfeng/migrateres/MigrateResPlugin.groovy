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
                def resDir = "${project.buildDir}/intermediates/res/merged/${variant.dirName}"

                def migrateTaskName = "migrate${variant.name.capitalize()}Resource"

                project.task("${migrateTaskName}") {
                    doLast {
                        project.extensions["${EXT_CONFIG_NAME}"].subTasks.each { subTask ->

                            project.logger.info("subTask: ${subTask.name}")

                            switch (subTask.from) {
                                case VALUES_PATTERN:
                                    mergeTask(project, resDir, subTask)
                                    break
                                default:
                                    copyTask(project, resDir, subTask)
                            }
                        }
                    }
                }

                def migrateTask = project.tasks[migrateTaskName]
                def processTask = project.tasks["process${variant.name.capitalize()}Resources"]

                migrateTask.dependsOn processTask.taskDependencies.getDependencies()
                processTask.dependsOn migrateTask
            }
        }
    }

    static void copyTask(project, resDir, subTask) {
        def srcFolder = new File("${resDir}/${subTask.from}")

        if (!srcFolder.exists()) {
            project.logger.warn("not exist:${srcFolder.getCanonicalPath()}")
            return
        }

        if (!srcFolder.isDirectory()) {
            throw new GradleException("not directory:${srcFolder.getCanonicalPath()}")
        }

        srcFolder.eachFileRecurse(FileType.FILES) { file ->
            project.logger.info("from:${subTask.from}/${file.name} to:${subTask.to}")
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
        def srcFile = new File("${resDir}/${subTask.from}/${subTask.from}.xml")

        if (!srcFile.exists()) {
            project.logger.warn("not exist:${srcFile.getCanonicalPath()}")
            return
        }

        if (!srcFile.isFile()) {
            throw new GradleException("not file:${srcFile.getCanonicalPath()}")
        }

        def srcXml = new XmlParser().parse("${resDir}/${subTask.from}/${subTask.from}.xml")

        subTask.to.each { dest ->
            def toFile = new File("${resDir}/${dest}/${dest}.xml")

            project.logger.info("from:${subTask.from}/${subTask.from}.xml to:${dest}/${dest}.xml")

            if (!toFile.exists()) {
                project.copy {
                    from "${resDir}/${subTask.from}/${subTask.from}.xml"
                    into "${resDir}/${dest}"
                    rename "${subTask.from}.xml", "${dest}.xml"
                }
                return
            }

            if (!toFile.isFile()) {
                throw new GradleException("not file:${toFile.getCanonicalPath()}")
            }

            def toXml = new XmlParser().parse(toFile)

            srcXml.children().each { child ->
                def findName = toXml.findAll { it.attribute("name") == child.attribute("name") }

                if (findName.size() > 0)
                    throw new GradleException("${child.attribute('name')} already exist in ${dest}.xml")

                toXml.append(child)
            }

            toXml.children().sort { it.attribute('name') }

            toFile.withWriter('UTF-8') { outWriter ->
                XmlUtil.serialize(toXml, outWriter)
            }
        }

        project.delete "${resDir}/${subTask.from}"
    }
}