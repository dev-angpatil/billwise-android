buildscript {
    extra["compose_version"]   = "1.5.4"
    extra["room_version"]      = "2.6.1"
    extra["nav_version"]       = "2.7.7"
    extra["lifecycle_version"] = "2.7.0"
    extra["retrofit_version"]  = "2.9.0"
}
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
