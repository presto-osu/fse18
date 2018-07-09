/*
 * FlowAnalysis.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.analysis;

import android.graphics.Color;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.wear.watchface.graph.STG;
import edu.osu.cse.presto.gator.wear.watchface.soot.ShimpleDefs;
import edu.osu.cse.presto.gator.wear.watchface.soot.graph.FlowGraph;
import edu.osu.cse.presto.gator.wear.watchface.soot.graph.ds.*;
import edu.osu.cse.presto.gator.wear.watchface.util.ClassUtil;
import edu.osu.cse.presto.gator.wear.watchface.util.GraphUtil;
import edu.osu.cse.presto.gator.wear.watchface.util.Hierarchy;
import edu.osu.cse.presto.gator.wear.watchface.util.JimpleUtil;
import org.apache.commons.lang3.tuple.Triple;
import soot.*;
import soot.jimple.*;
import soot.shimple.PhiExpr;
import soot.shimple.ShimpleBody;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowAnalysis {
  boolean useAlias = true;
  private final Log LOG = Log.Factory.getLog(FlowAnalysis.class.getSimpleName());
  private final int MAX_NUM_RECURSIVE_CALLS = 5;
  private FlowGraph flowgraph = FlowGraph.v();
  SootMethod callee;
  Stmt callSite;
  Body callSiteBody;
  Map<Set<Triple<String, Integer, Integer>>, STG.LabelName> expectedSigs;
  Set<List<Pair<SootMethod, Stmt>>> paths;
  List<Pair<SootMethod, Stmt>> curPath;
  List<Stmt> icfg;
  Set<STG.Label> labels;
  Set<STG.Label> acqLabels;
  Set<STG.Label> relLabels;
  Map<SootMethod, Integer> visited;
  STG stg;
  SetMultimap<Value, Integer> antiAliasMap;
  SetMultimap<Value, Integer> colorMap;

  public FlowAnalysis(STG stg, SootMethod callee, Stmt callSite, Body callSiteBody,
                      Map<Set<Triple<String, Integer, Integer>>, STG.LabelName> expectedSigs,
                      Set<List<Pair<SootMethod, Stmt>>> paths, List<Pair<SootMethod, Stmt>> curPath,
                      List<Stmt> icfg, Set<STG.Label> labels, Set<STG.Label> acqLabels,
                      Set<STG.Label> relLabels, Map<SootMethod, Integer> visited,
                      SetMultimap<Value, Integer> antiAliasMap, SetMultimap<Value, Integer> colorMap) {
    this.stg = stg;
    this.callee = callee;
    this.callSite = callSite;
    this.callSiteBody = callSiteBody;
    this.expectedSigs = expectedSigs;
    this.paths = paths;
    this.curPath = curPath;
    this.icfg = icfg;
    this.labels = labels;
    this.acqLabels = acqLabels;
    this.relLabels = relLabels;
    this.visited = visited;
    this.antiAliasMap = antiAliasMap;
    this.colorMap = colorMap;
  }

  public void run() {
    if (callee == null
            || checkExistencePaintRelatedAPI(callee, callSite, antiAliasMap, colorMap)
            || checkExistenceSensorRelatedAPI(callee, callSite, callSiteBody, expectedSigs, paths, curPath, labels, acqLabels, relLabels)
            || !callee.getDeclaringClass().isApplicationClass()
            || ClassUtil.isLibraryClass(callee.getDeclaringClass())
            || (callSiteBody != null && callee.equals(callSiteBody.getMethod()))) // does not allow recursive calls
      return;

    int num = visited.getOrDefault(callee, 0);
    if (num > MAX_NUM_RECURSIVE_CALLS) {
      return;
    }
    visited.put(callee, num + 1);

    Body body;
    try {
      body = callee.retrieveActiveBody();
    } catch (Exception e) {
      return;
    }

    flowAtCall(callee, body, callSite);

    List<Stmt> slice = flowPropagation(body);

    for (Stmt s : slice) {
      icfg.add(s);
      if (s.containsInvokeExpr()) {
//        LOG.debug("STAT.CALLEE: " + s.getInvokeExpr().getMethod());
        interproceduralAnalysis(s, body);
      }
    }
  }

  void interproceduralAnalysis(Stmt s, Body body) {
    doCHA(s, curPath,
            new FlowAnalysis(stg,
                    callee,
                    s,
                    body,
                    expectedSigs,
                    paths,
                    curPath,
                    icfg,
                    labels,
                    acqLabels,
                    relLabels,
                    visited,
                    antiAliasMap,
                    colorMap));
  }

  FlowAnalysis copy(SootMethod callee, Stmt callSite, List<Pair<SootMethod, Stmt>> curPath) {
    return new FlowAnalysis(stg,
            callee,
            callSite,
            callSiteBody,
            expectedSigs,
            paths,
            curPath,
            icfg,
            labels,
            acqLabels,
            relLabels,
            visited,
            antiAliasMap,
            colorMap);
  }

  void flowAtCall(SootMethod callee, Body calleeBody, Stmt callSite) {
    if (callSite == null)
      return;
    int num_param = callee.getParameterCount();
    if (!callee.isStatic()) {
      num_param++;
    }
    Iterator<Unit> stmts = calleeBody.getUnits().iterator();
    for (int i = 0; i < num_param; i++) {
      Stmt s = (Stmt) stmts.next();
      if (!(s instanceof DefinitionStmt)) {
        continue;
      }
      InvokeExpr ie = callSite.getInvokeExpr();
      Value actual;
      if (ie instanceof InstanceInvokeExpr) {
        if (i == 0) {
          actual = JimpleUtil.v().receiver(ie); // this
        } else {
          actual = ie.getArg(i - 1);
        }
      } else {
        actual = ie.getArg(i);
      }
      Local formal = JimpleUtil.v().lhsLocal(s);
      ConstantPropagation.record(formal, actual);
      Alias.of(formal).to(actual);
    }
  }

  List<Stmt> flowPropagation(Body body) {
    UnitGraph ucfg = new ExceptionalUnitGraph(body);
    List<Stmt> slice = Lists.newArrayList();
    flowPropagation(ucfg,
            Lists.newLinkedList(ucfg.getHeads()),
            slice,
            Sets.newHashSet(),
            Maps.newHashMap());
    return slice;
  }

  void flowPropagation(UnitGraph cfg, List<Unit> worklist, List<Stmt> path,
                       Set<Stmt> visited, Map<Unit, Boolean> ifResCache) {
//    if (callee.getSignature().contains("onAmbientModeChanged"))
//      LOG.info("...");
    while (!worklist.isEmpty()) {
      Stmt curStmt = (Stmt) worklist.remove(0);
      if (visited.contains(curStmt))
        continue;
      handleAssignment(curStmt, cfg);
      path.add(curStmt);
      visited.add(curStmt);
      if (curStmt instanceof IfStmt) {
        Value cond = ((IfStmt) curStmt).getCondition();
        Integer v1;
        Integer v2;
        if (useAlias) {
          v1 = Alias.solve(((ConditionExpr) cond).getOp1());
          v2 = Alias.solve(((ConditionExpr) cond).getOp2());
        } else {
          v1 = ConstantPropagation.solve(((ConditionExpr) cond).getOp1());
          v2 = ConstantPropagation.solve(((ConditionExpr) cond).getOp2());
        }
        if (v1 == null || v2 == null) {
          ifResCache.put(curStmt, null);
          worklist.addAll(cfg.getSuccsOf(curStmt));
        } else if (cond instanceof NeExpr && v1.intValue() != v2.intValue()
                || cond instanceof EqExpr && v1.intValue() == v2.intValue()
                || cond instanceof GeExpr && v1 >= v2
                || cond instanceof GtExpr && v1 > v2
                || cond instanceof LeExpr && v1 <= v2
                || cond instanceof LtExpr && v1 < v2) { // true;
          ifResCache.put(curStmt, true);
          worklist.add(((IfStmt) curStmt).getTarget()); // target
        } else if (cond instanceof NeExpr && v1.intValue() == v2.intValue()
                || cond instanceof EqExpr && v1.intValue() != v2.intValue()
                || cond instanceof GeExpr && v1 < v2
                || cond instanceof GtExpr && v1 <= v2
                || cond instanceof LeExpr && v1 > v2
                || cond instanceof LtExpr && v1 >= v2) { // false
          ifResCache.put(curStmt, false);
          worklist.add(cfg.getSuccsOf(curStmt).get(0)); // next stmt
        } else {
          LOG.error("should not be here: stmt:%s, v1=%s, v2=%s", curStmt, v1, v2);
        }
      } else if (curStmt instanceof ReturnStmt || curStmt instanceof ReturnVoidStmt) {
        // do nothing
      } else {
        worklist.addAll(cfg.getSuccsOf(curStmt));
      }
    }
  }

  void handleAssignment(Unit u, UnitGraph cfg) {
    Stmt s = (Stmt) u;
    if (s instanceof AssignStmt) {
      Value lhs = ((AssignStmt) s).getLeftOp();
      Value rhs = ((AssignStmt) s).getRightOp();
      ConstantPropagation.record(lhs, rhs);
      if (rhs instanceof PhiExpr) {
        for (Value arg : ((PhiExpr) rhs).getValues()) {
          Alias.of(lhs).to(arg);
        }
      } else {
        Alias.of(lhs).to(rhs);
      }
    }
  }

  void addSensor(STG.LabelName label, NAllocNode listener, int obj, Stmt site,
                 Set<STG.Label> labels, Set<STG.Label> acqLabels, Set<STG.Label> relLabels) {
    STG.Label newLabel = STG.Label.get(label, listener, obj, site);
    labels.add(newLabel);
    if (STG.LabelName.isAcq(label)) {
      acqLabels.add(newLabel);
    } else {
      relLabels.add(newLabel);
    }
  }

  boolean checkExistencePaintRelatedAPI(SootMethod callee, Stmt callSite,
                                        SetMultimap<Value, Integer> antiAliasMap,
                                        SetMultimap<Value, Integer> colorMap) {
    String calleeSig = callee.getSignature();
    if (calleeSig.equals(ClassUtil.PaintAPI.SET_COLOR_SIG)) {
      InvokeExpr ie = callSite.getInvokeExpr();
      Value rcv = JimpleUtil.v().receiver(ie);
      Value colorIntVal = ie.getArg(0);
      Integer val;
      if (useAlias) {
        val = Alias.solve(colorIntVal);
      } else {
        val = ConstantPropagation.solve(colorIntVal);
      }
      if (val != null) {
        colorMap.put(rcv, val);
        LOG.debug("=================== %s(%s)", ClassUtil.PaintAPI.SET_COLOR_SIG, val);
      } else {
        boolean found = false;
        StringBuilder sb = new StringBuilder();
        NOpNode opNode = flowgraph.getOrCreateOpNode(callSite);
        for (NNode n1 : GraphUtil.backwardReachableNodes(opNode)) {
          if (n1 instanceof NPaintSetColorOpNode) {
            for (NNode n2 : GraphUtil.backwardReachableNodes(n1)) {
              if (n2 instanceof NOrExprNode) {
              } else if (n2 instanceof NIntegerConstantNode) {
                sb.append(((NIntegerConstantNode) n2).value).append(',');
                colorMap.put(rcv, ((NIntegerConstantNode) n2).value);
                found = true;
              } else if (n2 instanceof NColorParseColorOpNode) {
                for (NNode n3 : GraphUtil.backwardReachableNodes(n2)) {
                  if (n3 instanceof NStringConstantNode && ((NStringConstantNode) n3).value.length() > 0) {
                    int color = Color.parseColor(((NStringConstantNode) n3).value);
                    sb.append(color).append(',');
                    colorMap.put(rcv, color);
                    found = true;
                  }
                }
              }
            }
          }
        }
        if (!found) {
//          colorMap.put(null, null);
          LOG.debug("=================== cannot resolve %s", ClassUtil.PaintAPI.SET_COLOR_SIG);
        } else {
          LOG.debug("=================== %s([%s])",
                  ClassUtil.PaintAPI.SET_COLOR_SIG, sb.deleteCharAt(sb.length() - 1));
        }
      }
      return true;
    } else if (ClassUtil.CanvasAPI.DRAW_COLOR_SIGS.containsKey(calleeSig)) {
      InvokeExpr ie = callSite.getInvokeExpr();
      Value rcv = JimpleUtil.v().receiver(ie);
      Value colorIntVal = ie.getArg(0);
      Integer val;
      if (useAlias) {
        val = Alias.solve(colorIntVal);
      } else {
        val = ConstantPropagation.solve(colorIntVal);
      }
      if (val != null) {
        colorMap.put(rcv, val);
        LOG.info("=================== %s(%s)", ClassUtil.PaintAPI.SET_COLOR_SIG, val);
      } else {
        boolean found = false;
        StringBuilder sb = new StringBuilder();
        NOpNode opNode = flowgraph.getOrCreateOpNode(callSite);
        for (NNode n1 : GraphUtil.backwardReachableNodes(opNode)) {
          if (n1 instanceof NCanvasDrawColorOpNode) {
            for (NNode n2 : GraphUtil.backwardReachableNodes(n1)) {
              if (n2 instanceof NOrExprNode) {
                Set<Integer> res = ((NOrExprNode) n2).solve();
                if (!res.isEmpty()) {
                  colorMap.putAll(rcv, res);
                  found = true;
                }
              } else if (n2 instanceof NIntegerConstantNode) {
                sb.append(((NIntegerConstantNode) n2).value).append(',');
                colorMap.put(rcv, ((NIntegerConstantNode) n2).value);
                found = true;
              } else if (n2 instanceof NColorParseColorOpNode) {
                for (NNode n3 : GraphUtil.backwardReachableNodes(n2)) {
                  if (n3 instanceof NStringConstantNode) {
                    int color = Color.parseColor(((NStringConstantNode) n3).value);
                    sb.append(color).append(',');
                    colorMap.put(rcv, color);
                    found = true;
                  }
                }
              }
            }
          }
        }
        if (!found) {
//          colorMap.put(null, null);
          LOG.info("=================== cannot resolve %s", ClassUtil.PaintAPI.SET_COLOR_SIG);
        } else {
          LOG.info("=================== %s([%s])",
                  ClassUtil.PaintAPI.SET_COLOR_SIG, sb.deleteCharAt(sb.length() - 1));
        }
      }
      return true;
    }
    return false;
  }

  boolean checkExistenceSensorRelatedAPI(SootMethod callee, Stmt callSite, Body callSiteBody,
                                         Map<Set<Triple<String, Integer, Integer>>, STG.LabelName> expectedSigs,
                                         Set<List<Pair<SootMethod, Stmt>>> paths, List<Pair<SootMethod, Stmt>> curPath,
                                         Set<STG.Label> labels, Set<STG.Label> acqLabels, Set<STG.Label> relLabels) {
    for (Set<Triple<String, Integer, Integer>> mtds : expectedSigs.keySet()) {
      STG.LabelName label = expectedSigs.get(mtds);
      for (Triple<String, Integer, Integer> mtd : mtds) {
        if (callee.getSignature().equals(mtd.getLeft())) {
          paths.add(Lists.newArrayList(curPath));
          InvokeExpr ie = callSite.getInvokeExpr();
          if (mtd.getMiddle() == -1) continue;
          for (NNode listener : GraphUtil.backwardReachableNodes(flowgraph.getOrCreateOpNode(callSite))) {
            if (listener instanceof NAllocNode) {
              if (mtd.getRight() == -1) { // no obj, just add -1
                addSensor(label, (NAllocNode) listener, -1, callSite,
                        labels, acqLabels, relLabels);
                LOG.debug("------ sensor: %s", -1);
              } else {
                Value obj = ie.getArg(mtd.getRight()); // obj
                if (!(obj instanceof Local)) {
                  LOG.error("sensor object in registration is not local at " + callSite);
                }
                ShimpleDefs useDef = new ShimpleDefs((ShimpleBody) callSiteBody);
                Value tmp = obj;
                while (true) {
                  List<Unit> defs;
                  try {
                    defs = useDef.getDefsOf(tmp);
                  } catch (ShimpleDefs.DefNotFoundException e) {
                    LOG.debug("Sensor is not obtained locally, fall back to flow graph");
                    NOpNode opNode = flowgraph.getOrCreateOpNode(callSite);
                    for (NNode n1 : GraphUtil.backwardReachableNodes(opNode)) {
                      if (n1 instanceof NSensorGetSensorOpNode) {
                        for (NNode n2 : GraphUtil.backwardReachableNodes(n1)) {
                          if (n2 instanceof NIntegerConstantNode) {
                            addSensor(label, (NAllocNode) listener, ((NIntegerConstantNode) n2).value, callSite,
                                    labels, acqLabels, relLabels);
                          }
                        }
                      }
                    }
                    break;
                  }
                  if (defs.size() != 1) {
                    // should not happen as we're using SSA
                    LOG.error("more than one def " + callSite);
                  }
                  Stmt stmt = (Stmt) defs.get(0);
                  LOG.debug("------ def: %s", stmt);
                  if (stmt.containsInvokeExpr()) {
                    InvokeExpr i = stmt.getInvokeExpr();
                    SootMethod m = i.getMethod();
                    Integer objIdx = ClassUtil.SensorManagerAPI.GET_SENSOR_SIGS.get(m.getSignature());
                    if (obj == null || objIdx == null) {
                      LOG.error("unexpected invocation in use-def chain");
                    }
                    Value sensorArg = i.getArg(objIdx);
                    if (sensorArg instanceof IntConstant) {
                      addSensor(label, (NAllocNode) listener, ((IntConstant) sensorArg).value, callSite,
                              labels, acqLabels, relLabels);
                      LOG.debug("------ sensor: %s", sensorArg);
                    } else {
                      LOG.warn("------ getDefaultSensor param not int const: %s", sensorArg);
                      // TODO: Should use constant propagation
                      addSensor(label, (NAllocNode) listener, -1, callSite,
                              labels, acqLabels, relLabels);
                      LOG.debug("------ sensor: %s", -1);
                    }
                    break;
                  } else if (stmt instanceof DefinitionStmt) {
                    tmp = ((DefinitionStmt) stmt).getRightOp();
                  } else {
                    LOG.error("Should not be here.");
                  }
                }
              }
            }
          }
          return true;
        }
      }
    }
    return false;
  }

  void doCHA(Stmt s, List<Pair<SootMethod, Stmt>> curPath, FlowAnalysis analysis) {
    InvokeExpr ie = s.getInvokeExpr();
    SootMethod staticTgt;
    try {
      staticTgt = ie.getMethod(); // static target
    } catch (Exception e) {
      return;
    }

    // flow graph edges at non-virtual calls
    if (ie instanceof StaticInvokeExpr
            || ie instanceof SpecialInvokeExpr) {
      if (staticTgt.getDeclaringClass().isApplicationClass()) {
        curPath.add(new Pair<>(staticTgt, s));
        analysis.copy(staticTgt, s, curPath).run();
        curPath.remove(curPath.size() - 1);
      }
      return;
    }

    // sensor APIs
    for (Set<Triple<String, Integer, Integer>> mtdSet : analysis.expectedSigs.keySet()) {
      for (Triple<String, Integer, Integer> mtd : mtdSet) {
        if (staticTgt.getSignature().equals(mtd.getLeft())) {
          curPath.add(new Pair<>(staticTgt, s));
          analysis.copy(staticTgt, s, curPath).run();
          curPath.remove(curPath.size() - 1);
          return;
        }
      }
    }

    // paint APIs
    if (staticTgt.getSignature().equals(ClassUtil.PaintAPI.SET_ANTI_ALIAS_SIG)
            || staticTgt.getSignature().equals(ClassUtil.PaintAPI.SET_COLOR_SIG)
            || staticTgt.getSignature().equals(ClassUtil.PaintAPI.SET_COLOR_FILTER_SIG)
            || ClassUtil.CanvasAPI.DRAW_COLOR_SIGS.containsKey(staticTgt.getSignature())) {
      curPath.add(new Pair<>(staticTgt, s));
      analysis.copy(staticTgt, s, curPath).run();
      curPath.remove(curPath.size() - 1);
      return;
    }

    Local rcv_var = JimpleUtil.v().receiver(ie);
    Type rcv_t = rcv_var.getType();
    // could be ArrayType, for clone() calls
    if (!(rcv_t instanceof RefType)) {
      return;
    }
    SootClass rcv_cls = ((RefType) rcv_t).getSootClass();
    Hierarchy.v().CHA(staticTgt, rcv_cls, target -> {
      curPath.add(new Pair<>(target, s));
      analysis.copy(target, s, curPath).run();
      curPath.remove(curPath.size() - 1);
    });
  }
}
