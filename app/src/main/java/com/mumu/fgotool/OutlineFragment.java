package com.mumu.fgotool;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mumu.fgotool.records.ElectricityRecordHandler;
import com.mumu.fgotool.records.ElectricityRecordParser;
import com.mumu.fgotool.screencapture.PointSelectionActivity;
import com.mumu.fgotool.script.FGOJobHandler;
import com.mumu.fgotool.script.JobEventListener;
import com.mumu.fgotool.utility.Log;
import com.mumu.libjoshgame.JoshGameLibrary;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static android.content.Context.WINDOW_SERVICE;

public class OutlineFragment extends MainFragment implements JobEventListener {
    private static final String TAG = "FGOTool";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private Context mContext;
    private static boolean mFGOFlag = false;
    private FGOJobHandler mFGOJobs;

    // Data Holder
    private ElectricityRecordHandler mRecordHandler;
    private PrivatePackageManager mPPM;

    private Button mKillFGOButton;
    private Button mBackupAccountButton;
    private Button mRemoveAccountButton;
    private Button mInfoButton;
    private Button mRunJoshCmdButton;
    private Button mScreenCaptureButton;
    private TextView mAccountNumText;

    private OnFragmentInteractionListener mListener;
    private String mUpdatedString;
    private final Handler mHandler = new Handler();
    final Runnable mUpdateRunnable = new Runnable() {
        public void run() {
            updateView();
        }
    };

