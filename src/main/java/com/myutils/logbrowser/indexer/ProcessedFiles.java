package com.myutils.logbrowser.indexer;

import java.util.ArrayList;

public class  ProcessedFiles {
    private String fileName;
    private long id;

    public String getFileName() {
        return fileName;
    }

    public long getId() {
        return id;
    }

    public long getSize() {
        return size;
    }

    private   long size;

    public ProcessedFiles(String fileName, long id, long size) {
        this.fileName = fileName;
        this.id = id;
        this.size = size;
    }
}
