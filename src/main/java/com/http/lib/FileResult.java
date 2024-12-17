package com.http.lib;

import java.io.File;

public class FileResult {
    // attributes
    private File file;
    private FileStatus status;

    // constructors
    public FileResult(File file, FileStatus status) {
        this.file = file;
        this.status = status;
    }

    // getters
    public File getFile() {
        return file;
    }

    public FileStatus getStatus() {
        return status;
    }
}
