package com.mumu.fgotool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;

/*
 *  UpdateAgentReceiver
 *  This class receives any update event and sdcard insertion event
 *  for ZB501KL
 */
public class UpdateAgentReceiver extends BroadcastReceiver {
    private static final String TAG = "UpdateAgent";
    private static final String ACTION_BOOT_COMP = "android.intent.action.BOOT_COMPLETED";
    private static final String ACTION_UPDATE = "com.asus.updateagent.UPDATE_PACKAGE";
    private static final String ACTION_UPDATE_RESULT = "com.asus.updateagent.UPDATE_PACKAGE_RESULT";
    private static final String ACTION_PACKAGE_ADD = "android.intent.action.PACKAGE_ADDED";
    private static final String ACTION_PACKAGE_INSTALL = "android.intent.action.PACKAGE_INSTALL";
    private static final String TENCENT_PACKAGE_NAME = "com.tencent.dashcam";
    private static final boolean SHOULD_VERIFY_NAME = false;

    private static final String ACTION_SDCARD_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
    private static final String ACTION_SDCARD_REMOVED = "android.intent.action.MEDIA_REMOVED";
    private static final String FACTORY_APK_NAME = "SIGNED_open_FunctionTest.apk";
    private static final String FACTORY_APK_NAME_SIGNED = "SIGNED_XB501KL_FunctionTest.apk";

    private InstallUTSAppDaemon installDaemon = new InstallUTSAppDaemon();
    private static boolean shouldDaemonRunning = false;
    private Context mContext;

    private final Handler mHandler = new Handler();
    private int mReturnCode = 99;
    private static int mDelayTimeOfResultIntent = 4000;  /* sending out result after 4000 ms */

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        String intentContent;

