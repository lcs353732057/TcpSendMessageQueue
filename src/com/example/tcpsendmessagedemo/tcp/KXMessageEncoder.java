package com.example.tcpsendmessagedemo.tcp;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class KXMessageEncoder extends ProtocolEncoderAdapter {

	@Override
	public void encode(IoSession arg0, Object arg1, ProtocolEncoderOutput arg2)
			throws Exception {
		byte[] bytes = (byte[]) arg1;

		IoBuffer buffer = IoBuffer.allocate(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		arg2.write(buffer);
	}
}
