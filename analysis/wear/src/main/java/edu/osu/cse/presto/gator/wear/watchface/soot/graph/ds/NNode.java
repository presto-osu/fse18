/*
 * NNode.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds;

import com.google.common.collect.Lists;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public abstract class NNode {
  public static boolean verbose = false;
  public static int nextId = 0;
  public static int numberOfEdges = 0;
  public int id;

  public NNode() {
    nextId++;
    id = nextId;
  }

  // NOTE(tony): "alias" nodes/paths
  protected ArrayList<NNode> succ;
  protected ArrayList<NNode> pred;

  public synchronized Collection<NNode> getSuccessors() {
    if (succ == null || succ.isEmpty()) {
      return Collections.emptyList();
    } else {
      return Lists.newArrayList(succ);
    }
  }

  public synchronized int getNumberOfSuccessors() {
    return (succ == null ? 0 : succ.size());
  }

  public synchronized NNode getSuccessor(int index) {
    return succ.get(index);
  }

  public synchronized Collection<NNode> getPredecessors() {
    if (pred == null || pred.isEmpty()) {
      return Collections.emptyList();
    } else {
      return Lists.newArrayList(pred);
    }
  }

  public synchronized int getNumberOfPredecessors() {
    return (pred == null ? 0 : pred.size());
  }

  public synchronized NNode getPredecessor(int index) {
    return pred.get(index);
  }

  public synchronized void removeEdgeTo(NNode target) {
    if (succ != null && succ.contains(target)) {
      succ.remove(target);
      numberOfEdges--;
    } else {
      if (target.pred != null && target.pred.contains(this)) {
        throw new RuntimeException("Broken edge " + this + "===>" + target);
      }
      return;
    }
    if (target.pred == null || !target.pred.contains(this)) {
      throw new RuntimeException("Broken edge " + this + "===>" + target);
    }
    target.pred.remove(this);
  }

  public synchronized void addEdgeTo(NNode x) {
    addEdgeTo(x, null);
  }

  public synchronized void addEdgeTo(NNode x, Stmt s) {
    if (succ == null) {
      succ = Lists.newArrayListWithCapacity(4);
    }
    if (!succ.contains(x)) {
      succ.add(x);
      numberOfEdges++;
    } else {
      return;
    }
    // predecessors
    if (x.pred == null) {
      x.pred = Lists.newArrayListWithCapacity(4);
    }
    if (x.pred.contains(this)) {
      throw new RuntimeException();
    }
    x.pred.add(this);
  }
}
