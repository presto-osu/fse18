/*
 * ApkUtil.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.instrument.watchface.util;

import android.content.res.AXmlResourceParser;
import com.google.common.collect.Lists;
import edu.osu.cse.presto.gator.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ApkUtil {

  Log LOG = Log.Factory.getLog(ApkUtil.class.getSimpleName());

  /**
   * Only support detecting default multi-dex,
   * i.e., classes.dex, classes1.dex, classes2.dex, ...
   *
   * @param apkPath path of an APK file
   * @return if the APK contains multiple DEX files
   */
  public boolean isMultiDex(String apkPath) {
    boolean hasDex = false;
    ZipFile archive = null;
    try {
      try {
        archive = new ZipFile(apkPath);
        for (Enumeration<? extends ZipEntry> entries =
             archive.entries(); entries.hasMoreElements(); ) {
          ZipEntry entry = entries.nextElement();
          String entryName = entry.getName();
          if (entryName.startsWith("classes") && entryName.endsWith(".dex")) {
            if (hasDex)
              return true;
            hasDex = true;
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Error when looking for manifest in apk: " + e);
      }
    } finally {
      if (archive != null)
        try {
          archive.close();
        } catch (IOException e) {
          throw new RuntimeException("Error when looking for manifest in apk: " + e);
        }
    }
    return false;
  }

  /**
   * Find all watch face services.
   *
   * @param apkPath path of an APK file
   * @return a list of names for watch face services
   */
  public List<String> getWatchFaceServices(String apkPath) {
    List<String> ret = Lists.newArrayList();
    try {
      // Soot's way
      getWatchFaceServicesFromApk(apkPath, ret);
    } catch (Exception e) {
      // Fall back to apkanalyzer
      try {
        getWatchFaceServicesFromRawXmlIS(getRawManifestInputStream(apkPath), ret);
      } catch (IOException | XmlPullParserException e1) {
        e1.printStackTrace();
      }
    }
    return ret;
  }

  private String getApplicationId(String apkPath) throws IOException {
    String sdkPath = System.getenv("ANDROID_SDK");
    if (sdkPath == null) {
      sdkPath = System.getenv("HOME") + "/Android/Sdk";
      LOG.warn("$ANDROID_SDK is not set. Trying to use %s.", sdkPath);
    }
    String[] cmd = new String[]{sdkPath + "/tools/bin/apkanalyzer", "manifest", "application-id", apkPath};
    LOG.info("Run cmd: %s", String.join(" ", cmd));
    Process proc = Runtime.getRuntime().exec(cmd);
    String output = convertStreamToString(proc.getInputStream());
    return output.trim();
  }

  private InputStream getRawManifestInputStream(String apkPath) throws IOException {
    String sdkPath = System.getenv("ANDROID_SDK");
    if (sdkPath == null) {
      sdkPath = System.getenv("HOME") + "/Android/Sdk";
      LOG.warn("$ANDROID_SDK is not set. Trying to use %s.", sdkPath);
    }
    String[] cmd = new String[]{sdkPath + "/tools/bin/apkanalyzer", "manifest", "print", apkPath};
    LOG.info("Run cmd: %s", String.join(" ", cmd));
    Process proc = Runtime.getRuntime().exec(cmd);
    return proc.getInputStream();
  }

  private XmlPullParser getRawManifestXmlParser(InputStream manifestIS) throws XmlPullParserException {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
//    factory.setNamespaceAware(true);
    XmlPullParser xpp = factory.newPullParser();
    String output = convertStreamToString(manifestIS);
//    LOG.info("output:\n %s", output);
    xpp.setInput(new StringReader(output));
    return xpp;
  }

  private String convertStreamToString(java.io.InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  private void getWatchFaceServicesFromRawXmlIS(InputStream manifestIS, List<String> ret) throws XmlPullParserException, IOException {
    XmlPullParser xpp = getRawManifestXmlParser(manifestIS);
    getWatchFaceServicesFromXml(xpp, ret);
  }

  private void getWatchFaceServicesFromApk(String apkPath, List<String> ret) throws IOException, XmlPullParserException {
    InputStream manifestIS = null;
    ZipFile archive = null;
    try {
      try {
        archive = new ZipFile(apkPath);
        for (Enumeration<? extends ZipEntry> entries =
             archive.entries(); entries.hasMoreElements(); ) {
          ZipEntry entry = entries.nextElement();
          String entryName = entry.getName();
          // We are dealing with the Android manifest
          if (entryName.equals("AndroidManifest.xml")) {
            manifestIS = archive.getInputStream(entry);
            break;
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Error when looking for manifest in apk: " + e);
      }
      getWatchFaceServicesFromBinaryXmlIS(manifestIS, ret);
    } finally {
      if (archive != null)
        try {
          archive.close();
        } catch (IOException e) {
          throw new RuntimeException("Error when looking for manifest in apk: " + e);
        }
    }
  }

  private void getWatchFaceServicesFromBinaryXmlIS(InputStream manifestIS, List<String> ret) throws IOException, XmlPullParserException {
    AXmlResourceParser parser = new AXmlResourceParser();
    parser.open(manifestIS);
    getWatchFaceServicesFromXml(parser, ret);
  }

  private void getWatchFaceServicesFromXml(XmlPullParser parser, List<String> ret) throws IOException, XmlPullParserException {
    int depth = 0;
    String serviceName = null;
    String packageName = null;
    boolean inIntentFilter = false;
    boolean hasActionWallpaperService = false;
    boolean hasCategoryWatchFace = false;
    int eventType = parser.getEventType();
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        depth++;
        String tagName = parser.getName();
        if (depth == 1 && tagName.equals("manifest")) {
          for (int i = 0; i != parser.getAttributeCount(); ++i) {
            String attributeName = parser.getAttributeName(i);
//                String attributeValue = AXMLPrinter.getAttributeValue(parser, i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals("package")) {
              packageName = attributeValue;
              break;
            }
          }
        } else if (depth == 3 && tagName.equals("service")) {
          for (int i = 0; i != parser.getAttributeCount(); ++i) {
            String attributeName = parser.getAttributeName(i);
//                String attributeValue = AXMLPrinter.getAttributeValue(parser, i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals("name") || attributeName.equals("android:name")) {
              serviceName = attributeValue;
              if (serviceName.startsWith(".")) {
                serviceName = packageName + serviceName;
              }
              break;
            }
          }
        } else if (depth == 4 && tagName.equals("intent-filter")) {
          inIntentFilter = true;
        } else if (depth == 5 && tagName.equals("action") && inIntentFilter) {
          for (int i = 0; i != parser.getAttributeCount(); ++i) {
            String attributeName = parser.getAttributeName(i);
//                String attributeValue = AXMLPrinter.getAttributeValue(parser, i);
            String attributeValue = parser.getAttributeValue(i);
            if ((attributeName.equals("name") || attributeName.equals("android:name"))
                    && attributeValue.equals("android.service.wallpaper.WallpaperService")) {
              hasActionWallpaperService = true;
              break;
            }
          }
        } else if (depth == 5 && tagName.equals("category") && inIntentFilter) {
          for (int i = 0; i != parser.getAttributeCount(); ++i) {
            String attributeName = parser.getAttributeName(i);
//                String attributeValue = AXMLPrinter.getAttributeValue(parser, i);
            String attributeValue = parser.getAttributeValue(i);
            if ((attributeName.equals("name") || attributeName.equals("android:name"))
                    && attributeValue.equals("com.google.android.wearable.watchface.category.WATCH_FACE")) {
              hasCategoryWatchFace = true;
              break;
            }
          }
        }
      } else if (eventType == XmlPullParser.END_TAG) {
        String tagName = parser.getName();
        if (depth == 4 && tagName.equals("intent-filter")) {
          inIntentFilter = false;
        } else if (depth == 3 && tagName.equals("service")) {
          if (hasActionWallpaperService && hasCategoryWatchFace) {
            if (serviceName == null) {
              throw new RuntimeException("Service name is null!");
            }
            ret.add(serviceName);
          }
          hasActionWallpaperService = false;
          hasCategoryWatchFace = false;
        }
        depth--;
      }
      eventType = parser.next();
    }
  }

  public String getPackageName(File apk) {
    String packageName = null;
    try {
      InputStream manifestIS = getRawManifestInputStream(apk.getAbsolutePath());
      XmlPullParser parser = getRawManifestXmlParser(manifestIS);
      int eventType = parser.getEventType();
      int depth = 0;
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          depth++;
          String tagName = parser.getName();
          if (depth == 1 && tagName.equals("manifest")) {
            for (int i = 0; i != parser.getAttributeCount(); ++i) {
              String attributeName = parser.getAttributeName(i);
              String attributeValue = parser.getAttributeValue(i);
              if (attributeName.equals("package")) {
                packageName = attributeValue;
                break;
              }
            }
          }
        }
        eventType = parser.next();
      }
    } catch (IOException | XmlPullParserException e) {
      LOG.error(e);
    }
    return packageName;
  }

  public void renameApkNameToPackageName(final String inputDir, final String outputDir) throws IOException {
    Files.newDirectoryStream(Paths.get(inputDir), "*.apk").forEach(
            new Consumer<Path>() {
              @Override
              public void accept(Path path) {
                try {
                  String pack = getApplicationId(path.toString());
                  if (pack.isEmpty()) {
                    LOG.info("%s has no package name", path);
                    return;
                  }
                  Path source = Paths.get(path.toString());
                  Path target = Paths.get(outputDir, pack + ".apk");
                  if (!target.toFile().exists()) {
                    LOG.info(source + " >>> " + target);
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    //        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                  }
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
            });
//    File inputDirFile = new File(inputDir);
//    for (final File apkFile : inputDirFile.listFiles()) {
//      if (!apkFile.getName().endsWith(".apk")) {
//        continue;
//      }
//      String pack = getApplicationId(apkFile.getAbsolutePath());
//      if (pack.isEmpty()) {
//        LOG.info("%s has no package name", apkFile);
//        continue;
//      }
//      Path source = Paths.get(apkFile.getAbsolutePath());
//      Path target = Paths.get(outputDir, pack + ".apk");
//      if (!target.toFile().exists()) {
//        LOG.info(source + " >>> " + target);
//        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
////        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
//      }
  }

  // make singleton
  private ApkUtil() {
  }

  public static ApkUtil v() {
    return LazyHolder.INSTANCE;
  }

  private static class LazyHolder {
    private static final ApkUtil INSTANCE = new ApkUtil();
  }
}
