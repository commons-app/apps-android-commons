LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libtranscode
LOCAL_SRC_FILES := transcode.c
LOCAL_SHARED_LIBRARIES := gstreamer_android
LOCAL_LDLIBS := -landroid -llog

include $(BUILD_SHARED_LIBRARY)

GSTREAMER_SDK_ROOT := $(GSTREAMER_SDK_ROOT_ANDROID)
GSTREAMER_NDK_BUILD_PATH := $(GSTREAMER_SDK_ROOT)/share/gst-android/ndk-build/
include $(GSTREAMER_NDK_BUILD_PATH)/plugins.mk
GSTREAMER_PLUGINS := \
	$(GSTREAMER_PLUGINS_CORE) \
	$(GSTREAMER_PLUGINS_PLAYBACK) \
	debug \
	audioparsers id3demux isomp4 ogg vorbis wavparse \
	amrnb amrwbdec faad mad mpegaudioparse

include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer.mk
