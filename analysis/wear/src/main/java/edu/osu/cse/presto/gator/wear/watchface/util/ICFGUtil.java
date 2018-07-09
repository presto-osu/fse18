/*
 * ICFGUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.util;

import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.wear.watchface.graph.STG;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;

import java.util.List;
import java.util.Set;

public class ICFGUtil {
  static Log LOG = Log.Factory.getLog("ICFGUtil");

  public static void refine(List<Stmt> icfg, SootMethod inMethod,
                            SetMultimap<STG.Callback, STG.Label> labelMap,
                            SetMultimap<STG.Callback, STG.Label> acqLabelMap,
                            SetMultimap<STG.Callback, STG.Label> relLabelMap,
                            Set<List<Pair<SootMethod, Stmt>>> paths, STG.Callback cb,
                            Set<STG.Label> labels,
                            Set<STG.Label> acqLabels,
                            Set<STG.Label> relLabels) {
    Set<Stmt> visited = Sets.newHashSet();
    Set<Stmt> icfgVisited = Sets.newHashSet(icfg);
    Body body = inMethod.getActiveBody();
    UnitGraph ucfg = new ExceptionalUnitGraph(body);
    // TODO: 1. remove acq not downward-exposed
    Set<STG.Label> acqToRemove = Sets.newHashSet();
    for (STG.Label acqLabel : acqLabels) {
      List<Unit> worklist = Lists.newArrayList(ucfg.getTails());
      boolean[] res = new boolean[]{false};
      while (!worklist.isEmpty()) {
        Stmt curStmt = (Stmt) worklist.remove(0);
        if (visited.contains(curStmt) || !icfgVisited.contains(curStmt))
          continue;
        visited.add(curStmt);
        if (acqLabel.site.equals(curStmt)) {
          res[0] = true;
          break; // meet the acq, stop traversal
        } else if (curStmt instanceof InvokeStmt) {
          boolean goon = true;
          for (STG.Label rel : relLabels) {
            if (rel.site.equals(curStmt)) {
              if (rel.listener.equals(acqLabel.listener)
                      && (rel.obj == -1 || rel.obj == acqLabel.obj)) {
                goon = false;
              }
            }
          }
          if (goon) {
            interproceduralAnalysis(curStmt, visited, icfgVisited, acqLabel, relLabels, res, acqAnalysis);
            if (!res[0])
              worklist.addAll(ucfg.getPredsOf(curStmt));
          }
        } else {
          worklist.addAll(ucfg.getSuccsOf(curStmt));
        }
      }
      if (!res[0]) {
        acqToRemove.add(acqLabel);
      }
    }
    // acqLabels.removeAll(acqToRemove);
    // labels.removeAll(acqToRemove);
    // end 1

    // 2. remove rel that are not guaranteed to flow to exit
    Set<STG.Label> relToRemove = Sets.newHashSet();
    for (STG.Label relLabel : relLabels) {
      List<Unit> worklist = Lists.newArrayList(ucfg.getHeads());
      while (!worklist.isEmpty()) {
        Stmt curStmt = (Stmt) worklist.remove(0);
        if (visited.contains(curStmt) || !icfgVisited.contains(curStmt))
          continue;
        visited.add(curStmt);
        if (relLabel.site.equals(curStmt)) {
          // do nothing
        } else if (curStmt instanceof InvokeStmt) {
          interproceduralAnalysis(curStmt, visited, icfgVisited, relLabel, null, null, relAnalysis);
          worklist.addAll(ucfg.getPredsOf(curStmt));
        } else if (curStmt instanceof ReturnStmt || curStmt instanceof ReturnVoidStmt) {
          relToRemove.add(relLabel); // reach the exit, exist path avoid rel
        } else {
          worklist.addAll(ucfg.getSuccsOf(curStmt));
        }
      }
    }
    relLabels.removeAll(relToRemove);
    labels.removeAll(relToRemove);
    // end 2

    labelMap.putAll(cb, labels);
    acqLabelMap.putAll(cb, acqLabels);
    relLabelMap.putAll(cb, relLabels);
    LOG.info("[acq labels] %s", acqLabels);
    LOG.info("[rel labels] %s", relLabels);
    LOG.info("[all labels] %s", labels);
    for (List<Pair<SootMethod, Stmt>> list : paths) {
      StringBuilder sb = new StringBuilder();
      for (Pair<SootMethod, Stmt> p : list) {
        sb.append(p.getO1()).append(',');
      }
//      LOG.info("[found] In \"%s\" via \"[%s]\"", inMethod, sb);
    }
  }

  abstract static class Analysis {
    abstract void analyze(SootMethod mtd, Set<Stmt> visited, Set<Stmt> icfgVisited, STG.Label curLabel, Set<STG.Label> labels, boolean[] boolResult);
  }

  static Analysis acqAnalysis = new Analysis() {
    @Override
    void analyze(SootMethod mtd, Set<Stmt> visited, Set<Stmt> icfgVisited, STG.Label acqLabel, Set<STG.Label> relLabels, boolean[] boolResult) {
      Body body = mtd.getActiveBody();
      UnitGraph ucfg = new ExceptionalUnitGraph(body);
      List<Unit> worklist = Lists.newArrayList(ucfg.getTails());
      while (!worklist.isEmpty()) {
        Stmt curStmt = (Stmt) worklist.remove(0);
        if (visited.contains(curStmt) || !icfgVisited.contains(curStmt))
          continue;
        visited.add(curStmt);
        if (acqLabel.site.equals(curStmt)) {
          boolResult[0] = true;
          break; // meet the acq, stop traversal
        } else if (curStmt instanceof InvokeStmt) {
          boolean goon = true;
          for (STG.Label rel : relLabels) {
            if (rel.site.equals(curStmt)) {
              if (rel.listener == acqLabel.listener
                      && (rel.obj == -1 || rel.obj == acqLabel.obj)) {
                goon = false;
              }
            }
          }
          if (goon) {
            interproceduralAnalysis(curStmt, visited, icfgVisited, acqLabel, relLabels, boolResult, this);
            if (!boolResult[0])
              worklist.addAll(ucfg.getPredsOf(curStmt));
          }
        } else {
          worklist.addAll(ucfg.getPredsOf(curStmt));
        }
      }
    }
  };

  static Analysis relAnalysis = new Analysis() {
    @Override
    void analyze(SootMethod mtd, Set<Stmt> visited, Set<Stmt> icfgVisited, STG.Label relLabel, Set<STG.Label> labels, boolean[] boolResult) {
      Body body = mtd.getActiveBody();
      UnitGraph ucfg = new ExceptionalUnitGraph(body);
      List<Unit> worklist = Lists.newArrayList(ucfg.getHeads());
      while (!worklist.isEmpty()) {
        Stmt curStmt = (Stmt) worklist.remove(0);
        if (visited.contains(curStmt) || !icfgVisited.contains(curStmt))
          continue;
        visited.add(curStmt);
        if (relLabel.site.equals(curStmt)) {
          // do nothing
        } else if (curStmt instanceof InvokeStmt) {
          interproceduralAnalysis(curStmt, visited, icfgVisited, relLabel, labels, boolResult, this);
          worklist.addAll(ucfg.getSuccsOf(curStmt));
        } else {
          worklist.addAll(ucfg.getSuccsOf(curStmt));
        }
      }
    }
  };


  private static void interproceduralAnalysis(Stmt curStmt, Set<Stmt> visited, Set<Stmt> icfgVisited, STG.Label curLabel, Set<STG.Label> labels, boolean[] boolResult, Analysis analysis) {
    InvokeExpr ie = curStmt.getInvokeExpr();
    SootMethod staticTgt;
    try {
      staticTgt = ie.getMethod(); // static target
    } catch (Exception e) {
      return;
    }
    if (ie instanceof StaticInvokeExpr
            || ie instanceof SpecialInvokeExpr) {
      if (staticTgt.getDeclaringClass().isApplicationClass()) {
        analysis.analyze(staticTgt, visited, icfgVisited, curLabel, labels, boolResult);
      }
      return;
    }

    Local rcv_var = JimpleUtil.v().receiver(ie);
    Type rcv_t = rcv_var.getType();
    // could be ArrayType, for clone() calls
    if (!(rcv_t instanceof RefType)) {
      return;
    }
    SootClass rcv_cls = ((RefType) rcv_t).getSootClass();
    Hierarchy.v().CHA(staticTgt, rcv_cls, target -> analysis.analyze(target, visited, icfgVisited, curLabel, labels, boolResult));
  }
}
