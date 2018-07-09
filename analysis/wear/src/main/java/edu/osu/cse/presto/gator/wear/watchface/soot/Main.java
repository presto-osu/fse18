/*
 * Main.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.soot;

import com.google.common.collect.Lists;
import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.wear.watchface.util.Configuration;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class Main {
  private static final Log LOG = Log.Factory.getLog(Main.class.getName());

  public static void main(String[] args) {
    String sdkPlatformPath = args[0];
    String apkPath = args[1];
    Configuration.STG_PATH_LENGTH_K = Integer.parseInt(args[2]);

    Log.setShowTime(Boolean.parseBoolean(args[3]));

//        if (ApkUtil.v().isMultiDex(apkPath)) {
//            LOG.error(apkPath + " contains multiple DEX files!!!");
//        }
    runWJTP(sdkPlatformPath, apkPath);
  }

  // whole Jimple transformation pack
  static void runWJTP(String sdkPlatformPath, String apkPath) {
    setup(sdkPlatformPath, apkPath);

    PackManager.v().getPack("wstp").add(new Transform("wstp.myInstrumenter", new MySceneTransformer(apkPath)));

    final String[] sootArgs =
            {"-ws", "-p", "cg", "enabled:false", "-keep-line-number", "-allow-phantom-refs", "-process-multiple-dex"};
//    {"-ws", "-p", "cg.spark", "enabled:true", "-keep-line-number", "-allow-phantom-refs", "-process-multiple-dex"};
    soot.Main.main(sootArgs);
  }

  static void setup(String sdkPlatformPath, String apkPath) {
    // prefer Android APK files// -src-prec apk// prefer Android APK files// -src-prec apk
    Options.v().set_src_prec(Options.src_prec_apk);
    // output as APK, too//-f J
    Options.v().set_output_format(Options.output_format_none);
    // set Android platform jars and apk
    Options.v().set_android_jars(sdkPlatformPath);
    Options.v().set_process_dir(Lists.newArrayList(apkPath));
  }
}
