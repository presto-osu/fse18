/*
 * NOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.Map;
import java.util.Set;

/**
 * Each NOpNode corresponds one special API call that we want to simulate.
 * Typically, one such API call is invoking framework method that we want to
 * avoid analyzing. In terms of OO design, it seems better to do it with
 * inheritance. But as long as it is not too painful and error-prone, we would
 * like to keep the current implementation.
 */
public abstract class NOpNode extends NNode {
  private static Map<String, Set<NOpNode>> opNodes = Maps.newHashMap();
  private static Map<Stmt, NOpNode> stmtAndNodes = Maps.newHashMap();

  public final static NOpNode NullNode = new NOpNode(true) {
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
  };

  private String opType;
  public Pair<Stmt, SootMethod> callSite;

  public Type receiverType;
  public Type parameterType;

  // Some operation nodes are added to model certain features of the framework.
  // These nodes do not correspond directly to statements in the application,
  // and we would like to annotate them and thus have the ability the exclude
  // them in certain measurements. To make it maintainable (i.e., force
  // ourselves to look at each and every creation of operation nodes), we make
  // it a parameter of the constructor.
  public boolean artificial;

  private NOpNode(boolean artificial) {
    this.artificial = artificial;
  }

  @VisibleForTesting
  public NOpNode(Pair<Stmt, SootMethod> callSite, boolean artificial) {
    this(artificial);
    this.opType = this.getClass().getSimpleName();
    this.callSite = callSite;
    Set<NOpNode> nodes = opNodes.get(opType);
    if (nodes == null) {
      nodes = Sets.newHashSet();
      opNodes.put(opType, nodes);
    }
    nodes.add(this);
    if (callSite != null) {
      stmtAndNodes.put(callSite.getO1(), this);
    }
  }

  public String shortDescription() {
    return opType + "[" + id + "]";
  }

  public String getOpType() {
    return opType;
  }

  @Override
  public String toString() {
    return opType + "[" + (callSite == null ? "<call site unknown>" : callSite.getO1() + " @ "
            + callSite.getO2() + "]" + id);
  }

  public static Set<NOpNode> getNodes(Class<? extends NOpNode> klass) {
    String type = klass.getSimpleName();
    Set<NOpNode> result = opNodes.get(type);
    if (result == null) {
      result = Sets.newHashSet();
    }
    return result;
  }

  public static NOpNode lookupByStmt(Stmt s) {
    return stmtAndNodes.get(s);
  }

  public boolean consumesLayoutId() {
    return false;
  }

  public boolean consumesMenuId() {
    return false;
  }

  // Default stubs
  public NVarNode getReceiver() {
    throw new RuntimeException("Fail to get receiver for " + this.toString());
  }

  public abstract boolean hasReceiver();

  public NNode getParameter() {
    throw new RuntimeException("Fail to get parameter for " + this.toString());
  }

  public abstract boolean hasParameter();

  public NVarNode getLhs() {
    throw new RuntimeException("Fail to get lhs for " + this.toString());
  }

  public abstract boolean hasLhs();
}

