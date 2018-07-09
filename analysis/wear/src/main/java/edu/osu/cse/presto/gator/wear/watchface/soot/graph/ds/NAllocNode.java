/*
 * NAllocNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.Type;
import soot.jimple.Expr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;

public class NAllocNode extends NObjectNode {
  private Expr e;

  public NAllocNode(Expr e) {
    this.e = e;
  }

  @Override
  public SootClass getClassType() {
    Type type = e.getType();
    if (e instanceof NewExpr) {
      return ((RefType) type).getSootClass();
    } else if (e instanceof NewArrayExpr || e instanceof NewMultiArrayExpr) {
      return Scene.v().getSootClass(type.toString());
    }
    return null;
  }

  @Override
  public String toString() {
    return "NEW[" + e + "]" + id;
  }
}
