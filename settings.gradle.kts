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

rootProject.name = "Loorve"

// 현재 모듈
include(":app")

// 멀티모듈 추가 시 아래에 include(":feature:xxx"), include(":core:xxx") 형태로 추가
// include(":core:network")
// include(":feature:home")
