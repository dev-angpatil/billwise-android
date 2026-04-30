buildscript {
    ext {
        compose_version = "1.5.4"
        room_version = "2.6.1"
        nav_version = "2.7.7"
        lifecycle_version = "2.7.0"
        retrofit_version = "2.9.0"
    }
}
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
