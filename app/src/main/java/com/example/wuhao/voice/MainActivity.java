package com.example.wuhao.voice;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.libra.sinvoice.Common;
import com.libra.sinvoice.LogHelper;
import com.libra.sinvoice.Prosumer;
import com.libra.sinvoice.XSVPlayer;
import com.libra.sinvoice.XSVRecognition;

public class MainActivity extends Activity implements XSVRecognition.Listener,
        XSVPlayer.Listener {

    private final static String TAG = "MainActivityxx";
    private final static int MSG_SET_RECG_TEXT = 1;
    private final static int MSG_RECG_START = 2;
    private final static int MSG_RECG_END = 3;
    private final static int MSG_PLAY_TEXT = 4;

    // private final static int[] TOKENS = null;
    // private final static String TOKENS_str = null;
    // private final static int TOKEN_LEN = 10;
    private final static int[] TOKENS = { 32, 32, 32, 32, 32, 32 };
    private final static String TOKENS_str = "Beeba20141";
    private final static int TOKEN_LEN = TOKENS.length;

    private final static String BAKCUP_LOG_PATH = "/sinvoice_backup";

    private Handler mHanlder;
    private XSVPlayer mSinVoicePlayer;
    private XSVRecognition mRecognition;
    private boolean mIsReadFromFile;
    private String mSdcardPath;
    private PowerManager.WakeLock mWakeLock;
    private EditText mPlayTextView;
    private TextView mRecognisedTextView;
    // private TextView mRegState;
    private String mPlayText;
    private char mRecgs[] = new char[100];
    private int mRecgCount;
    private int mSoundEffectIndex = 0;
    private int mSoundEffectCount = 0;

    static {
        System.loadLibrary("sinvoice");
        LogHelper.d(TAG, "sinvoice jnicall loadlibrary");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIsReadFromFile = false;

        try {
            String[] ll = getAssets().list("sound_effect");
            mSoundEffectCount = ll.length;
            LogHelper.d(TAG, "mSoundEffectCount:" + mSoundEffectCount);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);

        mSdcardPath = Environment.getExternalStorageDirectory().getPath();

        mSinVoicePlayer = new XSVPlayer();
        mSinVoicePlayer.init(this);
        mSinVoicePlayer.setListener(this);

        mRecognition = new XSVRecognition();
        mRecognition.init(this);
        mRecognition.setListener(this);

        mPlayTextView = (EditText) findViewById(R.id.playtext);
        mPlayTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mRecognisedTextView = (TextView) findViewById(R.id.regtext);
        mHanlder = new RegHandler(this);

        Button nextSoundEffect = (Button) findViewById(R.id.next_sound_effect);
        nextSoundEffect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                ++mSoundEffectIndex;
                if ( mSoundEffectIndex > mSoundEffectCount ) {
                    mSoundEffectIndex = 0;
                }
            }
        });

        Button playStart = (Button) findViewById(R.id.start_play);
        playStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    byte[] strs = mPlayTextView.getText().toString()
                            .getBytes("UTF8");
                    if (null != strs) {
                        int len = strs.length;
                        int[] tokens = new int[len];
                        for (int i = 0; i < len; ++i) {
                            // tokens[i] = strs[i];
                            String xx = mPlayTextView.getText().toString();
                            tokens[i] = Common.DEFAULT_CODE_BOOK.indexOf(xx
                                    .charAt(i));
                            // tokens[i] =
                            // Common.DEFAULT_CODE_BOOK.indexOf(TOKENS_str.charAt(i));
                        }
                        mSinVoicePlayer.play(tokens, len, false, 2000, mSoundEffectIndex);
                    } else {
                        mSinVoicePlayer.play(TOKENS, TOKEN_LEN, false, 2000, mSoundEffectIndex);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        Button playStop = (Button) findViewById(R.id.stop_play);
        playStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mSinVoicePlayer.stop();
            }
        });

        Button recognitionStart = (Button) findViewById(R.id.start_reg);
        recognitionStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRecognition.start(mIsReadFromFile, TOKEN_LEN);
            }
        });

        Button recognitionStop = (Button) findViewById(R.id.stop_reg);
        recognitionStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mRecognition.stop();
            }
        });

        CheckBox cbReadFromFile = (CheckBox) findViewById(R.id.fread_from_file);
        cbReadFromFile
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton arg0,
                                                 boolean isChecked) {
                        mIsReadFromFile = isChecked;
                    }
                });

        Button btBackup = (Button) findViewById(R.id.back_debug_info);
        btBackup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                backup();
            }
        });

        Button btClearBackup = (Button) findViewById(R.id.clear_debug_info);
        btClearBackup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("information")
                        .setMessage("Sure to clear?")
                        .setPositiveButton("yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                        clearBackup();
                                    }
                                })
                        .setNegativeButton("no",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int whichButton) {
                                    }
                                }).show();
            }
        });

        Button btNextEffect = (Button) findViewById(R.id.next_mix);
        btNextEffect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mSinVoicePlayer.stop();
                mSinVoicePlayer.uninit();
                mSinVoicePlayer.init(MainActivity.this);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        mWakeLock.acquire();
    }

    @Override
    public void onPause() {
        super.onPause();

        mWakeLock.release();

        mSinVoicePlayer.stop();
        mRecognition.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mRecognition.uninit();
        mSinVoicePlayer.uninit();
    }

    private void clearBackup() {
        delete(new File(mSdcardPath + BAKCUP_LOG_PATH));

        Toast.makeText(this, "clear backup log info successful",
                Toast.LENGTH_SHORT).show();
    }

    private static void delete(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }

        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (int i = 0; i < childFiles.length; i++) {
                delete(childFiles[i]);
            }
            file.delete();
        }
    }

    private void backup() {
        mRecognition.stop();

        String timestamp = getTimestamp();
        String destPath = mSdcardPath + BAKCUP_LOG_PATH + "/back_" + timestamp;
        try {
            copyDirectiory(destPath, mSdcardPath + "/sinvoice");
            copyDirectiory(destPath, mSdcardPath + "/sinvoice_log");

            FileOutputStream fout = new FileOutputStream(destPath + "/text.txt");

            String str = mPlayTextView.getText().toString();
            byte[] bytes = str.getBytes();
            fout.write(bytes);

            str = mRecognisedTextView.getText().toString();
            bytes = str.getBytes();
            fout.write(bytes);

            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "backup log info successful", Toast.LENGTH_SHORT)
                .show();
    }

    private static class RegHandler extends Handler {
        private StringBuilder mTextBuilder = new StringBuilder();
        private MainActivity mAct;

        public RegHandler(MainActivity act) {
            mAct = act;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_RECG_TEXT:
                    char ch = (char) msg.arg1;
                    // mTextBuilder.append(ch);
                    mAct.mRecgs[mAct.mRecgCount++] = ch;
                    break;

                case MSG_RECG_START:
                    // mTextBuilder.delete(0, mTextBuilder.length());
                    mAct.mRecgCount = 0;
                    break;

                case MSG_RECG_END:
                    LogHelper.d(TAG, "recognition end gIsError:" + msg.arg1);
                    if (mAct.mRecgCount > 0) {
                        byte[] strs = new byte[mAct.mRecgCount];
                        for (int i = 0; i < mAct.mRecgCount; ++i) {
                            strs[i] = (byte) mAct.mRecgs[i];
                        }
                        try {
                            String strReg = new String(strs, "UTF8");
                            if (msg.arg1 >= 0) {
                                Log.d(TAG, "reg ok!!!!!!!!!!!!");
                                if (null != mAct) {
                                    mAct.mRecognisedTextView.setText(strReg);
                                    // mAct.mRegState.setText("reg ok(" + msg.arg1 +
                                    // ")");
                                }
                            } else {
                                Log.d(TAG, "reg error!!!!!!!!!!!!!");
                                mAct.mRecognisedTextView.setText(strReg);
                                // mAct.mRegState.setText("reg err(" + msg.arg1 +
                                // ")");
                                // mAct.mRegState.setText("reg err");
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case MSG_PLAY_TEXT:
                    // mAct.mPlayTextView.setText(mAct.mPlayText);
                    break;
            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void onSinVoiceRecognitionStart(Prosumer self) {
        mHanlder.sendEmptyMessage(MSG_RECG_START);
    }

    @Override
    public void onSinVoiceRecognition(Prosumer self, char ch) {
        mHanlder.sendMessage(mHanlder.obtainMessage(MSG_SET_RECG_TEXT, ch, 0));
    }

    @Override
    public void onSinVoiceRecognitionEnd(Prosumer self, int result) {
        mHanlder.sendMessage(mHanlder.obtainMessage(MSG_RECG_END, result, 0));
    }

    @Override
    public void onProdumerStart(Prosumer self) {
    }

    @Override
    public void onProdumerStop(Prosumer self) {
    }

    @Override
    public void onSinToken(Prosumer self, int[] tokens) {
    }

    private static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        return sdf.format(new Date());
    }

    private static void copyFile(File targetFile, File sourceFile)
            throws IOException {
        FileInputStream input = new FileInputStream(sourceFile);
        BufferedInputStream inBuff = new BufferedInputStream(input);

        FileOutputStream output = new FileOutputStream(targetFile);
        BufferedOutputStream outBuff = new BufferedOutputStream(output);

        byte[] b = new byte[1024 * 5];
        int len;
        while ((len = inBuff.read(b)) != -1) {
            outBuff.write(b, 0, len);
        }
        outBuff.flush();

        inBuff.close();
        outBuff.close();
        output.close();
        input.close();
    }

    private static void copyDirectiory(String targetDir, String sourceDir)
            throws IOException {
        (new File(targetDir)).mkdirs();
        File[] file = (new File(sourceDir)).listFiles();
        if (null != file) {
            for (int i = 0; i < file.length; i++) {
                if (file[i].isFile()) {
                    File sourceFile = file[i];
                    File targetFile = new File(
                            new File(targetDir).getAbsolutePath()
                                    + File.separator + file[i].getName());
                    copyFile(targetFile, sourceFile);
                }
                if (file[i].isDirectory()) {
                    String srcPath = sourceDir + "/" + file[i].getName();
                    String targetPath = targetDir + "/" + file[i].getName();
                    copyDirectiory(targetPath, srcPath);
                }
            }
        }
    }

}
