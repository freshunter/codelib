package com.kkk.netconf.server.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.Map;

import org.apache.sshd.ClientSession;
import org.apache.sshd.client.channel.ChannelDirectTcpip;
import org.apache.sshd.client.future.DefaultOpenFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.Channel;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.channel.ChannelOutputStream;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoConnectFuture;
import org.apache.sshd.common.io.IoConnector;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.BufferUtils;
import org.apache.sshd.common.util.Readable;
import org.apache.sshd.server.channel.AbstractServerChannel;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.channel.OpenChannelException;

import com.kkk.netconf.server.Server;
import com.kkk.netconf.server.netconf.CalixHello;

public class CTcpipServerChannel<K>  extends AbstractServerChannel {

    public static class DirectTcpipFactory implements NamedFactory<Channel> {

        public String getName() {
            return "direct-tcpip";
        }

        public Channel create() {
//            return new ChannelDirectTcpip();
//            return new ChannelSession();
            return new CTcpipServerChannel(Type.Direct);
//            return new CTcpipServerChannel(Type.Forwarded);
        }
    }

    public static class ForwardedTcpipFactory implements NamedFactory<Channel> {

        public String getName() {
            return "forwarded-tcpip";
        }

        public Channel create() {
            return new CTcpipServerChannel(Type.Forwarded);
        }
    }

    private enum Type {
        Direct,
        Forwarded
    }

    private final Type type;
    private IoConnector connector;
    private IoSession ioSession;
    private OutputStream out;

    public CTcpipServerChannel(Type type) {
        this.type = type;
    }

    protected OpenFuture doInit(Buffer buffer) {
        final OpenFuture f = new DefaultOpenFuture(this);

        String hostToConnect = buffer.getString();
        int portToConnect = buffer.getInt();
        String originatorIpAddress = buffer.getString();
        int originatorPort = buffer.getInt();
        log.info("Receiving request for direct tcpip: hostToConnect={}, portToConnect={}, originatorIpAddress={}, originatorPort={}",
                new Object[] { hostToConnect, portToConnect, originatorIpAddress, originatorPort });

        if (portToConnect == Server.getSSHServer().getPort()) {
	        portToConnect += Server.NETCONF_SERVER_PORT_OFFSET;
	        log.info("portToConnect the same as ssh port, add {} changed to {}", Server.NETCONF_SERVER_PORT_OFFSET, portToConnect);
        }
        
        log.info("Start request for direct tcpip: hostToConnect={}, portToConnect={}, originatorIpAddress={}, originatorPort={}",
                new Object[] { hostToConnect, portToConnect, originatorIpAddress, originatorPort });

        SshdSocketAddress address = null;
        switch (type) {
            case Direct:    address = new SshdSocketAddress(hostToConnect, portToConnect); break;
            case Forwarded: address = service.getTcpipForwarder().getForwardedPort(portToConnect); break;
        }
        final ForwardingFilter filter = getSession().getFactoryManager().getTcpipForwardingFilter();
        if (address == null || filter == null || !filter.canConnect(address, getSession())) {
            super.close(true);
            f.setException(new OpenChannelException(SshConstants.SSH_OPEN_ADMINISTRATIVELY_PROHIBITED, "Connection denied"));
            return f;
        }

        // TODO: revisit for better threading. Use async io ?
        out = new ChannelOutputStream(this, remoteWindow, log, SshConstants.SSH_MSG_CHANNEL_DATA);
        IoHandler handler = new IoHandler() {
        	
        	/**
        	 * kkkkkkk
        	 * message received from fowarding address, then send to ssh client. 
        	 * 
        	 * meaning: write data to cms
        	 */
            public void messageReceived(IoSession session, Readable message) throws Exception {
                if (state.get() != OPENED) {
                    log.debug("Ignoring write to channel {} in CLOSING state", id);
                } else {
                    Buffer buffer = new Buffer();
                    buffer.putBuffer(message);
//                    log.info("======receive message from Netconf sim byte:" + buffer.array());
                    String msg = new String(buffer.getCompactData()).trim();
                    log.debug("======Southboundssh:" + msg);

//                    out.write(buffer.array(), buffer.rpos(), buffer.available());
                    out.write(msg.getBytes(Server.CHARSET));
                    out.flush();
                }
            }
            public void sessionCreated(IoSession session) throws Exception {
            }
            public void sessionClosed(IoSession session) throws Exception {
                close(false);
            }
            public void exceptionCaught(IoSession ioSession, Throwable cause) throws Exception {
                close(true);
            }
        };
        connector = getSession().getFactoryManager().getIoServiceFactory()
                .createConnector(handler);
        IoConnectFuture future = connector.connect(address.toInetSocketAddress());
        future.addListener(new SshFutureListener<IoConnectFuture>() {
            public void operationComplete(IoConnectFuture future) {
                if (future.isConnected()) {
                    ioSession = future.getSession();
                    f.setOpened();
                    log.info("======session opened:{}  {}", ioSession.getLocalAddress().toString(), ioSession.getRemoteAddress().toString());
                    
                } else if (future.getException() != null) {
                    closeImmediately0();
                    if (future.getException() instanceof ConnectException) {
                        f.setException(new OpenChannelException(
                            SshConstants.SSH_OPEN_CONNECT_FAILED,
                            future.getException().getMessage(),
                            future.getException()));
                    } else {
                        f.setException(future.getException());
                    }
                }
            }
        });
        return f;
    }
    
