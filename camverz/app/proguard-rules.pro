# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source file attributes for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Enable optimization passes and access modification for high DEX optimization score
-optimizationpasses 5
-allowaccessmodification

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
# AndroidX App Startup, WorkManager & Room Database
# ==============================================================================
-dontwarn androidx.startup.**
-keepnames class androidx.startup.**
-keepnames class * implements androidx.startup.Initializer
-keepclassmembers class * implements androidx.startup.Initializer {
    <init>();
}

-dontwarn androidx.work.**
-keepnames class androidx.work.impl.WorkDatabase**
-keepclassmembers class androidx.work.impl.WorkDatabase** {
    <init>();
}
-keepnames class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keepclassmembers class * extends androidx.work.ListenableWorker {
    <init>(...);
}

# ==============================================================================
# Google ML Kit & Component Discovery / Registrars
# ==============================================================================
-dontwarn com.google.mlkit.**
-keep class * implements com.google.mlkit.common.sdkinternal.ComponentRegistrar {
    <init>();
}
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    <init>();
}
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepnames class com.google.mlkit.vision.face.internal.FaceRegistrar
-keepnames class com.google.mlkit.common.internal.CommonComponentRegistrar
-keepnames class com.google.mlkit.vision.common.internal.VisionCommonRegistrar
-keepclassmembers class com.google.mlkit.vision.face.internal.FaceRegistrar {
    <init>();
}
-keepclassmembers class com.google.mlkit.common.internal.CommonComponentRegistrar {
    <init>();
}
-keepclassmembers class com.google.mlkit.vision.common.internal.VisionCommonRegistrar {
    <init>();
}

# ==============================================================================
# Google Play Services & Auth & Google Sign-In
# ==============================================================================
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.auth.api.signin.internal.** { *; }
-keep class com.google.android.gms.auth.api.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.common.annotation.KeepName
-keepnames class * implements com.google.android.gms.common.annotation.KeepName
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keepclassmembers class com.google.android.gms.auth.api.signin.GoogleSignInOptions { *; }
-keepclassmembers class com.google.android.gms.auth.api.signin.GoogleSignInAccount { *; }
-keepclassmembers class com.google.android.gms.auth.api.signin.GoogleSignInClient { *; }

# Prevent R8 / ProGuard from conflicting org.json against Android framework org.json
-dontwarn org.json.**
-keep class org.json.** { *; }
-keepclassmembers class org.json.** { *; }

# ==============================================================================
# Retrofit 2 & OkHttp 3 & Okio
# ==============================================================================
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# ==============================================================================
# Gson & Model Serialized Fields
# ==============================================================================
-dontwarn sun.misc.**
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.InstanceCreator { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve model class fields for JSON deserialization
-keep class com.mohit.camverz.User { *; }
-keep class com.mohit.camverz.Post { *; }
-keep class com.mohit.camverz.Comment { *; }
-keep class com.mohit.camverz.Message { *; }
-keep class com.mohit.camverz.Conversation { *; }
-keep class com.mohit.camverz.Notification { *; }
-keep class com.mohit.camverz.VerificationSession { *; }
-keep class com.mohit.camverz.RealMeetPost { *; }
-keep class com.mohit.camverz.RealMeetRequest { *; }
-keep class com.mohit.camverz.PartyPost { *; }
-keep class com.mohit.camverz.FantasyPost { *; }
-keep class com.mohit.camverz.RealMeetStore { *; }
-keep class com.mohit.camverz.StoryItem { *; }
-keep class com.mohit.camverz.UserStories { *; }
-keep class com.mohit.camverz.CommunityNotification { *; }
-keep class com.mohit.camverz.LanguageModel { *; }
-keep class com.mohit.camverz.api.** { *; }

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

-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }

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
# ironSource / LevelPlay SDK & Mediation Adapters
# ==============================================================================
-dontwarn com.ironsource.**
-dontwarn com.unity3d.mediation.**
-keep class com.ironsource.** { *; }
-keepclassmembers class com.ironsource.** { *; }
-keep class com.unity3d.mediation.** { *; }
-keepclassmembers class com.unity3d.mediation.** { *; }
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.RewardedVideoAdapterApi { *; }
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.InterstitialAdapterApi { *; }
-keepclassmembers class * implements com.ironsource.mediationsdk.sdk.BannerAdapterApi { *; }
-keep class com.ironsource.adapters.** { *; }
-keep class com.ironsource.mediationsdk.** { *; }
-keep class com.ironsource.lifecycle.** { *; }

# ==============================================================================
# Meta Audience Network (Facebook Ads)
# ==============================================================================
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.infer.annotation.**
-keep public class com.facebook.ads.** { public protected *; }
-keeppackagenames com.facebook.*

# ==============================================================================
# Google Mobile Ads (AdMob) & Mediation Adapters
# ==============================================================================
-dontwarn com.google.android.gms.ads.**
-keep public class com.google.android.gms.ads.** { public protected *; }
-dontwarn com.google.ads.mediation.facebook.**
-keep class com.google.ads.mediation.facebook.** { *; }
-keepclassmembers class com.google.ads.mediation.facebook.** { *; }

# ==============================================================================
# Unity Ads SDK, Protobuf & Gateway Protocol
# ==============================================================================
-dontwarn com.unity3d.ads.**
-dontwarn com.unity3d.services.**
-dontwarn com.unity3d.**
-dontwarn com.inmobi.**
-dontwarn gatewayprotocol.**
-dontwarn com.google.protobuf.**
-keep class com.unity3d.** { *; }
-keepclassmembers class com.unity3d.** { *; }
-keep class gatewayprotocol.** { *; }
-keepclassmembers class gatewayprotocol.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class com.google.protobuf.** { *; }

# Keep JavascriptInterface required for Unity Ads & In-App WebView
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ==============================================================================
# Google Tink Crypto & Transitive Warnings
# ==============================================================================
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.api.client.http.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**