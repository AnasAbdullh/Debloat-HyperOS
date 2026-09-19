# Shizuku API uses AIDL-generated binder classes reflectively.
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }

# Room entities/DAOs referenced via generated code.
-keep class com.debloat.hyperos.data.entity.** { *; }

# Kotlinx Serialization: keep serializer() companions for our preset models.
-keepattributes *Annotation*, InnerClasses
-keepclasseswithmembers class com.debloat.hyperos.data.PresetRoot { *; }
-keepclasseswithmembers class com.debloat.hyperos.data.PresetCategory { *; }
-keepclasseswithmembers class com.debloat.hyperos.data.PresetApp { *; }
-keep,includedescriptorclasses class com.debloat.hyperos.data.**$$serializer { *; }
# Shizuku Service & IPC Reflection
-keep class moe.shizuku.server.** { *; }
-keepclassmembers class * implements rikka.shizuku.Shizuku$* { *; }
-dontwarn rikka.shizuku.**

# Room Database implementation & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep interface * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlin Coroutines & Flow Internals
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
