package com.mumu.fgotool;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mumu.fgotool.utility.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.BufferUnderflowException;

public class OutlineFragment extends MainFragment {
    private static final String TAG = "ProjectLI";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;
    private Context mContext;

    private Button mKillFGOButton;
    private Button mBackupAccountButton;
    private Button mRemoveAccountButton;
    private Button mInfoButton;

    private OnFragmentInteractionListener mListener;

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

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
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
    public void onDetailClick() {
        Log.d(TAG, "on detail click on outline");
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mBackupAccountButton = (Button) view.findViewById(R.id.button_backup_account);
        mRemoveAccountButton = (Button) view.findViewById(R.id.button_remove_account);
        mInfoButton = (Button) view.findViewById(R.id.button_refresh);

        mBackupAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.outline_backup_fgo_title)
                        .setMessage(R.string.outline_backup_fgo_msg)
                        .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    String thisAccount = "account2";
                                    createNewAccountFolder(thisAccount);
                                    new ApplicationManager(mContext).callJosh("com.aniplex.fategrandorder", "b:com.mumu.fgotool/files/" + thisAccount);
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
                                    new ApplicationManager(mContext).callJosh("com.aniplex.fategrandorder", "d:account");
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

        mInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.outline_backup_fgo_title)
                        .setMessage(R.string.outline_backup_fgo_msg)
                        .setPositiveButton(R.string.action_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    String thisAccount = "account2";
                                    createNewAccountFolder(thisAccount);
                                    new ApplicationManager(mContext).callJosh("com.aniplex.fategrandorder", "restore:com.mumu.fgotool/files/" + thisAccount);
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
}
