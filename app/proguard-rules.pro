-dontobfuscate
-dontoptimize
-keepattributes SourceFile,LineNumberTable,Exceptions
-keep class org.fdroid.fdroid.** {*;}
-dontskipnonpubliclibraryclassmembers
-dontwarn android.test.**

-dontwarn javax.naming.**
-dontwarn org.slf4j.**
-dontnote org.apache.http.**
-dontnote android.net.http.**
-dontnote **ILicensingService

# Needed for espresso https://stackoverflow.com/a/21706087
-dontwarn org.xmlpull.v1.**

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

-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.ext.**
-keep class org.codehaus.** { *; }
-keepclassmembers public final enum org.codehaus.jackson.annotate.JsonAutoDetect$Visibility {
public static final org.codehaus.jackson.annotate.JsonAutoDetect$Visibility *; }
-keep public class org.fdroid.** {
  *;
}

# This is necessary so that RemoteWorkManager can be initialized (also marked with @Keep)
-keep class androidx.work.multiprocess.RemoteWorkManagerClient {
    public <init>(...);
}

-keep class org.acra.config.MailSenderConfiguration {
    public <init>(...);
}