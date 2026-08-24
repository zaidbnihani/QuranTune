# Add project specific ProGuard rules here.
-dontwarn **
-ignorewarnings

# Keep Reactor / Blockhound missing classes
-dontwarn reactor.blockhound.**

# Keep Kotlin & Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}

# Keep Firebase and Google Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep META-INF services
-keepresources META-INF/services/**