    /**
     * kkkkkk
     * receive data from client
     */
    /**
     * kkkk
     * then write the data to the forwarding addr(ip:port).
     * 
     */
    public void handleData(Buffer buffer) throws IOException {
        int len = buffer.getInt();
        if (len < 0 || len > Buffer.MAX_LEN) {
            throw new IllegalStateException("Bad item length: " + len);
        }
        log.debug("Received SSH_MSG_CHANNEL_DATA on channel {}", this);
//        log.debug("=====Received channel data: {}", BufferUtils.printHex(buffer.array(), buffer.rpos(), len));
//        log.info("======Received data:" + new String(buffer.array()));
        log.debug("======Northboundssh:" + new String(buffer.getCompactData()));
//        log.info("======Received data:" + buffer.getString());
        if (log.isTraceEnabled()) {
            log.trace("Received channel data: {}", BufferUtils.printHex(buffer.array(), buffer.rpos(), len));
        }
        doWriteData(buffer.array(), buffer.rpos(), len);
        
        //send a end delimiter, then make sure sim server could get the whole msg once.
        byte[] end = Server.LINE_DELIMITER.getBytes(Server.CHARSET);
        doWriteData(end, 0, end.length);
    }

    private void closeImmediately0() {
        // We need to close the channel immediately to remove it from the
        // server session's channel table and *not* send a packet to the
        // client.  A notification was already sent by our caller, or will
        // be sent after we return.
        //
        super.close(true);

        // We also need to dispose of the connector, but unfortunately we
        // are being invoked by the connector thread or the connector's
        // own processor thread.  Disposing of the connector within either
        // causes deadlock.  Instead create a new thread to dispose of the
        // connector in the background.
        //
        new Thread("TcpIpServerChannel-ConnectorCleanup") {
            @Override
            public void run() {
                connector.dispose();
            }
        }.start();
    }

    public CloseFuture close(boolean immediately) {
        return super.close(immediately).addListener(new SshFutureListener<CloseFuture>() {
            public void operationComplete(CloseFuture sshFuture) {
                closeImmediately0();
            }
        });
    }

    protected void doWriteData(byte[] data, int off, final int len) throws IOException {
        // Make sure we copy the data as the incoming buffer may be reused
    	Buffer buf = new Buffer(data, off, len);
    	buf = new Buffer(buf.getCompactData());
        ioSession.write(buf).addListener(new SshFutureListener<IoWriteFuture>() {
            public void operationComplete(IoWriteFuture future) {
                try {
                    localWindow.consumeAndCheck(len);
                } catch (IOException e) {
                    session.exceptionCaught(e);
                }
            }
        });
    }

    protected void doWriteExtendedData(byte[] data, int off, int len) throws IOException {
        throw new UnsupportedOperationException(type + "Tcpip channel does not support extended data");
    }

}
