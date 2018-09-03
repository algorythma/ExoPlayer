package com.google.android.exoplayer2.ext.rtmp;

import android.util.Log;

import net.butterflytv.rtmp_client.RTMPCallback;
import net.butterflytv.rtmp_client.RTMPMarker;

public class RtmpCallbackListener implements RTMPCallback {

    private final RTMPListener<? super RtmpDataSource> cbListener;
    private final RtmpDataSource ds;


    public RtmpCallbackListener (RTMPListener<? super RtmpDataSource> cbListener, RtmpDataSource ds) {
        this.cbListener = cbListener;
        this.ds = ds;
    }

    @Override
    public void dataCallback(RTMPMarker marker) {
        Log.i ("EXOPlayerRtmpCBListener", "invoked dataCallback");
        if (cbListener == null)
            return;
        cbListener.onData(ds, marker);
    }

    @Override
    public void functionCallback() {
        Log.i ("EXOPlayerRtmpCBListener", "invoked functionCallback");
        if (cbListener == null)
            return;
        cbListener.onFunction(ds);

    }
}
