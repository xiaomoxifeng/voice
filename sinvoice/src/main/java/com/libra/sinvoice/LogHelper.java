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

import android.util.Log;

public class LogHelper {
    private static final String ROOT_TAG = "SinVoice";

    public static final int d(String classTag, String privateTag, String msg) {
        return Log.d(String.format("%s %s %s", ROOT_TAG, classTag, privateTag), msg);
    }

    public static final int d(String classTag, String msg) {
        return d(classTag, "", getLogInfo(msg));
    }

    public static final int i(String classTag, String privateTag, String msg) {
        return Log.i(String.format("%s %s %s", ROOT_TAG, classTag, privateTag), msg);
    }

    public static final int i(String classTag, String msg) {
        return i(classTag, "", getLogInfo(msg));
    }

    public static final int e(String classTag, String privateTag, String msg) {
        return Log.e(String.format("%s %s %s", ROOT_TAG, classTag, privateTag), msg);
    }

    public static final int e(String classTag, String msg) {
        return e(classTag, "", getLogInfo(msg));
    }

    public static final int v(String classTag, String privateTag, String msg) {
        return Log.v(String.format("%s %s %s", ROOT_TAG, classTag, privateTag), msg);
    }

    public static final int v(String classTag, String msg) {
        return v(classTag, "", getLogInfo(msg));
    }

    private static final String getLogInfo(String msg) {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[2];
        return String.format("File:%s, Function:%s, Line:%d, ThreadId:%d, %s", traceElement.getFileName(), traceElement.getMethodName(), traceElement.getLineNumber(), Thread.currentThread().getId(), msg);
    }
}
