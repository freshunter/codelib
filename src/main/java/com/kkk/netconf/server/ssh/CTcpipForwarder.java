package com.kkk.netconf.server.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.Closeable;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.TcpipForwarder;
import org.apache.sshd.common.forward.TcpipClientChannel;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoAcceptor;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.CloseableUtils;
import org.apache.sshd.common.util.Readable;

import com.kkk.netconf.server.netconf.NetconfProcessor;

public class CTcpipForwarder extends CloseableUtils.AbstractInnerCloseable implements TcpipForwarder, IoHandler {

//	private static final Log log = LogFactory.getLog(CalixTcpipForwarder.class);
    private final ConnectionService service;
    private final Session session;
    private final Map<Integer, SshdSocketAddress> localToRemote = new HashMap<Integer, SshdSocketAddress>();
    private final Map<Integer, SshdSocketAddress> remoteToLocal = new HashMap<Integer, SshdSocketAddress>();
    private final Set<SshdSocketAddress> localForwards = new HashSet<SshdSocketAddress>();
    protected IoAcceptor acceptor;

    public CTcpipForwarder(ConnectionService service) {
        this.service = service;
        this.session = service.getSession();
    }

    //
    // TcpIpForwarder implementation
    //

    public synchronized SshdSocketAddress startLocalPortForwarding(SshdSocketAddress local, SshdSocketAddress remote) throws IOException {
        if (local == null) {
            throw new IllegalArgumentException("Local address is null");
        }
        if (remote == null) {
            throw new IllegalArgumentException("Remote address is null");
        }
        if (local.getPort() < 0) {
            throw new IllegalArgumentException("Invalid local port: " + local.getPort());
        }
        if (isClosed()) {
            throw new IllegalStateException("TcpipForwarder is closed");
        }
        if (isClosing()) {
            throw new IllegalStateException("TcpipForwarder is closing");
        }
        log.info("start local port forwarding:" + local.toString() + " remote:" + remote.toString());
        SshdSocketAddress bound = doBind(local);
        localToRemote.put(bound.getPort(), remote);
        return bound;
    }

    public synchronized void stopLocalPortForwarding(SshdSocketAddress local) throws IOException {
        log.info("stop local port forwarding:" + local.toString());
        if (localToRemote.remove(local.getPort()) != null && acceptor != null) {
            acceptor.unbind(local.toInetSocketAddress());
            if (acceptor.getBoundAddresses().isEmpty()) {
                close();
            }
        }
    }

