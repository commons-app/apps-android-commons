#include <jni.h>
#include <gst/gst.h>
#include <gio/gio.h>
#include <android/log.h>

#define CAT_FMT "%s:%d:%s"

static GstClockTime _priv_gst_info_start_time;

/* Declaration of static plugins */
 GST_PLUGIN_STATIC_DECLARE(coreelements);
  GST_PLUGIN_STATIC_DECLARE(coreindexers);
  GST_PLUGIN_STATIC_DECLARE(adder);
  GST_PLUGIN_STATIC_DECLARE(app);
  GST_PLUGIN_STATIC_DECLARE(audioconvert);
  GST_PLUGIN_STATIC_DECLARE(audiorate);
  GST_PLUGIN_STATIC_DECLARE(audioresample);
  GST_PLUGIN_STATIC_DECLARE(audiotestsrc);
  GST_PLUGIN_STATIC_DECLARE(ffmpegcolorspace);
  GST_PLUGIN_STATIC_DECLARE(gdp);
  GST_PLUGIN_STATIC_DECLARE(gio);
  GST_PLUGIN_STATIC_DECLARE(pango);
  GST_PLUGIN_STATIC_DECLARE(typefindfunctions);
  GST_PLUGIN_STATIC_DECLARE(videorate);
  GST_PLUGIN_STATIC_DECLARE(videoscale);
  GST_PLUGIN_STATIC_DECLARE(videotestsrc);
  GST_PLUGIN_STATIC_DECLARE(volume);
  GST_PLUGIN_STATIC_DECLARE(autodetect);
  GST_PLUGIN_STATIC_DECLARE(videofilter);
  GST_PLUGIN_STATIC_DECLARE(uridecodebin);
  GST_PLUGIN_STATIC_DECLARE(playback);
  GST_PLUGIN_STATIC_DECLARE(debug);
  GST_PLUGIN_STATIC_DECLARE(audioparsers);
  GST_PLUGIN_STATIC_DECLARE(id3demux);
  GST_PLUGIN_STATIC_DECLARE(isomp4);
  GST_PLUGIN_STATIC_DECLARE(ogg);
  GST_PLUGIN_STATIC_DECLARE(vorbis);
  GST_PLUGIN_STATIC_DECLARE(wavparse);
  GST_PLUGIN_STATIC_DECLARE(amrnb);
  GST_PLUGIN_STATIC_DECLARE(amrwbdec);
  GST_PLUGIN_STATIC_DECLARE(faad);
  GST_PLUGIN_STATIC_DECLARE(mad);
  GST_PLUGIN_STATIC_DECLARE(mpegaudioparse);


/* Declaration of static gio modules */


/* Call this function to register static plugins */
void
gst_android_register_static_plugins (void)
{
   GST_PLUGIN_STATIC_REGISTER(coreelements);
  GST_PLUGIN_STATIC_REGISTER(coreindexers);
  GST_PLUGIN_STATIC_REGISTER(adder);
  GST_PLUGIN_STATIC_REGISTER(app);
  GST_PLUGIN_STATIC_REGISTER(audioconvert);
  GST_PLUGIN_STATIC_REGISTER(audiorate);
  GST_PLUGIN_STATIC_REGISTER(audioresample);
  GST_PLUGIN_STATIC_REGISTER(audiotestsrc);
  GST_PLUGIN_STATIC_REGISTER(ffmpegcolorspace);
  GST_PLUGIN_STATIC_REGISTER(gdp);
  GST_PLUGIN_STATIC_REGISTER(gio);
  GST_PLUGIN_STATIC_REGISTER(pango);
  GST_PLUGIN_STATIC_REGISTER(typefindfunctions);
  GST_PLUGIN_STATIC_REGISTER(videorate);
  GST_PLUGIN_STATIC_REGISTER(videoscale);
  GST_PLUGIN_STATIC_REGISTER(videotestsrc);
  GST_PLUGIN_STATIC_REGISTER(volume);
  GST_PLUGIN_STATIC_REGISTER(autodetect);
  GST_PLUGIN_STATIC_REGISTER(videofilter);
  GST_PLUGIN_STATIC_REGISTER(uridecodebin);
  GST_PLUGIN_STATIC_REGISTER(playback);
  GST_PLUGIN_STATIC_REGISTER(debug);
  GST_PLUGIN_STATIC_REGISTER(audioparsers);
  GST_PLUGIN_STATIC_REGISTER(id3demux);
  GST_PLUGIN_STATIC_REGISTER(isomp4);
  GST_PLUGIN_STATIC_REGISTER(ogg);
  GST_PLUGIN_STATIC_REGISTER(vorbis);
  GST_PLUGIN_STATIC_REGISTER(wavparse);
  GST_PLUGIN_STATIC_REGISTER(amrnb);
  GST_PLUGIN_STATIC_REGISTER(amrwbdec);
  GST_PLUGIN_STATIC_REGISTER(faad);
  GST_PLUGIN_STATIC_REGISTER(mad);
  GST_PLUGIN_STATIC_REGISTER(mpegaudioparse);

}

