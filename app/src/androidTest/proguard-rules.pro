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

# This is necessary so that RemoteWorkManager can be initialized (also marked with @Keep)
-keep class androidx.work.multiprocess.RemoteWorkManagerClient {
    public <init>(...);
}
