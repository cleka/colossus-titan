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

public class FileFormatException extends java.io.IOException {
    protected int line;
    protected int column;

    public FileFormatException() {
            this(null);
    }

    public FileFormatException(String msg) {
            this(msg,-1,-1);
    }

    public FileFormatException(String msg, int line, int column) {
            super(msg);
            this.line = line;
            this.column = column;
    }

    /** Returns the line number of the bad formatting, or -1 if unknown */
    public int getLine() { return line; }

    /** Returns the column number of the bad formatting, or -1 if unknown */
    public int getColumn() { return column; }
}

