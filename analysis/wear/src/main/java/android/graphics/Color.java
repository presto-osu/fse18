/*
 * Color.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package android.graphics;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.osu.cse.presto.gator.Log;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Color {
  private static final Log LOG = Log.Factory.getLog(Color.class.getSimpleName());

  public static String PARSE_COLOR_SIG = "<android.graphics.Color: int parseColor(java.lang.String)>";

  public static final int BLACK = 0xFF000000;
  public static final int DKGRAY = 0xFF444444;
  public static final int GRAY = 0xFF888888;
  public static final int LTGRAY = 0xFFCCCCCC;
  public static final int WHITE = 0xFFFFFFFF;
  public static final int RED = 0xFFFF0000;
  public static final int GREEN = 0xFF00FF00;
  public static final int BLUE = 0xFF0000FF;
  public static final int YELLOW = 0xFFFFFF00;
  public static final int CYAN = 0xFF00FFFF;
  public static final int MAGENTA = 0xFFFF00FF;

  public static int parseColor(String colorString) {
    if (colorString.charAt(0) == '#') {
      // Use a long to avoid rollovers on #ffXXXXXX
      long color = Long.parseLong(colorString.substring(1), 16);
      if (colorString.length() == 7) {
        // Set the alpha value
        color |= 0x00000000ff000000;
      } else if (colorString.length() != 9) {
        throw new IllegalArgumentException("Unknown color");
      }
      return (int) color;
    } else {
      Integer color = sColorNameMap.get(colorString.toLowerCase(Locale.ROOT));
      if (color != null) {
        return color;
      }
    }
    throw new IllegalArgumentException("Unknown color");
  }

  public static String getName(int color) {
    String name = sNameColorMap.get(color);
    LOG.debug("............ %s %s %s", color, Integer.toHexString(color), name);
    if (name != null) {
      return name;
    }
    return Integer.toHexString(color);
//    throw new IllegalArgumentException("Unknown color");
  }

  private static final Map<String, Integer> sColorNameMap;
  private static final Map<Integer, String> sNameColorMap;


  public static final Set<String> allowedColorsInAmbient = Sets.newHashSet();

  static {
    sColorNameMap = Maps.newHashMap();
    sColorNameMap.put("black", BLACK);
    sColorNameMap.put("darkgray", DKGRAY);
    sColorNameMap.put("gray", GRAY);
    sColorNameMap.put("lightgray", LTGRAY);
    sColorNameMap.put("white", WHITE);
    sColorNameMap.put("red", RED);
    sColorNameMap.put("green", GREEN);
    sColorNameMap.put("blue", BLUE);
    sColorNameMap.put("yellow", YELLOW);
    sColorNameMap.put("cyan", CYAN);
    sColorNameMap.put("magenta", MAGENTA);
    sColorNameMap.put("aqua", 0xFF00FFFF);
    sColorNameMap.put("fuchsia", 0xFFFF00FF);
    sColorNameMap.put("darkgrey", DKGRAY);
    sColorNameMap.put("grey", GRAY);
    sColorNameMap.put("lightgrey", LTGRAY);
    sColorNameMap.put("lime", 0xFF00FF00);
    sColorNameMap.put("maroon", 0xFF800000);
    sColorNameMap.put("navy", 0xFF000080);
    sColorNameMap.put("olive", 0xFF808000);
    sColorNameMap.put("purple", 0xFF800080);
    sColorNameMap.put("silver", 0xFFC0C0C0);
    sColorNameMap.put("teal", 0xFF008080);

    sNameColorMap = Maps.newHashMap();
    for (Map.Entry<String, Integer> e : sColorNameMap.entrySet()) {
      sNameColorMap.put(e.getValue(), e.getKey());
    }
    sNameColorMap.put(0x0, "black");
    sNameColorMap.put(0xff181818, "black");
    sNameColorMap.put(0xff333333, "darkgray");
    sNameColorMap.put(0xff222222, "black");
    sNameColorMap.put(0xff4f4f4f, "black");
    sNameColorMap.put(0xffbbbbbb, "lightgray");
    sNameColorMap.put(0xffc9c9c9, "lightgray");
    sNameColorMap.put(0xffb6b6b6, "lightgray");
    sNameColorMap.put(0xffaaaaaa, "lightgray");
    sNameColorMap.put(0xff62635e, "lightgray");
    sNameColorMap.put(0xff424242, "lightgray");
    sNameColorMap.put(0xff434358, "darknavy");

    allowedColorsInAmbient.add("black");
    allowedColorsInAmbient.add("grey");
    allowedColorsInAmbient.add("lightgrey");
    allowedColorsInAmbient.add("darkgrey");
    allowedColorsInAmbient.add("gray");
    allowedColorsInAmbient.add("lightgray");
    allowedColorsInAmbient.add("darkgray");
    allowedColorsInAmbient.add("white");
  }
}
