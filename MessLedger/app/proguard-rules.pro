# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Firebase keep rules
-keep class com.google.firebase.** { *; }
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
}

# Hilt keep rules
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedEntryPoint
-keep,allowobfuscation,allowshrinking interface dagger.hilt.internal.GeneratedComponent

# Models keep rules for Firestore serialization
-keep class com.messledger.app.data.model.** { *; }
