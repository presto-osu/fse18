/*
 * NFakeThisNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import soot.SootClass;

public class NFakeThisNode extends NAllocNode {
  private SootClass clz;

  public NFakeThisNode(SootClass clz) {
    super(null);
    this.clz = clz;
  }

  @Override
  public SootClass getClassType() {
    return clz;
  }

  @Override
  public String toString() {
    return "This[" + clz + "]" + id;
  }
}
