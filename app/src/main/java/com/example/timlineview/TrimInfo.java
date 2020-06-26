package com.example.timlineview;

public class TrimInfo {
    public TrimInfo(long start, long end, String url) {
        this.startSeconds = start;
        this.endSeconds = end;
        this.url = url;
    }
    public final long startSeconds;
    public final long endSeconds;
    public final String url;
}
