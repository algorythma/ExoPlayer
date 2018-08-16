package com.google.android.exoplayer2.ext.rtmp;

import android.util.Log;

import net.butterflytv.rtmp_client.RTMPCallback;

import java.util.Arrays;

public class RtmpCallbackListener implements RTMPCallback {

    private final RTMPListener<? super RtmpDataSource> cbListener;
    private final RtmpDataSource ds;


    public RtmpCallbackListener (RTMPListener<? super RtmpDataSource> cbListener, RtmpDataSource ds) {
        this.cbListener = cbListener;
        this.ds = ds;
    }

    @Override
    public void dataCallback(byte[] buffer) {
        Log.i ("EXOPlayerRtmpCBListener", "invoked dataCallback");
        if (cbListener == null)
            return;
        RTMPMarker marker = new RTMPMarker ("DefaultType", "DefaultUid", -1, buffer);
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
