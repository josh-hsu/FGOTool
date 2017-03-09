package com.mumu.fgotool;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;

public class ApplicationManager {

    /* install options */
    public static final int INSTALL_FORWARD_LOCK = 0x00000001;
    public static final int INSTALL_REPLACE_EXISTING = 0x00000002;
    public static final int INSTALL_ALLOW_TEST = 0x00000004;
    public static final int INSTALL_EXTERNAL = 0x00000008;
    public static final int INSTALL_INTERNAL = 0x00000010;
    public static final int INSTALL_FROM_ADB = 0x00000020;
    public static final int INSTALL_ALL_USERS = 0x00000040;
    public static final int INSTALL_ALLOW_DOWNGRADE = 0x00000080;
    public static final int INSTALL_GRANT_RUNTIME_PERMISSIONS = 0x00000100;

    public static final int INSTALL_SUCCEEDED = 1;

    public static final int INSTALL_FAILED_ALREADY_EXISTS = -1;

    public static final int INSTALL_FAILED_INVALID_APK = -2;

    public static final int INSTALL_FAILED_INVALID_URI = -3;

    public static final int INSTALL_FAILED_INSUFFICIENT_STORAGE = -4;

    public static final int INSTALL_FAILED_DUPLICATE_PACKAGE = -5;

    public static final int INSTALL_FAILED_NO_SHARED_USER = -6;

    public static final int INSTALL_FAILED_UPDATE_INCOMPATIBLE = -7;

    public static final int INSTALL_FAILED_SHARED_USER_INCOMPATIBLE = -8;

    public static final int INSTALL_FAILED_MISSING_SHARED_LIBRARY = -9;

    public static final int INSTALL_FAILED_REPLACE_COULDNT_DELETE = -10;

    public static final int INSTALL_FAILED_DEXOPT = -11;

    public static final int INSTALL_FAILED_OLDER_SDK = -12;

    public static final int INSTALL_FAILED_CONFLICTING_PROVIDER = -13;

    public static final int INSTALL_FAILED_NEWER_SDK = -14;

    public static final int INSTALL_FAILED_TEST_ONLY = -15;

    public static final int INSTALL_FAILED_CPU_ABI_INCOMPATIBLE = -16;

    public static final int INSTALL_FAILED_MISSING_FEATURE = -17;

    public static final int INSTALL_FAILED_CONTAINER_ERROR = -18;

    public static final int INSTALL_FAILED_INVALID_INSTALL_LOCATION = -19;

    public static final int INSTALL_FAILED_MEDIA_UNAVAILABLE = -20;

    public static final int INSTALL_PARSE_FAILED_NOT_APK = -100;

    public static final int INSTALL_PARSE_FAILED_BAD_MANIFEST = -101;

    public static final int INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION = -102;

    public static final int INSTALL_PARSE_FAILED_NO_CERTIFICATES = -103;

    public static final int INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES = -104;

    public static final int INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING = -105;

    public static final int INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME = -106;

    public static final int INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID = -107;

    public static final int INSTALL_PARSE_FAILED_MANIFEST_MALFORMED = -108;

    public static final int INSTALL_PARSE_FAILED_MANIFEST_EMPTY = -109;

    public static final int INSTALL_FAILED_INTERNAL_ERROR = -110;

    private PackageInstallObserver observer;
    private PackageManager pm;
    private Method method;
    private Method josh_method;

    private OnInstalledPackaged onInstalledPackaged;

    class PackageInstallObserver extends IPackageInstallObserver.Stub {

        public void packageInstalled(String packageName, int returnCode) throws RemoteException {
            if (onInstalledPackaged != null) {
                onInstalledPackaged.packageInstalled(packageName, returnCode);
            }
        }
    }

    public ApplicationManager(Context context) throws SecurityException, NoSuchMethodException {

        observer = new PackageInstallObserver();
        pm = context.getPackageManager();

        Class<?>[] types = new Class[] {Uri.class, IPackageInstallObserver.class, int.class, String.class};
        method = pm.getClass().getMethod("installPackage", types);

        Class<?>[] josh_types = new Class[] {String.class, String.class};
        josh_method = pm.getClass().getMethod("moveApplicationData", josh_types);
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

    public void installPackage(File apkFile, int option) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (!apkFile.exists()) throw new IllegalArgumentException();
        Uri packageURI = Uri.fromFile(apkFile);
        installPackage(packageURI, option);
    }

    public void installPackage(Uri apkFile, int option) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        method.invoke(pm, new Object[] {apkFile, observer, option, null});
    }

    public void callJosh(String para1, String para2) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        josh_method.invoke(pm, new Object[] {para1, para2});
    }

}
