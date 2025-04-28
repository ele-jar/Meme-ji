// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    // Removed: alias(libs.plugins.google.gms.google.services) apply false
    // Removed: alias(libs.plugins.hilt) apply false
}
true // Needed to make the plugins block work without applying plugins directly
