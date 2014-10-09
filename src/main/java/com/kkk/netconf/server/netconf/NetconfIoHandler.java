package com.kkk.netconf.server.netconf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kkk.netconf.server.BehaviourContainer;
import com.kkk.netconf.server.MessageStore;
import com.kkk.netconf.server.Server;

public class NetconfIoHandler extends IoHandlerAdapter {

    private MessageStore messageStore = null;
    private BehaviourContainer behaviourContainer = null;
    private AbstractNetconfProcessor netconfProcessor;

    private static final Logger log = LoggerFactory.getLogger(IoHandlerAdapter.class);
    private Charset charset = Server.CHARSET;
    private CharsetEncoder charsetEncoder = charset.newEncoder();
    private CharsetDecoder charsetDecoder = charset.newDecoder();

    public NetconfIoHandler(MessageStore messageStore, BehaviourContainer behaviourContainer) {
	this.messageStore = messageStore;
	this.behaviourContainer = behaviourContainer;
    }

    public void sessionCreated(IoSession session) throws Exception {
	log.info("netconf sim session created:" + session.getRemoteAddress().toString());
	netconfProcessor = new TcpipForwardNetconfProcessor(session);
    }

    public void sessionOpened(IoSession session) throws Exception {
	log.info("netconf sim session opened");
	try {
	    netconfProcessor.sendHello();
	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
    }

    public void sessionClosed(IoSession session) throws Exception {
	// Empty handler
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
	// Empty handler
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
	if (log.isWarnEnabled()) {
	    log.warn("EXCEPTION, please implement " + getClass().getName() + ".exceptionCaught() for proper handling:",
		    cause);
	}
    }

    //the first msg:
    //<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" type="cms" vers="13.01.117" local-ip-addr="10.245.15.151"/><rpc message-id="1" nodename="NTWK-kkktest"><get-config><source><running/></source><filter type="subtree"><top><object><type>System</type><id/><children><type>CraftUser</type><attr-list>name level passwd admin prev-passwd</attr-list></children></object></top></filter></get-config></rpc>
    public void messageReceived(IoSession session, Object message) throws Exception {
	log.trace("******received data from client[netconf sim]:" + message.toString().trim());
	netconfProcessor.processCms(message.toString().trim());
//	session.write(message.toString().trim());
    }

    public void messageSent(IoSession session, Object message) throws Exception {
	log.trace("******sent data to client[netconf sim]:" + message.toString());
    }

}
