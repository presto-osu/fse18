/*
 * JsonIO.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package edu.osu.cse.presto.gator.wear.watchface.util;

import com.google.gson.Gson;

import java.io.*;

public class JsonIO {
    private static final Gson gson = new Gson();

    public static <T> T read(String jsonFile, Class<T> clz) throws FileNotFoundException {
        BufferedReader r = new BufferedReader(new FileReader(jsonFile));
        return gson.fromJson(r, clz);
    }

    public static <T> void write(T g, String toPath) throws IOException {
        FileWriter fileWriter = new FileWriter(toPath);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(gson.toJson(g));
        printWriter.close();
    }
}
