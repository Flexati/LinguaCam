# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Timber
-keep class timber.log.Timber { *; }

# Play Billing — la libreria mantiene le sue keep rules ma rendiamo esplicito
# il contratto listener / SDK response codes.
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-keepclassmembers class * extends com.android.billingclient.** { *; }

# Keep all classes in our app package
-keep class com.lingolens.** { *; }

# Keep data classes
-keepclassmembers class com.lingolens.** {
    *** get*();
    void set*(***);
}

# Remove logging in release builds
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
