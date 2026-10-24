plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.metrolauncher.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.metrolauncher.tv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            res.srcDir(layout.buildDirectory.dir("generated/res/launcherAssets"))
        }
    }
}

val localFontsDir = file("${project.rootDir}/local-assets/fonts")
val localIconsDir = file("${project.rootDir}/local-assets/icons")
val baseFontsDir = file("${projectDir}/src/main/res/font")
val generatedResDir = layout.buildDirectory.dir("generated/res/launcherAssets")

val prepareLauncherAssets = tasks.register("prepareLauncherAssets") {
    val localSegoe = File(localFontsDir, "segoeui.ttf")
    val localMdl2 = File(localFontsDir, "segmdl2.ttf")
    val fallbackInter = File(baseFontsDir, "inter_regular.ttf")
    val fallbackMdl2 = File(baseFontsDir, "segmdl2_fallback.ttf")
    val destFontDir = generatedResDir.get().asFile.resolve("font")
    val destDrawableDir = generatedResDir.get().asFile.resolve("drawable-nodpi")

    inputs.dir(baseFontsDir)
    if (localFontsDir.exists()) inputs.dir(localFontsDir)
    if (localIconsDir.exists()) inputs.dir(localIconsDir)
    outputs.dir(generatedResDir)

    doLast {
        destFontDir.mkdirs()
        // 1. Resolve UI font (Local Segoe UI if provided by developer, else bundled Inter)
        if (localSegoe.exists()) {
            logger.lifecycle("[Launcher Assets] Using local proprietary Segoe UI font for build.")
            localSegoe.copyTo(File(destFontDir, "segoeui.ttf"), overwrite = true)
        } else {
            logger.lifecycle("[Launcher Assets] Local Segoe UI not found. Using bundled open-source Inter font.")
            fallbackInter.copyTo(File(destFontDir, "segoeui.ttf"), overwrite = true)
        }

        // 2. Resolve Icon font (Local Segoe MDL2 Assets if provided by developer, else bundled open-source icons)
        if (localMdl2.exists()) {
            logger.lifecycle("[Launcher Assets] Using local proprietary Segoe MDL2 Assets font for build.")
            localMdl2.copyTo(File(destFontDir, "segmdl2.ttf"), overwrite = true)
        } else {
            logger.lifecycle("[Launcher Assets] Local Segoe MDL2 not found. Using bundled open-source Material Icons fallback.")
            fallbackMdl2.copyTo(File(destFontDir, "segmdl2.ttf"), overwrite = true)
        }

        // 3. Resolve Local App Brand Icons (e.g. Projectivy icons placed in local-assets/icons)
        if (localIconsDir.exists()) {
            val iconFiles = localIconsDir.listFiles { f -> f.extension == "png" } ?: emptyArray()
            if (iconFiles.isNotEmpty()) {
                destDrawableDir.mkdirs()
                iconFiles.forEach { iconFile ->
                    iconFile.copyTo(File(destDrawableDir, iconFile.name), overwrite = true)
                }
                logger.lifecycle("[Launcher Assets] Injected ${iconFiles.size} local custom app brand icons into build.")
            }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareLauncherAssets)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.leanback:leanback:1.0.0")
}
