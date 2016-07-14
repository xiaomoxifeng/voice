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

public class BufferData {
    public byte mData[];

    private int mFilledSize;
    private int mMaxBufferSize;

    public BufferData(int maxBufferSize) {
        mMaxBufferSize = maxBufferSize;
        reset();

        if (maxBufferSize > 0) {
            mMaxBufferSize = maxBufferSize;
            mData = new byte[mMaxBufferSize];
        } else {
            mData = null;
        }
    }

    final public void reset() {
        mFilledSize = 0;
    }

    final public int getMaxBufferSize() {
        return mMaxBufferSize;
    }

    final public void setFilledSize(int size) {
        mFilledSize = size;
    }

    final public int getFilledSize() {
        return mFilledSize;
    }
}
