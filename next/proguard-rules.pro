-dontobfuscate
-keepattributes SourceFile,LineNumberTable,Exceptions

# Anything less causes issues like not finding primary constructor in ReflectionDiffer
-keep class org.fdroid.** {*;}

# Logging
-keep class ch.qos.logback.classic.android.LogcatAppender
-keepclassmembers class ch.qos.logback.** { *; }
-keepclassmembers class  org.slf4j.impl.** { *; }

# Needed for instrumentation tests (for some werid inexplicable reason)
-keep class kotlin.LazyKt
-keep class kotlin.collections.CollectionsKt

# for debugging (comment in when needed)
#-printconfiguration build/outputs/logs/r8-configuration.txt
