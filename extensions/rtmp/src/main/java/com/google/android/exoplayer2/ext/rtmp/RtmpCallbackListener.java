package com.google.android.exoplayer2.ext.rtmp;

import android.util.Log;

import com.google.android.exoplayer2.upstream.CallbackListener;

import net.butterflytv.rtmp_client.RTMPCallback;

public class RtmpCallbackListener implements RTMPCallback {

    private final CallbackListener<? super RtmpDataSource> cbListener;
    private final RtmpDataSource ds;


    public RtmpCallbackListener (CallbackListener<? super RtmpDataSource> cbListener, RtmpDataSource ds) {
        this.cbListener = cbListener;
        this.ds = ds;
    }

    @Override
    public void dataCallback(byte[] buffer) {
        Log.i ("EXOPlayerRtmpCBListener", "invoked dataCallback");
        cbListener.dataCallback(ds, buffer);
    }

    @Override
    public void functionCallback() {
        Log.i ("EXOPlayerRtmpCBListener", "invoked functionCallback");
        cbListener.functionCallback(ds);

    }
}
