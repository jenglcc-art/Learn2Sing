# Add project specific ProGuard rules here.
# Keep Retrofit/Gson model classes
-keep class com.learn2sing.app.api.** { *; }
-keep class com.learn2sing.app.VideoItem { *; }
-keep class com.learn2sing.app.LyricsLine { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
