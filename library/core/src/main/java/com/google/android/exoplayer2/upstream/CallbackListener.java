package com.google.android.exoplayer2.upstream;

public interface CallbackListener<S> {
    void dataCallback (S source, byte[] buffer);
    void functionCallback (S source);
}