/* Call this function to load GIO modules */
void
gst_android_load_gio_modules (void)
{
  
}

void
gst_debug_logcat (GstDebugCategory * category, GstDebugLevel level,
    const gchar * file, const gchar * function, gint line,
    GObject * object, GstDebugMessage * message, gpointer unused)
{
  GstClockTime elapsed;
  gint android_log_level;
  gchar *tag;

  if (level > gst_debug_category_get_threshold (category))
    return;

  elapsed = GST_CLOCK_DIFF (_priv_gst_info_start_time,
      gst_util_get_timestamp ());

  switch (level) {
    case GST_LEVEL_ERROR:
      android_log_level = ANDROID_LOG_ERROR;
      break;
    case GST_LEVEL_WARNING:
      android_log_level = ANDROID_LOG_WARN;
      break;
    case GST_LEVEL_INFO:
      android_log_level = ANDROID_LOG_INFO;
      break;
    case GST_LEVEL_DEBUG:
      android_log_level = ANDROID_LOG_DEBUG;
      break;
    default:
      android_log_level = ANDROID_LOG_VERBOSE;
      break;
  }

  tag = g_strdup_printf ("GStreamer+%s",
      gst_debug_category_get_name (category));
  __android_log_print (android_log_level, tag,
      "%" GST_TIME_FORMAT " " CAT_FMT " %s\n", GST_TIME_ARGS (elapsed),
      file, line, function, gst_debug_message_get (message));
  g_free (tag);
}

static gboolean
get_application_dirs (JNIEnv * env, jobject context, gchar ** cache_dir,
    gchar ** files_dir)
{
  jclass context_class;
  jmethodID get_cache_dir_id, get_files_dir_id;
  jclass file_class;
  jmethodID get_absolute_path_id;
  jobject dir;
  jstring abs_path;
  const gchar *abs_path_str;

  *cache_dir = *files_dir = NULL;

  context_class = (*env)->GetObjectClass (env, context);
  if (!context_class) {
    return FALSE;
  }
  get_cache_dir_id =
      (*env)->GetMethodID (env, context_class, "getCacheDir",
      "()Ljava/io/File;");
  get_files_dir_id =
      (*env)->GetMethodID (env, context_class, "getFilesDir",
      "()Ljava/io/File;");
  if (!get_cache_dir_id || !get_files_dir_id) {
    return FALSE;
  }

  file_class = (*env)->FindClass (env, "java/io/File");
  get_absolute_path_id =
      (*env)->GetMethodID (env, file_class, "getAbsolutePath",
      "()Ljava/lang/String;");
  if (!get_absolute_path_id) {
    return FALSE;
  }

  dir = (*env)->CallObjectMethod (env, context, get_cache_dir_id);
  if ((*env)->ExceptionCheck (env)) {
    return FALSE;
  }

  if (dir) {
    abs_path = (*env)->CallObjectMethod (env, dir, get_absolute_path_id);
    if ((*env)->ExceptionCheck (env)) {
      return FALSE;
    }
    abs_path_str = (*env)->GetStringUTFChars (env, abs_path, NULL);
    if ((*env)->ExceptionCheck (env)) {
      return FALSE;
    }
    *cache_dir = abs_path ? g_strdup (abs_path_str) : NULL;

    (*env)->ReleaseStringUTFChars (env, abs_path, abs_path_str);
    (*env)->DeleteLocalRef (env, abs_path);
    (*env)->DeleteLocalRef (env, dir);
  }

  dir = (*env)->CallObjectMethod (env, context, get_files_dir_id);
  if ((*env)->ExceptionCheck (env)) {
    return FALSE;
  }
  if (dir) {
    abs_path = (*env)->CallObjectMethod (env, dir, get_absolute_path_id);
    if ((*env)->ExceptionCheck (env)) {
      return FALSE;
    }
    abs_path_str = (*env)->GetStringUTFChars (env, abs_path, NULL);
    if ((*env)->ExceptionCheck (env)) {
      return FALSE;
    }
    *files_dir = files_dir ? g_strdup (abs_path_str) : NULL;

    (*env)->ReleaseStringUTFChars (env, abs_path, abs_path_str);
    (*env)->DeleteLocalRef (env, abs_path);
    (*env)->DeleteLocalRef (env, dir);
  }

  (*env)->DeleteLocalRef (env, file_class);
  (*env)->DeleteLocalRef (env, context_class);

  return TRUE;
}

