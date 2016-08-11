package com.shopgun.android.sdk.log;

import com.shopgun.android.utils.log.Logger;
import com.shopgun.android.utils.log.QuietLogger;

public class SgnLog {

    private static volatile Logger mLogger = new QuietLogger();

    public static void setLogger(Logger logger) {
        mLogger = logger == null ? new QuietLogger() : logger;
    }

    public static int v(String tag, String msg) {
        return mLogger.v(tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return mLogger.v(tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        return mLogger.d(tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return mLogger.d(tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        return mLogger.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return mLogger.i(tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        return mLogger.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return mLogger.w(tag, msg, tr);
    }

    public static int e(String tag, String msg) {
        return mLogger.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return mLogger.e(tag, msg, tr);
    }

}
