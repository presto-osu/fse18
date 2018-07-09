/*
 * NObjectNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import soot.SootClass;

public abstract class NObjectNode extends NNode {
  public abstract SootClass getClassType();
}
