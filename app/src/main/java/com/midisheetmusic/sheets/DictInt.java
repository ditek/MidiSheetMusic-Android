/*
 * Copyright (c) 2007-2011 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */


package com.midisheetmusic.sheets;


/**@class DictInt
 *  The DictInt class is a dictionary mapping integers to integers.
 */
public class DictInt {
    private int[] keys;    /** Sorted array of integer keys */
    private int[] values;  /** Array of integer values */
    private int size;      /** Number of keys */
    private int lastpos;   /** The index from the last "get" method */

    /** Create a new DictInt instance with the given capacity.
     * Initialize two int arrays,  one to store the keys and one
     * to store the values.
     */
    public DictInt() {
        size = 0;
        int amount = 23;
        lastpos = 0;
        keys = new int[amount];
        values = new int[amount];
    }

    /** Increase the capacity of the key/value arrays  */
    private void resize() {
        int newcapacity = keys.length * 2;
        int[] newkeys = new int[newcapacity];
        int[] newvalues = new int[newcapacity];
        for (int i = 0; i < keys.length; i++) {
            newkeys[i] = keys[i];
            newvalues[i] = values[i];
        }
        keys = newkeys;
        values = newvalues;
    }

    /** Add the given key/value pair to this dictionary.
     * This assumes the key is not already in the dictionary.
     * If the keys/values arrays are full, then resize them.
     * The keys array must be kept in sorted order, so insert
     * the new key/value in the correct sorted position.
     */
    public void add(int key, int value) {
        if (size == keys.length) {
            resize();
        }

        int pos = size-1;
        while (pos >= 0 && key < keys[pos]) {
            keys[pos+1] = keys[pos];
            values[pos+1] = values[pos];
            pos--;
        }
        keys[pos+1] = key;
        values[pos+1] = value;
        size++;
    }


    /** Set the given key to the given value */
    public void set(int key, int value) {
        if (contains(key)) {
            keys[lastpos] = key;
            values[lastpos] = value;
        }
        else {
            add(key, value);
        }
    }

    /** Return true if this dictionary contains the given key.
     * If true, set lastpos = the index position of the key.
     */
    public boolean contains(int key) {
        if (size == 0)
            return false;

        /* The SymbolWidths class calls this method many times,
         * passing the keys in sorted order.  To speed up performance,
         * we start searching at the position of the last key (lastpos),
         * instead of starting at the beginning of the array.
         */
        if (lastpos < 0 || lastpos >= size || key < keys[lastpos])
            lastpos = 0;

        while (lastpos < size && key > keys[lastpos]) {
            lastpos++;
        }
        return lastpos < size && key == keys[lastpos];
    }


    /** Get the value for the given key. */
    int get(int key) {
        if (contains(key)) {
            return values[lastpos];
        }
        else {
            return 0;
        }
    }

    /** Return the number of key/value pairs */
    int count() {
        return size;
    }

    /** Return the key at the given index */
    int getKey(int index) {
        return keys[index];
    }

}


