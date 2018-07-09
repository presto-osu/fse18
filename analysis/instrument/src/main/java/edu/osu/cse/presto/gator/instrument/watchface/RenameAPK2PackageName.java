/*
 * RenameAPK2PackageName.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.instrument.watchface;


import edu.osu.cse.presto.gator.Log;
import edu.osu.cse.presto.gator.instrument.watchface.util.ApkUtil;

import java.io.IOException;

public class RenameAPK2PackageName {

  public static void main(String[] args) throws IOException {
    Log.setShowTime(false);
    ApkUtil.v().renameApkNameToPackageName(args[0], args[1]);
  }
}
