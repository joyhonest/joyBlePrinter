# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-keep class com.joyhonest.joyBlePrinter.joyBlePrinterClient.**{*;}

-verbose

#-keepclasseswithmembers class * {
#    native <methods>;
#}

#-keep class com.joyhonest.joyBlePrinter.joyBlePrinterClient.**{*;}
-dontskipnonpubliclibraryclassmembers


-keep interface com.joyhonest.joyBlePrinter.** {
    *;
}

-keep class com.joyhonest.joyBlePrinter.joyBlePrinterClient
{
    public static <methods>;
    private static void onGetData(byte[],int);
}


-dontwarn com.joyhonest.joyBlePrinter.joyBlePrinterClient.**




-keep class com.joyhonest.joyBlePrinter.joyBlePrinter
-keep class com.joyhonest.joyBlePrinter.joyBlePrinter
{
    public boolean isConnected();
}
-keepclassmembers class com.joyhonest.joyBlePrinter.joyBlePrinter {
    java.lang.String sName;
    java.lang.String sMacAddress;
}

