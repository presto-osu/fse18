/*
 * NPhiNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import soot.shimple.PhiExpr;

public class NPhiNode extends NNode {
  public PhiExpr phiExpr;

  public NPhiNode(PhiExpr phiExpr) {
    this.phiExpr = phiExpr;
  }

  @Override
  public String toString() {
    return "Phi[" + phiExpr + "]" + id;
  }
}
