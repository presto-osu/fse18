/*
 * FlowAnalysisWithParam.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot.analysis;

import com.google.common.collect.SetMultimap;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.wear.watchface.graph.STG;
import edu.osu.cse.presto.gator.wear.watchface.util.ClassUtil;
import org.apache.commons.lang3.tuple.Triple;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowAnalysisWithParam extends FlowAnalysis {
  private final Log LOG = Log.Factory.getLog(FlowAnalysisWithParam.class.getSimpleName());
  private boolean param;
  private int paramValue;

  public FlowAnalysisWithParam(STG stg, SootMethod callee, Stmt callSite, Body callSiteBody,
                               Map<Set<Triple<String, Integer, Integer>>, STG.LabelName> expectedSigs,
                               Set<List<Pair<SootMethod, Stmt>>> paths, List<Pair<SootMethod, Stmt>> curPath,
                               List<Stmt> icfg, Set<STG.Label> labels, Set<STG.Label> acqLabels,
                               Set<STG.Label> relLabels, Map<SootMethod, Integer> visited,
                               SetMultimap<Value, Integer> antiAliasMap, SetMultimap<Value, Integer> colorMap,
                               boolean param) {
    super(stg, callee, callSite, callSiteBody, expectedSigs, paths, curPath,
            icfg, labels, acqLabels, relLabels, visited, antiAliasMap, colorMap);
    this.param = param;
    if (param)
      paramValue = 1;
    else
      paramValue = 0;

    if (callSite == null) { // onAmbientModeChanged or onVisibilityChanged
      Body body;
      try {
        body = callee.retrieveActiveBody();
      } catch (Exception e) {
        return;
      }
      for (Unit u : body.getUnits()) {
        Stmt s = (Stmt) u;
        if (s instanceof IdentityStmt) {
          Value rhs = ((IdentityStmt) s).getRightOp();
          if (rhs instanceof ParameterRef) { // skip THIS
            Value lhs = ((IdentityStmt) s).getLeftOp();
            ConstantPropagation.record(lhs, IntConstant.v(paramValue));
            Alias.of(lhs).onlyTo(IntConstant.v(paramValue));
            break; // as we only have one boolean parameter
          }
        }
      }
    }
  }

  @Override
  void interproceduralAnalysis(Stmt s, Body body) {
    doCHA(s, curPath,
            new FlowAnalysisWithParam(stg,
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
                    colorMap,
                    param));
  }

  @Override
  FlowAnalysis copy(SootMethod callee, Stmt callSite, List<Pair<SootMethod, Stmt>> curPath) {
    return new FlowAnalysisWithParam(stg,
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
            colorMap,
            param);
  }

  @Override
  void handleAssignment(Unit u, UnitGraph cfg) {
    super.handleAssignment(u, cfg);
    Stmt s = (Stmt) u;
    if (s instanceof AssignStmt) {
      Value lhs = ((AssignStmt) s).getLeftOp();
      Value rhs = ((AssignStmt) s).getRightOp();
      if (rhs instanceof InvokeExpr) {
        SootMethod tgt = ((InvokeExpr) rhs).getMethod();
        if (tgt.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.ENGINE_ISINAMBIENTMODE_MTD_SUBSIG)
                || tgt.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.ENGINE_ISVISIBLE_MTD_SUBSIG)) {
          ConstantPropagation.record(lhs, IntConstant.v(paramValue));
          Alias.of(lhs).to(IntConstant.v(paramValue));
        }
      }
    }
  }
}
