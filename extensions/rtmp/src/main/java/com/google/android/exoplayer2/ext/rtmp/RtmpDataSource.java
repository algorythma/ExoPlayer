/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.ext.rtmp;

import android.net.Uri;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import java.io.IOException;
import net.butterflytv.rtmp_client.RtmpClient;
import net.butterflytv.rtmp_client.RtmpClient.RtmpIOException;

/**
 * A Real-Time Messaging Protocol (RTMP) {@link DataSource}.
 */
public final class RtmpDataSource implements DataSource {

  static {
    ExoPlayerLibraryInfo.registerModule("goog.exo.rtmp");
  }

  @Nullable private final TransferListener<? super RtmpDataSource> listener;
  @Nullable private final RTMPListener<? super RtmpDataSource> cbListener;
  @Nullable private RtmpCallbackListener rtmpcbListener;

  private RtmpClient rtmpClient;
  private Uri uri;

  public RtmpDataSource() {
    this(null, null);
  }

  public RtmpDataSource (@Nullable RTMPListener<? super RtmpDataSource> cbListener) {
    this (null, cbListener);
  }

  public RtmpDataSource(@Nullable TransferListener<? super RtmpDataSource> listener) {
    this (listener, null);
  }

  /**
   * @param listener An optional listener.
   */
  public RtmpDataSource(@Nullable TransferListener<? super RtmpDataSource> listener,
                        @Nullable RTMPListener<? super RtmpDataSource> cbListener) {
    this.listener = listener;
    this.cbListener = cbListener;
  }

  @Override
  public long open(DataSpec dataSpec) throws RtmpIOException {
    rtmpcbListener = new RtmpCallbackListener (this.cbListener, this);
    rtmpClient = new RtmpClient(rtmpcbListener);
    rtmpClient.open(dataSpec.uri.toString(), false);

    this.uri = dataSpec.uri;
    if (listener != null) {
      listener.onTransferStart(this, dataSpec);
    }
    return C.LENGTH_UNSET;
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength) throws IOException {
    int bytesRead = rtmpClient.read(buffer, offset, readLength);
    if (bytesRead == -1) {
      return C.RESULT_END_OF_INPUT;
    }
    if (listener != null) {
      listener.onBytesTransferred(this, bytesRead);
    }
    return bytesRead;
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength, boolean []isMarker) throws IOException {
    int bytesRead = rtmpClient.read(buffer, offset, readLength, isMarker);
    if (bytesRead == -1) {
      return C.RESULT_END_OF_INPUT;
    }
    if (listener != null) {
      listener.onBytesTransferred(this, bytesRead);
    }
    return bytesRead;
  }

  @Override
  public void close() {
    if (uri != null) {
      uri = null;
      if (listener != null) {
        listener.onTransferEnd(this);
      }
    }
    if (rtmpClient != null) {
      rtmpClient.close();
      rtmpClient = null;
    }
  }

  @Override
  public Uri getUri() {
    return uri;
  }

  @Override
  public void markerToastDisplay () {
    rtmpClient.markerToastDisplay();
  }

}
