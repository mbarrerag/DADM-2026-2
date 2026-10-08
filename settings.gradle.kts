pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DADM-2026-2"

include(":Reto1")
include(":Reto2")
include(":Reto4")
include(":Reto5")
include(":Reto6")

project(":Reto1").projectDir = file("Reto1/app")
project(":Reto2").projectDir = file("Reto2/app")
project(":Reto4").projectDir = file("Reto4/app")
project(":Reto5").projectDir = file("Reto5/app")
project(":Reto6").projectDir = file("Reto6/app")
