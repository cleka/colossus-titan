/*
 * $Id$
 *
 * (C) Copyright 2002 by Yuval Oren. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *   
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package com.bluecast.io;

import java.io.CharConversionException;

/**
 * Converts bytes to characters.
 *
 * @author Yuval Oren
 * @version $Revision$
 *
 */
 public interface CharsetDecoder {

    /** Minimum number of characters produced per byte using
     *  this decoder.
     */
    public int minBytesPerChar();

    /** Minimum number of characters produced per byte using
     *  this decoder.
     */
    public int maxBytesPerChar();


    /**
     * Decodes an array of bytes into characters.
     *
     * @param in_buf    input byte buffer
     * @param in_off    starting byte buffer offset
     * @param in_len    max number of bytes to read
     * @param out_buf   output character buffer
     * @param out_off   char buffer offset at which to start writing
     * @param out_len   max number of chars to write
     * @param result    an array of size >= 2 where results are returned:
     *                  result[0] = number of bytes read.
     *                  result[1] = number of chars written
     */
    public void decode(byte[] in_buf, int in_off, int in_len,
                      char[] out_buf, int out_off, int out_len, int[] result)
    throws CharConversionException;

    public CharsetDecoder newInstance();

}
