package com.http.lib;

public enum FileStatus {
    FILE_FOUND, // file was found
    FILE_NOT_FOUND, // file wasn't found
    DIRECTORY_NO_SLASH, // a directory was found with trailing "/" --> should send a 301
}