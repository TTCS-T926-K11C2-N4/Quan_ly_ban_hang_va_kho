package com.oms.model;

// Một dòng file_objects: tệp nằm ở UPLOAD_DIR/storageKey
public class StoredFile {

    private final long id;
    private final String storageKey;
    private final String contentType;

    public StoredFile(long id, String storageKey, String contentType) {
        this.id = id;
        this.storageKey = storageKey;
        this.contentType = contentType;
    }

    public long getId() {
        return id;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }
}
