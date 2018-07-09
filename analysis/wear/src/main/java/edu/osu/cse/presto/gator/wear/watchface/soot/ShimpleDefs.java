/*
 * ShimpleDefs.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot;

import com.google.common.collect.Maps;
import soot.*;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.shimple.ShimpleBody;
import soot.util.Chain;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ShimpleDefs {
    protected Map<Value, List<Unit>> localToDefs;
    protected Map<SootField, List<Unit>> fieldToDefs;

    public ShimpleDefs(ShimpleBody sb) {
        if (!sb.isSSA())
            throw new RuntimeException("ShimpleBody is not in proper SSA form as required by ShimpleLocalDefs.  You may need to rebuild it or use SimpleLocalDefs instead.");

        {
            Chain<Unit> unitsChain = sb.getUnits();
            Iterator<Unit> unitsIt = unitsChain.iterator();
            localToDefs = Maps.newHashMapWithExpectedSize(unitsChain.size() * 2 + 1);
            fieldToDefs = Maps.newHashMap();

            while (unitsIt.hasNext()) {
                Unit unit = unitsIt.next();
                for (ValueBox valueBox : unit.getDefBoxes()) {
                    Value value = (valueBox).getValue();

                    // map locals and fields
                    if (value instanceof Local)
                        localToDefs.put(value, Collections.singletonList(unit));
                    if (value instanceof FieldRef)
                        fieldToDefs.put(((FieldRef) value).getField(), Collections.singletonList(unit));
                }
            }
        }
    }

    public List<Unit> getDefsOf(Value l) throws DefNotFoundException {
        if (l instanceof InstanceFieldRef) {
            for (SootField v : fieldToDefs.keySet()) {
                if (((InstanceFieldRef) l).getField().equals(v)) {
                    return fieldToDefs.get(v);
                }
            }
        }
        List<Unit> defs = localToDefs.get(l);

        if (defs == null)
            throw new DefNotFoundException("Value " + l + " not found in Body.");

        return defs;
    }

    public static class DefNotFoundException extends Exception {
        DefNotFoundException(String msg) {
            super(msg);
        }
    }
}
