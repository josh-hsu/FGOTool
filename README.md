# FGOTool
Upper layer control app for Android App data operations 

## Framework patching

### frameworks/base
```compare
diff --git a/core/java/android/app/ApplicationPackageManager.java b/core/java/android/app/ApplicationPackageManager.java
index 14ff31b9162..aa34f4f7b8f 100644
--- a/core/java/android/app/ApplicationPackageManager.java
+++ b/core/java/android/app/ApplicationPackageManager.java
@@ -2332,4 +2332,15 @@ final class ApplicationPackageManager extends PackageManager {
         }
     }
     //---
+
+    //Josh+++
+    @Override
+    public void moveApplicationData(String packageName, String dataType) {
+        try{
+            mPM.moveApplicationData(packageName, dataType);
+        } catch(RemoteException e) {
+            //Should never happen!
+        }
+    }
+    //Josh---
 }
diff --git a/core/java/android/content/pm/IPackageManager.aidl b/core/java/android/content/pm/IPackageManager.aidl
index b35299eda4b..9bfec29fe3d 100644
--- a/core/java/android/content/pm/IPackageManager.aidl
+++ b/core/java/android/content/pm/IPackageManager.aidl
@@ -530,4 +530,7 @@ interface IPackageManager {
     //+++ Api to get untrusted app list
     List<String> getUntrustedAppList();
     //---
+    //+++ Josh
+    void moveApplicationData(String packageName, String dataType);
+    //--- Josh
 }
diff --git a/core/java/android/content/pm/PackageManager.java b/core/java/android/content/pm/PackageManager.java
index 9653a7ad3c0..147cc3dca33 100755
--- a/core/java/android/content/pm/PackageManager.java
+++ b/core/java/android/content/pm/PackageManager.java
@@ -5063,4 +5063,12 @@ public abstract class PackageManager {
      */
     public abstract void refreshApp2sdBlacklist();
     //---
+
+
+    //+++ Josh
+    /**
+     * @hide
+     */
+    public abstract void moveApplicationData(String packageName, String dataType);
+    //--- Josh
 }
diff --git a/services/core/java/com/android/server/pm/Installer.java b/services/core/java/com/android/server/pm/Installer.java
index 5f0ebef360e..31366cd2a9f 100644
--- a/services/core/java/com/android/server/pm/Installer.java
+++ b/services/core/java/com/android/server/pm/Installer.java
@@ -121,6 +121,17 @@ public final class Installer extends SystemService {
         return mInstaller.execute(builder.toString());
     }
 
+    // +++ Josh
+    public int callInstalldToMoveAppData(String packageName, String dataType) {
+        StringBuilder builder = new StringBuilder("moveappdata");
+        builder.append(' ');
+        builder.append(packageName);
+        builder.append(' ');
+        builder.append(dataType);
+        return mInstaller.execute(builder.toString());
+    }
+    // --- Josh
+
     // BEGIN leo_liao@asus.com
     public int removeIdmap(String targetApkPath, String overlayApkPath) {
         StringBuilder builder = new StringBuilder("rmidmap");
diff --git a/services/core/java/com/android/server/pm/PackageManagerService.java b/services/core/java/com/android/server/pm/PackageManagerService.java
index e0291722417..3da88f8767f 100755
--- a/services/core/java/com/android/server/pm/PackageManagerService.java
+++ b/services/core/java/com/android/server/pm/PackageManagerService.java
@@ -17685,4 +17685,14 @@ public class PackageManagerService extends IPackageManager.Stub {
         mContext.sendBroadcast(intent);
     }
     //---
+
+    //+++ Josh
+    public void moveApplicationData(String packageName, String dataType) {
+        Log.d(TAG, "Josh: package name: " + packageName + " data type: " + dataType);
+        int retCode = mInstaller.callInstalldToMoveAppData(packageName, dataType);
+        if (retCode < 0) {
+            Log.w(TAG, "Couldn't remove cache files for package: " + packageName);
+        }
+    }
+    //--- Josh
 }
diff --git a/test-runner/src/android/test/mock/MockPackageManager.java b/test-runner/src/android/test/mock/MockPackageManager.java
index c5037e5c164..122609e8686 100644
--- a/test-runner/src/android/test/mock/MockPackageManager.java
+++ b/test-runner/src/android/test/mock/MockPackageManager.java
@@ -1009,4 +1009,13 @@ public class MockPackageManager extends PackageManager {
         throw new UnsupportedOperationException();
     }
     //---
+
+    //+++ Josh
+    /**
+     * @hide
+     */
+    public void moveApplicationData(String packageName, String dataType){
+        throw new UnsupportedOperationException();
+    }
+    //--- Josh
 }
 ```
 
