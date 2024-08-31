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

/**
 * LPC Predictor utility class.
 *
 * @author kc7bfi
 */
public class LPCPredictor {

    /**
     * Restore the signal from the LPC compression.
     *
     * @param residual       The residual signal
     * @param dataLen        The length of the residual data
     * @param qlpCoeff
     * @param order          The predicate order
     * @param lpQuantization
     * @param data           The restored signal (output)
     * @param startAt        The starting position in the data array
     */
    public static void restoreSignal(int[] residual, int dataLen, int[] qlpCoeff, int order, int lpQuantization, int[] data, int startAt) {
//        logger.log(Level.DEBUG, "Q=" + lpQuantization);
        for (int i = 0; i < dataLen; i++) {
            int sum = 0;
            for (int j = 0; j < order; j++) {
                sum += qlpCoeff[j] * data[startAt + i - j - 1];
            }
//            if (logger.isLoggable(Level.DEBUG)) System.out.print((sum >> lpQuantization) + " ");
            data[startAt + i] = residual[i] + (sum >> lpQuantization);
        }
//        if (logger.isLoggable(Level.DEBUG)) {
//            System.out.println();
//            for (int i = 0; i < dataLen + startAt; i++) System.out.print(data[i] + " ");
//            System.out.println();
//            for (int j = 0; j < order; j++) System.out.print(qlpCoeff[j] + " ");
//            System.out.println();
//            System.exit(1);
//        }
    }

    /**
     * Restore the signal from the LPC compression.
     *
     * @param residual       The residual signal
     * @param dataLen        The length of the residual data
     * @param qlpCoeff
     * @param order          The predicate order
     * @param lpQuantization
     * @param data           The restored signal (output)
     * @param startAt        The starting position in the data array
     */
    public static void restoreSignalWide(int[] residual, int dataLen, int[] qlpCoeff, int order, int lpQuantization, int[] data, int startAt) {
        for (int i = 0; i < dataLen; i++) {
            long sum = 0;
            for (int j = 0; j < order; j++)
                sum += (long) qlpCoeff[j] * (long) (data[startAt + i - j - 1]);
            data[startAt + i] = residual[i] + (int) (sum >> lpQuantization);
        }
    }
}
