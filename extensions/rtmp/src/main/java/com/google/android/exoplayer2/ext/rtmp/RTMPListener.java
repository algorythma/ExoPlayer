package com.google.android.exoplayer2.ext.rtmp;

import net.butterflytv.rtmp_client.RTMPMarker;

public interface RTMPListener<S> {
    void onData (S source, RTMPMarker marker);
    void onFunction (S source);
}
