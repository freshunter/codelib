package com.kkk.netconf.server.ssh;

import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.session.ServerSession;

/**
 * 
 * @author khuang
 *
 */
public class CServerSession extends ServerSession {
    public CServerSession(ServerFactoryManager server, IoSession ioSession) throws Exception {
        super(server, ioSession);
    }
    public void handleMessage(Buffer buffer) throws Exception {
        super.handleMessage(buffer);
    }
}