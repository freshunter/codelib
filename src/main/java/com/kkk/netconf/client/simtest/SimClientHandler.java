package com.kkk.netconf.client.simtest;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimClientHandler extends IoHandlerAdapter {
    private Logger log = LoggerFactory.getLogger(getClass());
    private Charset charset = Charset.forName("UTF-8");
    private CharsetEncoder charsetEncoder = charset.newEncoder();  
    private CharsetDecoder charsetDecoder = charset.newDecoder();
    private static AtomicInteger ai = new AtomicInteger();

    public SimClientHandler() {
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
	String str = "<rpc message-id=\"1\" nodename=\"NTWK-kkktest\"><get-config><source><running/></source><filter type=\"subtree\"><top><object><type>System</type><id/><children><type>CraftUser</type><attr-list>name level passwd admin prev-passwd</attr-list></children></object></top></filter></get-config></rpc>";
//	IoBuffer sent = IoBuffer
//	        .wrap(str
//	                .getBytes());
//	session.write(sent);
	session.write(str);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
	log.error(cause.getMessage(), cause);
	session.close(true);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
//	IoBuffer recv = (IoBuffer) message;
//	IoBuffer sent = IoBuffer.allocate(recv.remaining());
//	String msg = recv.getString(charsetDecoder);
//	log.info("******received msg: " + msg);		
////	log.info("netconf get hex dump:" + recv.getHexDump());
////	log.info("netconf get:" + recv.getString(charsetDecoder));
//	
//	sent.put(msg.getBytes(charset));
//	sent.flip();
//	session.write(sent);

//	log.info("******received msg: " + message.toString());
	ai.getAndIncrement();
	if(ai.get() > 5) {
	    throw new Exception("exit");
	}
	
	session.write(message.toString());

	
    }
    
    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
//	log.info("******sent data: " + message.toString());
    }
}