        switch (intentAction) {
            case ACTION_BOOT_COMP:
                Log.d(TAG, "Receive boot complete.");
                installPackageFromExternalStorage(context);
                break;
            case ACTION_UPDATE:
                int installOption;
                String ret;
                intentContent = intent.getStringExtra("packagePath");
                installOption = intent.getIntExtra("installOption", -1);
                Log.d(TAG, "Receive update request: " + intentContent);
                Toast.makeText(context, "Update request: " + intentContent, Toast.LENGTH_SHORT).show();

                ret = PropertyService.getProperty("persist.sys.ua.delay");
                if (!ret.equals("")) {
                    mDelayTimeOfResultIntent = Integer.parseInt(ret);
                }

                Log.d(TAG, "Delay broadcasting intent time to " + mDelayTimeOfResultIntent);

                if (intentContent != null) {
                    if (installOption == -1) {
                        Log.d(TAG, "Install package with no option, set it default INSTALL_REPLACE_EXISTING");
                        installPackageOnPath(context, intentContent, PrivatePackageManager.INSTALL_REPLACE_EXISTING);
                    } else {
                        installPackageOnPath(context, intentContent, installOption);
                    }
                } else {
                    Log.e(TAG, "Cannot install package with name null");
                }

                break;
            case ACTION_UPDATE_RESULT:
                intentContent = intent.getStringExtra("packageResult");
                Log.d(TAG, "Receive update package result " + intentContent);
                break;
            case ACTION_PACKAGE_ADD:
                Log.d(TAG, "Receive ACTION_PACKAGE_ADD ");
                break;
            case ACTION_PACKAGE_INSTALL:
                Log.d(TAG, "Receive ACTION_PACKAGE_INSTALL ");
                break;
            case ACTION_SDCARD_MOUNTED:
                Log.d(TAG, "Receive ACTION_SDCARD_MOUNTED ");
                installPackageFromExternalStorage(context);
                break;
            case ACTION_SDCARD_REMOVED:
                Log.d(TAG, "Receive ACTION_SDCARD_REMOVED ");
                break;
            default:
                Log.e(TAG, "Unimplemented intent action " + intentAction);
                break;
        }
    }

    private void installPackageFromExternalStorage(Context context) {
        mContext = context;
        shouldDaemonRunning = true;
        //installDaemon.start();
    }

    private int installPackageOnPath(final Context context, String path, int option) {
        try {
            final PrivatePackageManager am = PrivatePackageManager.getInstance();
            am.init(context.getPackageManager());
            am.setOnInstalledPackaged(new OnInstalledPackaged() {
                public void packageInstalled(String packageName, int returnCode) {
                    mReturnCode = returnCode;
                    if (returnCode == PrivatePackageManager.INSTALL_SUCCEEDED) {
                        Log.d(TAG, "Install succeeded");
                    } else {
                        Log.d(TAG, "Install failed: " + returnCode);
                    }

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Now sending install result to host");
                            sendInstallPackageResult(context, mReturnCode);
                        }
                    }, mDelayTimeOfResultIntent);
                }
            });

            if (isApkPackageNameMatched(context, path, TENCENT_PACKAGE_NAME))
                am.installPackage(path, option);
            else
                Log.w(TAG, "Sorry, your apk file is not matched to Tencent Dashcam");
        } catch (Exception e) {
            Log.d(TAG, "Install Package Failed: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }

        return 0;
    }

    private void sendInstallPackageResult(Context context, int result) {
        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATE_RESULT);
        intent.putExtra("packageInstallResult", result);
        intent.putExtra("packageResult", "RESULT HERE");
        context.sendBroadcast(intent);
    }

    private boolean isApkPackageNameMatched(Context context, String path, String name) {
        PackageManager pm;

        if (!SHOULD_VERIFY_NAME)
            return true;

        pm = context.getPackageManager();
        if (pm != null) {
            PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
            if (pi != null) {
                if (pi.packageName.equals(name)) {
                    Log.d(TAG, "Package name of this APK file is matched!");
                    return true;
                } else {
                    Log.w(TAG, "Error, the package with name " + pi.packageName + " is not matched");
                }
            } else {
                Log.e(TAG, "Error, cannot verify APK file");
            }
        } else {
            Log.e(TAG, "Error, cannot get PackageManager");
        }
        return false;
    }

    public static HashSet<String> getExternalMounts() {
        final HashSet<String> out = new HashSet<>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        String s = "";
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                s = s + new String(buffer);
            }
            is.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // parse output
        final String[] lines = s.split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec")) {
                if (line.matches(reg)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/"))
                            if (!part.toLowerCase(Locale.US).contains("vold"))
                                out.add(part);
                    }
                }
            }
        }
        return out;
    }

    private class InstallFactoryAppDaemon extends Thread {
        public void run() {
            Log.d(TAG, "Install Factory App Daemon is started.");
            while(shouldDaemonRunning) {
                //checking if apk present
                HashSet<String> mountPath = getExternalMounts();
                for(String externalPath: mountPath) {
                    String apkPath = externalPath + "/" + FACTORY_APK_NAME;
                    String signedApkPath = externalPath + "/" + FACTORY_APK_NAME_SIGNED;
                    File f = new File(apkPath);
                    File f2 = new File(signedApkPath);
                    if(f.exists() && !f.isDirectory()) {
                        Log.d(TAG, "APK found in " + apkPath + ", installing ...");
                        installPackageOnPath(mContext, apkPath, PrivatePackageManager.INSTALL_REPLACE_EXISTING);
                        shouldDaemonRunning = false;
                    }

                    if(f2.exists() && !f2.isDirectory()) {
                        Log.d(TAG, "APK found in " + signedApkPath + ", installing ...");
                        installPackageOnPath(mContext, signedApkPath, PrivatePackageManager.INSTALL_REPLACE_EXISTING);
                        shouldDaemonRunning = false;
                    }
                }

                //sleep till next run
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Factory App Installer has finished gracefully.");
        }
    }

    private class InstallUTSAppDaemon extends Thread {
        public void run() {
            Log.d(TAG, "Install UTS App Daemon is started.");

            //checking if apk present
            String path = "/asusfw";
            Log.d(TAG, "Traverse path: " + path);
            File directory = new File(path);
            File[] files = directory.listFiles();
            Log.d(TAG, "Size: "+ files.length);
            for (File f : files)
            {
                Log.d(TAG, "Looking for FileName:" + f.getName());
                if (f.getName().contains("apk")) {
                    Log.d(TAG, "Matched APK file, installing");
                    installPackageOnPath(mContext, f.getAbsolutePath(), PrivatePackageManager.INSTALL_REPLACE_EXISTING);
                }
            }

            Log.d(TAG, "UTS App Installer has finished gracefully.");
        }
    }
}
