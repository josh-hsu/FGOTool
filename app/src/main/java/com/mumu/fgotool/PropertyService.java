package com.mumu.fgotool;

import android.app.Application;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * PropertyService
 *
 * PropertyService can set and get property, it also
 * can execute shell command and retrieve the result back.
 */
public class PropertyService extends Application {
    private static final String TAG = "PropertyService";

    public static String getProperty(String property) {
        String ret = runCommand("getprop " + property);
        ret = ret.replace("\n", "").replace("\r", "");
        return ret;
    }

    public static void setProperty(String property, int value) {
        runCommandNoOutput("setprop " + property + " " + value);
    }

    /*
     * Run the specific command, you should not execute a command that will
     * cost more than 5 seconds.
     * TODO: Add timeout design for those time consumed commands
     */
    public static String runCommand(String cmdInput){
        String retStr = "";
        BufferedReader output;

        Log.d(TAG, "running command " + cmdInput);

        String[] cmd = {"/system/bin/sh", "-c", cmdInput};
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            output = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            retStr = output.readLine();
            if (retStr != null) {
                Log.d(TAG, "execute command " + cmdInput + " [result]: " + retStr);
            } else {
                retStr = "";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return retStr;
    }

    /*
     * Run the specific command, you should not execute a command that will
     * cost more than 5 seconds.
     * This function will only execute with no output
     */
    public static void runCommandNoOutput(String cmdInput){
        String[] cmd = {"/system/bin/sh", "-c", cmdInput};
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

