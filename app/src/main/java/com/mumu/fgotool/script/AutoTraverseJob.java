package com.mumu.fgotool.script;

import com.mumu.fgotool.PrivatePackageManager;
import com.mumu.fgotool.records.ElectricityRecordHandler;
import com.mumu.fgotool.utility.Log;
import com.mumu.libjoshgame.Cmd;
import com.mumu.libjoshgame.JoshGameLibrary;
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
        mGL = JoshGameLibrary.getInstance();
        mGL.SetGameOrientation(ScreenPoint.SO_Landscape);
        mGL.SetScreenDimension(1080, 1920);
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
        Cmd.RunCommand("am force-stop com.aniplex.fategrandorder");
    }

    private void startFGO() {
        Cmd.RunCommand("am start \"com.aniplex.fategrandorder/jp.delightworks.Fgo.player.AndroidPlugin\"");
    }

    private int changeNextAccount() throws Exception {
        String thisAccount;

        if (mCurrentIndex >= mAccountHandler.getCount())
            return -1;
        else
            thisAccount = mAccountHandler.getRecord(mCurrentIndex);

        Log.d(TAG, "Now restoring account record: " + thisAccount);
        sendMessage("" + (mCurrentIndex + 1) + " / " + mAccountHandler.getCount());
        mPPM.moveData("com.aniplex.fategrandorder", "restore:com.mumu.fgotool/files/" + thisAccount);
        mCurrentIndex++;
        return 0;
    }

    private class AutoTraverseRoutine extends Thread {
        ScreenPoint pointScreenCenter = new ScreenPoint(0,0,0,0,500,1090,ScreenPoint.SO_Portrait);
        ScreenPoint pointExitBulletin = new ScreenPoint(0,0,0,0,1871,37,ScreenPoint.SO_Landscape);

        private void main() throws Exception {
            boolean shouldRunning = true;
            while (shouldRunning) {
                stopFGO();
                sleep(1000);
                shouldRunning = (changeNextAccount() == 0);
                sleep(1000);
                startFGO();
                sleep(48000);
                mGL.getInputSvc().TapOnScreen(pointScreenCenter.coord);
                sleep(500);
                mGL.getInputSvc().TapOnScreen(pointScreenCenter.coord);
                sleep(500);
                mGL.getInputSvc().TapOnScreen(pointScreenCenter.coord);
                sleep(500);
                mGL.getInputSvc().TapOnScreen(pointScreenCenter.coord);
                sleep(500);
                mGL.getInputSvc().TapOnScreen(pointScreenCenter.coord);
                sleep(7000);
                mGL.getInputSvc().TapOnScreen(pointExitBulletin.coord);
                sleep(2000);
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
