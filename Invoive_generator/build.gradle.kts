plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}

// Keep build output out of OneDrive (thousands of temporary files would sync otherwise).
val outsideBuildRoot = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir"), "AmitabhToursBuild")
allprojects {
    layout.buildDirectory.set(File(outsideBuildRoot, project.name))
}
