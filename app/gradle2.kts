plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.softopus.grundrechenarten"
    compilesdk = 35

    defaultconfig {
        applicationid = "com.softopus.grundrechenarten"
        minsdk = 24
        targetsdk = 35
        versioncode = 1
        versionname = "1.03.15"

        testinstrumentationrunner = "androidx.test.runner.androidjunitrunner"
    }

    buildtypes {
        release {
            isminifyenabled = false
            proguardfiles(
                getdefaultproguardfile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileoptions {
        sourcecompatibility = javaversion.version_11
        targetcompatibility = javaversion.version_11
    }
    kotlinoptions {
        jvmtarget = "11"
    }
    buildfeatures {
        compose = true
    }
    sourcesets {
        getbyname("main") {
            assets {
                srcdirs("src\\main\\assets", "src\\main\\assets")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testimplementation(libs.junit)
    androidtestimplementation(libs.androidx.junit)
    androidtestimplementation(libs.androidx.espresso.core)
    androidtestimplementation(platform(libs.androidx.compose.bom))
    androidtestimplementation(libs.androidx.ui.test.junit4)
    debugimplementation(libs.androidx.ui.tooling)
    debugimplementation(libs.androidx.ui.test.manifest)
}