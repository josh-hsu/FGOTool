package com.mumu.fgotool;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.widget.Toast;

import com.mumu.fgotool.utility.Log;

public class PrivatePackageManager {
    private static final String TAG = "PrivatePackageManager";

    /* install options */
    static final int INSTALL_REPLACE_EXISTING = 0x00000002;
    static final int INSTALL_SUCCEEDED = 1;

    private static PrivatePackageManager mSelf;
    private static boolean mInitialized = false;
    private PackageInstallObserver mPackageInstallObserver;
    private PackageManager mPM;

    private Method mInstallPackageMethod;
    private Method mMoveDataMethod;
    private Method mRunCmdMethod;

    private OnInstalledPackaged onInstalledPackaged;

    private class PackageInstallObserver extends IPackageInstallObserver.Stub {

        public void packageInstalled(String packageName, int returnCode) throws RemoteException {
            if (onInstalledPackaged != null) {
                onInstalledPackaged.packageInstalled(packageName, returnCode);
            }
        }
    }

    public static PrivatePackageManager getInstance() {
        if (mSelf == null) {
            mSelf = new PrivatePackageManager();
        }

        return mSelf;
    }

    private PrivatePackageManager()  {
        Log.d(TAG, "An PrivatePackageManager has been created.");
    }

    public void init(PackageManager pm) {
        setPackageManager(pm);

        mPackageInstallObserver = new PackageInstallObserver();

        try {
            Class<?>[] types = new Class[]{Uri.class, IPackageInstallObserver.class, int.class, String.class};
            mInstallPackageMethod = mPM.getClass().getMethod("installPackage", types);

            Class<?>[] run_types = new Class[]{String.class, String.class};
            mMoveDataMethod = mPM.getClass().getMethod("moveApplicationData", run_types);
            mRunCmdMethod = mPM.getClass().getMethod("joshCmd", run_types);

        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire methods. " + e.getMessage());
            return;
        }

        mInitialized = true;
    }

    public void setPackageManager(PackageManager pm) {
        this.mPM = pm;
    }

    public void setOnInstalledPackaged(OnInstalledPackaged onInstalledPackaged) {
        this.onInstalledPackaged = onInstalledPackaged;
    }

    public void installPackage(String apkFile) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        installPackage(apkFile, INSTALL_REPLACE_EXISTING);
    }

    public void installPackage(String apkFile, int option) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        installPackage(new File(apkFile), option);
    }

    void installPackage(File apkFile, int option) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (!apkFile.exists()) throw new IllegalArgumentException();
        Uri packageURI = Uri.fromFile(apkFile);
        installPackage(packageURI, option);
    }

    void installPackage(Uri apkFile, int option) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        mInstallPackageMethod.invoke(mPM, new Object[] {apkFile, mPackageInstallObserver, option, null});
    }

    public int moveData(String para1, String para2) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (!mInitialized) {
            Log.e(TAG, "PPM not initialized!!");
        } else {
            mMoveDataMethod.invoke(mPM, new Object[]{para1, para2});
        }
        return 0;
    }

    public void runCmd(String cmd) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (!mInitialized) {
            Log.e(TAG, "PPM not initialized!!");
        } else {
            mRunCmdMethod.invoke(mPM, new Object[]{cmd, ""});
        }
    }

}
