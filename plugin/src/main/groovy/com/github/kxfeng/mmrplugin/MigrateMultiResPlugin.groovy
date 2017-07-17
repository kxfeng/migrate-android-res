package com.github.kxfeng.mmrplugin

import groovy.io.FileType
import groovy.xml.XmlUtil
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.regex.Pattern

class MigrateMultiResPlugin implements Plugin<Project> {

    Pattern VALUES_PATTERN = ~/values\S*/

    @Override
    void apply(Project project) {

        project.extensions.create('migrateMultiRes', MigrateMultiResExtension)

        project.afterEvaluate {

            project.android.applicationVariants.each { variant ->
                def resDir = "${project.buildDir}/intermediates/res/merged/${variant.dirName}"

                def migrateTaskName = "migrate${variant.name.capitalize()}MultiResource"

                project.task("${migrateTaskName}") {
                    doLast {
                        project.extensions.migrateMultiRes.subTasks.each { subTask ->

                            println "${migrateTaskName} SubTask: ${subTask.name}"

                            new File("${resDir}/${subTask.from}").eachFileRecurse(FileType.FILES) { file ->
                                println "from:${subTask.from}/${file.name} to:${subTask.to}"
                            }

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

                def processTaskName = "process${variant.name.capitalize()}Resources"

                project.tasks[migrateTaskName].dependsOn project.tasks[processTaskName].taskDependencies.getDependencies()
                project.tasks[processTaskName].dependsOn project.tasks[migrateTaskName]
            }
        }
    }

    static void copyTask(project, resDir, subTask) {
        subTask.to.each { dest ->
            project.copy {
                from "${resDir}/${subTask.from}"
                into "${resDir}/${dest}"
            }
        }
        project.delete "${resDir}/${subTask.from}"
    }

    static void mergeTask(project, resDir, subTask) {
        def srcXml = new XmlParser().parse("${resDir}/${subTask.from}/${subTask.from}.xml")

        subTask.to.each { dest ->
            def toPath = "${resDir}/${dest}/${dest}.xml"

            def toXml = new XmlParser().parse(toPath)

            srcXml.children().each { child ->
                def findName = toXml.findAll { it.attribute("name") == child.attribute("name") }

                if (findName.size() > 0)
                    throw new GradleException("${child.attribute('name')} already exist in ${dest}.xml")

                toXml.append(child)
            }

            toXml.children().sort { it.attribute('name') }

            new File(toPath).withWriter('UTF-8') { outWriter ->
                XmlUtil.serialize(toXml, outWriter)
            }
        }

        project.delete "${resDir}/${subTask.from}"
    }
}