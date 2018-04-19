-dontobfuscate
-dontoptimize
-keepattributes SourceFile,LineNumberTable,Exceptions
-keep class org.fdroid.fdroid.** {*;}
-dontskipnonpubliclibraryclassmembers
-dontwarn android.test.**
-dontwarn com.android.support.test.**

-dontwarn javax.naming.**
-dontwarn org.slf4j.**
-dontnote org.apache.http.**
-dontnote android.net.http.**
-dontnote android.support.**
-dontnote **ILicensingService

# StrongHttpsClient and its support classes are totally unused, so the
# ch.boye.httpclientandroidlib.** classes are also unneeded
-dontwarn info.guardianproject.netcipher.client.**

# These libraries are known to break if minification is enabled on them. They
# use reflection to instantiate classes, for example. If the keep flags are
# removed, proguard will strip classes which are required, which may result in
# crashes.
-keep class kellinwood.security.zipsigner.** {*;}
-keep class org.bouncycastle.** {*;}

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

# The rxjava library depends on sun.misc.Unsafe, which is unavailable on Android
# The rxjava team is aware of this, and mention in the docs that they only use
# the unsafe functionality if the platform supports it.
#  - https://github.com/ReactiveX/RxJava/issues/1415#issuecomment-48390883
#  - https://github.com/ReactiveX/RxJava/blob/1.x/src/main/java/rx/internal/util/unsafe/UnsafeAccess.java#L23
-dontwarn rx.internal.util.**

-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.ext.**
-keep class org.codehaus.** { *; }
-keepclassmembers public final enum org.codehaus.jackson.annotate.JsonAutoDetect$Visibility {
public static final org.codehaus.jackson.annotate.JsonAutoDetect$Visibility *; }
-keep public class your.class.** {
  *;
}