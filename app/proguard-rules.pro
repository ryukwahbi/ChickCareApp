# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Suppress warnings from Google Play Services
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**

# Keep Google Play Services classes
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }

# Suppress ProviderInstaller warnings
-dontwarn com.google.android.gms.security.ProviderInstaller**

# Fix for Google Play Services Location companion object warning
# Keep all members of internal location classes (includes companion objects)
-keepclassmembers class com.google.android.gms.internal.location.** {
    *;
}

# Keep companion objects for all Google Play Services classes
# This will preserve Kotlin companion objects during R8/ProGuard processing
-keepclassmembers class com.google.android.gms.** {
    static ** Companion;
}

# Suppress reflection warnings for Google Play Services
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}