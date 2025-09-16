package controllers;

import java.io.*;
import java.nio.file.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileItem {
    private final Path path;
    private final String name;
    private final long size;
    private final String modified;
    private final String type;

    public FileItem(Path p) {
    	if (p == null) throw new IllegalArgumentException("Path cannot be null");
        this.path = p;
        this.name = p.getFileName().toString();
        long s;
        try { s = Files.size(p); } catch (IOException e) { s = 0; }
        this.size = s;
        String mod;
        try {
            mod = Files.getLastModifiedTime(p).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (IOException e) {
            mod = "";
        }
        this.modified = mod;
        this.type = Files.isDirectory(p) ? "Folder" : "File";
    }

    public Path getPath() { return path; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public String getModified() { return modified; }
    public String getType() { return type; }
}