    public OutlineFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MoneyFragment.
     */
    public static OutlineFragment newInstance(String param1, String param2) {
        OutlineFragment fragment = new OutlineFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        mPPM = PrivatePackageManager.getInstance();
        mPPM.init(mContext.getPackageManager());
        mFGOJobs = FGOJobHandler.getHandler();
        mFGOJobs.setJobEventListener(FGOJobHandler.AUTO_TRAVERSE_JOB, this);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        prepareGL();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ontline, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onFabClick(View view) {
        Log.d(TAG, "Fab click from outline");
        final Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            final MainActivity deskClockActivity = (MainActivity) activity;
            deskClockActivity.showSnackBarMessage("Test for outline");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        prepareView(view);
        prepareData();
        updateView();
    }

    @Override
    public void onDetailClick() {
        Log.d(TAG, "Detail click on electricity fragment");
        showBottomSheet();
    }

    private void prepareGL() {
        int w, h;

        WindowManager wm = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Log.d(TAG, "Display size = " + size.toString());

        // we always treat the short edge as width
        // TODO: we need to find a new way to get actual panel width and height
        if (size.x > size.y) {
            w = size.y;
            if (size.x > 2000)
                h = 2160;
            else
                h = size.x;
        } else {
            w = size.x;
            if (size.y > 2000)
                h = 2160;
            else
                h = size.y;
        }

        JoshGameLibrary.getInstance().setScreenDimension(w, h);
    }

    private void prepareView(View view) {
        mBackupAccountButton = (Button) view.findViewById(R.id.button_backup_account);
        mRemoveAccountButton = (Button) view.findViewById(R.id.button_remove_account);
        mInfoButton = (Button) view.findViewById(R.id.button_refresh);
        mRunJoshCmdButton = (Button) view.findViewById(R.id.button_test_game);
        mAccountNumText = (TextView) view.findViewById(R.id.textViewAccountNum);
        mScreenCaptureButton = (Button) view.findViewById(R.id.button_screenshot);

        mBackupAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddDialog();
            }
        });

        mRemoveAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.outline_remove_fgo_title)
                        .setMessage(R.string.outline_remove_fgo_msg)
                        .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    mPPM.moveData("com.aniplex.fategrandorder", "delete:account");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })
                        .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        });
        mRemoveAccountButton.setBackgroundColor(Color.RED);

        mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomSheet();
            }
        });

        mRunJoshCmdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runAutoLoginRoutine();
            }
        });

        mScreenCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(getMainActivity(), PointSelectionActivity.class);
                startActivity(intent);
            }
        });
    }

    private void prepareData() {
        mRecordHandler = ElectricityRecordHandler.getHandler();
        mRecordHandler.initOnce(getActivity().getResources(), getActivity().getFilesDir().getAbsolutePath());
    }

    /*
     * updateView will be called when mUpdateRunnable is triggered
     */
    private void updateView() {
        String accountNumText = getString(R.string.outline_account_num);
        String currentProgressText = getString(R.string.outline_current_progress);
        accountNumText = accountNumText + " " + mRecordHandler.getCount();
        currentProgressText = currentProgressText + " " + mUpdatedString;

        mAccountNumText.setText(accountNumText);

        if (mFGOFlag)
            mRunJoshCmdButton.setText(currentProgressText);
        else
            mRunJoshCmdButton.setText(R.string.outline_start_auto_traverse);
    }

    private void showBottomSheet() {
        ElectricityBottomSheet ebs = new ElectricityBottomSheet();
        ebs.show(getFragmentManager(), ebs.getTag());
    }

    private void runAutoLoginRoutine() {
        if (!mFGOFlag) {
            mFGOJobs.setExtra(FGOJobHandler.AUTO_TRAVERSE_JOB, ElectricityRecordHandler.getHandler());
            mFGOJobs.startJob(FGOJobHandler.AUTO_TRAVERSE_JOB);
            mRunJoshCmdButton.setText(R.string.outline_stop_auto_traverse);
        } else {
            mFGOJobs.stopJob(FGOJobHandler.AUTO_TRAVERSE_JOB);
            mRunJoshCmdButton.setText(R.string.outline_start_auto_traverse);
        }

        mFGOFlag = !mFGOFlag;
    }

    @Override
    public void onEventReceived(String msg, Object extra) {
        Log.d(TAG, "Message Received " + msg);
        mUpdatedString = msg;
        mHandler.post(mUpdateRunnable);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    void createNewAccountFolder(String folderName) {
        String baseFileName = mContext.getFilesDir().getAbsolutePath() +
                File.separator + folderName;
        File folderBase = new File(baseFileName);
        File folderFiles = new File(baseFileName + "/files");
        File folderPrefs = new File(baseFileName + "/shared_prefs");

        if (!folderBase.exists()) {
            if (!folderBase.mkdirs())
                Log.d(TAG, "folder " + baseFileName + " create fail");
        }

        if (!folderFiles.exists()) {
            if (!folderFiles.mkdirs())
                Log.d(TAG, "folder " + baseFileName + "/files create fail");
        }

        if (!folderPrefs.exists()) {
            if (!folderPrefs.mkdirs())
                Log.d(TAG, "folder " + baseFileName + "/shared_prefs create fail");
        }
    }

    /*
     *  Add electricity
     */
    private void showAddDialog() {
        new MaterialDialog.Builder(getContext())
                .title(getString(R.string.electric_add))
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input(getString(R.string.electric_add_field_holder), mRecordHandler.getTitle(0), new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        Log.d(TAG, "Get input " + input);
                        try {
                            String nextSerial = mRecordHandler.getNextSerial();
                            addNewRecordFromUser("account" + nextSerial, "NOW", input.toString());
                            updateView();
                            createNewAccountFolder("account" + nextSerial);
                            mPPM.moveData("com.aniplex.fategrandorder", "backupAll:com.mumu.fgotool/files/" + "account" + nextSerial);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }).negativeText(getString(R.string.electric_add_cancel)).show();
    }

    private int addNewRecordFromUser(String record, String date, String title) {
        String targetDate;

        if (date.equals("NOW")) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
            targetDate = df.format(Calendar.getInstance().getTime());
        } else {
            targetDate = date;
        }

        try {
            mRecordHandler.addRecord(new ElectricityRecordParser.Entry(mRecordHandler.getNextSerial(), targetDate, record, title));
            mRecordHandler.refreshFromFile();
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Fail to add record " + e.getMessage());
        }

        return -1;
    }
}
