/*
 * Timer.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator;

public class Timer {
  private long startTime = System.currentTimeMillis();

  public void reset() {
    startTime = System.currentTimeMillis();
  }

  public long getInterval() {
    return System.currentTimeMillis() - startTime;
  }
}
