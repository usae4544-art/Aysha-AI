# Keep Vosk and JNA native bindings intact in R8 release builds
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep Room databases and network clients
-keep class * extends androidx.room.RoomDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn okhttp3.**
-dontwarn okio.**
