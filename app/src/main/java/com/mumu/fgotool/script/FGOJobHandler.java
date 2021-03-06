package com.mumu.fgotool.script;

import com.mumu.fgotool.utility.Log;

/**
 * FGOJobHandler
 * Start or Stop any Job of FGO
 */

public class FGOJobHandler {
    public static final String TAG = "FGOJobHandler";
    private static FGOJobHandler mHandler;
    public static final int AUTO_TRAVERSE_JOB = 0;
    public static final int TOTAL_JOB = 1;

    private FGOJob[] mJobList;

    private FGOJobHandler() {
        mJobList = new FGOJob[TOTAL_JOB];
        mJobList[0] = new AutoTraverseJob("traverse_job", AUTO_TRAVERSE_JOB);
    }

    public static FGOJobHandler getHandler() {
        if (mHandler == null)
            mHandler = new FGOJobHandler();

        return mHandler;
    }

    public void startJob(int idx) {
        if (idx >= TOTAL_JOB) {
            Log.d(TAG, "Fail to start job " + idx + ", no such index.");
        } else {
            mJobList[idx].start();
        }
    }

    public void stopJob(int idx) {
        if (idx >= TOTAL_JOB) {
            Log.d(TAG, "Fail to stop job " + idx + ", no such index.");
        } else {
            mJobList[idx].stop();
        }
    }

    public void setExtra(int idx, Object object) {
        if (idx >= TOTAL_JOB) {
            Log.d(TAG, "Setting extra data for job " + idx + " failed, no such index.");
        } else {
            mJobList[idx].setExtra(object);
        }
    }

    public void setJobEventListener(int idx, JobEventListener el) {
        if (idx >= TOTAL_JOB) {
            Log.d(TAG, "Setting JobEventListener for job " + idx + " failed, no such index.");
        } else {
            mJobList[idx].setJobEventListener(el);
        }
    }

    static class FGOJob {
        private String mJobName;
        private int mJobIndex;
        private boolean mShouldJobRunning;

        FGOJob(String name, int idx) {
            mJobIndex = idx;
            mJobName = name;
            mShouldJobRunning = false;
        }

        public String getJobName() {
            return mJobName;
        }

        public int getJobIndex() {
            return mJobIndex;
        }

        public boolean isShouldJobRunning() {
            return mShouldJobRunning;
        }

        public void start() {
            mShouldJobRunning = true;
            Log.d(TAG, "calling super start");
        }

        public void stop() {
            mShouldJobRunning = false;
            Log.d(TAG, "calling super stop");
        }

        public void setExtra(Object object) {
            Log.d(TAG, "calling super setExtra");
        }

        public void setJobEventListener(JobEventListener el) {
            Log.d(TAG, "calling super setJobEventListener");
        }
    }
}
