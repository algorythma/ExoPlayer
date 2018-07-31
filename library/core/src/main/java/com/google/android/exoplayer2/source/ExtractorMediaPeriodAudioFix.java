package com.google.android.exoplayer2.source;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.MimeTypes;

public class ExtractorMediaPeriodAudioFix extends ExtractorMediaPeriod {

    int validSampleQCount;

    /**
     * @param uri                               The {@link Uri} of the media stream.
     * @param dataSource                        The data source to read the media.
     * @param extractors                        The extractors to use to read the data source.
     * @param minLoadableRetryCount             The minimum number of times to retry if a loading error occurs.
     * @param eventDispatcher                   A dispatcher to notify of events.
     * @param listener                          A listener to notify when information about the period changes.
     * @param allocator                         An {@link Allocator} from which to obtain media buffer allocations.
     * @param customCacheKey                    A custom key that uniquely identifies the original stream. Used for cache
     *                                          indexing. May be null.
     * @param continueLoadingCheckIntervalBytes The number of bytes that should be loaded between each
     *                                          invocation of {@link Callback#onContinueLoadingRequested(SequenceableLoader)}.
     */
    public ExtractorMediaPeriodAudioFix(
            Uri uri,
            DataSource dataSource,
            Extractor[] extractors,
            int minLoadableRetryCount,
            MediaSourceEventListener.EventDispatcher eventDispatcher,
            Listener listener,
            Allocator allocator,
            @Nullable String customCacheKey,
            int continueLoadingCheckIntervalBytes) {
        super(uri, dataSource, extractors, minLoadableRetryCount, eventDispatcher, listener, allocator, customCacheKey, continueLoadingCheckIntervalBytes);
        Log.i ("ExtMediaPeriodAFix", "invoked super constructor()");
    }

    @Override
    public void discardBuffer(long positionUs, boolean toKeyframe) {

        int trackCount = validSampleQCount;

        for (int i = 0; i < trackCount; i++) {
            sampleQueues[i].discardTo(positionUs, toKeyframe, trackEnabledStates[i]);
        }
    }

    @Override
    public long getBufferedPositionUs() {

        if (loadingFinished) {
            return C.TIME_END_OF_SOURCE;
        } else if (isPendingReset()) {
            return pendingResetPositionUs;
        }
        long largestQueuedTimestampUs;
        if (haveAudioVideoTracks) {
            // Ignore non-AV tracks, which may be sparse or poorly interleaved.
            largestQueuedTimestampUs = Long.MAX_VALUE;
            int trackCount = validSampleQCount;
            for (int i = 0; i < trackCount; i++) {
                if (trackIsAudioVideoFlags[i]) {
                    largestQueuedTimestampUs = Math.min(largestQueuedTimestampUs,
                            sampleQueues[i].getLargestQueuedTimestampUs());
                }
            }
        } else {
            largestQueuedTimestampUs = getLargestQueuedTimestampUs();
        }
        return largestQueuedTimestampUs == Long.MIN_VALUE ? lastSeekPositionUs
                : largestQueuedTimestampUs;
    }

    @Override
    public void maybeFinishPrepare() {

        int validQCount = 0;
        boolean videoStream = false, nullStream = false;

        Log.i ("ExtMediaPeriodAFix", "maybeFinishPrepare invoked, using validSampleQCount: " +
                validSampleQCount);

        if (released || prepared || seekMap == null || !sampleQueuesBuilt) {
            return;
        }
        for (SampleQueue sampleQueue : sampleQueues) {
            if (sampleQueue.getUpstreamFormat() == null) {
                Log.e ("EXO_PLAYER_EXTMEDPER", "UpstreamFormat is NULL");
                nullStream = true;
                if (videoStream)
                    return;
            } else {
                if (sampleQueue.getUpstreamFormat().sampleMimeType.contains ("video")) {
                    videoStream = true;
                }
                validQCount++;
            }
        }

        if (validQCount == 0 || (videoStream && nullStream)) {
            Log.e ("EXOPLAYER_EXTMEDPER", "maybeFinishPrepare: Returning as validQCOunt: " +
                    validQCount + ", videoStream: " + videoStream + ", nullStream: " + nullStream);
            return;
        }

        loadCondition.close();
        validSampleQCount = validQCount;
        int trackCount = validQCount;
        TrackGroup[] trackArray = new TrackGroup[trackCount];
        trackIsAudioVideoFlags = new boolean[trackCount];
        trackEnabledStates = new boolean[trackCount];
        trackFormatNotificationSent = new boolean[trackCount];
        durationUs = seekMap.getDurationUs();
        for (int i = 0; i < trackCount; i++) {
            Format trackFormat = sampleQueues[i].getUpstreamFormat();
            if (trackFormat == null)
                continue;
            trackArray[i] = new TrackGroup(trackFormat);
            String mimeType = trackFormat.sampleMimeType;
            boolean isAudioVideo = MimeTypes.isVideo(mimeType) || MimeTypes.isAudio(mimeType);
            trackIsAudioVideoFlags[i] = isAudioVideo;
            haveAudioVideoTracks |= isAudioVideo;
        }
        tracks = new TrackGroupArray(trackArray);
        if (minLoadableRetryCount == ExtractorMediaSource.MIN_RETRY_COUNT_DEFAULT_FOR_MEDIA
                && length == C.LENGTH_UNSET && seekMap.getDurationUs() == C.TIME_UNSET) {
            actualMinLoadableRetryCount = ExtractorMediaSource.DEFAULT_MIN_LOADABLE_RETRY_COUNT_LIVE;
        }
        prepared = true;
        listener.onSourceInfoRefreshed(durationUs, seekMap.isSeekable());
        callback.onPrepared(this);
    }

    @Override
    boolean seekInsideBufferUs(long positionUs) {
        int trackCount = validSampleQCount;
        for (int i = 0; i < trackCount; i++) {
            SampleQueue sampleQueue = sampleQueues[i];
            sampleQueue.rewind();
            boolean seekInsideQueue = sampleQueue.advanceTo(positionUs, true, false)
                    != SampleQueue.ADVANCE_FAILED;
            // If we have AV tracks then an in-buffer seek is successful if the seek into every AV queue
            // is successful. We ignore whether seeks within non-AV queues are successful in this case, as
            // they may be sparse or poorly interleaved. If we only have non-AV tracks then a seek is
            // successful only if the seek into every queue succeeds.
            if (!seekInsideQueue && (trackIsAudioVideoFlags[i] || !haveAudioVideoTracks)) {
                return false;
            }
        }
        return true;
    }
}