### framework/native
 
```compare
 diff --git a/cmds/installd/commands.cpp b/cmds/installd/commands.cpp
index 64cb93cba..feb9e8c85 100755
--- a/cmds/installd/commands.cpp
+++ b/cmds/installd/commands.cpp
@@ -1774,6 +1774,144 @@ int rm_idmap(const char *target_apk, const char *overlay_apk)
     return 0;
 }
 
+//+++ Josh
+#define FGO_DONT_REMOVE   1
+#define FGO_SHOULD_REMOVE 0
+#define FGO_DATA_TYPE_ID_FILE    0
+#define FGO_DATA_TYPE_PREF       1
+
+bool startsWith(const char *pre, const char *str)
+{
+    size_t lenpre = strlen(pre);
+    size_t lenstr = strlen(str);
+    return lenstr < lenpre ? false : strncmp(pre, str, lenpre) == 0;
+}
+
+int fgo_files_exclusion_predicate(const char* name, const int isDir)
+{
+    char *dot = strrchr(name, '.');
+
+    if (isDir)
+        return FGO_DONT_REMOVE;
+
+    if (dot && !strcmp(dot, ".dat"))
+        return FGO_SHOULD_REMOVE;
+    else if (dot && !strcmp(dot, ".xml"))
+        return FGO_SHOULD_REMOVE;
+    else
+        return FGO_DONT_REMOVE;
+}
+
+int fgo_get_file_owner_group(const char* path, struct stat* info)
+{
+    int ret = stat(path, info);
+    if (ret != 0) {
+        ALOGE("Josh: Get file stat error [%d].\n", ret);
+    }
+    return ret;
+}
+
+int fgo_move_files(const char* origin, const char* dest, int type)
+{
+    std::string _spkgFilesdir(StringPrintf("/data/data/%s/files", origin));
+    std::string _spkgPrefsdir(StringPrintf("/data/data/%s/shared_prefs", origin));
+    std::string _dpkgFilesdir(StringPrintf("/data/data/%s/files", dest));
+    std::string _dpkgPrefsdir(StringPrintf("/data/data/%s/shared_prefs", dest));
+
+    const char* spkgFiledir = _spkgFilesdir.c_str();
+    const char* spkgPrefdir = _spkgPrefsdir.c_str();
+    const char* dpkgFiledir = _dpkgFilesdir.c_str();
+    const char* dpkgPrefdir = _dpkgPrefsdir.c_str();
+
+    struct stat info;
+    int ret = 0;
+
+    switch (type) {
+    case FGO_DATA_TYPE_ID_FILE:
+        ret = fgo_get_file_owner_group(spkgFiledir, &info);
+        if (ret != 0)
+            return ret;
+
+        ret = copy_dir_files(spkgFiledir, dpkgFiledir, info.st_uid, info.st_gid, fgo_files_exclusion_predicate);
+        break;
+    case FGO_DATA_TYPE_PREF:
+        ret = fgo_get_file_owner_group(spkgPrefdir, &info);
+        if (ret != 0)
+            return ret;
+
+        ret = copy_dir_files(spkgPrefdir, dpkgPrefdir, info.st_uid, info.st_gid, fgo_files_exclusion_predicate);
+        break;
+    default:
+        break;
+    }
+
+    return ret;
+}
+
+/*
+ * this function removes the following files
+ * /data/data/[app_path]/files/ *.dat
+ * /data/data/[app_path]/shared_prefs/ *
+ */
+int fgo_remove_account(const char *app_path)
+{
+    std::string _pkgFilesdir(StringPrintf("/data/data/%s/files", app_path));
+    std::string _pkgPrefsdir(StringPrintf("/data/data/%s/shared_prefs", app_path));
+    const char* pkgFiledir = _pkgFilesdir.c_str();
+    const char* pkgPrefdir = _pkgPrefsdir.c_str();
+
+    ALOGD("Josh: delect account info %s\n", app_path);
+    int ret = delete_dir_contents(pkgFiledir, 0, fgo_files_exclusion_predicate);
+    if (ret) {
+        ALOGE("Josh: delect pkg files dir failed [%d].\n", ret);
+        return ret;
+    }
+
+    ret = delete_dir_contents(pkgPrefdir, 0, fgo_files_exclusion_predicate);
+    if (ret) {
+        ALOGE("Josh: delect pkg pref dir failed [%d].\n", ret);
+        return ret;
+    }
+
+    return 0;
+}
+
+/*
+ * move_app_data is the entry of fgoTool
+ * consider this as a basic argument parsing function
+ */
+int move_app_data(const char *app_path, const char *data_type)
+{
+    int ret = 0;
+
+    ALOGD("Josh: installd: %s %s\n", app_path, data_type);
+
+    if (startsWith("delete:account", data_type)) {
+        ALOGD("Josh: deleting account info %s\n", app_path);
+        fgo_remove_account(app_path);
+    } else if (startsWith("backupAll:", data_type)) {
+        ALOGD("Josh: move account info %s\n", app_path);
+        std::string destFileName(StringPrintf("%s", data_type));
+        const char* file_name = destFileName.c_str() + 10;
+        fgo_move_files(app_path, file_name, FGO_DATA_TYPE_ID_FILE);
+        fgo_move_files(app_path, file_name, FGO_DATA_TYPE_PREF);
+    } else if (startsWith("restore:", data_type)) {
+        ALOGD("Josh: restore account info %s\n", app_path);
+        std::string srcFileName(StringPrintf("%s", data_type));
+        const char* file_name = srcFileName.c_str() + 8;
+        fgo_move_files(file_name, app_path, FGO_DATA_TYPE_ID_FILE);
+        fgo_move_files(file_name, app_path, FGO_DATA_TYPE_PREF);
+    } else if (startsWith("backupPref:", data_type)) {
+        ALOGD("Josh: backup preference info %s\n", app_path);
+        std::string srcFileName(StringPrintf("%s", data_type));
+        const char* file_name = srcFileName.c_str() + 11;
+        fgo_move_files(app_path, file_name, FGO_DATA_TYPE_PREF);
+    }
+
+    return ret;
+}
+//--- Josh
+
 int restorecon_data(const char* uuid, const char* pkgName,
                     const char* seinfo, uid_t uid)
 {
diff --git a/cmds/installd/installd.cpp b/cmds/installd/installd.cpp
index 8af3126f0..9607a0ce7 100755
--- a/cmds/installd/installd.cpp
+++ b/cmds/installd/installd.cpp
@@ -168,6 +168,13 @@ static int do_rm_idmap(char **arg, char reply[REPLY_MAX] __unused)
     // END leo_liao@asus.com
 }
 
+//+++ Josh
+static int do_move_app_data(char **arg, char reply[REPLY_MAX] __unused)
+{
+    return move_app_data(arg[0], arg[1]);
+}
+//--- Josh
+
 static int do_restorecon_data(char **arg, char reply[REPLY_MAX] __attribute__((unused)))
 {
     return restorecon_data(parse_null(arg[0]), arg[1], arg[2], atoi(arg[3]));
@@ -223,6 +230,9 @@ struct cmdinfo cmds[] = {
     // BEGIN leo_liao@asus.com
     { "rmidmap",              2, do_rm_idmap },
     // END leo_liao@asus.com
+    // +++ Josh
+    { "moveappdata",          2, do_move_app_data },
+    // --- Josh
     { "restorecondata",       4, do_restorecon_data },
     { "createoatdir",         2, do_create_oat_dir },
     { "rmpackagedir",         1, do_rm_package_dir },
diff --git a/cmds/installd/installd.h b/cmds/installd/installd.h
index 367f3d790..ac59986f6 100755
--- a/cmds/installd/installd.h
+++ b/cmds/installd/installd.h
@@ -182,6 +182,12 @@ int delete_dir_contents_fd(int dfd, const char *name);
 
 int copy_dir_files(const char *srcname, const char *dstname, uid_t owner, gid_t group);
 
+int copy_dir_files(const char *srcname,
+                   const char *dstname,
+                   uid_t owner,
+                   uid_t group,
+                   int (*exclusion_predicate)(const char *name, const int is_dir));
+
 int lookup_media_dir(char basepath[PATH_MAX], const char *dir);
 
 int64_t data_disk_free(const std::string& data_path);
@@ -259,3 +265,6 @@ int link_file(const char *relative_path, const char *from_base, const char *to_b
 // BEGIN leo_liao@asus.com
 int rm_idmap(const char *target_path, const char *overlay_path);
 // END leo_liao@asus.com
+// +++ Josh
+int move_app_data(const char *app_path, const char *data_type);
+// --- Josh
diff --git a/cmds/installd/utils.cpp b/cmds/installd/utils.cpp
index 7db3fb90c..c770f162a 100644
--- a/cmds/installd/utils.cpp
+++ b/cmds/installd/utils.cpp
@@ -19,6 +19,8 @@
 #include <base/stringprintf.h>
 #include <base/logging.h>
 
+#include <selinux/selinux.h>
+
 #define CACHE_NOISY(x) //x
 
 using android::base::StringPrintf;
@@ -344,7 +346,8 @@ static int _copy_owner_permissions(int srcfd, int dstfd)
     return 0;
 }
 
-static int _copy_dir_files(int sdfd, int ddfd, uid_t owner, gid_t group)
+static int _copy_dir_files(int sdfd, int ddfd, uid_t owner, gid_t group,
+           int (*exclusion_predicate)(const char *name, const int is_dir))
 {
     int result = 0;
     if (_copy_owner_permissions(sdfd, ddfd) != 0) {
@@ -366,6 +369,11 @@ static int _copy_dir_files(int sdfd, int ddfd, uid_t owner, gid_t group)
         }
 
         const char *name = de->d_name;
+        /* check using the exclusion predicate, if provided */
+        if (exclusion_predicate && exclusion_predicate(name, (de->d_type == DT_DIR))) {
+            continue;
+        }
+
         int fsfd = openat(sdfd, name, O_RDONLY | O_NOFOLLOW | O_CLOEXEC);
         int fdfd = openat(ddfd, name, O_WRONLY | O_NOFOLLOW | O_CLOEXEC | O_CREAT, 0600);
         if (fsfd == -1 || fdfd == -1) {
@@ -387,6 +395,11 @@ static int _copy_dir_files(int sdfd, int ddfd, uid_t owner, gid_t group)
                 ALOGW("Couldn't copy %s: %s\n", name, strerror(errno));
                 result = -1;
             }
+
+            if (exclusion_predicate) {
+                ALOGD("Josh: this is for JoshTool, restore it sepolicy\n");
+                fsetfilecon(fdfd, /*(security_context_t)*/"u:object_r:app_data_file:s0:c512,c768");
+            }
         }
         close(fdfd);
         close(fsfd);
@@ -395,10 +408,21 @@ static int _copy_dir_files(int sdfd, int ddfd, uid_t owner, gid_t group)
     return result;
 }
 
+/*
+ * This is disabled because no one is using it.
+ */
+#if 0
+static int _copy_dir_files(int sdfd, int ddfd, uid_t owner, gid_t group)
+{
+    return _copy_dir_files(sdfd, ddfd, owner, group, NULL);
+}
+#endif
+
 int copy_dir_files(const char *srcname,
                    const char *dstname,
                    uid_t owner,
-                   uid_t group)
+                   uid_t group,
+                   int (*exclusion_predicate)(const char *name, const int is_dir))
 {
     int res = 0;
     DIR *ds = NULL;
@@ -421,7 +445,7 @@ int copy_dir_files(const char *srcname,
     int sdfd = dirfd(ds);
     int ddfd = dirfd(dd);
     if (sdfd != -1 && ddfd != -1) {
-        res = _copy_dir_files(sdfd, ddfd, owner, group);
+        res = _copy_dir_files(sdfd, ddfd, owner, group, exclusion_predicate);
     } else {
         res = -errno;
     }
@@ -430,6 +454,14 @@ int copy_dir_files(const char *srcname,
     return res;
 }
 
+int copy_dir_files(const char *srcname,
+                   const char *dstname,
+                   uid_t owner,
+                   uid_t group)
+{
+    return copy_dir_files(srcname, dstname, owner, group, NULL);
+}
+
 int lookup_media_dir(char basepath[PATH_MAX], const char *dir)
 {
     DIR *d;
 ```
 
