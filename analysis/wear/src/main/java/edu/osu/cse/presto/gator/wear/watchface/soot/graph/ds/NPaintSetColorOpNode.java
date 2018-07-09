/*
 * NPaintSetColorOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class NPaintSetColorOpNode extends NOpNode {
  public NPaintSetColorOpNode(NNode colorIntConstNode, NVarNode rcvNode, Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    colorIntConstNode.addEdgeTo(this, callSite.getO1());
    rcvNode.addEdgeTo(this, callSite.getO1());
  }

  @Override
  public boolean hasReceiver() {
    return true;
  }

  @Override
  public boolean hasParameter() {
    return true;
  }

  @Override
  public boolean hasLhs() {
    return false;
  }
}
