-dontobfuscate
-dontoptimize
-keepattributes SourceFile,LineNumberTable,Exceptions
-keep class org.fdroid.fdroid.** {*;}

# Logging
-keep class ch.qos.logback.classic.android.LogcatAppender
-keepclassmembers class ch.qos.logback.** { *; }
-keepclassmembers class  org.slf4j.impl.** { *; }
