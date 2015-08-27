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

# The nature of the Java security suite implementations are that they use a
# lot of reflection to instantiate classes. The end result is that proguard
# excludes classes which may be required, depending on the security algorithms
# required by certain certificates.
#   Reference: https://gitlab.com/fdroid/fdroidclient/issues/88
-keep class kellinwood.** {*;}
-keep class javax.jmdns.** {*;}
-keep class org.spongycastle.** {*;}
-keep class eu.chainfire.** {*;}

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
