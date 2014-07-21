package com.example.tcpsendmessagedemo.tcp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Administrator on 13-7-23.
 */
public class KeepAlivePacket {
    private static final int PACKAGE_HEADER_LENGTH = 6;

    public static byte[] encodeKeepAlivePacket(int msgId) {

        //发送心跳，无body部分
        int bodyLength = 0;

        ByteBuffer buf = ByteBuffer.allocate(PACKAGE_HEADER_LENGTH + bodyLength);
        buf = buf.order(ByteOrder.LITTLE_ENDIAN);

        //head
        ByteBuffer bufHead = SocketPackageUtil.encodePackageHead(msgId, bodyLength);
        buf.put(bufHead);

        byte[] keepAlivePackage = buf.array();
        return keepAlivePackage;
    }
}
