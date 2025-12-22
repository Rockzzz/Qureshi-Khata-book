# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# ========== GOOGLE API CLIENT ==========
# Keep fields annotated with @Key
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# Keep GenericJson
-keep class * extends com.google.api.client.json.GenericJson { *; }

# Keep Google Drive API classes
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }

# Suppress warnings
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontwarn com.google.android.gms.**
-dontwarn sun.misc.Unsafe
-dontwarn java.nio.file.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# ========== GSON ==========
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements java.lang.reflect.Type

# Keep Gson TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep data classes used for backup
-keep class com.example.tabelahisabapp.data.backup.** { *; }
-keep class com.example.tabelahisabapp.data.db.entity.** { *; }

# ========== ROOM ==========
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ========== HILT ==========
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }