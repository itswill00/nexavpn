# R8 rules for NexaVPN

# WireGuard library (config parser uses reflection-free but keep names for
# wg-quick INI keys mapping)
-keep class com.wireguard.config.** { *; }
-keep class com.wireguard.crypto.** { *; }
-keep class com.wireguard.android.backend.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *** INSTANCE; }
-keepclassmembers class com.nexavpn.app.data.** { *** Companion; }
-keepclasseswithmembers class com.nexavpn.app.data.** { public static **$Companion Companion; }
-keepclasseswithmembers class com.nexavpn.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
