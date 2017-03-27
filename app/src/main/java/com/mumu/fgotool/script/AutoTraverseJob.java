package com.mumu.fgotool.script;

import com.mumu.fgotool.PrivatePackageManager;
import com.mumu.fgotool.records.ElectricityRecordHandler;
import com.mumu.fgotool.utility.Log;
import com.mumu.libjoshgame.Cmd;
import com.mumu.libjoshgame.JoshGameLibrary;
import com.mumu.libjoshgame.ScreenCoord;
import com.mumu.libjoshgame.ScreenPoint;

/**
 * AutoTraverseJob
 * Traverse all accounts in list
 */

class AutoTraverseJob extends FGOJobHandler.FGOJob {
    private static final String TAG = "AutoTraverseJob";
    private ElectricityRecordHandler mAccountHandler;
    private AutoTraverseRoutine mRoutine;
    private PrivatePackageManager mPPM;
    private JoshGameLibrary mGL;
    private JobEventListener mListener;
    private int mCurrentIndex = -1;

    AutoTraverseJob (String jobName, int jobIndex) {
        super(jobName, jobIndex);
        mPPM = PrivatePackageManager.getInstance();

        /* JoshGameLibrary basic initial */
        mGL = JoshGameLibrary.getInstance();
        mGL.setPackageManager(mPPM.getPM());
        mGL.setGameOrientation(ScreenPoint.SO_Landscape);
        mGL.setAmbiguousRange(0x0A);
        mGL.setScreenDimension(1080, 1920);
    }

    @Override
    public void start() {
        super.start();
        Log.d(TAG, "starting job " + getJobName());

        mCurrentIndex = 0;
        mRoutine = null;
        mRoutine = new AutoTraverseRoutine();
        mRoutine.start();
    }

    @Override
    public void stop() {
        super.stop();
        Log.d(TAG, "stopping job " + getJobName());

        mRoutine.interrupt();
    }

    @Override
    public void setExtra(Object object) {
        if (object instanceof ElectricityRecordHandler) {
            mAccountHandler = (ElectricityRecordHandler) object;
            if (!mAccountHandler.getAvailable()) {
                Log.w(TAG, "Account Record Handler is not available yet, might be a bug.");
            } else {
                Log.d(TAG, "Account number: " + mAccountHandler.getCount());
            }
        } else {
            Log.e(TAG, "Set extra for AutoTraverseJob failed, wrong data type");
        }
    }

    public void setJobEventListener(JobEventListener el) {
        mListener = el;
    }

    public void sendEvent(String msg, Object extra) {
        if (mListener != null) {
            mListener.onEventReceived(msg, extra);
        } else {
            Log.w(TAG, "There is no event listener registered.");
        }
    }

    public void sendMessage(String msg) {
        sendEvent(msg, null);
    }

    private void stopFGO() {
        mGL.runCommand("am force-stop com.aniplex.fategrandorder");
    }

    private void startFGO() {
        mGL.runCommand("am start \"com.aniplex.fategrandorder/jp.delightworks.Fgo.player.AndroidPlugin\"");
    }

    private void restoreCurrentAccount() throws Exception {
        if (mCurrentIndex < mAccountHandler.getCount() && mCurrentIndex > 0) {
            String thisAccount = mAccountHandler.getRecord(mCurrentIndex);
            Log.d(TAG, "Now restoring account record: " + thisAccount);
            sendMessage("" + (mCurrentIndex + 1) + " / " + mAccountHandler.getCount() + " :" + mAccountHandler.get(mCurrentIndex).title);
            mPPM.moveData("com.aniplex.fategrandorder", "restore:com.mumu.fgotool/files/" + thisAccount);
        }
    }

    private void backupCurrentAccountPrefs() throws Exception {
        if (mCurrentIndex < mAccountHandler.getCount() && mCurrentIndex > 0) {
            String thisAccount = mAccountHandler.getRecord(mCurrentIndex);
            Log.d(TAG, "Now backup account prefs: " + thisAccount);
            sendMessage("Now backing up prefs .. ");
            mPPM.moveData("com.aniplex.fategrandorder", "backupPref:com.mumu.fgotool/files/" + thisAccount);
        }
    }

    private class AutoTraverseRoutine extends Thread {
        ScreenPoint pointScreenCenter = new ScreenPoint(0,0,0,0,500,1090,ScreenPoint.SO_Portrait);
        ScreenPoint pointExitBulletin = new ScreenPoint(0x3D,0x3D,0x3D,0xff,1020,1871,ScreenPoint.SO_Landscape);
        ScreenPoint pointHomeGiftBox = new ScreenPoint(229,64,39,0xff,646,1013,ScreenPoint.SO_Landscape);
        ScreenPoint pointHomeOSiRaSe = new ScreenPoint(0,0,4,0xff,219,78,ScreenPoint.SO_Landscape);
        ScreenPoint pointHomeApAdd = new ScreenPoint(201,142,85,0xff,262,1049,ScreenPoint.SO_Landscape);
        ScreenCoord pointCloseDialog = new ScreenCoord(238, 968, ScreenPoint.SO_Portrait);

        private void main() throws Exception {
            boolean shouldRunning = true;
            mCurrentIndex = 0;
            stopFGO();

            while (shouldRunning) {
                restoreCurrentAccount();
                sleep(1000);
                startFGO();
                sleep(48000);
                while (!mGL.getCaptureService().colorIs(pointExitBulletin) ||
                        !mGL.getCaptureService().colorIs(pointHomeApAdd)) {
                    mGL.getInputService().tapOnScreen(pointScreenCenter.coord);
                    sleep(2000);
                }

                mGL.getInputService().tapOnScreen(pointCloseDialog);
                sleep(1000);
                mGL.getInputService().tapOnScreen(pointCloseDialog);
                sleep(1000);

                backupCurrentAccountPrefs();
                stopFGO();
                sleep(1000);

                mCurrentIndex++;
                if (mCurrentIndex >= mAccountHandler.getCount())
                    shouldRunning = false;
            }
        }

        public void run() {
            try {
                main();
            } catch (Exception e) {
                Log.e(TAG, "Routine caught an exception " + e.getMessage());
            }
        }
    }
}
