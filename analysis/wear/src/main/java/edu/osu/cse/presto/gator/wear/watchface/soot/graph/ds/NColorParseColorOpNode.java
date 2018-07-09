/*
 * NColorParseColorOpNode.java - part of the GATOR project
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

public class NColorParseColorOpNode extends NOpNode {
  NNode colorNode;
  NVarNode lhsNode;

  public NColorParseColorOpNode(NNode colorNode, NVarNode lhsNode, Pair<Stmt, SootMethod> callSite) {
    super(callSite, true);
    this.colorNode = colorNode;
    this.lhsNode = lhsNode;
    colorNode.addEdgeTo(this, callSite.getO1());
    this.addEdgeTo(lhsNode, callSite.getO1());
  }

  @Override
  public boolean hasReceiver() {
    return false;
  }

  @Override
  public boolean hasParameter() {
    return true;
  }

  @Override
  public NNode getParameter() {
    return this.colorNode;
  }

  @Override
  public boolean hasLhs() {
    return true;
  }

  @Override
  public NVarNode getLhs() {
    return this.lhsNode;
  }
}
