# The JNI entry points are resolved by their Java names, so the native bridge and its
# external methods must survive R8 unchanged.
-keep class com.propslab.mathx.LlamaNative { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# WorkManager constructs workers reflectively from their class names.
-keep class com.propslab.work.** extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
