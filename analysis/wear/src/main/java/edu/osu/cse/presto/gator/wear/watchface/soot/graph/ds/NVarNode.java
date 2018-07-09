/*
 * NVarNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import soot.Local;

public class NVarNode extends NRefNode {
  public Local l;

  public NVarNode(Local l) {
    this.l = l;
  }

  public String toString() {
    return "VAR[" + l + "]" + id;
  }
}
