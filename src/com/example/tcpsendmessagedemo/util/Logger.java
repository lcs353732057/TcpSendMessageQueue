package com.example.tcpsendmessagedemo.util;

import android.util.Log;

/**
 * Utility log tool.
 * 
 * @author raoaming
 * 
 */
public class Logger {

	private final static String tag = "KXShow_GoddessPlan";
	public static int logLevel = Log.DEBUG;

	private static boolean logFlag = true;

	private static Logger logger = new Logger();

	public static Logger getLogger() {
		return logger;
	}

	private Logger() {
	}

	private String getFunctionName() {
		StackTraceElement[] sts = Thread.currentThread().getStackTrace();

		if (sts == null) {
			return null;
		}

		for (StackTraceElement st : sts) {
			if (st.isNativeMethod()) {
				continue;
			}

			if (st.getClassName().equals(Thread.class.getName())) {
				continue;
			}

			if (st.getClassName().equals(this.getClass().getName())) {
				continue;
			}

			return "[ " + Thread.currentThread().getName() + ": "
					+ st.getFileName() + ":" + st.getLineNumber() + " ]";
		}

		return null;
	}

	public void i(Object str) {
		if (!logFlag)
			return;
		if (logLevel <= Log.INFO) {
			String name = getFunctionName();
			if (name != null) {
				Log.i(tag, name + " - " + str);
			} else {
				Log.i(tag, str.toString());
			}
		}
	}

	public void v(Object str) {
		if (!logFlag)
			return;
		if (logLevel <= Log.VERBOSE) {
			String name = getFunctionName();
			if (name != null) {
				Log.v(tag, name + " - " + str);
			} else {
				Log.v(tag, str.toString());
			}

		}
	}

	public void w(Object str) {
		if (!logFlag)
			return;
		if (logLevel <= Log.WARN) {
			String name = getFunctionName();

			if (name != null) {
				Log.w(tag, name + " - " + str);
			} else {
				Log.w(tag, str.toString());
			}

		}
	}

	public void e(Object str) {
		if (!logFlag)
			return;
		if (logLevel <= Log.ERROR) {

			String name = getFunctionName();
			if (name != null) {
				Log.e(tag, name + " - " + str);
			} else {
				Log.e(tag, str.toString());
			}
		}
	}

	public void e(Exception ex) {
		if (!logFlag)
			return;
		if (logLevel <= Log.ERROR) {
			Log.e(tag, "error", ex);
		}
	}

	public void d(Object str) {
		if (!logFlag)
			return;
		if (logLevel <= Log.DEBUG) {
			String name = getFunctionName();
			if (name != null) {
				Log.d(tag, name + " - " + str);
			} else {
				Log.d(tag, str.toString());
			}

		}
	}
}
