# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==============================================================================
# Retrofit 2
# ==============================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ==============================================================================
# Gson
# ==============================================================================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.mohitt.camverz.api.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ==============================================================================
# Socket.IO & Engine.IO
# ==============================================================================
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# ==============================================================================
# WebRTC SDK
# ==============================================================================
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# ==============================================================================
# Glide
# ==============================================================================
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**
-keepclassmembers class * {
    @com.bumptech.glide.annotation.GlideOption <methods>;
    @com.bumptech.glide.annotation.GlideType <methods>;
}

# ==============================================================================
# Google Play Services & Sign-In
# ==============================================================================
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }

# ==============================================================================
# Meta Audience Network (Facebook)
# ==============================================================================
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.infer.annotation.**
-keep class com.facebook.ads.** { *; }

# ==============================================================================
# Model classes (kept to prevent R8 minification from breaking Gson serialization)
# ==============================================================================
-keep class com.mohitt.camverz.User { *; }
-keep class com.mohitt.camverz.Post { *; }
-keep class com.mohitt.camverz.Comment { *; }
-keep class com.mohitt.camverz.Message { *; }
-keep class com.mohitt.camverz.Conversation { *; }
-keep class com.mohitt.camverz.Notification { *; }
-keep class com.mohitt.camverz.VerificationSession { *; }