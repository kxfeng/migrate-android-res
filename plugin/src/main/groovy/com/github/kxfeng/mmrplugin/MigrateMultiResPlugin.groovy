package com.github.kxfeng.mmrplugin

import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project

class MigrateMultiResPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.extensions.create('migrateMultiRes', MigrateMultiResExtension)

        project.afterEvaluate {

            project.android.applicationVariants.each { variant ->
                def resDir = "${project.buildDir}/intermediates/res/merged/${variant.dirName}"

                def migrateTaskName = "migrate${variant.name.capitalize()}MultiResource"

                project.task("${migrateTaskName}") << {

                    project.extensions.migrateMultiRes.subTasks.each { subTask ->
                        new File("${resDir}/${subTask.from}").eachFileRecurse(FileType.FILES) { file ->
                            println "${migrateTaskName} from:${subTask.from}/${file.name} into:${subTask.into}"
                        }

                        subTask.into.each { dest ->
                            project.copy {
                                from "${resDir}/${subTask.from}"
                                into "${resDir}/${dest}"
                            }
                        }
                        project.delete "${resDir}/${subTask.from}"
                    }
                }

                def processTaskName = "process${variant.name.capitalize()}Resources"

                project.tasks[migrateTaskName].dependsOn project.tasks[processTaskName].taskDependencies.getDependencies()
                project.tasks[processTaskName].dependsOn project.tasks[migrateTaskName]
            }
        }
    }
}