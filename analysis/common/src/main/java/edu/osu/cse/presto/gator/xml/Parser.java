/*
 * Parser.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.xml;

/**
 * @param <V> the data type of the input XML,
 *            e.g., {@link java.io.File}, {@link java.io.InputStream}, etc.
 * @param <T> the data type of the result
 */
public interface Parser<V, T> {
    /**
     * Parsing an XML
     * @param xml
     * @return
     */
    T parse(V xml);
}

