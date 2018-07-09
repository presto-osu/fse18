/*
 * NFieldNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import soot.SootField;

public class NFieldNode extends NRefNode {
  public SootField f;

  public String toString() {
    return "FLD[" + f + "]" + id;
  }
}
