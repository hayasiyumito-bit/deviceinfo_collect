
# -dontoptimize

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

#-keep class com.android.device.DInfo{*;}
-keep class com.xxxx.sentry.Sentry{*;}
-repackageclasses 'com.android.device'
-repackageclasses 'com.xxxx.sentry'