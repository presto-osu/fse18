/*
 * NContainerGetOpNode.java - part of the GATOR project
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

public class NContainerGetOpNode extends NOpNode {
  public NContainerGetOpNode(NNode listNode, NNode lhsNode, Pair<Stmt, SootMethod> callSite) {
    super(callSite, false);
    // TODO: currently does not handle containers
    listNode.addEdgeTo(lhsNode);
  }

  @Override
  public boolean hasReceiver() {
    return false;
  }

  @Override
  public boolean hasParameter() {
    return false;
  }

  @Override
  public boolean hasLhs() {
    return false;
  }
}
