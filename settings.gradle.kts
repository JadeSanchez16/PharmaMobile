rootProject.name = "PHARMAMOBILEE"

pluginManagement {
    buildscript {
        repositories { google(); mavenCentral() }
        dependencies {
            // Kotlin 2.4 requiere R8 >= 9.1.29. Primera version estable publicada compatible.
            classpath("com.android.tools:r8:9.1.31")
        }
    }
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")
