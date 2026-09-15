# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
# Preserve line numbers and source file attributes for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==============================================================================
# General Attributes & Annotations
# ==============================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes *Annotation*

# ==============================================================================
# Retrofit 2 & OkHttp 3 & Okio
# (Retrofit, OkHttp, and Okio supply their own AAR consumer rules)
# ==============================================================================
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ==============================================================================
# Gson & Model Serialized Fields
# (Preserve fields annotated with @SerializedName or model class fields for JSON serialization,
# while allowing class name obfuscation and method optimization/shrinking)
# ==============================================================================
-dontwarn sun.misc.**
-dontwarn com.google.gson.**
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve model class fields for JSON deserialization
-keepclassmembers class com.mohitt.camverz.User { <fields>; }
-keepclassmembers class com.mohitt.camverz.Post { <fields>; }
-keepclassmembers class com.mohitt.camverz.Comment { <fields>; }
-keepclassmembers class com.mohitt.camverz.Message { <fields>; }
-keepclassmembers class com.mohitt.camverz.Conversation { <fields>; }
-keepclassmembers class com.mohitt.camverz.Notification { <fields>; }
-keepclassmembers class com.mohitt.camverz.VerificationSession { <fields>; }
-keepclassmembers class com.mohitt.camverz.RealMeetPost { <fields>; }
-keepclassmembers class com.mohitt.camverz.RealMeetRequest { <fields>; }
-keepclassmembers class com.mohitt.camverz.PartyPost { <fields>; }
-keepclassmembers class com.mohitt.camverz.FantasyPost { <fields>; }
-keepclassmembers class com.mohitt.camverz.RealMeetStore { <fields>; }
-keepclassmembers class com.mohitt.camverz.StoryItem { <fields>; }
-keepclassmembers class com.mohitt.camverz.UserStories { <fields>; }
-keepclassmembers class com.mohitt.camverz.CommunityNotification { <fields>; }
-keepclassmembers class com.mohitt.camverz.api.** { <fields>; }

# ==============================================================================
# Socket.IO & Engine.IO
# ==============================================================================
-dontwarn io.socket.**
-keep class io.socket.client.Socket { *; }
-keep class io.socket.emitter.Emitter { *; }

# ==============================================================================
# WebRTC SDK & JNI Zero
# (Keep JNI callbacks and native method bindings instead of keeping the whole package)
# ==============================================================================
-dontwarn org.webrtc.**
-dontwarn org.jni_zero.**

-keepclasseswithmembers class * {
    native <methods>;
}
-keepclasseswithmembers class * {
    @org.webrtc.CalledByNative *;
}
-keepclasseswithmembers class * {
    @org.jni_zero.CalledByNative *;
}
-keepclasseswithmembers class * {
    @org.jni_zero.NativeMethods *;
}
-keep class org.webrtc.EglBase** { *; }
-keep class org.webrtc.VideoFrame** { *; }
-keep class org.webrtc.SurfaceViewRenderer { *; }
-keep class org.webrtc.TextureViewRenderer { *; }

# ==============================================================================
# Glide
# ==============================================================================
-dontwarn com.bumptech.glide.**
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keepclassmembers class * {
    @com.bumptech.glide.annotation.GlideOption <methods>;
    @com.bumptech.glide.annotation.GlideType <methods>;
}

# ==============================================================================
# Google Play Services, Auth & Ads (AdMob)
# (Google Play Services AARs supply their consumer rules. Keep KeepName annotations)
# ==============================================================================
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.common.annotation.KeepName
-keepnames class * implements com.google.android.gms.common.annotation.KeepName
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# ==============================================================================
# ironSource / LevelPlay SDK & Mediation Adapters
# (Keep mediation adapter classes and public interfaces required for reflection lookup)
# ==============================================================================
-dontwarn com.ironsource.**
-dontwarn com.unity3d.mediation.**
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.RewardedVideoAdapterApi { *; }
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.InterstitialAdapterApi { *; }
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.BannerAdapterApi { *; }
-keep class com.ironsource.mediationsdk.integration.IntegrationHelper { public *; }
-keep class com.ironsource.adapters.** { *; }

# ==============================================================================
# Meta Audience Network (Facebook Ads)
# ==============================================================================
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.infer.annotation.**
-keep class com.facebook.ads.** { public *; }

# ==============================================================================
# Unity Ads SDK & InMobi
# ==============================================================================
-dontwarn com.unity3d.ads.**
-dontwarn com.unity3d.services.**
-dontwarn com.inmobi.**

# ==============================================================================
# Google Tink Crypto & Transitive Warnings
# ==============================================================================
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.api.client.http.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.****