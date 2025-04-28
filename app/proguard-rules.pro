# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in androidx.navigation.safeargs.gradle.plugin

# Keep application class
-keep class com.example.memesji.MemesJiApp { *; }

# Keep activities
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# Keep fragments (including generated NavArgs classes if applicable)
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.fragment.app.DialogFragment
-keep class **/*Args

# Keep ViewModels and their constructors
-keep class androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.AndroidViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep data classes (used by Retrofit/Gson and Parcelize)
-keepclasseswithmembers class com.example.memesji.data.** {
    *;
}
-keep class com.example.memesji.data.** { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep interfaces used by Retrofit
-keep interface com.example.memesji.data.remote.ApiService { *; }

# Keep Retrofit classes (including internal OkHttp/Okio if needed)
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# Gson rules (ensure models are kept and serialization works)
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.TypeAdapter

# Glide rules
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType
-keep public enum com.bumptech.glide.load.resource.bitmap.DownsampleStrategy$SampleSizeRounding

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepnames class kotlinx.coroutines.android.** { *; }
-keepnames class kotlinx.coroutines.flow.** { *; }
-keepclassmembers class kotlinx.coroutines.flow.** {
    *;
}

# Keep specific classes used via reflection or JNI if any (unlikely for this app)

# Keep resource names (like R class members) if accessed via reflection (usually not needed)
#-keepclassmembers class **.R$* {
#    public static <fields>;
#}

# Keep ViewBinding classes if reflection is used (usually not needed with direct access)
#-keep class * extends androidx.viewbinding.ViewBinding { *; }

# Add any other specific rules for other libraries if needed

# From original file, keep data/ApiService
-keep class com.example.memesji.data.** { *; }
-keep interface com.example.memesji.data.remote.ApiService { *; }
