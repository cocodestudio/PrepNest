package com.cocode.prepnest;

import android.content.Context;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogUtils {
    private Context context;
    private File logFile;
    private Fragment fragment;

    public LogUtils(Context context) {
        this.context = context;
        File externalDir = context.getExternalFilesDir(null);
        this.logFile = new File(externalDir, "logs.txt");
    }

    public LogUtils(Fragment fragment) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
        File externalDir = context.getExternalFilesDir(null);
        this.logFile = new File(externalDir, "logs.txt");
    }

    public void createLogFile() {
        //File externalDir = context.getExternalFilesDir(null);
        //File logFile = new File(externalDir, "logs txt");

        if (logFile.exists()) {
            boolean deleted = logFile.delete();

            if (!deleted) {
                Toast.makeText(context, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        try {
            FileWriter writer = new FileWriter(logFile);
            writer.write("########## PrepNest ##########\n########## VERSION 1.0.0 ##########\n\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addActivity() {
        try {
            FileWriter writer = new FileWriter(logFile, true);
            writer.write("--- " + context.getClass().getName() + " ---\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addFragment() {
        try {
            FileWriter writer = new FileWriter(logFile, true);
            writer.write("--- " + fragment.getClass().getName() + " ---\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addLog(String title, String message) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss", Locale.getDefault());
            Date date = new Date();
            FileWriter writer = new FileWriter(logFile, true);
            writer.write("[" + title + "] " + message + " [" + formatter.format(date) + "]\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
