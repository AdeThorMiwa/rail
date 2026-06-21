allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

// Plugin subprojects may target older compileSdk versions whose transitive
// dependencies now require SDK 34+. Override after each subproject evaluates,
// skipping :app which manages its own settings.
subprojects {
    if (name != "app") {
        afterEvaluate {
            extensions.findByType<com.android.build.api.dsl.LibraryExtension>()?.compileSdk = 34
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
