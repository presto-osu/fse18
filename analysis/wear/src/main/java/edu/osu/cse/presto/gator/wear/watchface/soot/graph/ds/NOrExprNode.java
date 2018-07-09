/*
 * NOrExprNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import com.google.common.collect.Sets;
import edu.osu.cse.presto.gator.wear.watchface.util.GraphUtil;
import soot.jimple.Expr;

import java.util.Set;

public class NOrExprNode extends NNode {

  Expr expr;
  NNode op1;
  NNode op2;

  public NOrExprNode(Expr expr, NNode op1, NNode op2) {
    op1.addEdgeTo(this);
    op2.addEdgeTo(this);
    this.expr = expr;
    this.op1 = op1;
    this.op2 = op2;
  }

  public Set<Integer> solve() {
    Set<Integer> s1 = Sets.newHashSet(), s2 = Sets.newHashSet();
    for (NNode p : GraphUtil.backwardReachableNodes(op1)) {
      if (p instanceof NIntegerConstantNode) {
        s1.add(((NIntegerConstantNode) p).value);
      }
    }
    for (NNode p : GraphUtil.backwardReachableNodes(op2)) {
      if (p instanceof NIntegerConstantNode) {
        s2.add(((NIntegerConstantNode) p).value);
      }
    }
    Set<Integer> res = Sets.newHashSet();
    for (int v1 : s1) {
      for (int v2 : s2) {
        res.add(v1 | v2);
      }
    }
    return res;
  }
}
