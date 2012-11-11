#include <gst/gst.h>

#include <jni.h>
#include <android/log.h>

static int init(void)
{
    /* XXX: ZERO thread-safety guarantees here */
    static gboolean inited = 0;

    if (inited)
        return 0;

    gst_init(NULL, NULL);
    return 0;
}

static int transcode(const char *infile, const char *outfile,
        const char *profile, jobject cb_obj, JNIEnv *env)
{
    GstElement *pipeline;
    GstBus *bus;
    GstMessage *msg;
    gchar pipeline_str[1024];
    int ret = 0;

    init();

    snprintf(pipeline_str, 1024,
            "filesrc location=\"%s\" ! "
            "progressreport silent=true format=percent update-freq=1 ! "
            "decodebin2 ! audioconvert ! vorbisenc ! oggmux ! "
            "filesink location=\"%s\"",
            infile, outfile);

    pipeline = gst_parse_launch(pipeline_str, NULL);

    gst_element_set_state (pipeline, GST_STATE_PLAYING);

    bus = gst_element_get_bus(pipeline);

    for (;;) {
        msg = gst_bus_timed_pop_filtered(bus, GST_CLOCK_TIME_NONE,
                GST_MESSAGE_ERROR | GST_MESSAGE_EOS | GST_MESSAGE_ELEMENT);

        switch (GST_MESSAGE_TYPE(msg)) {
            case GST_MESSAGE_ELEMENT: {
                const GstStructure *s = gst_message_get_structure(msg);
                int percent;
                jclass cb_class;
                jmethodID cb_id;

                if (!cb_obj)
                    break;

                if (!g_str_equal(gst_structure_get_name(s), "progress"))
                    break;

                gst_structure_get_int(s, "percent", &percent);

                cb_class = (*env)->FindClass(env, "org/wikimedia/commons/Transcoder$TranscoderProgressCallback");
                if ((*env)->ExceptionCheck(env)) {
                    __android_log_print(ANDROID_LOG_ERROR, "GStreamer", "Class not found");
                    break;
                }

                cb_id = (*env)->GetMethodID(env, cb_class, "transcodeProgressCb", "(I)V");
                if ((*env)->ExceptionCheck(env)) {
                    __android_log_print(ANDROID_LOG_ERROR, "GStreamer", "Method not found");
                    break;
                }

                (*env)->CallVoidMethod(env, cb_obj, cb_id, percent);
                if ((*env)->ExceptionCheck(env)) {
                    __android_log_print(ANDROID_LOG_ERROR, "GStreamer", "Method call failed");
                    break;
                }

                break;
            }

            case GST_MESSAGE_ERROR: {
                GError *err = NULL;
                gchar *debug_info = NULL;

                gst_message_parse_error(msg, &err, &debug_info);

                GST_ERROR_OBJECT(pipeline, "%s -- %s", err->message,
                        debug_info ? debug_info : "no debug info");

                g_error_free(err);
                g_free(debug_info);

                ret = -1;
                goto done;
            }

            case GST_MESSAGE_EOS:
                goto done;

            default:
                break;
        }
    }

done:
    if (msg != NULL)
        gst_message_unref (msg);

    gst_object_unref (bus);
    gst_element_set_state (pipeline, GST_STATE_NULL);
    gst_object_unref (pipeline);

    return ret;
}

jint Java_org_wikimedia_commons_Transcoder_transcode(JNIEnv* env,
        jclass *klass, jstring infile, jstring outfile, jstring profile,
        jobject cb_obj)
{
    const char *in;
    const char *out;
    const char *prof = NULL;

    if (!infile || !outfile)
        return -1;

    in = (*env)->GetStringUTFChars(env, infile, 0);
    out = (*env)->GetStringUTFChars(env, outfile, 0);

    if (profile)
        prof = (*env)->GetStringUTFChars(env, profile, 0);

    return transcode(in, out, prof, cb_obj, env);
}


#ifdef TEST
int main(int argc, char **argv)
{
    if (argc != 3)
        return -1;

    transcode(argv[1], argv[2], NULL);

    return 0;
}
#endif
