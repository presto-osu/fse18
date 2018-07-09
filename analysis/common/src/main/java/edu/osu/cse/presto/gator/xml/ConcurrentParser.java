/*
 * ConcurrentParser.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.xml;

import edu.osu.cse.presto.gator.concurrent.Job;

/**
 * XML parser with concurrency support.
 * Every concurrent parser is also a job to be submitted to the executor.
 */
public abstract class ConcurrentParser<V, T> implements Parser<V, T>, Job<V, T> {
    private V xml;

    private ConcurrentParser() {
    }

    public ConcurrentParser(V xml) {
        this.xml = xml;
    }

    @Override
    public V prepare() {
        return this.xml;
    }

    @Override
    public T process(V data) {
        return parse(data);
    }
}
