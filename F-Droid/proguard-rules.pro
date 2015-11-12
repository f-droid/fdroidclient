-dontobfuscate
-dontoptimize
-keepattributes SourceFile,LineNumberTable,Exceptions
-keep class org.fdroid.fdroid.** {*;}
-dontskipnonpubliclibraryclassmembers
-dontwarn android.test.**
-dontwarn com.android.support.test.**

-dontwarn javax.naming.**
-dontnote android.support.**
-dontnote **ILicensingService

# These libraries are known to break if minification is enabled on them. They
# use reflection to instantiate classes, for example. If the keep flags are
# removed, proguard will strip classes which are required, which may result in
# crashes.
-keep class kellinwood.security.zipsigner.** {*;}
-keep class org.spongycastle.** {*;}

# This keeps class members used for SystemInstaller IPC.
#   Reference: https://gitlab.com/fdroid/fdroidclient/issues/79
-keepclassmembers class * implements android.os.IInterface {
    public *;
}

# Samsung Android 4.2 bug
# https://code.google.com/p/android/issues/detail?id=78377
-keepnames class !android.support.v7.internal.view.menu.**, ** {*;}

-keep public class android.support.v7.widget.** {*;}
-keep public class android.support.v7.internal.widget.** {*;}

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}
