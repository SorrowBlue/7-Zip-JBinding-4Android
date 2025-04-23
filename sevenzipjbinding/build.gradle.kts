import com.android.build.api.variant.BuildConfigField
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "com.sorrowblue.sevenzipjbinding"
version = "16.02-2.03"

android {
    namespace = "net.sf.sevenzipjbinding"
    compileSdk = 36
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        all {
        }
    }
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
        }
    }
    buildFeatures {
        buildConfig = true
    }
    packaging.jniLibs.keepDebugSymbols.add("**/*.so")
}

java {
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Provider を取得
val gitTagProvider: Provider<String> = providers.of(GitTagValueSource::class) {}

androidComponents {
    onVariants(selector().all()) { variant ->
        val tag = checkNotNull(gitTagProvider.orNull) { "No git tag found." }
        val version = checkNotNull(releaseVersionOrSnapshot(tag)) { "git tag is not valid." }
        val timeStr = LocalDateTime.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        variant.buildConfigFields.set(
            mapOf(
                "VERSION_NAME" to BuildConfigField("String", "\"$version\"", ""),
                "BUILT_DATE" to BuildConfigField("String", "\"$timeStr\"", "")
            )
        )
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

extensions.configure<PublishingExtension> {
    repositories {
        mavenLocal()
    }
}

mavenPublishing {
    val artifactId = "7-Zip-JBinding-4Android"
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true
        )
    )
    afterEvaluate {
        val tag = checkNotNull(gitTagProvider.orNull) { "No git tag found." }
        val version = checkNotNull(releaseVersionOrSnapshot(tag)) { "git tag is not valid." }
        coordinates(groupId = group.toString(), artifactId = artifactId, version = version)
        logger.lifecycle("publish ${group}:$artifactId:$version")
    }

    pom {
        name.set(artifactId)
        description.set("7-Zip-JBinding-4Android")
        inceptionYear.set("2024")
        url.set("https://github.com/SorrowBlue/7-Zip-JBinding-4Android")
        licenses {
            license {
                name.set("GNU Lesser General Public License version 2.1")
                url.set("https://opensource.org/license/LGPL-2.1")
            }
        }
        developers {
            developer {
                id.set("sorrowblue")
                name.set("Sorrow Blue")
                url.set("https://github.com/SorrowBlue")
            }
        }
        scm {
            url.set("https://github.com/SorrowBlue/7-Zip-JBinding-4Android")
            connection.set("scm:git:https://github.com/SorrowBlue/7-Zip-JBinding-4Android.git")
            developerConnection.set("scm:git:ssh://git@github.com/SorrowBlue/7-Zip-JBinding-4Android.git")
        }
    }
}

fun releaseVersionOrSnapshot(tag: String): String? {
    val regex = Regex("""(^\d+\.\d+\-\d+\.)(\d+)([\w-]*)$""")
    val groups = regex.find(tag)?.groups ?: return null
    return if (groups.size == 4) {
        if (groups[3]?.value?.isEmpty() == true) {
            groups.first()!!.value
        } else {
            "${groups[1]!!.value}${groups[2]!!.value.toInt().plus(1)}-SNAPSHOT"
        }
    } else {
        null
    }
}

// パラメータは不要だが、インターフェースとして定義が必要
interface GitTagParameters : ValueSourceParameters

// Gitコマンドを実行して最新タグを取得するValueSource
abstract class GitTagValueSource @Inject constructor(
    private val execOperations: ExecOperations
) : ValueSource<String, GitTagParameters> {

    override fun obtain(): String {
        return try {
            // 標準出力をキャプチャするためのByteArrayOutputStream
            val stdout = ByteArrayOutputStream()
            // git describe コマンドを実行
            val result = execOperations.exec {
                // commandLine("git", "tag", "--sort=-creatordate") // もし作成日時順の最新タグが良い場合
                commandLine("git", "describe", "--tags", "--abbrev=1")
                standardOutput = stdout
                // エラーが発生してもGradleビルドを止めないようにし、戻り値で判断
                isIgnoreExitValue = true
                // エラー出力は捨てる (必要ならキャプチャも可能)
                errorOutput = ByteArrayOutputStream()
            }

            if (result.exitValue == 0) {
                // 成功したら標準出力をトリムして返す
                stdout.toString().trim().removePrefix("v")
            } else {
                // gitコマンド失敗時 (タグがない、gitリポジトリでない等)
                println("Warning: Could not get git tag. (Exit code: ${result.exitValue})")
                "UNKNOWN" // または適切なデフォルト値
            }
        } catch (e: Exception) {
            // その他の予期せぬエラー
            println("Warning: Failed to execute git command: ${e.message}")
            "UNKNOWN" // または適切なデフォルト値
        }
    }
}