static void
gst_android_init (JNIEnv * env, jclass klass, jobject context)
{
  gchar *cache_dir;
  gchar *files_dir;
  gchar *registry;
  GError *error = NULL;

  if (gst_is_initialized ()) {
    __android_log_print (ANDROID_LOG_INFO, "GStreamer",
        "GStreamer already initialized");
    return;
  }

  if (!get_application_dirs (env, context, &cache_dir, &files_dir))
    return;

  if (cache_dir) {
    g_setenv ("TMP", cache_dir, TRUE);
    g_setenv ("TMPDIR", cache_dir, TRUE);
    g_setenv ("XDG_RUNTIME_DIR", cache_dir, TRUE);
    g_setenv ("XDG_CACHE_DIR", cache_dir, TRUE);
    registry = g_build_filename (cache_dir, "registry.bin", NULL);
    g_setenv ("GST_REGISTRY", registry, TRUE);
    g_free (registry);
    g_setenv ("GST_REUSE_PLUGIN_SCANNER", "no", TRUE);
    /* FIXME: Should probably also set GST_PLUGIN_SCANNER and GST_PLUGIN_SYSTEM_PATH */
  }
  if (files_dir) {
    g_setenv ("HOME", files_dir, TRUE);
    g_setenv ("XDG_DATA_DIRS", files_dir, TRUE);
    g_setenv ("XDG_CONFIG_DIRS", files_dir, TRUE);
  }
  g_free (cache_dir);
  g_free (files_dir);

  /* Disable this for releases if performance is important
   * or increase the threshold to get more information */
  gst_debug_set_active (TRUE);
  gst_debug_set_default_threshold (GST_LEVEL_WARNING);
  gst_debug_remove_log_function (gst_debug_log_default);
  gst_debug_add_log_function ((GstLogFunction) gst_debug_logcat, NULL);
  /* get time we started for debugging messages */
  _priv_gst_info_start_time = gst_util_get_timestamp ();

  if (!gst_init_check (NULL, NULL, &error)) {
    gchar *message = g_strdup_printf ("GStreamer initialization failed: %s",
        error && error->message ? error->message : "(no message)");
    jclass exception_class = (*env)->FindClass (env, "java/lang/Exception");
    __android_log_print (ANDROID_LOG_ERROR, "GStreamer", message);
    (*env)->ThrowNew (env, exception_class, message);
    g_free (message);
    return;
  }
  gst_android_register_static_plugins ();
  gst_android_load_gio_modules();
  __android_log_print (ANDROID_LOG_INFO, "GStreamer",
      "GStreamer initialization complete");
}

static JNINativeMethod native_methods[] = {
  {"init", "(Landroid/content/Context;)V", (void *) gst_android_init}
};

jint
JNI_OnLoad (JavaVM * vm, void *reserved)
{
  JNIEnv *env = NULL;

  if ((*vm)->GetEnv (vm, (void **) &env, JNI_VERSION_1_4) != JNI_OK) {
    __android_log_print (ANDROID_LOG_ERROR, "GStreamer",
        "Could not retrieve JNIEnv");
    return 0;
  }
  jclass klass = (*env)->FindClass (env, "com/gst_sdk/GStreamer");
  if (!klass) {
    __android_log_print (ANDROID_LOG_ERROR, "GStreamer",
        "Could not retrieve class com.gst_sdk.GStreamer");
    return 0;
  }
  if ((*env)->RegisterNatives (env, klass, native_methods,
          G_N_ELEMENTS (native_methods))) {
    __android_log_print (ANDROID_LOG_ERROR, "GStreamer",
        "Could not register native methods for com.gst_sdk_GStreamer");
    return 0;
  }

  return JNI_VERSION_1_4;
}
