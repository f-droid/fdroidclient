package org.fdroid.fdroid.nearby.httpish;

public class FileDetails {

    private String cacheTag;
    private long fileSize;

    public String getCacheTag() {
        return cacheTag;
    }

    public long getFileSize() {
        return fileSize;
    }

    void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    void setCacheTag(String cacheTag) {
        this.cacheTag = cacheTag;
    }
}
