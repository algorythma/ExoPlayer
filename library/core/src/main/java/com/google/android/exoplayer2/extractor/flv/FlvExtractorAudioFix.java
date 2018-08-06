package com.google.android.exoplayer2.extractor.flv;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.util.ParsableByteArray;

import java.io.IOException;

public class FlvExtractorAudioFix extends FlvExtractor {

    public FlvExtractorAudioFix() {
        super();
        Log.i ("FlvExtAudioFix", "invoked FlvExtractor constructor");
    }

    static final int FLV_ATAG = 1;
    static final int FLV_VTAG = 2;
    static final int FLV_AVTAGS = (FLV_ATAG | FLV_VTAG);
    static final int FLV_NOTAGS = 0;

    int hasBothAVTags (ExtractorInput input, int hdrSize) throws IOException, InterruptedException {
        int skipBytes, tagType, tagDataSize, itr = 0, ret = FLV_NOTAGS;

        input.resetPeekPosition();
        skipBytes = hdrSize - 9 + 4;
        input.advancePeekPosition (skipBytes);

        Log.i ("FlvExtAudioFix", "areBothAudioVideoPresent: currPeekPos: " +
                input.getPeekPosition() + ", curReadpos: " + input.getPosition() +
                ", skipBytes: " + skipBytes);
        while (itr < 6) {
            if (!input.peekFully(sniffBuffer.data, 0, FLV_TAG_HEADER_SIZE, true)) {
                Log.e("FlvExtAudioFix", "areBothAudioVideoPresent: peek" + itr + " failed");
                return ret;
            }
            sniffBuffer.setPosition(0);
            tagType = sniffBuffer.readUnsignedByte();
            tagDataSize = sniffBuffer.readUnsignedInt24();

            input.advancePeekPosition(tagDataSize + 4);
            Log.i("FlvExtAudioFix", "areBothAudioVideoPresent: itr: " + itr +
                    ": currPeekPos: " + input.getPeekPosition() + ", curReadpos: " +
                    input.getPosition() + ", ta1DataSize[" + itr + "]: " + tagDataSize +
                    ", tagType[" + itr + "]: " + tagType);
            itr++;
            if (tagType == TAG_TYPE_AUDIO)
                ret |= FLV_ATAG;
            else if (tagType == TAG_TYPE_VIDEO)
                ret |= FLV_VTAG;
        }
        input.resetPeekPosition();

        return ret;
    }
    @Override
    /**
     * Reads an FLV container header from the provided {@link ExtractorInput}.
     *
     * @param input The {@link ExtractorInput} from which to read.
     * @return True if header was read successfully. False if the end of stream was reached.
     * @throws IOException If an error occurred reading or parsing data from the source.
     * @throws InterruptedException If the thread was interrupted.
     */
    boolean readFlvHeader(ExtractorInput input) throws IOException, InterruptedException {
        if (!input.readFully(headerBuffer.data, 0, FLV_HEADER_SIZE, true)) {
            // We've reached the end of the stream.
            return false;
        }

        headerBuffer.setPosition(0);
        headerBuffer.skipBytes(4);
        int flags = headerBuffer.readUnsignedByte();
        int hdrSize = headerBuffer.readInt();
        boolean hasAudio = false; // = (flags & 0x04) != 0;
        boolean hasVideo = false; // = (flags & 0x01) != 0;

        int ret = hasBothAVTags(input, hdrSize);
        Log.i ("FlvExtAudioFix", "readFlvHeader: ret: " + ret);
        if (ret == FLV_ATAG)
            hasAudio = true;
        else if (ret == FLV_VTAG)
            hasVideo = true;
        else if (ret == FLV_AVTAGS)
            hasAudio = hasVideo = true;

        if (hasAudio && audioReader == null) {
            audioReader = new AudioTagPayloadReader(
                    extractorOutput.track(TAG_TYPE_AUDIO, C.TRACK_TYPE_AUDIO));
        }
        if (hasVideo && videoReader == null) {
            videoReader = new VideoTagPayloadReader(
                    extractorOutput.track(TAG_TYPE_VIDEO, C.TRACK_TYPE_VIDEO));
        }
        extractorOutput.endTracks();

        // We need to skip any additional content in the FLV header, plus the 4 byte previous tag size.
        bytesToNextTagHeader = hdrSize - FLV_HEADER_SIZE + 4;
        state = STATE_SKIPPING_TO_TAG_HEADER;
        Log.i ("flvext", "readFlvHeader -> STATE_SKIPPING_TO_TAG_HEADER");
        return true;
    }
}
