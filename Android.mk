LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := F-Droid
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := F-Droid

fdroid_dir := $(LOCAL_PATH)
fdroid_apk := F-Droid/build/outputs/apk/F-Droid-release-unsigned.apk

$(LOCAL_PATH)/$(fdroid_apk):
	cd $(fdroid_dir) && gradle assembleRelease

LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(fdroid_apk)
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)