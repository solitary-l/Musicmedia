package com.example.musicmedia;

import java.io.File;
import java.io.FilenameFilter;

class MyFilter implements FilenameFilter {
    private String type;

    public MyFilter(String type) {
        this.type = type;
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith(type);
    }
}


