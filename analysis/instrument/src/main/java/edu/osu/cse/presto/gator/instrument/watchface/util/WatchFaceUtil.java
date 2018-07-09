/*
 * WatchFaceUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.instrument.watchface.util;

import edu.osu.cse.presto.gator.Log;
import soot.*;
import soot.jimple.*;

public class WatchFaceUtil {
    private final Log LOG = Log.Factory.getLog(WatchFaceUtil.class.getName());

    /**
     * Find the {@code onDraw()} in the engine of a custom watch face service.
     *
     * @param myWatchFaceClz watch face class extending {@code CanvasWatchFaceService} or {@code Gles2WatchFaceService}
     * @return {@code onDraw()} method of the watch face class's engine
     */
    public SootMethod getOnDrawMtd(SootClass myWatchFaceClz) {
        SootClass myEngineClass = getMyEngineClass(myWatchFaceClz);
        String onDrawMtdSubSig = getMyEngineOnDrawMtdSubSig(myEngineClass);
        SootMethod onDrawMtd = myEngineClass.getMethodUnsafe(onDrawMtdSubSig);
        while (onDrawMtd == null) {
            // may be in the super class
            myEngineClass = myEngineClass.getSuperclass();
            onDrawMtd = myEngineClass.getMethodUnsafe(onDrawMtdSubSig);
        }
        return onDrawMtd;
    }

    /**
     * Find the engine class of a custom watch face service.
     * First look at the synthetic {@code onCreateEngine()} to locate the real
     * {@code onCreateEngine()} method. Then look at the base type of the {@code new}
     * in the real method.
     *
     * @param myWatchFaceClz any {@code WatchFaceService} subclass
     * @return the engine class of the given watch face service
     */
    public SootClass getMyEngineClass(SootClass myWatchFaceClz) {
        String watchfaceServiceOnCreateEngineSubsig =
                "android.service.wallpaper.WallpaperService$Engine onCreateEngine()";
        String canvasWatchFaceServiceOnCreateEngineSubsig =
                "android.support.wearable.watchface.CanvasWatchFaceService$Engine onCreateEngine()";
        SootClass watchFaceServiceClz = myWatchFaceClz;
        // get synthetic onCreateEngine
        SootMethod myWatchfaceServiceOnCreateEngine =
                watchFaceServiceClz.getMethodUnsafe(canvasWatchFaceServiceOnCreateEngineSubsig);
        if (myWatchfaceServiceOnCreateEngine == null) {
            do {
                // try another synthetic onCreateEngine
                myWatchfaceServiceOnCreateEngine =
                        watchFaceServiceClz.getMethodUnsafe(watchfaceServiceOnCreateEngineSubsig);
                watchFaceServiceClz = watchFaceServiceClz.getSuperclass();
            } while (myWatchfaceServiceOnCreateEngine == null);
        }
        for (Unit u : myWatchfaceServiceOnCreateEngine.retrieveActiveBody().getUnits()) {
            Stmt s = (Stmt) u;
            if (s instanceof IdentityStmt || !(s instanceof AssignStmt) || !s.containsInvokeExpr())
                continue;
            // get the real onCreateEngine
            String onCreateEngineMtdSubsig = s.getInvokeExpr().getMethod().getSubSignature();
            SootMethod onCreateEngineMtd = myWatchFaceClz.getMethodUnsafe(onCreateEngineMtdSubsig);
            while (onCreateEngineMtd == null) {
                myWatchFaceClz = myWatchFaceClz.getSuperclass();
                onCreateEngineMtd = myWatchFaceClz.getMethodUnsafe(onCreateEngineMtdSubsig);
            }
            for (Unit unit : onCreateEngineMtd.retrieveActiveBody().getUnits()) {
                Stmt stmt = (Stmt) unit;
                if (stmt instanceof IdentityStmt || !(stmt instanceof AssignStmt))
                    continue;
                Value newOp = ((AssignStmt) stmt).getRightOp();
                if (!(newOp instanceof NewExpr))
                    continue;
                RefType baseType = ((NewExpr) newOp).getBaseType();
                SootClass engineClz = baseType.getSootClass();
                FastHierarchy hier = Scene.v().getOrMakeFastHierarchy();
                if (!hier.isSubclass(engineClz, Scene.v().getSootClass("android.service.wallpaper.WallpaperService$Engine")))
                    continue;
                return engineClz;
            }
        }
        throw new RuntimeException(String.format("Cannot find %1$s.Engine", myWatchFaceClz));
    }

    /**
     * Can handle {@code CanvasWatchFaceService.Engine} and {@code Gles2WatchFaceService.Engine}.
     * We look at {@code CanvasWatchFaceService.onSurfaceRedrawNeeded()} and
     * {@code Gles2WatchFaceService.onSurfaceRedrawNeeded()} to find the (obfuscated) {@code draw()}
     * method, in which later we find {@code onDraw()}.
     *
     * @param myEngineClz class extending {@code CanvasWatchFaceService.Engine} or {@code Gles2WatchFaceService.Engine}
     * @return the functional {@code onDraw()} of the given engine
     */
    public String getMyEngineOnDrawMtdSubSig(SootClass myEngineClz) {
        String onSurfaceRedrawNeededSubsig = "void onSurfaceRedrawNeeded(android.view.SurfaceHolder)";
        SootMethod onSurfaceRedrawNeededMtd = myEngineClz.getMethodUnsafe(onSurfaceRedrawNeededSubsig);
        while (onSurfaceRedrawNeededMtd == null) {
            myEngineClz = myEngineClz.getSuperclass();
            onSurfaceRedrawNeededMtd = myEngineClz.getMethodUnsafe(onSurfaceRedrawNeededSubsig);
        }
        Stmt drawStmt = null;
        for (Unit unit : onSurfaceRedrawNeededMtd.retrieveActiveBody().getUnits()) {
            Stmt s = (Stmt) unit;
            if (!(s instanceof ReturnVoidStmt)) {
                drawStmt = s;
                continue;
            }
            if (!(drawStmt instanceof InvokeStmt) || !drawStmt.containsInvokeExpr()
                    || !(drawStmt.getInvokeExpr() instanceof SpecialInvokeExpr)) {
                throw new RuntimeException(String.format("Cannot find draw()/drawFrame() invocation in %1$s.onSurfaceRedrawNeeded()", myEngineClz));
            }
            SootMethod drawMtd = drawStmt.getInvokeExpr().getMethod();
            boolean isDraw = false;
            Stmt pre1Stmt = null;
            Stmt pre2Stmt = null;
            Stmt pre3Stmt = null;
            for (Unit du : drawMtd.retrieveActiveBody().getUnits()) {
                Stmt ds = (Stmt) du;
                if (ds.containsInvokeExpr()
                        && ds.getInvokeExpr().getMethod().getSignature().equals("<android.view.SurfaceHolder: android.graphics.Rect getSurfaceFrame()>")) {
                    isDraw = true;
                } else if (ds.containsInvokeExpr()
                        && ds.getInvokeExpr().getMethod().getSignature().equals("<android.opengl.EGL14: boolean eglSwapBuffers(android.opengl.EGLDisplay,android.opengl.EGLSurface)>")) {
                    if (!(pre3Stmt instanceof InvokeStmt) || !pre3Stmt.containsInvokeExpr()
                            || !(pre3Stmt.getInvokeExpr() instanceof VirtualInvokeExpr)) {
                        throw new RuntimeException(String.format("Cannot find onDraw() invocation in %1$s.drawFrame()", myEngineClz));
                    }
                    return pre3Stmt.getInvokeExpr().getMethod().getSubSignature();
                } else if (isDraw) {
                    if (!(ds instanceof InvokeStmt) || !ds.containsInvokeExpr()
                            || !(ds.getInvokeExpr() instanceof VirtualInvokeExpr)) {
                        throw new RuntimeException(String.format("Cannot find onDraw() invocation in %1$s.draw()", myEngineClz));
                    }
                    return ds.getInvokeExpr().getMethod().getSubSignature();
                }
                pre3Stmt = pre2Stmt;
                pre2Stmt = pre1Stmt;
                pre1Stmt = ds;
            }
        }
        throw new RuntimeException(String.format("Cannot find %1$s.onDraw()", myEngineClz));
    }

    // make singleton
    private WatchFaceUtil() {
    }

    public static WatchFaceUtil v() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final WatchFaceUtil INSTANCE = new WatchFaceUtil();
    }
}
