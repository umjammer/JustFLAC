package org.kc7bfi.jflac.frame;

import java.io.IOException;

import org.kc7bfi.jflac.io.BitInputStream;


/**
 * JustFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2013  Dmitriy Rogatkin
 * <p>
 * BSD license
 */
public class EntropyPartitionedRice2 extends EntropyCodingMethod {

    private static final int ENTROPY_CODING_METHOD_PARTITIONED_RICE2_PARAMETER_LEN = 5; // bits
    private static final int ENTROPY_CODING_METHOD_PARTITIONED_RICE2_RAW_LEN = 5; // bits
    private static final int ENTROPY_CODING_METHOD_PARTITIONED_RICE2_ESCAPE_PARAMETER = 31;

    /**
     * Read compressed signal residual data.
     *
     * @param is             The InputBitStream
     * @param predictorOrder The predicate order
     * @param partitionOrder The partition order
     * @param header         The FLAC Frame Header
     * @param residual       The residual signal (output)
     * @throws IOException On error reading from InputBitStream
     */
    @Override
    void readResidual(BitInputStream is, int predictorOrder, int partitionOrder, Header header, int[] residual) throws IOException {
        // TODO add propagate resync exception
//        logger.log(Level.DEBUG, "readResidual Pred="+predictorOrder+" part="+partitionOrder);
        int sample = 0;
        int partitions = 1 << partitionOrder;
        int partitionSamples = partitionOrder > 0 ? header.blockSize >> partitionOrder : header.blockSize - predictorOrder;
//        logger.log(Level.DEBUG, String.format("Allocating %d or %d  predict %d  bs: %d%n", partitionOrder, partitionSamples, predictorOrder, header.blockSize));
        if (predictorOrder == 0) {
            if (header.blockSize < predictorOrder) {
                //logger.log(Level.DEBUG, String.format("NEED RESYNC  %d - %d%n", header.blockSize, predictorOrder));
                return;
            }
        } else {
            if (partitionSamples < predictorOrder) {
//                logger.log(Level.DEBUG, String.format("NEED RESYNC2  %d - %d%n", partitionSamples , predictorOrder));
                return;
            }
        }
        contents.ensureSize(Math.max(6, partitionOrder));
        //contents.parameters = new int[partitions];

        for (int partition = 0; partition < partitions; partition++) {
            int riceParameter = is.readRawUInt(ENTROPY_CODING_METHOD_PARTITIONED_RICE2_PARAMETER_LEN);
            contents.parameters[partition] = riceParameter;
            if (riceParameter < ENTROPY_CODING_METHOD_PARTITIONED_RICE2_ESCAPE_PARAMETER) {
                int u = (partitionOrder == 0 || partition > 0) ? partitionSamples : partitionSamples - predictorOrder;
//                logger.log(Level.DEBUG, String.format("Rice: %d n:%d%n", riceParameter, u));
                is.readRiceSignedBlock(residual, sample, u, riceParameter);
                sample += u;
            } else {
                riceParameter = is.readRawUInt(ENTROPY_CODING_METHOD_PARTITIONED_RICE2_RAW_LEN);
                contents.rawBits[partition] = riceParameter;
                for (int u = (partitionOrder == 0 || partition > 0) ? 0 : predictorOrder; u < partitionSamples; u++, sample++) {
                    residual[sample] = is.readRawInt(riceParameter);
                }
            }
        }
    }
}
