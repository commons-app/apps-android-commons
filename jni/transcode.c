#include <gst/gst.h>

#include <jni.h>

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
        const char *profile)
{
    GstElement *pipeline;
    GstBus *bus;
    GstMessage *msg;
    gchar pipeline_str[1024];

    init();

    snprintf(pipeline_str, 1024,
            "filesrc location=\"%s\" ! decodebin2 ! audioconvert ! "
            "vorbisenc ! oggmux ! filesink location=\"%s\"",
            infile, outfile);

    pipeline = gst_parse_launch(pipeline_str, NULL);

    gst_element_set_state (pipeline, GST_STATE_PLAYING);

    bus = gst_element_get_bus(pipeline);
    msg = gst_bus_timed_pop_filtered(bus, GST_CLOCK_TIME_NONE,
            GST_MESSAGE_ERROR | GST_MESSAGE_EOS);

    if (GST_MESSAGE_TYPE(msg) == GST_MESSAGE_ERROR) {
        GError *err = NULL;
        gchar *debug_info = NULL;

        gst_message_parse_error(msg, &err, &debug_info);

        GST_ERROR_OBJECT(pipeline, "%s -- %s", err->message,
                debug_info ? debug_info : "no debug info");

        g_error_free(err);
        g_free(debug_info);
    }

    if (msg != NULL)
        gst_message_unref (msg);

    gst_object_unref (bus);
    gst_element_set_state (pipeline, GST_STATE_NULL);
    gst_object_unref (pipeline);

    return 0;
}

jint Java_org_wikimedia_commons_Transcoder_transcode(JNIEnv* env,
        jclass *klass, jstring infile, jstring outfile, jstring profile)
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

    return transcode(in, out, prof);
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
