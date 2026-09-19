# ----------------------------------------------------
# Shizuku API, Service & Binder Reflection
# ----------------------------------------------------
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class moe.shizuku.server.** { *; }
-keepclassmembers class * implements rikka.shizuku.Shizuku$* { *; }
-dontwarn rikka.shizuku.**

# ضمان حماية ميثود newProcess المستدعاة عبر getDeclaredMethod

# ----------------------------------------------------
# Room Database, Entities & DAOs
# ----------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep interface * extends androidx.room.RoomDatabase
-keep class com.debloat.hyperos.data.entity.** { *; }
-keep interface com.debloat.hyperos.data.dao.** { *; }
-dontwarn androidx.room.paging.**

# ----------------------------------------------------
# Kotlinx Serialization & App Data Models
# ----------------------------------------------------
-keepattributes *Annotation*, InnerClasses, Signature

# Presets Models
-keepclasseswithmembers class com.debloat.hyperos.data.PresetRoot { *; }
-keepclasseswithmembers class com.debloat.hyperos.data.PresetCategory { *; }
-keepclasseswithmembers class com.debloat.hyperos.data.PresetApp { *; }
-keep,includedescriptorclasses class com.debloat.hyperos.data.**$$serializer { *; }

# حماية أي Data Class مستخدم في التحديثات (UpdateChecker)
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# ----------------------------------------------------
# Coroutines & Flow Internals
# ----------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}