# migrate-android-res
A gradle plugin to migrate Android resources at build time

## Usage 
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.github.kxfeng:migrate-android-res:1.1.1'
    }
}
apply plugin: 'com.github.kxfeng.migrate-android-res'

migrateAndroidRes {
    // this plugin will make some changes (modify and delete files) to your working directory, so you'd
    // better enable this after you have saved your working files.
    enable = true

    // task name (any name you like) 
    layout_zh_task {
        from "layout-zh"                     // one source folder 
        to "layout-zh-rCN", "layout-zh-rTW"  // multiple destination folders
    }

    values_zh_task {
        from "values-zh"
        to "values-zh-rCN", "values-zh-rTW"
    }
}
```

## Description 
From Android 7.0, the strategy of resolving language resources has changed ([document](https://developer.android.com/guide/topics/resources/multilingual-support.html)). But for Chinese resources, the strategy is not as described as the document. For Android 7.0+, there are two seperated language define:  

| Language | Resource qualifier |
| -------- | -------------------------- |
| Simplified Chinese (Hans) | zh zh_CN zh_SG  |
| Traditional Chinese (Hant) | zh-TW zh-HK zh-MO   |
- Android 7.0+ test result:  

| User Settings | App Resources	| Resource Resolution |
|---------------|---------------|---------------------|
| zh_CN_#Hans | default (en) <br> zh <br> zh_TW | Try zh_CN => Fail <br> Try children of Hans => zh <br> Use zh |
| zh_HK_#Hans | default (en) <br> zh <br> zh_TW <br> zh_HK | Try children of Hans => zh <br> Use zh |
| zh_HK_#Hant | default (en) <br> zh <br> zh_TW | Try zh_HK => Fail <br> Try children of Hant => zh_TW <br> Use zh_TW |
| zh_HK_#Hant | default (en) <br> zh | Try zh_HK => Fail <br> Try children of Hant => Fail  <br> Use default |
| zh_TW_#Hant | default (en) <br> zh | Try zh_TW => Fail <br> Try children of Hant => Fail <br> Use default |

- Android 6.0 test result:  

| User Settings | App Resources	| Resource Resolution |
|---------------|---------------|---------------------|
| zh_CN | default (en) <br> zh <br> zh_TW | Try zh_CN => Fail <br> Try zh => zh <br> Use zh |
| zh_TW | default (en) <br> zh <br> zh_CN | Try zh_TW => Fail <br> Try zh => zh <br> Use zh |

So, to support Simplified Chinese and Traditional Chinese in all devices, I suggeste to provide two resources: zh-CN for Hans and zh-TW for Hant.

However, when you build an app, there maybe some resources which are same for both Hans and Hant. So you need to put these resource both in zh-CN and zh-TW folder. Everytime doing a copy job is bored and may lead to mistake. 

This plugin is writen to resolve problem above. You can just put resources which are same for Hans and Hant in zh folder, such as values-zh, layout_zh, drawable-xhdpi-zh, when build the project, the plugin can help you migrate resources from zh to zh-CN and zh-TW. There will be no zh folder in the final apk, instead two copies of resources are in zh-CN and zh-TW folders separately. 

This plugin can used to migrate any Android resource based on qualifier, not just chinese language qualifier.

License
-------

    Copyright 2019 kxfeng

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
