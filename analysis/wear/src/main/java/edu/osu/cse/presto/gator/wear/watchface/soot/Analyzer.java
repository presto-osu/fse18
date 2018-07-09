/*
 * Analyzer.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot;

import com.google.common.collect.*;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.wear.watchface.graph.STG;
import edu.osu.cse.presto.gator.wear.watchface.soot.analysis.Alias;
import edu.osu.cse.presto.gator.wear.watchface.soot.analysis.ConstantPropagation;
import edu.osu.cse.presto.gator.wear.watchface.soot.analysis.FlowAnalysis;
import edu.osu.cse.presto.gator.wear.watchface.soot.analysis.FlowAnalysisWithParam;
import edu.osu.cse.presto.gator.wear.watchface.util.ClassUtil;
import edu.osu.cse.presto.gator.wear.watchface.util.ICFGUtil;
import org.apache.commons.lang3.tuple.Triple;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Analyzer {
  private final Log LOG = Log.Factory.getLog(Analyzer.class.getSimpleName());
  private static Analyzer instance;

  public static Analyzer v() {
    if (instance == null)
      instance = new Analyzer();
    return instance;
  }

  // <mtd, label>
  private Map<Set<Triple<String, Integer, Integer>>, STG.LabelName> METHOD_TO_LABEL = Maps.newHashMap();

  {
    METHOD_TO_LABEL.put(Sets.newHashSet(ClassUtil.SensorAPI.REGISTER_LISTENER_SIGS.values()),
            STG.LabelName.SensorLabelName.register_sensor_listener);
    METHOD_TO_LABEL.put(Sets.newHashSet(ClassUtil.SensorAPI.UNREGISTER_LISTENER_SIGS.values()),
            STG.LabelName.SensorLabelName.unregister_sensor_listener);
//    METHOD_TO_LABEL.put(Sets.newHashSet(ClassUtil.LocationAPI.REQUEST_UPDATES_SIGS.values()),
//            STG.LabelName.LocationLabelName.request_location_listener);
//    METHOD_TO_LABEL.put(Sets.newHashSet(ClassUtil.LocationAPI.REMOVE_UPDATES_SIGS.values()),
//            STG.LabelName.LocationLabelName.remove_location_listener);
  }

  public Map<STG.Callback, SootMethod> getCorrespondingCallbackMethods(SootClass clz) {
    Map<STG.Callback, SootMethod> cb = new HashMap<>();
    for (SootMethod mtd : clz.getMethods()) {
      if (mtd.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.ENGINE_ONCREATE_MTD_SUBSIG)) {
        if (cb.containsKey(STG.ON_CREATE))
          LOG.warn("------ duplicate method for %s", ClassUtil.WatchFaceNonObfAPI.ENGINE_ONCREATE_MTD_SUBSIG);
        cb.put(STG.ON_CREATE, mtd);
      } else if (mtd.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.ENGINE_ONDESTROY_MTD_SUBSIG)) {
        if (cb.containsKey(STG.ON_DESTROY))
          LOG.warn("------ duplicate method for %s", ClassUtil.WatchFaceNonObfAPI.ENGINE_ONDESTROY_MTD_SUBSIG);
        cb.put(STG.ON_DESTROY, mtd);
      } else if (mtd.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.ENGINE_ONAMBIENTMODECHANGED_MTD_SUBSIG)) {
        if (cb.containsKey(STG.ON_AMBIENT_MODE_CHANGED_TRUE)
                || cb.containsKey(STG.ON_AMBIENT_MODE_CHANGED_FALSE))
          LOG.warn("------ duplicate method for %s", ClassUtil.WatchFaceNonObfAPI.ENGINE_ONAMBIENTMODECHANGED_MTD_SUBSIG);
        cb.put(STG.ON_AMBIENT_MODE_CHANGED_TRUE, mtd);
        cb.put(STG.ON_AMBIENT_MODE_CHANGED_FALSE, mtd);
      } else if (mtd.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.ENGINE_ONVISIBILITYCHANGED_MTD_SUBSIG)) {
        if (cb.containsKey(STG.ON_VISIBILITY_CHANGED_TRUE)
                || cb.containsKey(STG.ON_VISIBILITY_CHANGED_FALSE))
          LOG.warn("------ duplicate method for %s", ClassUtil.WatchFaceNonObfAPI.ENGINE_ONVISIBILITYCHANGED_MTD_SUBSIG);
        cb.put(STG.ON_VISIBILITY_CHANGED_TRUE, mtd);
        cb.put(STG.ON_VISIBILITY_CHANGED_FALSE, mtd);
      } else if (mtd.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.ENGINE_ONDRAW_MTD_SUBSIG)) {
        if (cb.containsKey(STG.ON_DRAW))
          LOG.warn("------ duplicate method for %s", ClassUtil.WatchFaceNonObfAPI.ENGINE_ONDRAW_MTD_SUBSIG);
        cb.put(STG.ON_DRAW, mtd);
      }
    }

    final String ON_CREATE_ENGINE_MTD_SUBSIG = String.format(
            ClassUtil.WatchFaceNonObfAPI.SERVICE_ONCREATEENGINE_MTD_SUBSIG_FMT, clz.getName());
    for (SootClass wfservice : Scene.v().getApplicationClasses()) {
      if (ClassUtil.isWatchFaceService(wfservice)) {
        if (wfservice.getMethodUnsafe(ON_CREATE_ENGINE_MTD_SUBSIG) != null) {
          for (SootMethod mtd : wfservice.getMethods()) {
            if (mtd.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.SERVICE_ONCREATE_MTD_SUBSIG)) {
              if (cb.containsKey(STG.WF_ON_CREATE))
                LOG.warn("------ duplicate method for %s", ClassUtil.WatchFaceNonObfAPI.SERVICE_ONCREATE_MTD_SUBSIG);
              cb.put(STG.WF_ON_CREATE, mtd);
            } else if (mtd.getSubSignature().equals(ClassUtil.WatchFaceNonObfAPI.SERVICE_ONDESTROY_MTD_SUBSIG)) {
              if (cb.containsKey(STG.WF_ON_DESTROY))
                LOG.warn("------ duplicate method for %s: %s => %s",
                        STG.WF_ON_DESTROY,
                        mtd,
                        cb.get(STG.WF_ON_DESTROY));
              cb.put(STG.WF_ON_DESTROY, mtd);
            } else if (mtd.getSubSignature().equals(ON_CREATE_ENGINE_MTD_SUBSIG)) {
              if (cb.containsKey(STG.WF_ON_CREATE_ENGINE))
                LOG.warn("------ duplicate method for %s", ON_CREATE_ENGINE_MTD_SUBSIG);
              cb.put(STG.WF_ON_CREATE_ENGINE, mtd);
            }
          }
        }
      }
    }
    return cb;
  }


  public Map<SootClass, SootMethod> getListenerCallbacks() {
    Map<SootClass, SootMethod> cb = Maps.newHashMap();
    for (SootClass clz : Scene.v().getApplicationClasses()) {
      for (SootMethod mtd : clz.getMethods()) {
//        if (mtd.getSubSignature().equals(ClassUtil.SensorEventListenerAPI.ON_ACCURACY_CHANGED_SUBSIG)) {
//          cb.put(clz, mtd);
//        } else
        if (mtd.getSubSignature().equals(ClassUtil.SensorEventListenerAPI.ON_SENSOR_CHANGED_SUBSIG)) {
          cb.put(clz, mtd);
        }
      }
    }
    return cb;
  }

  public void lookupWithParam(STG stg, STG.Callback cb, SootMethod inMethod,
                              boolean param, boolean callOnDraw,
                              SetMultimap<STG.Callback, STG.Label> labelMap,
                              SetMultimap<STG.Callback, STG.Label> acqLabelMap,
                              SetMultimap<STG.Callback, STG.Label> relLabelMap,
                              SetMultimap<STG.Callback, SetMultimap<Value, Integer>> antiAliasMap,
                              SetMultimap<STG.Callback, SetMultimap<Value, Integer>> colorMap,
                              boolean retainAlias) {
    LOG.info("=============== %s ===============", cb);
    Set<List<Pair<SootMethod, Stmt>>> paths = Sets.newHashSet();
    Set<STG.Label> labels = Sets.newHashSet();
    Set<STG.Label> acqLabels = Sets.newHashSet();
    Set<STG.Label> relLabels = Sets.newHashSet();
    SetMultimap<Value, Integer> antiAlias = HashMultimap.create();
    SetMultimap<Value, Integer> color = HashMultimap.create();
    List<Stmt> icfg = Lists.newArrayList();
    if (!retainAlias) {
      Alias.clearContext();
      ConstantPropagation.clearContext();
    }
    FlowAnalysisWithParam analysis =
            new FlowAnalysisWithParam(stg, inMethod,
                    null,
                    null,
                    METHOD_TO_LABEL,
                    paths,
                    Lists.newArrayList(),
                    icfg,
                    labels,
                    acqLabels,
                    relLabels,
                    Maps.newHashMap(),
                    antiAlias,
                    color,
                    param);
    analysis.run();
    if (callOnDraw) {
      analysis = new FlowAnalysisWithParam(stg, stg.getConcreteCallback(STG.ON_DRAW),
              null,
              null,
              METHOD_TO_LABEL,
              paths,
              Lists.newArrayList(),
              icfg,
              labels,
              acqLabels,
              relLabels,
              Maps.newHashMap(),
              antiAlias,
              color,
              param);
      analysis.run();
    }
    antiAliasMap.put(cb, antiAlias);
    colorMap.put(cb, color);
    if (paths.size() > 0) {
      ICFGUtil.refine(icfg, inMethod, labelMap, acqLabelMap, relLabelMap, paths, cb, labels, acqLabels, relLabels);
    }
  }

  public void lookup(STG stg, STG.Callback cb, SootMethod inMethod,
                     SetMultimap<STG.Callback, STG.Label> labelMap,
                     SetMultimap<STG.Callback, STG.Label> acqLabelMap,
                     SetMultimap<STG.Callback, STG.Label> relLabelMap,
                     SetMultimap<STG.Callback, SetMultimap<Value, Integer>> antiAliasMap,
                     SetMultimap<STG.Callback, SetMultimap<Value, Integer>> colorMap,
                     boolean retainAlias) {
    LOG.info("=============== %s ===============", cb);
    Set<List<Pair<SootMethod, Stmt>>> paths = Sets.newHashSet();
    List<Stmt> icfg = Lists.newArrayList();
    Set<STG.Label> labels = Sets.newHashSet();
    Set<STG.Label> acqLabels = Sets.newHashSet();
    Set<STG.Label> relLabels = Sets.newHashSet();
    SetMultimap<Value, Integer> antiAlias = HashMultimap.create();
    SetMultimap<Value, Integer> color = HashMultimap.create();
    if (!retainAlias) {
      Alias.clearContext();
      ConstantPropagation.clearContext();
    }
    FlowAnalysis analysis =
            new FlowAnalysis(stg, inMethod,
                    null,
                    null,
                    METHOD_TO_LABEL,
                    paths,
                    Lists.newArrayList(),
                    icfg,
                    labels,
                    acqLabels,
                    relLabels,
                    Maps.newHashMap(),
                    antiAlias,
                    color);
    analysis.run();
    antiAliasMap.put(cb, antiAlias);
    colorMap.put(cb, color);
    if (paths.size() > 0) {
      ICFGUtil.refine(icfg, inMethod, labelMap, acqLabelMap, relLabelMap, paths, cb, labels, acqLabels, relLabels);
    }
  }
}
