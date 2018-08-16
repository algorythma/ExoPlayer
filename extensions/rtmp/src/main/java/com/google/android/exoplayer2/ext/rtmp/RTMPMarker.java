package com.google.android.exoplayer2.ext.rtmp;

import java.util.Arrays;

public class RTMPMarker {
    private final String type;
    private final String uid;
    private final int retryIndex;
    private final byte[] data;

    RTMPMarker (String type, String uid, int retryIndex, byte[] data) {
        this.type = type;
        this.uid = uid;
        this.retryIndex = retryIndex;
        this.data = Arrays.copyOf (data, data.length);
    }

    public String getType() {
        return this.type;
    }

    public String getUid() {
        return this.uid;
    }

    public int getRetryIndex () {
        return this.retryIndex;
    }

    public byte[] getData () {
        return this.data;
    }
}