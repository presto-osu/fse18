/*
 * MySceneTransformer.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.instrument.watchface;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.instrument.watchface.runtime.Tracker;
import edu.osu.cse.presto.gator.instrument.watchface.util.ApkUtil;
import edu.osu.cse.presto.gator.instrument.watchface.util.JimpleUtil;
import soot.*;
import soot.jimple.IdentityStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MySceneTransformer extends SceneTransformer {
    private final Log LOG = Log.Factory.getLog(MySceneTransformer.class.getName());
    private List<String> wfServices;
    private List<SootClass> watchfaceServiceClasses;
    private String apkPath;
    // core classes
    private SootClass CANVAS_WATCH_FACE_SERVICE_CLZ;
    private SootClass GLES2_WATCH_FACE_SERVICE_CLZ;
    private SootClass CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ;
    private SootClass GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ;
    // for logging the instrumented methods
    private final Set<SootMethod> hasInstr = Sets.newHashSet();

    MySceneTransformer(String apkPath) {
        this.apkPath = apkPath;
        wfServices = ApkUtil.v().getWatchFaceServices(apkPath);
        if (wfServices.isEmpty()) {
            LOG.error(apkPath + " contains no watch face!!!");
        }
    }

    private synchronized void init() {
        if (watchfaceServiceClasses != null)
            return;
        watchfaceServiceClasses = Lists.newArrayList();
        for (String myWatchFace : wfServices) {
            SootClass myWatchFaceServiceClz = Scene.v().getSootClass(myWatchFace);
            if (!myWatchFaceServiceClz.isConcrete()) {
                LOG.error("My watch face service class is not concrete!");
            }
            watchfaceServiceClasses.add(myWatchFaceServiceClz);
        }

        CANVAS_WATCH_FACE_SERVICE_CLZ =
                Scene.v().getSootClassUnsafe(Tracker.NonObf.CANVAS_WATCH_FACE_SERVICE_CLZ_NAME);
        GLES2_WATCH_FACE_SERVICE_CLZ =
                Scene.v().getSootClassUnsafe(Tracker.NonObf.GLES2_WATCH_FACE_SERVICE_CLZ_NAME);
        if (CANVAS_WATCH_FACE_SERVICE_CLZ == null && GLES2_WATCH_FACE_SERVICE_CLZ == null) {
            LOG.error(apkPath + " is obfuscated!!! Ignore!!!");
        }

        CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ =
                Scene.v().getSootClassUnsafe(Tracker.NonObf.CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ_NAME);
        GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ =
                Scene.v().getSootClassUnsafe(Tracker.NonObf.GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ_NAME);
        if (CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ == null && GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ == null
                || CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ.isPhantom() && GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ.isPhantom()) {
            LOG.error(apkPath + " is obfuscated!!! Ignore!!!");
        }
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        init();

        FastHierarchy hier = Scene.v().getOrMakeFastHierarchy();
        if (CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ != null
                && CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ.isConcrete()) {
            for (SootClass clz : hier.getSubclassesOf(CANVAS_WATCH_FACE_SERVICE_ENGINE_CLZ)) {
                // instrument onCreate
                SootMethod onCreateMtd = clz.getMethod(Tracker.NonObf.ON_CREATE_MTD_SUBSIG);
                instrumentWithEngineParamStartAndEnd(onCreateMtd);

                // instrument onDraw
                SootMethod onDrawMtd = clz.getMethod(Tracker.NonObf.ON_DRAW_CANVAS_MTD_SUBSIG);
                instrumentWithEngineParamStartAndEnd(onDrawMtd);

                // instrument onAmbientModeChanged
                SootMethod onAmbientModeChangedMtd = clz.getMethodUnsafe(Tracker.NonObf.ON_AMBIENT_MODE_CHANGED_MTD_SUBSIG);
//        com.example.android.wearable.watchface.CalendarWatchFaceService$Engine
                if (null == onAmbientModeChangedMtd) {
                    SootClass customWatchFaceService = Scene.v().loadClass("android.support.wearable.watchface.CustomWatchFaceService$Engine", SootClass.BODIES);
                    onAmbientModeChangedMtd = new SootMethod("onAmbientModeChanged",
                            Arrays.asList(new Type[]{BooleanType.v()}),
                            VoidType.v(), Modifier.PUBLIC);
                    clz.addMethod(onAmbientModeChangedMtd);
                    onAmbientModeChangedMtd.setActiveBody(customWatchFaceService.getMethod(Tracker.NonObf.ON_AMBIENT_MODE_CHANGED_MTD_SUBSIG).retrieveActiveBody());
                } else
                    instrumentWithEngineParamStartAndEnd(onAmbientModeChangedMtd);

                // instrument onVisibilityChanged
                SootMethod onVisibilityChangedMtd = clz.getMethodUnsafe(Tracker.NonObf.ON_VISIBILITY_CHANGED_MTD_SUBSIG);
                if (null == onVisibilityChangedMtd) {
                    SootClass customWatchFaceService = Scene.v().getSootClass("android.support.wearable.watchface.CustomWatchFaceService$Engine");
                    onVisibilityChangedMtd = new SootMethod("onVisibilityChanged",
                            Arrays.asList(new Type[]{BooleanType.v()}),
                            VoidType.v(), Modifier.PUBLIC);
                    clz.addMethod(onVisibilityChangedMtd);
                    onVisibilityChangedMtd.setActiveBody(customWatchFaceService.getMethod(Tracker.NonObf.ON_VISIBILITY_CHANGED_MTD_SUBSIG).retrieveActiveBody());
                } else
                    instrumentWithEngineParamStartAndEnd(onVisibilityChangedMtd);
            }
        }
//    if (GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ != null
//            && GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ.isConcrete()) {
//      for (SootClass clz : hier.getSubclassesOf(GLES2_WATCH_FACE_SERVICE_ENGINE_CLZ)) {
//        // instrument onDraw
//        SootMethod onDrawMtd = clz.getMethod(Tracker.NonObf.ON_DRAW_GLES2_MTD_SUBSIG);
//        instrumentWithEngineParamStartAndEnd(onDrawMtd);
//
//        // instrument onAmbientModeChanged
//        SootMethod onAmbientModeChangedMtd = clz.getMethod(Tracker.NonObf.ON_AMBIENT_MODE_CHANGED_MTD_SUBSIG);
//        instrumentWithEngineParamStartAndEnd(onAmbientModeChangedMtd);
//
//        // instrument onVisibilityChanged
//        SootMethod onVisibilityChangedMtd = clz.getMethod(Tracker.NonObf.ON_VISIBILITY_CHANGED_MTD_SUBSIG);
//        instrumentWithEngineParamStartAndEnd(onVisibilityChangedMtd);
//      }
//    }
    }

    void instrumentWithEngineParamStartAndEnd(SootMethod mtd) {
        if (hasInstr.contains(mtd)) {
            return;
        }

        LOG.info("@@@@ " + mtd);

        Body b = mtd.retrieveActiveBody();
        UnitGraph g = new ExceptionalUnitGraph(b);
        Local thisLocal = b.getThisLocal();

        List<Unit> heads = Lists.newLinkedList(g.getHeads());
        while (!heads.isEmpty()) {
            Unit u = heads.remove(0);
            if (u instanceof IdentityStmt) {
                heads.addAll(g.getSuccsOf(u));
            } else {
                if (mtd.getSubSignature().equals(Tracker.NonObf.ON_DRAW_CANVAS_MTD_SUBSIG)
                        || mtd.getSubSignature().equals(Tracker.NonObf.ON_DRAW_GLES2_MTD_SUBSIG)) {
                    JimpleUtil.v().insertTrackerLogOnDrawBefore(b, Tracker.MTD_START_TAG, thisLocal, u);
                } else if (mtd.getSubSignature().equals(Tracker.NonObf.ON_CREATE_MTD_SUBSIG)) {
                    JimpleUtil.v().insertTrackerLogOnCreateBefore(b, Tracker.MTD_START_TAG, thisLocal, u);
                } else {
                    JimpleUtil.v().insertTrackerLogTransitionBefore(b, Tracker.MTD_START_TAG, thisLocal, b.getParameterLocal(0), u);
                }
            }
        }

        List<Unit> tails = Lists.newLinkedList(g.getTails());
        while (!tails.isEmpty()) {
            Unit u = tails.remove(0);
            if (mtd.getSubSignature().equals(Tracker.NonObf.ON_DRAW_CANVAS_MTD_SUBSIG)
                    || mtd.getSubSignature().equals(Tracker.NonObf.ON_DRAW_GLES2_MTD_SUBSIG)) {
                JimpleUtil.v().insertTrackerLogOnDrawBefore(b, Tracker.MTD_END_TAG, thisLocal, u);
            } else if (mtd.getSubSignature().equals(Tracker.NonObf.ON_CREATE_MTD_SUBSIG)) {
                JimpleUtil.v().insertTrackerLogOnCreateBefore(b, Tracker.MTD_END_TAG, thisLocal, u);
            } else {
                JimpleUtil.v().insertTrackerLogTransitionBefore(b, Tracker.MTD_END_TAG, thisLocal, b.getParameterLocal(0), u);
            }
        }

        b.validate();

        hasInstr.add(mtd);
    }
}
