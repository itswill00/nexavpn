# R8 rules for NexaVPN

# WireGuard library (config parser uses reflection-free but keep names for
# wg-quick INI keys mapping)
-keep class com.wireguard.config.** { *; }
-keep class com.wireguard.crypto.** { *; }
-keep class com.wireguard.android.backend.** { *; }

# kotlinx.serialization — CRITICAL: keep @SerialName and serializer metadata,
# otherwise R8 strips annotation values and JSON parsing silently breaks in
# release builds (fields fall back to Kotlin property names).
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *** INSTANCE; }
-keepclassmembers class com.nexavpn.app.data.** { *** Companion; }
-keepclasseswithmembers class com.nexavpn.app.data.** {
    public static **$Companion Companion;
}
-keepclasseswithmembers class com.nexavpn.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep the SerialName annotation and its value
-keep @kotlinx.serialization.SerialName class * { *; }
-keep class kotlinx.serialization.SerialName { *; }
-keep class kotlinx.serialization.Serializable { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# kotlinx.serialization official R8 rules (full mode)
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep SerialName values (annotation default values are read reflectively by the plugin-generated code)
-keepattributes AnnotationDefault
-keep class kotlinx.serialization.annotations.** { *; }

# Generated serializer classes (VpnServer$$serializer etc.) live inside the
# same package as the data classes — keep them entirely.
-keep,includedescriptorclasses class com.nexavpn.app.data.**$$serializer { *; }
-keepclassmembers class com.nexavpn.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.nexavpn.app.data.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
