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


package com.bluecast.util;

final public class IndexedObjectImpl implements IndexedObject {
    private int index;
    private Object object;

    public IndexedObjectImpl(int index, Object object) {
        this.index = index;
        this.object = object;
    }

    final public int getIndex() { return index; }
    final public void setIndex(int index) { this.index = index; }

    final public Object getObject() { return object; }
    final public void setObject(Object object) { this.object = object; }

    final public Object clone() {
        return new IndexedObjectImpl(index,object);
    }

    final public boolean equals(Object o) {
        if (o instanceof IndexedObject) {
            IndexedObject i = (IndexedObject) o;
            return (index == i.getIndex() && object.equals(i.getObject()));
        }
        else {
            return false;
        }
    }
}
