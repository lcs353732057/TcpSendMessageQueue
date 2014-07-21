package com.example.tcpsendmessagedemo.tcp;


import com.example.tcpsendmessagedemo.util.Const;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class TokenPacket {
    public static final String UID = "uid";

    public static final String TIMESTAMP = "timestamp";

    public static final String SIGN = "sign";

    private static final int PACKAGE_HEADER_LENGTH = 6;

    public static final String CMD = "MSG_CONFIRM";

    public static final String USER_MSG = "USER_MSG";

    public static final String GROUP_MSG = "GROUP_MSG";

    public static final int CONFIRM_MSG_ID = 203;

    public static byte[] encodeTokenPacket(int uid, int timestamp, String sign, String token) {

        JSONObject jsonObjectA = new JSONObject();
        try {
            jsonObjectA.put(UID, uid);
            jsonObjectA.put(TIMESTAMP, timestamp);
            jsonObjectA.put(SIGN, sign);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int aLength = jsonObjectA.toString().getBytes().length;

        JSONObject jsonObjectB = new JSONObject();
        try {
            jsonObjectB.put("device_system_name", "android");
            jsonObjectB.put("token", token);
            JSONObject object = new JSONObject();
            jsonObjectB.put("group_list", object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int bLength = jsonObjectB.toString().getBytes().length;

        // key=1,length=2,body=length--->
        int bodyLength = aLength + bLength + 2 * 5;

        ByteBuffer buf = ByteBuffer
                .allocate(PACKAGE_HEADER_LENGTH + bodyLength);
        buf = buf.order(ByteOrder.LITTLE_ENDIAN);

        // head
        ByteBuffer bufHead = SocketPackageUtil.encodePackageHead(1, bodyLength);
        buf.put(bufHead);

        // body,A报文
        ByteBuffer bufBodyA = SocketPackageUtil.encodePacketBody((byte) 'A',
                jsonObjectA.toString());
        buf.put(bufBodyA);

        // body,B报文
        ByteBuffer bufBodyB = SocketPackageUtil
                .encodePacketBody((byte) 'B', jsonObjectB.toString());
        buf.put(bufBodyB);

        byte[] loginPackage = buf.array();
        return loginPackage;
    }

    /**
     * @param msgIds
     * @param uid
     * @param chatType 0:群组；1:私聊
     * @return
     */
    public static byte[] confirmPacket(ArrayList<String> msgIds, int uid, int chatType, int gid) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < msgIds.size(); i++) {
            if (i == msgIds.size() - 1) {
                sb.append(msgIds.get(i));
            } else {
                sb.append(msgIds.get(i) + ",");
            }
        }

        JSONObject jsonObjectB = new JSONObject();
        try {
            jsonObjectB.put("cmd", CMD);
            jsonObjectB.put("uid", uid);
            jsonObjectB.put("chat_type", chatType);
            jsonObjectB.put("msg_id", sb.toString());
            jsonObjectB.put("gid", gid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int bLength = jsonObjectB.toString().getBytes().length;

        // key=1,length=2,body=length--->
        int bodyLength = bLength + 5;

        ByteBuffer buf = ByteBuffer
                .allocate(PACKAGE_HEADER_LENGTH + bodyLength);
        buf = buf.order(ByteOrder.LITTLE_ENDIAN);

        // head
        ByteBuffer bufHead = SocketPackageUtil.encodePackageHead(CONFIRM_MSG_ID, bodyLength);
        buf.put(bufHead);

        // body,B报文
        ByteBuffer bufBodyB = SocketPackageUtil
                .encodePacketBody((byte) 'B', jsonObjectB.toString());
        buf.put(bufBodyB);

        byte[] confirmData = buf.array();
        return confirmData;
    }

    /**
     * 组装发送消息体
     *
     * @param nsMessage
     * @return
     */
    public static byte[] sendMsgPacket(NSMessage nsMessage) {
        JSONObject jsonObjectB = new JSONObject();
        try {
            jsonObjectB.put("cmd", GROUP_MSG);//群聊
            jsonObjectB.put("group_id", nsMessage.getGroupId());
            jsonObjectB.put("msg", nsMessage.getText());
            jsonObjectB.put("style", 0);
            jsonObjectB.put("mymsgid", nsMessage.getLocalId());
            jsonObjectB.put("remark", "");
            jsonObjectB.put("timestamp", nsMessage.getLocalTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        int bLength = jsonObjectB.toString().getBytes().length;

        // key=1,length=2,body=length--->
        int bodyLength = bLength + 5;

        ByteBuffer buf = ByteBuffer
                .allocate(PACKAGE_HEADER_LENGTH + bodyLength);
        buf = buf.order(ByteOrder.LITTLE_ENDIAN);

        // head
        ByteBuffer bufHead = SocketPackageUtil.encodePackageHead(CONFIRM_MSG_ID, bodyLength);
        buf.put(bufHead);

        // body,B报文
        ByteBuffer bufBodyB = SocketPackageUtil
                .encodePacketBody((byte) 'B', jsonObjectB.toString());
        buf.put(bufBodyB);

        byte[] sendData = buf.array();
        return sendData;
    }

    /**
     * 发送消息确认命令
     *
     * @param msgIds
     * @param chatType
     */
    public static void sendConfirmPacket(ArrayList<String> msgIds, int chatType, int gid) {
        TcpClient.getInstance().sendMsg(confirmPacket(msgIds, Const.UID, chatType, gid));
    }

    /**
     * @param msgId
     * @param chatType
     * @param gid
     */
    public static void sendConfirmMsg(int msgId, int chatType, int gid) {
        ArrayList<String> msgIds = new ArrayList<String>();
        msgIds.add(String.valueOf(msgId));
        TokenPacket.sendConfirmPacket(msgIds, chatType, gid);
    }
}
