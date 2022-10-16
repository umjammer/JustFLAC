/*
 *  libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2000,2001,2002,2003  Josh Coalson
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

package org.kc7bfi.jflac;

import java.io.File;

import org.kc7bfi.jflac.frame.EntropyPartitionedRiceContents;
import org.kc7bfi.jflac.io.BitOutputStream;

public class FLACEncoder {

    private static class verify_input_fifo {
        int[][] data = new int[Constants.MAX_CHANNELS][Constants.MAX_BLOCK_SIZE];
        int size; /* of each data[] in samples */
        int tail;
    }

    private static class verify_output {
        byte[] data;
        int capacity;
        int bytes;
    }

    private static final int ENCODER_IN_MAGIC = 0;
    private static final int ENCODER_IN_METADATA = 1;
    private static final int ENCODER_IN_AUDIO = 2;

    // stream encoder states

    /** The encoder is in the normal OK state. */
    private static final int STREAM_ENCODER_OK = 0;

    /** An error occurred in the underlying verify stream decoder;
     * check stream_encoder_get_verify_decoder_state().
     */
    private static final int STREAM_ENCODER_VERIFY_DECODER_ERROR = 1;

    /** The verify decoder detected a mismatch between the original
     * audio signal and the decoded audio signal.
     */
    private static final int STREAM_ENCODER_VERIFY_MISMATCH_IN_AUDIO_DATA = 2;

    /** The encoder was initialized before setting all the required callbacks. */
    private static final int STREAM_ENCODER_INVALID_CALLBACK = 3;

    /** The encoder has an invalid setting for number of channels. */
    private static final int STREAM_ENCODER_INVALID_NUMBER_OF_CHANNELS = 4;

    /** The encoder has an invalid setting for bits-per-sample.
     * FLAC supports 4-32 bps but the reference encoder currently supports
     * only up to 24 bps.
     */
    private static final int STREAM_ENCODER_INVALID_BITS_PER_SAMPLE = 5;

    /** The encoder has an invalid setting for the input sample rate. */
    private static final int STREAM_ENCODER_INVALID_SAMPLE_RATE = 6;

    /** The encoder has an invalid setting for the block size. */
    private static final int STREAM_ENCODER_INVALID_BLOCK_SIZE = 7;

    /** The encoder has an invalid setting for the maximum LPC order. */
    private static final int STREAM_ENCODER_INVALID_MAX_LPC_ORDER = 8;

    /** The encoder has an invalid setting for the precision of the quantized linear predictor coefficients. */
    private static final int STREAM_ENCODER_INVALID_QLP_COEFF_PRECISION = 9;

    /** Mid/side coding was specified but the number of channels is not equal to 2. */
    private static final int STREAM_ENCODER_MID_SIDE_CHANNELS_MISMATCH = 10;

    /** @deprecated */
    private static final int STREAM_ENCODER_MID_SIDE_SAMPLE_SIZE_MISMATCH = 11;

    /** Loose mid/side coding was specified but mid/side coding was not. */
    private static final int STREAM_ENCODER_ILLEGAL_MID_SIDE_FORCE = 12;

    /** The specified block size is less than the maximum LPC order. */
    private static final int STREAM_ENCODER_BLOCK_SIZE_TOO_SMALL_FOR_LPC_ORDER = 13;

    /** The encoder is bound to the "streamable subset" but other settings violate it. */
    private static final int STREAM_ENCODER_NOT_STREAMABLE = 14;

    /** An error occurred while writing the stream; usually, the write_callback returned an error. */
    private static final int STREAM_ENCODER_FRAMING_ERROR = 15;

    /** The metadata input to the encoder is invalid, in one of the following ways:
     * - stream_encoder_set_metadata() was called with a null pointer but a block count > 0
     * - One of the metadata blocks contains an undefined type
     * - It contains an illegal CUESHEET as checked by format_cuesheet_is_legal()
     * - It contains an illegal SEEKTABLE as checked by format_seektable_is_legal()
     * - It contains more than one SEEKTABLE block or more than one VORBIS_COMMENT block
     */
    private static final int STREAM_ENCODER_INVALID_METADATA = 16;

    /**< An error occurred while writing the stream; usually, the write_callback returned an error. */
    private static final int STREAM_ENCODER_FATAL_ERROR_WHILE_ENCODING = 17;

    /**< The write_callback returned an error. */
    private static final int STREAM_ENCODER_FATAL_ERROR_WHILE_WRITING = 18;

    /**< Memory allocation failed. */
    private static final int STREAM_ENCODER_MEMORY_ALLOCATION_ERROR = 19;

    /** stream_encoder_init() was called when the encoder was
     * already initialized, usually because
     * stream_encoder_finish() was not called.
     */
    private static final int STREAM_ENCODER_ALREADY_INITIALIZED = 20;

    /** The encoder is in the uninitialized state. */
    private static final int STREAM_ENCODER_UNINITIALIZED = 21;

    // Private class data

    /** current size (in samples) of the signal and residual buffers */
    int input_capacity;
    /** the integer version of the input signal */
    int[][] integer_signal = new int[Constants.MAX_CHANNELS][Constants.MAX_BLOCK_SIZE];
    /** the integer version of the mid-side input signal (stereo only) */
    int[][] integer_signal_mid_side = new int[2][Constants.MAX_BLOCK_SIZE];
    /** the floating-point version of the input signal */
    double[][] real_signal = new double[Constants.MAX_CHANNELS][Constants.MAX_BLOCK_SIZE];
    /** the floating-point version of the mid-side input signal (stereo only) */
    double[][] real_signal_mid_side = new double[2][Constants.MAX_BLOCK_SIZE];
    /** the effective bits per sample of the input signal (stream bps - wasted bits) */
    int[] subframe_bps = new int[Constants.MAX_CHANNELS];
    /** the effective bits per sample of the mid-side input signal (stream bps - wasted bits + 0/1) */
    int[] subframe_bps_mid_side = new int[2];
    /** each channel has a candidate and best workspace where the subframe residual signals will be stored */
    int[][] residual_workspace = new int[Constants.MAX_CHANNELS][2];
    int[][] residual_workspace_mid_side = new int[2][2];
    EntropyPartitionedRiceContents[][] partitioned_rice_contents_workspace = new EntropyPartitionedRiceContents[Constants.MAX_CHANNELS][2];
    EntropyPartitionedRiceContents[][] partitioned_rice_contents_workspace_mid_side = new EntropyPartitionedRiceContents[Constants.MAX_CHANNELS][2];
    EntropyPartitionedRiceContents[][] partitioned_rice_contents_workspace_ptr = new EntropyPartitionedRiceContents[Constants.MAX_CHANNELS][2];
    EntropyPartitionedRiceContents[][] partitioned_rice_contents_workspace_ptr_mid_side = new EntropyPartitionedRiceContents[Constants.MAX_CHANNELS][2];
    /** index into the above workspaces */
    int[] best_subframe = new int[Constants.MAX_CHANNELS];
    int[] best_subframe_mid_side = new int[2];
    /** size in bits of the best subframe for each channel */
    int[] best_subframe_bits = new int[Constants.MAX_CHANNELS];
    int[] best_subframe_bits_mid_side = new int[2];
    /** the current frame being worked on */
    BitOutputStream frame = new BitOutputStream();
    /** exact number of frames the encoder will use before trying both independent and mid/side frames again */
    double loose_mid_side_stereo_frames_exact;
    /** rounded number of frames the encoder will use before trying both independent and mid/side frames again */
    int loose_mid_side_stereo_frames;
    /** number of frames using the current channel assignment */
    int loose_mid_side_stereo_frame_count;
    int last_channel_assignment;
    int current_sample_number;
    int current_frame_number;
    /** use slow 64-bit versions of some functions because of the block size */
    boolean use_wide_by_block;
    /** use slow 64-bit versions of some functions because of the min partition order and blocksize */
    boolean use_wide_by_partition;
    /** use slow 64-bit versions of some functions because of the lpc order */
    boolean use_wide_by_order;
    /** our initial guess as to whether precomputing the partitions sums will be a speed improvement */
    boolean precompute_partition_sums;
    boolean disable_constant_subframes;
    boolean disable_fixed_subframes;
    boolean disable_verbatim_subframes;
    int[] integer_signal_unaligned = new int[Constants.MAX_CHANNELS];
    int[] integer_signal_mid_side_unaligned = new int[2];
    double[] real_signal_unaligned = new double[Constants.MAX_CHANNELS];
    double[] real_signal_mid_side_unaligned = new double[2];
    int[][] residual_workspace_unaligned = new int[Constants.MAX_CHANNELS][2];
    int[][] residual_workspace_mid_side_unaligned = new int[2][2];
    /** from find_best_partition_order_() */
    EntropyPartitionedRiceContents[] partitioned_rice_contents_extra = new EntropyPartitionedRiceContents[2];
    /**
     * The data for the verify section
     */
    private static class VerifyData {
        FLACDecoder decoder;
        int state_hint;
        boolean needs_magic_hack;
        verify_input_fifo input_fifo;
        verify_output output;
        private class error_stats {
            long absolute_sample;
            int frame_number;
            int channel;
            int sample;
            int expected;
            int got;
        }
    }
    private VerifyData verifyData = new VerifyData();
    boolean is_being_deleted; /* if true, call to ..._finish() from ..._delete() will not call the callbacks */

    // protected
    int state;
    boolean verify;
    boolean streamable_subset;
    boolean do_mid_side_stereo;
    boolean loose_mid_side_stereo;
    int channels;
    int bits_per_sample;
    int sample_rate;
    int blocksize;
    int max_lpc_order;
    int qlp_coeff_precision;
    boolean do_qlp_coeff_prec_search;
    boolean do_exhaustive_model_search;
    boolean do_escape_coding;
    int min_residual_partition_order;
    int max_residual_partition_order;
    int rice_parameter_search_dist;
    long total_samples_estimate;
    //StreamMetadata **metadata;
    int num_metadata_blocks;

    // private

    // Public static class data

    private static final String[] StreamEncoderStateString = new String[] {
            "STREAM_ENCODER_OK",
            "STREAM_ENCODER_VERIFY_DECODER_ERROR",
            "STREAM_ENCODER_VERIFY_MISMATCH_IN_AUDIO_DATA",
            "STREAM_ENCODER_INVALID_CALLBACK",
            "STREAM_ENCODER_INVALID_NUMBER_OF_CHANNELS",
            "STREAM_ENCODER_INVALID_BITS_PER_SAMPLE",
            "STREAM_ENCODER_INVALID_SAMPLE_RATE",
            "STREAM_ENCODER_INVALID_BLOCK_SIZE",
            "STREAM_ENCODER_INVALID_MAX_LPC_ORDER",
            "STREAM_ENCODER_INVALID_QLP_COEFF_PRECISION",
            "STREAM_ENCODER_MID_SIDE_CHANNELS_MISMATCH",
            "STREAM_ENCODER_MID_SIDE_SAMPLE_SIZE_MISMATCH",
            "STREAM_ENCODER_ILLEGAL_MID_SIDE_FORCE",
            "STREAM_ENCODER_BLOCK_SIZE_TOO_SMALL_FOR_LPC_ORDER",
            "STREAM_ENCODER_NOT_STREAMABLE",
            "STREAM_ENCODER_FRAMING_ERROR",
            "STREAM_ENCODER_INVALID_METADATA",
            "STREAM_ENCODER_FATAL_ERROR_WHILE_ENCODING",
            "STREAM_ENCODER_FATAL_ERROR_WHILE_WRITING",
            "STREAM_ENCODER_MEMORY_ALLOCATION_ERROR",
            "STREAM_ENCODER_ALREADY_INITIALIZED",
            "STREAM_ENCODER_UNINITIALIZED"
    };

    private static final String[] StreamEncoderWriteStatusString = new String[] {
            "STREAM_ENCODER_WRITE_STATUS_OK",
            "STREAM_ENCODER_WRITE_STATUS_FATAL_ERROR"
    };

    // Class constructor/destructor

    public FLACEncoder() {
        setDefaults();

        is_being_deleted = false;
        
        state = STREAM_ENCODER_UNINITIALIZED;
    }

    public void encode(File inputFile, File outputFile) {
    	 throw new UnsupportedOperationException("encode");
    }
    
    public void setVerify(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        verify = value;
    }

    public void setStreamableSubset(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        streamable_subset = value;
    }

    public void setDoMidSideStereo(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        do_mid_side_stereo = value;
    }

    public void setLooseMidSideStereo(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        loose_mid_side_stereo = value;
    }

    public void setChannels(int value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        channels = value;
    }

    public void setBitsPerSample(int value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        bits_per_sample = value;
    }

    public void setSampleRate(int value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        sample_rate = value;
    }

    public void setBlocksize(int value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        blocksize = value;
    }

    public void setMaxLPCOrder(int value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        max_lpc_order = value;
    }

    public void setQLPCoeffPrecision(int value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        qlp_coeff_precision = value;
    }

    public void setDoQLPCoeffPrecSearch(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        do_qlp_coeff_prec_search = value;
    }

    public void setDoExhaustiveModelSearch(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        do_exhaustive_model_search = value;
    }

    public void setMinResidualPartitionOrder(int value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        min_residual_partition_order = value;
    }

    public void setMaxResidualPartitionOrder(int value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        max_residual_partition_order = value;
    }

    public void setTotalSamplesEstimate(long value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        total_samples_estimate = value;
    }

    /*
     * These three functions are not static, but not publically exposed in
     * include/FLAC/ either.  They are used by the test suite.
     */
    public void disableConstantSubframes(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        disable_constant_subframes = value;
    }

    public void disableFixedSubframes(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        disable_fixed_subframes = value;
    }

    public void disableVerbatimSubframes(boolean value) {
        if (state != STREAM_ENCODER_UNINITIALIZED) return;
        disable_verbatim_subframes = value;
    }

    public int getState() {
        return state;
    }

    public boolean getVerify() {
        return verify;
    }

    public boolean getStreamableSubset() {
        return streamable_subset;
    }

    public boolean getDoMidSideStereo() {
        return do_mid_side_stereo;
    }

    public boolean getLooseMidSideStereo() {
        return loose_mid_side_stereo;
    }

    public int getChannels() {
        return channels;
    }

    public int getBitsPerSample() {
        return bits_per_sample;
    }

    public int getSampleRate() {
        return sample_rate;
    }

    public int getBlocksize() {
        return blocksize;
    }

    public int getMaxLPCOrder() {
        return max_lpc_order;
    }

    public int getQLPCoeffPrecision() {
        return qlp_coeff_precision;
    }

    public boolean getDoQLPCoeffPrecSearch() {
        return do_qlp_coeff_prec_search;
    }

    public boolean getDoEscapeCoding() {
        return do_escape_coding;
    }

    public boolean getDoExhaustiveModelSearch() {
        return do_exhaustive_model_search;
    }

    public int getMinResidualPartitionOrder() {
        return min_residual_partition_order;
    }

    public int getMaxResidualPartitionOrder() {
        return max_residual_partition_order;
    }

    public int getRiceParameterSearchDist() {
        return rice_parameter_search_dist;
    }

    public long getTotalSamplesEstimate() {
        return total_samples_estimate;
    }

    // Private class methods

    public void setDefaults() {

        verify = false;
        streamable_subset = true;
        do_mid_side_stereo = false;
        loose_mid_side_stereo = false;
        channels = 2;
        bits_per_sample = 16;
        sample_rate = 44100;
        blocksize = 1152;
        max_lpc_order = 0;
        qlp_coeff_precision = 0;
        do_qlp_coeff_prec_search = false;
        do_exhaustive_model_search = false;
        do_escape_coding = false;
        min_residual_partition_order = 0;
        max_residual_partition_order = 0;
        rice_parameter_search_dist = 0;
        total_samples_estimate = 0;
        num_metadata_blocks = 0;

        disable_constant_subframes = false;
        disable_fixed_subframes = false;
        disable_verbatim_subframes = false;
    }
}
