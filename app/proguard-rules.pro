# Release build rules. Room, Media3, Coil and Compose ship their own consumer rules; these only
# keep what this app looks up by name.

# Library data: Room entities and enums stored or restored by name (sort orders, theme, alarms).
-keep class com.ttggg5.holovoice.data.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep line numbers so crash reports stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
