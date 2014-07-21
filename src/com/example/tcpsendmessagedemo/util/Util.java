package com.example.tcpsendmessagedemo.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by Administrator on 2014/7/4.
 */
public class Util {
    public static char[] getChars(byte[] bytes) {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);

        return cb.array();
    }

    // Check if we are online
    public static boolean isNetworkAvailable(Context cxt) {
        ConnectivityManager mConnMan = (ConnectivityManager) cxt
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mConnMan.getActiveNetworkInfo();
        if (info == null) {
            return false;
        }
        return info.isConnected();
    }
}
