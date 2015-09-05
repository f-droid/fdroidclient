package org.fdroid.fdroid.net.bluetooth;

public class FileDetails {

    private String cacheTag;
    private int fileSize;

    public String getCacheTag() {
        return cacheTag;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public void setCacheTag(String cacheTag) {
        this.cacheTag = cacheTag;
    }
}
