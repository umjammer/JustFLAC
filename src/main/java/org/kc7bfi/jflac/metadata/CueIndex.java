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

package org.kc7bfi.jflac.metadata;

import java.io.IOException;

import org.kc7bfi.jflac.io.BitInputStream;

/**
 * An entry into the cue track.
 * @author kc7bfi
 */
public class CueIndex {

    /** bits */
    private static final int CUESHEET_INDEX_OFFSET_LEN = 64;
    /** bits */
    private static final int CUESHEET_INDEX_NUMBER_LEN = 8;
    /** bits */
    private static final int CUESHEET_INDEX_RESERVED_LEN = 3 * 8;

    /** Offset in samples, relative to the track offset, of the index point. */
    protected long offset;
    /** The index point number. */
    protected byte number;
    
    /**
     * The constructor.
     * @param is                The InputBitStream
     * @throws IOException      Thrown if error reading from InputBitStream
     */
    public CueIndex(BitInputStream is) throws IOException {
        offset = is.readRawULong(CUESHEET_INDEX_OFFSET_LEN);
        number = (byte) is.readRawUInt(CUESHEET_INDEX_NUMBER_LEN);
        is.skipBitsNoCRC(CUESHEET_INDEX_RESERVED_LEN);
    }
}
