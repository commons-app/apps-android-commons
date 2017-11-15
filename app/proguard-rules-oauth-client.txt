# Needed to keep generic types and @Key annotations accessed via reflection
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

-keepclasseswithmembers class * {
  @com.google.api.client.util.Key <fields>;
}

-keepclasseswithmembers class * {
  @com.google.api.client.util.Value <fields>;
}

-keepnames class com.google.api.client.http.HttpTransport

# Needed by google-http-client-android when linking against an older platform version
-dontwarn com.google.api.client.extensions.android.**

# Needed by google-api-client-android when linking against an older platform version
-dontwarn com.google.api.client.googleapis.extensions.android.**

# Do not obfuscate but allow shrinking of android-oauth-client
-keepnames class com.wuman.android.auth.** { *; }