LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := BelMarket
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := BelMarket

belmarket_root  := $(LOCAL_PATH)
belmarket_dir   := app
belmarket_out   := $(PWD)/$(OUT_DIR)/target/common/obj/APPS/$(LOCAL_MODULE)_intermediates
belmarket_build := $(belmarket_root)/$(belmarket_dir)/build
belmarket_apk   := build/outputs/apk/$(belmarket_dir)-release-unsigned.apk

$(belmarket_root)/$(belmarket_dir)/$(belmarket_apk):
	rm -Rf $(belmarket_build)
	mkdir -p $(belmarket_out)
	ln -sf $(belmarket_out) $(belmarket_build)
	cd $(belmarket_root)/$(belmarket_dir) && gradle assembleRelease

LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(belmarket_dir)/$(belmarket_apk)
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)
