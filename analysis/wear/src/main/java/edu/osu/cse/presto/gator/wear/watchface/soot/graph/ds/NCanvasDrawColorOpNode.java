/*
 * NCanvasDrawColorOpNode.java - part of the GATOR project
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

public class NCanvasDrawColorOpNode extends NOpNode {


  public NCanvasDrawColorOpNode(NNode colorNode, NNode rcvNode,
                                Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    // The sensor id flows to sensor object.
    colorNode.addEdgeTo(this);
    rcvNode.addEdgeTo(this);
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

  @Override
  public NNode getParameter() {
    return this.pred.get(0);
  }
}
