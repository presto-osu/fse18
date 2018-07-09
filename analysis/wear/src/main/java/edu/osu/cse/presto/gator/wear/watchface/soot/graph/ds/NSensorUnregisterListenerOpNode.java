/*
 * NSensorUnregisterListenerOpNode.java - part of the GATOR project
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

public class NSensorUnregisterListenerOpNode extends NOpNode {

  public NSensorUnregisterListenerOpNode(NVarNode sensorNode, NNode listenerNode,
                                         Pair<Stmt, SootMethod> callSite, boolean artificial) {
    super(callSite, artificial);
    listenerNode.addEdgeTo(this, callSite.getO1());
    if (sensorNode != null) sensorNode.addEdgeTo(this, callSite.getO1());
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
