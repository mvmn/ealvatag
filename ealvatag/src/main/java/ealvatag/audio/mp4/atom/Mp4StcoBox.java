package ealvatag.audio.mp4.atom;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import ealvatag.audio.exceptions.CannotReadException;
import ealvatag.audio.mp4.Mp4AtomIdentifier;
import ealvatag.audio.mp4.Mp4AudioHeader;
import okio.BufferedSource;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * StcoBox ( media (stream) header), holds offsets into the Audio data
 */
public class Mp4StcoBox extends AbstractMp4Box {
//    public static final int VERSION_FLAG_POS = 0;
//    public static final int OTHER_FLAG_POS = 1;
//    public static final int NO_OF_OFFSETS_POS = 4;

    private static final int VERSION_FLAG_LENGTH = 1;
    private static final int OTHER_FLAG_LENGTH = 3;
    private static final int NO_OF_OFFSETS_LENGTH = 4;
    private static final int OFFSET_LENGTH = 4;
    private int noOfOffSets;
    private int firstOffSet;

    /**
     * Construct box from data and show contents
     *
     * @param header header info
     * @param buffer data of box (doesnt include header data)
     */
    public Mp4StcoBox(Mp4BoxHeader header, ByteBuffer buffer) {
        this.header = header;

        //Make a slice of databuffer then we can work with relative or absolute methods safetly
        dataBuffer = buffer.slice();
        dataBuffer.order(ByteOrder.BIG_ENDIAN);
        //Skip the flags
        dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH);

        //No of offsets
        this.noOfOffSets = dataBuffer.getInt();

        //First Offset, useful for sanity checks
        firstOffSet = dataBuffer.getInt();
    }

    public Mp4StcoBox(final Mp4BoxHeader stcoBoxHeader, final BufferedSource bufferedSource, final Mp4AudioHeader audioHeader)
            throws IOException {
        Preconditions.checkArgument(Mp4AtomIdentifier.STCO.matches(stcoBoxHeader.getId()));

        int dataSize = stcoBoxHeader.getDataLength();

        //Skip the flags
        bufferedSource.skip(VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH);
        dataSize -= VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH;

        //No of offsets
        noOfOffSets = bufferedSource.readInt();
        dataSize -= 4;

        //First Offset, useful for sanity checks
        firstOffSet = bufferedSource.readInt();
        dataSize -= 4;

        audioHeader.setAudioDataStartPosition(firstOffSet);
        audioHeader.setAudioDataEndPosition(audioHeader.getFileSize());
        audioHeader.setAudioDataLength(audioHeader.getFileSize() - firstOffSet);

        // skip the rest, we don't need it
        bufferedSource.skip(dataSize);
    }

    public void adjustOffsets(int adjustment) {
        //Skip the flags
        dataBuffer.rewind();
        dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + NO_OF_OFFSETS_LENGTH);
        for (int i = 0; i < noOfOffSets; i++) {
            int offset = dataBuffer.getInt();

            //Calculate new offset and update buffer
            offset = offset + adjustment;
            dataBuffer.position(dataBuffer.position() - OFFSET_LENGTH);
            dataBuffer.putInt(offset);
        }
    }

    public int getNoOfOffSets() {
        return noOfOffSets;
    }

    public int getFirstOffSet() {
        return firstOffSet;
    }

    @SuppressWarnings({"unused", "SpellCheckingInspection"}) @VisibleForTesting
    public static Mp4StcoBox getStco(RandomAccessFile raf) throws IOException, CannotReadException {
        FileChannel fc = raf.getChannel();
        Mp4BoxHeader moovHeader = Mp4BoxHeader.seekWithinLevel(fc, Mp4AtomIdentifier.MOOV.getFieldName());
        if (moovHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        ByteBuffer moovBuffer = ByteBuffer.allocate(moovHeader.getLength() - Mp4BoxHeader.HEADER_LENGTH);
        fc.read(moovBuffer);
        moovBuffer.rewind();

        //Level 2-Searching for "mvhd" somewhere within "moov", we make a slice after finding header
        //so all getFields() methods will be relative to mvdh positions
        Mp4BoxHeader boxHeader = Mp4BoxHeader.seekWithinLevel(moovBuffer, Mp4AtomIdentifier.MVHD.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        ByteBuffer mvhdBuffer = moovBuffer.slice();
        Mp4MvhdBox mvhd = new Mp4MvhdBox(boxHeader, mvhdBuffer);
        mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());

        //Level 2-Searching for "trak" within "moov"
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.TRAK.getFieldName());

        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        int endOfFirstTrackInBuffer = mvhdBuffer.position() + boxHeader.getDataLength();

        //Level 3-Searching for "mdia" within "trak"
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MDIA.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }

        //Level 4-Searching for "mdhd" within "mdia"
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MDHD.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }

        //Level 4-Searching for "minf" within "mdia"
        mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.MINF.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }

        //Level 5-Searching for "smhd" within "minf"
        //Only an audio track would have a smhd frame
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.SMHD.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        mvhdBuffer.position(mvhdBuffer.position() + boxHeader.getDataLength());

        //Level 5-Searching for "stbl within "minf"
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.STBL.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }

        //Level 6-Searching for "stco within "stbl"
        boxHeader = Mp4BoxHeader.seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.STCO.getFieldName());
        if (boxHeader == null) {
            throw new CannotReadException("This file does not appear to be an audio file");
        }
        return new Mp4StcoBox(boxHeader, mvhdBuffer);
    }
}
