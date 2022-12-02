-dontoptimize
-dontwarn
-dontobfuscate

-dontwarn android.test.**
-dontwarn android.support.test.**
-dontnote junit.framework.**
-dontnote junit.runner.**

# Uncomment this if you use Mockito
#-dontwarn org.mockito.**

-keep class org.hamcrest.** { *; }
-dontwarn org.hamcrest.**

-keep class org.junit.** { *; }
-dontwarn org.junit.**

-keep class junit.** { *; }
-dontwarn junit.**

-keep class androidx.arch.core.executor.ArchTaskExecutor {*;}

-keep class org.fdroid.download.Mirror {
    public static *** fromStrings(***);
}

# This is necessary so that RemoteWorkManager can be initialized (also marked with @Keep)
-keep class androidx.work.WorkManager { *; }
