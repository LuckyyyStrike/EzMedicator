-keep class de.ingomohrmann.ezmedicator.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# cron-utils
-keep class com.cronutils.** { *; }
-dontwarn com.cronutils.**
-dontwarn org.slf4j.**
