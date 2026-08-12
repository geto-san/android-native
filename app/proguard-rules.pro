# Add project specific ProGuard rules here.
# https://developer.android.com/studio/build/shrinker

# SQLCipher's native layer looks up SQLiteDatabase's fields (e.g. mNativeHandle) via JNI at
# library-load time, invisible to R8's usage analysis since nothing in Kotlin/Java code
# references them directly. Without this, a minified build aborts on startup with
# "NoSuchFieldError: no J field mNativeHandle" the moment libsqlcipher.so loads - a real
# crash found by actually installing and launching a minified build, not a hypothetical.
-keep class net.sqlcipher.** { *; }
-keep interface net.sqlcipher.** { *; }
