LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := F-Droid
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := \
  $(call all-java-files-under, src) \
  $(call all-java-files-under, extern/MemorizingTrustManager/src) \
  $(call all-java-files-under, extern/AndroidPinning/src) \
  $(call all-java-files-under, extern/UniversalImageLoader/library/src )

res_dirs = res extern/MemorizingTrustManager/res extern/AndroidPinning/res
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages org.fdroid.fdroid:de.duenndns.ssl:org.thoughtcrime.ssl.pinning

LOCAL_PRIVILEGED_MODULE := true
include $(BUILD_PACKAGE)

