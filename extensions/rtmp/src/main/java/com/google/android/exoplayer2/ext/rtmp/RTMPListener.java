package com.google.android.exoplayer2.ext.rtmp;

public interface RTMPListener<S> {
    void onData (S source, RTMPMarker marker);
    void onFunction (S source);
}
