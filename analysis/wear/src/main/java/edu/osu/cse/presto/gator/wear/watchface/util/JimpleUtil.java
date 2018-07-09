/*
 * JimpleUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.util;

import com.google.common.collect.Maps;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;

import java.util.Iterator;
import java.util.Map;

public class JimpleUtil {
    private static JimpleUtil instance;

    public static synchronized JimpleUtil v() {
        if (instance == null) {
            instance = new JimpleUtil();
        }
        return instance;
    }


    // /////////////////////////////////////////
    // General Jimple utils
    // Assume "l = ..."
    public Local lhsLocal(Stmt s) {
        return (Local) ((DefinitionStmt) s).getLeftOp();
    }

    // Assume "... = ..."
    public Value lhs(Stmt s) {
        return ((DefinitionStmt) s).getLeftOp();
    }

    public Local receiver(Stmt s) {
        return (Local) ((InstanceInvokeExpr) s.getInvokeExpr()).getBase();
    }

    public Local receiver(InvokeExpr ie) {
        if (ie instanceof InstanceInvokeExpr) {
            // It can be a static invoke.
            return (Local) ((InstanceInvokeExpr) ie).getBase();
        } else {
            return null;
        }
    }

    public Local thisLocal(SootMethod m) {
        IdentityStmt first = null;
        synchronized (m) {
            first = (IdentityStmt) m.retrieveActiveBody().getUnits().iterator().next();
        }
        if (!(first.getRightOp() instanceof ThisRef)) {
            throw new RuntimeException();
        }
        return lhsLocal(first);
    }

    /**
     * Returns the local variable corresponding to the n-th parameter in the
     * specified method. The counting starts from 0. For an instance method and
     * n=0, this method is equivalent to thisLocal().
     *
     * @param method the specified method
     * @param index  specifies the position of the parameter
     * @return
     */
    public Local localForNthParameter(SootMethod method, int index) {
        Iterator<Unit> stmts = null;
        synchronized (method) {
            stmts = method.retrieveActiveBody().getUnits().iterator();
        }
        for (int i = 0; i < index; i++) {
            stmts.next();
        }
        Stmt idStmt = (Stmt) stmts.next();
        if (!(idStmt instanceof DefinitionStmt)) {
            System.out.println("--- " + method);
            System.out.println(method.retrieveActiveBody());
        }
        return lhsLocal(idStmt);
    }

    // /////////////////////////////////////////
    // App-specific recording
    public Map<Stmt, SootMethod> s2m = Maps.newHashMap();

    public SootMethod lookup(Stmt s) {
        return s2m.get(s);
    }

    public void record(Stmt s, SootMethod m) {
        s2m.put(s, m);
    }
}