    public synchronized SshdSocketAddress startRemotePortForwarding(SshdSocketAddress remote, SshdSocketAddress local) throws IOException {
        log.info("start remote port forwarding:" + local.toString() + " remote:" + remote.toString());
        Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_GLOBAL_REQUEST);
        buffer.putString("tcpip-forward");
        buffer.putBoolean(true);
        buffer.putString(remote.getHostName());
        buffer.putInt(remote.getPort());
        Buffer result = session.request(buffer);
        if (result == null) {
            throw new SshException("Tcpip forwarding request denied by server");
        }
        int port = remote.getPort() == 0 ? result.getInt() : remote.getPort();
        // TODO: Is it really safe to only store the local address after the request ?
        remoteToLocal.put(port, local);
        return new SshdSocketAddress(remote.getHostName(), port);
    }

    public synchronized void stopRemotePortForwarding(SshdSocketAddress remote) throws IOException {
        log.info("stop remote port forwarding: remote:" + remote.toString());
        if (remoteToLocal.remove(remote.getPort()) != null) {
            Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_GLOBAL_REQUEST);
            buffer.putString("cancel-tcpip-forward");
            buffer.putBoolean(false);
            buffer.putString(remote.getHostName());
            buffer.putInt(remote.getPort());
            session.writePacket(buffer);
        }
    }

    public synchronized SshdSocketAddress getForwardedPort(int remotePort) {
        return remoteToLocal.get(remotePort);
    }

    public synchronized SshdSocketAddress localPortForwardingRequested(SshdSocketAddress local) throws IOException {
        if (local == null) {
            throw new IllegalArgumentException("Local address is null");
        }
        if (local.getPort() < 0) {
            throw new IllegalArgumentException("Invalid local port: " + local.getPort());
        }
        final ForwardingFilter filter = session.getFactoryManager().getTcpipForwardingFilter();
        if (filter == null || !filter.canListen(local, session)) {
            throw new IOException("Rejected address: " + local);
        }
        SshdSocketAddress bound = doBind(local);
        localForwards.add(bound);
        return bound;
    }

    public synchronized void localPortForwardingCancelled(SshdSocketAddress local) throws IOException {
        if (localForwards.remove(local) && acceptor != null) {
            acceptor.unbind(local.toInetSocketAddress());
            if (acceptor.getBoundAddresses().isEmpty()) {
                acceptor.close(true);
                acceptor = null;
            }
        }
    }

    public synchronized void close() {
        close(true);
    }

    @Override
    protected synchronized Closeable getInnerCloseable() {
        return builder().close(acceptor).build();
    }

    //
    // IoHandler implementation
    //

    public void sessionCreated(final IoSession session) throws Exception {
        final TcpipClientChannel channel;
        int localPort = ((InetSocketAddress) session.getLocalAddress()).getPort();
        if (localToRemote.containsKey(localPort)) {
            SshdSocketAddress remote = localToRemote.get(localPort);
            channel = new TcpipClientChannel(TcpipClientChannel.Type.Direct, session, remote);
            log.info("===========try to create direct-tcpip channel");
        } else {
            channel = new TcpipClientChannel(TcpipClientChannel.Type.Forwarded, session, null);
        }
        session.setAttribute(TcpipClientChannel.class, channel);
        this.service.registerChannel(channel);
        channel.open().addListener(new SshFutureListener<OpenFuture>() {
            public void operationComplete(OpenFuture future) {
                Throwable t = future.getException();
                if (t != null) {
                    CTcpipForwarder.this.service.unregisterChannel(channel);
                    channel.close(false);
                }
            }
        });
    }

    public void sessionClosed(IoSession session) throws Exception {
        TcpipClientChannel channel = (TcpipClientChannel) session.getAttribute(TcpipClientChannel.class);
        if (channel != null) {
            log.debug("IoSession {} closed, will now close the channel", session);
            channel.close(false);
        }
    }

    public void messageReceived(IoSession session, Readable message) throws Exception {
        log.info("=============IoSession read message:", message.toString());
        TcpipClientChannel channel = (TcpipClientChannel) session.getAttribute(TcpipClientChannel.class);
        Buffer buffer = new Buffer();
        buffer.putBuffer(message);
        channel.waitFor(ClientChannel.OPENED | ClientChannel.CLOSED, Long.MAX_VALUE);
        channel.getInvertedIn().write(buffer.array(), buffer.rpos(), buffer.available());
        channel.getInvertedIn().flush();
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        cause.printStackTrace();
        session.close(false);
    }

    //
    // Private methods
    //

    private SshdSocketAddress doBind(SshdSocketAddress address) throws IOException {
        if (acceptor == null) {
            acceptor = session.getFactoryManager().getIoServiceFactory().createAcceptor(this);
        }
        Set<SocketAddress> before = acceptor.getBoundAddresses();
        try {
            acceptor.bind(address.toInetSocketAddress());
            Set<SocketAddress> after = acceptor.getBoundAddresses();
            after.removeAll(before);
            if (after.isEmpty()) {
                throw new IOException("Error binding to " + address + ": no local addresses bound");
            }
            if (after.size() > 1) {
                throw new IOException("Multiple local addresses have been bound for " + address);
            }
            InetSocketAddress result = (InetSocketAddress) after.iterator().next();
            return new SshdSocketAddress(address.getHostName(), result.getPort());
        } catch (IOException bindErr) {
            if (acceptor.getBoundAddresses().isEmpty()) {
                close();
            }
            throw bindErr;
        }
    }

}
