LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := exec_helper
LOCAL_SRC_FILES := exec_helper.c
LOCAL_CFLAGS := -O2 -Wall

include $(BUILD_SHARED_LIBRARY)
