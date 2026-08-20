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

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends dagger.hilt.android.HiltApplication
-keep class * extends dagger.hilt.android.HiltActivity
-keep class * extends dagger.hilt.android.HiltFragment
-keep class * extends dagger.hilt.android.HiltViewModel
-keep class * extends dagger.hilt.android.HiltService
-keep class * extends dagger.hilt.android.HiltBroadcastReceiver

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class * implements androidx.room.RoomDatabase

# Moshi
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# Material3
-keep class androidx.compose.material3.** { *; }