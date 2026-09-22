# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source file attributes for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==============================================================================
# R8 Optimization & Repackaging for Maximum DEX Shrinking & Obfuscation (>45%)
# ==============================================================================
-repackageclasses 'a'
-allowaccessmodification
-optimizationpasses 5
-overloadaggressively

# ==============================================================================
# General Attributes & Annotations
# ==============================================================================
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes *Annotation*

# Preserve @Keep annotated classes and methods
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}

# ==============================================================================
# Retrofit 2 & OkHttp 3 & Okio
# ==============================================================================
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ==============================================================================
# Gson & Model Serialized Fields
# ==============================================================================
-dontwarn sun.misc.**
-dontwarn com.google.gson.**
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve model class fields for JSON deserialization
-keepclassmembers class com.mohit.camverz.User { <fields>; }
-keepclassmembers class com.mohit.camverz.Post { <fields>; }
-keepclassmembers class com.mohit.camverz.Comment { <fields>; }
-keepclassmembers class com.mohit.camverz.Message { <fields>; }
-keepclassmembers class com.mohit.camverz.Conversation { <fields>; }
-keepclassmembers class com.mohit.camverz.Notification { <fields>; }
-keepclassmembers class com.mohit.camverz.VerificationSession { <fields>; }
-keepclassmembers class com.mohit.camverz.RealMeetPost { <fields>; }
-keepclassmembers class com.mohit.camverz.RealMeetRequest { <fields>; }
-keepclassmembers class com.mohit.camverz.PartyPost { <fields>; }
-keepclassmembers class com.mohit.camverz.FantasyPost { <fields>; }
-keepclassmembers class com.mohit.camverz.RealMeetStore { <fields>; }
-keepclassmembers class com.mohit.camverz.StoryItem { <fields>; }
-keepclassmembers class com.mohit.camverz.UserStories { <fields>; }
-keepclassmembers class com.mohit.camverz.CommunityNotification { <fields>; }
-keepclassmembers class com.mohit.camverz.api.** { <fields>; }

# ==============================================================================
# Socket.IO & Engine.IO
# ==============================================================================
-dontwarn io.socket.**
-keepclassmembers class io.socket.client.Socket { *; }
-keepclassmembers class io.socket.emitter.Emitter { *; }

# ==============================================================================
# WebRTC SDK & JNI Zero
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
# Google Play Services & Ads (AdMob) & ML Kit
# ==============================================================================
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.common.annotation.KeepName
-keepnames class * implements com.google.android.gms.common.annotation.KeepName
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# ==============================================================================
# ironSource / LevelPlay SDK & Mediation Adapters
# ==============================================================================
-dontwarn com.ironsource.**
-dontwarn com.unity3d.mediation.**
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.RewardedVideoAdapterApi { *; }
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.InterstitialAdapterApi { *; }
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.BannerAdapterApi { *; }
-keep class com.ironsource.adapters.**

# ==============================================================================
# Meta Audience Network (Facebook Ads) - Optimized
# ==============================================================================
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.infer.annotation.**
-keep class com.facebook.ads.AudienceNetworkActivity { *; }
-keep class com.facebook.ads.internal.NetworkSettings { *; }

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