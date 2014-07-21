package com.example.tcpsendmessagedemo.tcp;


import com.example.tcpsendmessagedemo.util.Unsigned;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Administrator on 14-1-9.
 */
public class SocketPackageUtil {
    /**
     * @param jsonkey
     * @param jsonstr
     * @return
     */
    public static ByteBuffer encodePacketBody(Byte jsonkey, String jsonstr) {
        int lenJson = jsonstr.getBytes().length;
        int len = lenJson + 5;
        ByteBuffer buffer = ByteBuffer.allocate(len);
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(jsonkey);
        Unsigned.putUnsignedInt(buffer, lenJson);
        buffer.put(jsonstr.getBytes());
        buffer.flip();
        return buffer;
    }

    /**
     * @param msgid
     * @param bodylenth
     * @return
     */
    public static ByteBuffer encodePackageHead(int msgid, int bodylenth) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        Unsigned.putUnsignedShort(buffer, msgid);
        Unsigned.putUnsignedInt(buffer, bodylenth);
        buffer.flip();
        return buffer;
    }
}
