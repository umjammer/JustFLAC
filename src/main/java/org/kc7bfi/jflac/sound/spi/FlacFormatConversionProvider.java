/*
 * libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

package org.kc7bfi.jflac.sound.spi;

import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;


/**
 * A format conversion provider provides format conversion services from one or
 * more input formats to one or more output formats. Converters include codecs,
 * which encode and/or decode audio data, as well as transcoders, etc. Format
 * converters provide methods for determining what conversions are supported and
 * for obtaining an audio stream from which converted data can be read. The
 * source format represents the format of the incoming audio data, which will be
 * converted. The target format represents the format of the processed,
 * converted audio data. This is the format of the data that can be read from
 * the stream returned by one of the getAudioInputStream methods.
 *
 * @author Marc Gimpel, Wimba S.A. (marc@wimba.com)
 * @author Florian Bomers
 * @version $Revision: 1.1 $
 */
public class FlacFormatConversionProvider extends FormatConversionProvider {

    private static final Logger logger = Logger.getLogger(FlacFormatConversionProvider.class.getName());

    /** to disable encoding */
    private static final boolean HAS_ENCODING = false;

    /**
     * Obtains the set of source format encodings from which format conversion
     * services are provided by this provider.
     *
     * @return array of source format encodings. The array will always have a
     * length of at least 1.
     */
    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        if (HAS_ENCODING) {
            final AudioFormat.Encoding[] encodings = {
                    FlacEncoding.FLAC, AudioFormat.Encoding.PCM_SIGNED
            };
            return encodings;
        } else {
            final AudioFormat.Encoding[] encodings = {
                    FlacEncoding.FLAC
            };
            return encodings;
        }
    }

    /**
     * Obtains the set of target format encodings to which format conversion
     * services are provided by this provider.
     *
     * @return array of target format encodings. The array will always have a
     * length of at least 1.
     */
    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        if (HAS_ENCODING) {
            return new AudioFormat.Encoding[] { FlacEncoding.FLAC, AudioFormat.Encoding.PCM_SIGNED };
        } else {
            return new AudioFormat.Encoding[] { FlacEncoding.PCM_SIGNED };
        }
    }

    private boolean isBitSizeOK(AudioFormat format, boolean notSpecifiedOK) {
        int bitSize = format.getSampleSizeInBits();
        return (notSpecifiedOK && (bitSize == AudioSystem.NOT_SPECIFIED))
                || (bitSize == 8) || (bitSize == 16) || (bitSize == 24);
    }

    private boolean isChannelsOK(AudioFormat format, boolean notSpecifiedOK) {
        int channels = format.getChannels();
        return (notSpecifiedOK && (channels == AudioSystem.NOT_SPECIFIED))
                || (channels == 1) || (channels == 2);
    }

    /**
     * Obtains the set of target format encodings supported by the format
     * converter given a particular source format. If no target format encodings
     * are supported for this source format, an array of length 0 is returned.
     *
     * @param sourceFormat format of the incoming data.
     * @return array of supported target format encodings.
     */
    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        boolean bitSizeOK = isBitSizeOK(sourceFormat, true);
        boolean channelsOK = isChannelsOK(sourceFormat, true);
        if (HAS_ENCODING
                && bitSizeOK
                && channelsOK
                && !sourceFormat.isBigEndian()
                && sourceFormat.getEncoding().equals(
                AudioFormat.Encoding.PCM_SIGNED)) {
            // encoder
logger.finer("FLAC converter: can encode to PCM: " + sourceFormat);
            return new AudioFormat.Encoding[] { FlacEncoding.FLAC };
        } else if (bitSizeOK && channelsOK && sourceFormat.getEncoding().equals(FlacEncoding.FLAC)) {
            // decoder
logger.finer("FLAC converter: can decode to FLAC: " + sourceFormat);
            return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED };
        } else {
logger.finer("FLAC converter: cannot de/encode: " + sourceFormat);
            return new AudioFormat.Encoding[] {};
        }
    }

    /**
     * Obtains the set of target formats with the encoding specified supported
     * by the format converter. If no target formats with the specified encoding
     * are supported for this source format, an array of length 0 is returned.
     *
     * @param targetEncoding desired encoding of the outgoing data.
     * @param sourceFormat   format of the incoming data.
     * @return array of supported target formats.
     */
    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        return getTargetFormats(targetEncoding, sourceFormat, true);
    }

    /**
     * Like getTargetFormats(AudioFormat.Encoding, AudioFormat), but with
     * additional choice if AudioSystem.NOT_SPECIFIED is supported in
     * sourceFormat's bitSize or channels.
     *
     * @param notSpecifiedOK if true, bitSize and channels can have value
     *                       AudioSystem.NOT_SPECIFIED
     * @return array of supported target formats.
     */
    private AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat, boolean notSpecifiedOK) {
        boolean bitSizeOK = isBitSizeOK(sourceFormat, notSpecifiedOK);
        boolean channelsOK = isChannelsOK(sourceFormat, notSpecifiedOK);
        if (HAS_ENCODING
                && bitSizeOK
                && channelsOK
                && !sourceFormat.isBigEndian()
                && sourceFormat.getEncoding().equals(
                AudioFormat.Encoding.PCM_SIGNED)
                && targetEncoding.equals(FlacEncoding.FLAC)) {
            // encode to FLAC
logger.finer("FLAC converter: can encode: " + sourceFormat + " to " + targetEncoding);
            return new AudioFormat[] {
                    new AudioFormat(FlacEncoding.FLAC,
                            sourceFormat.getSampleRate(), //
                            AudioSystem.NOT_SPECIFIED,    // sample size in bits
                            sourceFormat.getChannels(),   //
                            AudioSystem.NOT_SPECIFIED,    // frame size
                            AudioSystem.NOT_SPECIFIED,    // frame rate
                            false)
            }; // little endian
        } else if (bitSizeOK && channelsOK
                && sourceFormat.getEncoding().equals(FlacEncoding.FLAC)
                && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            // decode to PCM
logger.finer("FLAC converter: can decode: " + sourceFormat + " to " + targetEncoding);
            return new AudioFormat[] {
                    new AudioFormat(sourceFormat.getSampleRate(), //
                            sourceFormat.getSampleSizeInBits(),   // sample size in bits
                            sourceFormat.getChannels(),           //
                            true,                                 // signed
                            false)
            }; // little endian (for PCM wav)
        } else {
logger.finer("FLAC converter: cannot de/encode: " + sourceFormat + " to " + targetEncoding);
            return new AudioFormat[] {};
        }
    }

    /**
     * Obtains an audio input stream with the specified encoding from the given
     * audio input stream.
     *
     * @param targetEncoding - desired encoding of the stream after processing.
     * @param sourceStream   - stream from which data to be processed should be
     *                       read.
     * @return stream from which processed data with the specified target
     * encoding may be read.
     */
    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        AudioFormat[] formats = getTargetFormats(targetEncoding, sourceStream.getFormat(), false);
        if (formats.length > 0) {
            return getAudioInputStream(formats[0], sourceStream);
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }

    /**
     * Obtains an audio input stream with the specified format from the given
     * audio input stream.
     *
     * @param targetFormat - desired data format of the stream after processing.
     * @param sourceStream - stream from which data to be processed should be
     *                     read.
     * @return stream from which processed data with the specified format may be
     * read.
     */
    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat[] formats = getTargetFormats(targetFormat.getEncoding(), sourceFormat, false);
        if (formats.length > 0) {
            if (sourceFormat.equals(targetFormat)) {
                return sourceStream;
            } else if (sourceFormat.getChannels() == targetFormat.getChannels()
                    && sourceFormat.getSampleSizeInBits() == targetFormat.getSampleSizeInBits()
                    && !targetFormat.isBigEndian()
                    && sourceFormat.getEncoding().equals(FlacEncoding.FLAC)
                    && targetFormat.getEncoding().equals(
                    AudioFormat.Encoding.PCM_SIGNED)) {
                // decoder
                return new Flac2PcmAudioInputStream(sourceStream, targetFormat,
                        -1);
            } else if (sourceFormat.getChannels() == targetFormat.getChannels()
                    && sourceFormat.getSampleSizeInBits() == targetFormat.getSampleSizeInBits()
                    && sourceFormat.getEncoding().equals(
                    AudioFormat.Encoding.PCM_SIGNED)
                    && targetFormat.getEncoding().equals(FlacEncoding.FLAC)) {
                // encoder

                throw new IllegalArgumentException("FLAC encoder not yet implemented");
            } else {
                throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }
}
