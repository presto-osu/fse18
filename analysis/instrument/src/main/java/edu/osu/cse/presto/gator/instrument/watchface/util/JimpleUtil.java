/*
 * JimpleUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.instrument.watchface.util;

import edu.osu.cse.presto.gator.instrument.watchface.runtime.Tracker;
import soot.*;
import soot.jimple.Expr;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;

public class JimpleUtil {
    private SootMethod trackerLogOnDraw = Scene.v()
            .getSootClass(Tracker.class.getName())
            .getMethod("void logOnDraw(int,java.lang.String)");
    private SootMethod trackerLogOnCreate = Scene.v()
            .getSootClass(Tracker.class.getName())
            .getMethod("void logOnCreate(int,java.lang.String)");
    private SootMethod trackerLog = Scene.v()
            .getSootClass(Tracker.class.getName())
            .getMethod("void logTransition(int,java.lang.String,java.lang.String,android.support.wearable.watchface.WatchFaceService$Engine,boolean)");

    public void insertTrackerLogTransitionBefore(Body b, int tag, Local engine, Local is, Unit point) {
        SootMethod mtd = b.getMethod();
        String mtdSubSig = mtd.getSubSignature();
        String clz = mtd.getDeclaringClass().getName();
        Expr e = Jimple.v().newStaticInvokeExpr(trackerLog.makeRef(),
                IntConstant.v(tag), StringConstant.v(clz),
                StringConstant.v(mtdSubSig), engine, is);
        insertBefore(b, Jimple.v().newInvokeStmt(e), point);
    }

    public void insertTrackerLogOnDrawBefore(Body b, int tag, Local engine, Unit point) {
        SootMethod mtd = b.getMethod();
        String clz = mtd.getDeclaringClass().getName();
        Expr e = Jimple.v().newStaticInvokeExpr(trackerLogOnDraw.makeRef(),
                IntConstant.v(tag), StringConstant.v(clz));
        insertBefore(b, Jimple.v().newInvokeStmt(e), point);
    }

    public void insertTrackerLogOnCreateBefore(Body b, int tag, Local engine, Unit point) {
        SootMethod mtd = b.getMethod();
        String clz = mtd.getDeclaringClass().getName();
        Expr e = Jimple.v().newStaticInvokeExpr(trackerLogOnCreate.makeRef(),
                IntConstant.v(tag), StringConstant.v(clz));
        insertBefore(b, Jimple.v().newInvokeStmt(e), point);
    }

    public void insertBefore(Body b, Unit toInsert, Unit point) {
        b.getUnits().insertBefore(toInsert, point);
    }

    public void insertAfter(Body b, Unit toInsert, Unit point) {
        b.getUnits().insertAfter(toInsert, point);
    }

    // make singleton
    private JimpleUtil() {
    }

    public static JimpleUtil v() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final JimpleUtil INSTANCE = new JimpleUtil();
    }
}
