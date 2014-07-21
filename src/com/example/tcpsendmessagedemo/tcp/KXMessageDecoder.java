package com.example.tcpsendmessagedemo.tcp;

import android.util.Log;

import com.example.tcpsendmessagedemo.BuildConfig;
import com.example.tcpsendmessagedemo.util.Util;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class KXMessageDecoder extends CumulativeProtocolDecoder {
    private static final int PACKAGE_HEADER_LENGTH = 6;
    public static final String TAG = "KXMessageDecoder";

    @Override
    protected boolean doDecode(IoSession arg0, IoBuffer in,
                               ProtocolDecoderOutput out) throws Exception {

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "doDecode: Json");
        }

        if (in.remaining() < PACKAGE_HEADER_LENGTH) {
            return false;
        }

        byte[] bytes = new byte[6];
        in.mark();
        in.get(bytes);

        byte[] lenBytes = new byte[4];
        System.arraycopy(bytes, 2, lenBytes, 0, 4);
        int bodyLen = byteArray2int(lenBytes);
        if (bodyLen <= 0) {
            return true;
        }
        if (bodyLen > in.remaining()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, " Data Not enough");
            }

            in.reset();
            return false;
        }

        byte[] bodyBytes = new byte[bodyLen];

        in.get(bodyBytes, 0, bodyLen);

        int pos = 0;
        HashMap<String, Object> bodyMap = new HashMap<String, Object>();
        while (pos < bodyLen) {
            pos = parseJsonMember(bodyBytes, bodyMap, pos);
        }
        out.write(new JSONObject(bodyMap));
        if (BuildConfig.DEBUG) {
            Log.d(TAG, " complete receive data");
        }

        if (in.remaining() > 0) {
            return true;
        }
        return false;
    }

    /**
     * @param bytes
     * @param outHashMap
     * @param pos
     * @return
     * @throws Exception
     */
    private int parseJsonMember(byte[] bytes,
                                HashMap<String, Object> outHashMap, int pos) throws Exception {

        byte jsonKey = bytes[pos++];
        int jsonLen = byteArray2int(new byte[]{bytes[pos++], bytes[pos++], bytes[pos++], bytes[pos++]});

        String jsonString;
        if (jsonLen > 0) {
            byte[] jsonValue = new byte[jsonLen];
            System.arraycopy(bytes, pos, jsonValue, 0, jsonLen);
            char[] bodyStr = Util.getChars(jsonValue);
            jsonString = new String(bodyStr);
            jsonString = jsonString.replace("\u0000", "");
        } else {
            jsonString = "";
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "receive json: " + jsonString);
        }
        switch (jsonKey) {
            case 'A':
                break;
            case 'B':
                JSONObject cmdjson = null;
                try {
                    cmdjson = new JSONObject(jsonString);
                    Iterator<?> keys = cmdjson.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        try {
                            outHashMap.put(key, cmdjson.get(key));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }

                break;
            case 'C':
                outHashMap.put("SS_USERS", jsonString);
                break;
            default:
                throw new Exception();
        }
        return pos + jsonLen;
    }

    /**
     * @param b
     * @return
     */
    public static short byteToShort(byte[] b) {
        short s = 0;
        short s0 = (short) (b[0] & 0xff);
        short s1 = (short) (b[1] & 0xff);
        s1 <<= 8;
        s = (short) (s0 | s1);
        return s;
    }

    /**
     * 将4字节的byte数组转成一个int值
     *
     * @param b
     * @return
     */
    public static int byteArray2int(byte[] b) {
        byte[] a = new byte[4];
        int i = 0, j = 0;
        for (; i < b.length; i++, j++) {//从b的尾部(即int值的低位)开始copy数据
            if (j < b.length)
                a[i] = b[j];
            else
                a[i] = 0;//如果b.length不足4,则将高位补0
        }
        int v0 = (a[3] & 0xff) << 24;//&0xff将byte值无差异转成int,避免Java自动类型提升后,会保留高位的符号位
        int v1 = (a[2] & 0xff) << 16;
        int v2 = (a[1] & 0xff) << 8;
        int v3 = (a[0] & 0xff);
        return v0 + v1 + v2 + v3;
    }

    public static byte[] intToByteArray1(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }

}
