# R8 rules for the release build.
#
# Hilt, Room, Compose, Navigation 3 and kotlinx.serialization ship their own keep rules inside
# their AARs/JARs, so nothing library-specific is needed here. Add rules only for code that is
# reached by reflection and isn't covered by those (none at the moment).

# Keep file names and line numbers so crash stack traces from release builds can be decoded
# with the mapping file (app/build/outputs/mapping/release/mapping.txt).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose/debug/info logs from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
