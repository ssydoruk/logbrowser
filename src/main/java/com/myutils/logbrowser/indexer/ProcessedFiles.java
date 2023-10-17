package com.myutils.logbrowser.indexer;

public class ProcessedFiles {
    private String fileName;
    private long id;
    private long size;

    public ProcessedFiles(String fileName, long id, long size) {
        this.fileName = fileName;
        this.id = id;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public long getId() {
        return id;
    }

    public long getSize() {
        return size;
    }
}
