/*
 * Main.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.instrument.watchface;

import com.google.common.collect.Lists;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.instrument.watchface.runtime.Tracker;
import edu.osu.cse.presto.gator.instrument.watchface.util.ApkUtil;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

public class Main {
    private static final Log LOG = Log.Factory.getLog(Main.class.getName());

    public static void main(String[] args) {
        String sdkPlatformPath = args[0];
        String apkPath = args[1];
        String runtimeTrackerDir = args[2];

        if (ApkUtil.v().isMultiDex(apkPath)) {
            LOG.error(apkPath + " is multi-dex!!!");
        }

        runWJTP(sdkPlatformPath, apkPath, runtimeTrackerDir);
    }

    // whole Jimple transformation pack
    static void runWJTP(String sdkPlatformPath, String apkPath, String runtimeTrackerDir) {
        setup(sdkPlatformPath, apkPath, runtimeTrackerDir);

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.myInstrumenter", new MySceneTransformer(apkPath)));

        final String[] sootArgs =
                {"-w", "-p", "cg", "enabled:false", "-keep-line-number", "-allow-phantom-refs"};
        soot.Main.main(sootArgs);
    }

    static void setup(String sdkPlatformPath, String apkPath, String runtimeTrackerDir) {

        // prefer Android APK files// -src-prec apk// prefer Android APK files// -src-prec apk
        Options.v().set_src_prec(Options.src_prec_apk);
        // output as APK, too//-f J
        Options.v().set_output_format(Options.output_format_dex);
        //    Options.v().set_output_format(Options.output_format_jimple);
        // set Android platform jars and apk
        Options.v().set_android_jars(sdkPlatformPath);
        Options.v().set_process_dir(Lists.newArrayList(apkPath));

        // load instrument helper class
        Options.v().set_prepend_classpath(true);
        Options.v().set_soot_classpath(runtimeTrackerDir);

        Scene.v().addBasicClass("android.support.wearable.watchface.CustomWatchFaceService$Engine", SootClass.SIGNATURES);


        Scene.v().loadClass(Tracker.class.getName(), SootClass.HIERARCHY).setApplicationClass();
        Scene.v().loadClass(Tracker.STATE.class.getName(), SootClass.HIERARCHY).setApplicationClass();
//    Scene.v().loadClass(Tracker.NonObf.class.getName(), SootClass.HIERARCHY).setApplicationClass();
    }
}
