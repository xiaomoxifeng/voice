/*
 * Copyright (C) 2015 Jesse Gu
 * 未经作者许可，禁止将该程序用于商业用途
 * 
 *************************************************************************
 **                   作者信息                                            **
 *************************************************************************
 ** Email: sinvoice@126.com                                             **
 ** QQ   : 29600731                                                     **
 ** Home: http://www.sinvoice.com或者http://112.74.216.30                **
 *************************************************************************
 */
package com.libra.sinvoice;

public class Queue {
    private final static String TAG = "DataQueue";

    private final static int STATE_reset = 1;
    private final static int STATE_set = 2;

    private final int mResCount;
    private BufferData[] mBufferData;

    private int mState;

    private int mStartIndex;
    private int mEndIndex;
    private int mCount;

    public Queue(int resCount) {
        mResCount = resCount;

        mState = STATE_reset;

        if (mResCount > 0) {
            mBufferData = new BufferData[mResCount];
        }

        mCount = 0;
        mStartIndex = 0;
        mEndIndex = 0;
    }

    public synchronized void reset() {
        if (STATE_set == mState) {
            mState = STATE_reset;

            mStartIndex = 0;
            mEndIndex = 0;
            mCount = 0;

            this.notifyAll();
            LogHelper.d(TAG, "gujicheng reset ok");
        } else {
            LogHelper.d(TAG, "already reseted");
        }
    }

    public synchronized void set(BufferData[] data) {
        if (STATE_reset == mState) {
            mState = STATE_set;
            if (null != data) {
                mCount = data.length;

                if ( mCount > mResCount ) {
                    mCount = mResCount;
                }
                for (int i = 0; i < mCount; ++i) {
                    mBufferData[i] = data[i];
                }
            } else {
                mCount = 0;
            }

            LogHelper.d(TAG, "set ok");
        } else {
            LogHelper.d(TAG, "already seted");
        }
    }

    final public int getCount() {
        return mCount;
    }

    public synchronized BufferData getBuffer() {
        BufferData ret = null;

        if (STATE_set == mState) {
            if (mCount <= 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
                if (STATE_reset == mState) {
                    LogHelper.d(TAG, "getBuffer, after waiing, state is reset");
                    return null;
                }
            }

            if ( mCount > 0 ) {
                ret = mBufferData[mStartIndex++];
                if (mStartIndex >= mResCount) {
                    mStartIndex = 0;
                }

                --mCount;
                if ( mCount + 1 == mResCount ) {
                    this.notify();
                }
            } else {
                LogHelper.e(TAG, "getBuffer error mCount:" + mCount);
            }
        } else {
            LogHelper.d(TAG, "getBuffer, state is reset");
        }
        return ret;
    }

    public synchronized boolean putBuffer(BufferData data) {
        boolean ret = false;
        if (STATE_set == mState ) {
            if ( mCount == mResCount ) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
                if (STATE_reset == mState) {
                    LogHelper.d(TAG, "putBuffer, after waiing, state is reset");
                    return false;
                }
            }

            if (mCount < mResCount) {
                mBufferData[mEndIndex++] = data;
                if (mEndIndex >= mResCount) {
                    mEndIndex = 0;
                }

                ++mCount;
                if (0 == mCount - 1) {
                    this.notify();
                }

                ret = true;
            } else {
                LogHelper.e(TAG, "putBuffer error mCount:" + mCount);
            }
        } else {
            LogHelper.d(TAG, "putBuffer, state is reset or data is null");
        }
        return ret;
    }
}
