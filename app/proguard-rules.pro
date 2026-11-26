# Keep Vosk and JNA native bindings intact in R8 release builds
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** {
    native <methods>;
    *mNativeData;
}
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Room entities/DAOs and OkHttp
-keep class * extends androidx.room.RoomDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okhttp3.**
-dontwarn okio.**
