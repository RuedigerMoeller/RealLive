package de.ruedigermoeller.reallive.impl;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 13.11.13
 * Time: 02:32
 * To change this template use File | Settings | File Templates.
 */
public class IntStack {

    int arr[];
    int currentOffset = 0;

    public IntStack(int size) {
        arr = new int[size];
    }

    public void push(int value) {
        arr[currentOffset++] = value;
    }

    public int pop() {
        if ( currentOffset > 0 ) {
            return arr[--currentOffset];
        }
        return Integer.MIN_VALUE;
    }

    public int getSize() {
        return currentOffset;
    }

    public void resize(int newSize) {
        int newArr[] = new int[newSize];
        System.arraycopy(arr,0,newArr,0,currentOffset);
        arr = newArr;
    }
}
