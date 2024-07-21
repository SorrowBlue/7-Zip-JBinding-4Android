// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.library") version "8.5.1" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "com.sorrowblue.sevenzipjbinding"
version = "16.02-2.03"

nexusPublishing {
    repositories {
        sonatype {
            stagingProfileId.set(findProperty("sonatypeStagingProfileId") as? String)
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
