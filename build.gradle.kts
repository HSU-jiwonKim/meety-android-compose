// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
<<<<<<< HEAD
    id("com.google.gms.google-services") version "4.4.0" apply false
=======
    id("com.google.gms.google-services") version "4.4.4" apply false
>>>>>>> ca178ecdf5d6d6fc1948225597641ad0fce1c061
}