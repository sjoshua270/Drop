# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in F:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes Signature
-keepattributes *Annotation*
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# To keep models intact for Firebase Database
-keepclassmembers class com.rethink.drop.models.** { *; }
-keepclassmembers class com.rethink.drop.viewholders.** { *; }

-dontwarn okio.**
-dontwarn retrofit2.**

# Fabric Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-printmapping mapping.txt
-dontwarn io.fabric.sdk.android.services.common.IdManager