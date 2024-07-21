plugins {
    id("com.android.library")
    `maven-publish`
    signing
}

group = "com.sorrowblue.sevenzipjbinding"
version = "16.02-2.03"

android {
    namespace = "net.sf.sevenzipjbinding"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        val release by getting {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

val androidSourcesJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
    from((android.sourceSets.getByName("main").kotlin.srcDirs() as com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet).srcDirs)
}
artifacts {
    archives(androidSourcesJar)
}

afterEvaluate {
    publishing {
        publications {
            val release by creating(MavenPublication::class) {
                artifactId = "7-Zip-JBinding-4Android"
                logger.lifecycle("Publish Library : $group:$artifactId:$version")
                components.forEach {
                    logger.lifecycle(it.name)
                }
                from(components["release"])
                pom {
                    name.set(artifactId)
                    description.set("7-Zip-JBinding-4Android")
                    url.set("https://github.com/SorrowBlue/7-Zip-JBinding-4Android")
                    licenses {
                        license {
                            name.set("GNU Lesser General Public License version 2.1")
                            url.set("https://opensource.org/license/LGPL-2.1")
                        }
                    }
                    developers {
                        developer {
                            id.set("sorrowblue_sb")
                            name.set("Sorrow Blue")
                            email.set("sorrowblue.sb@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:github.com/SorrowBlue/7-Zip-JBinding-4Android.git")
                        developerConnection.set("scm:git:ssh://github.com/SorrowBlue/7-Zip-JBinding-4Android.git")
                        url.set("https://github.com/SorrowBlue/7-Zip-JBinding-4Android/tree/master")
                    }
                }
            }
        }
    }
    signing {
        if (!hasProperty("signing.secretKeyRingFile")) {
            val signingKeyId: String? by project
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        }
        sign(publishing.publications)
    }
}
