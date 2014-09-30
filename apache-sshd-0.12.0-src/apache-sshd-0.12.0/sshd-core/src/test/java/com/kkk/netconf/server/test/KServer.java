package com.kkk.netconf.server.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.SshServer;
import org.apache.sshd.client.channel.ChannelDirectTcpip;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.util.BogusForwardingFilter;
import org.apache.sshd.util.BogusPasswordAuthenticator;
import org.apache.sshd.util.EchoShellFactory;
import org.apache.sshd.util.Utils;
import org.slf4j.LoggerFactory;

public class KServer {
	
    private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());

    private SshServer sshd;
    private int sshPort;
    private int echoPort;
    private IoAcceptor acceptor;
    private SshClient client;
    
    
	public static int getFreePort() throws Exception {
        ServerSocket s = new ServerSocket(0);
        try {
            return s.getLocalPort();
        } finally {
            s.close();
        }
    }

    ClientSession createNativeSession() throws Exception {
        client = SshClient.setUpDefaultClient();
        client.getProperties().put(SshServer.WINDOW_SIZE, "2048");
        client.getProperties().put(SshServer.MAX_PACKET_SIZE, "256");
        client.setTcpipForwardingFilter(new BogusForwardingFilter());
        client.start();
        ConnectFuture sessionFuture = client.connect("localhost", sshPort);
        sessionFuture.await();
        ClientSession session = sessionFuture.getSession();

        AuthFuture authPassword = session.authPassword("sshd", "sshd");
        authPassword.await();

        return session;
    }
	
	public void init() throws Exception {

        sshPort = 831;
        echoPort = 8322;//getFreePort();

        sshd = SshServer.setUpDefaultServer();
        sshd.getProperties().put(SshServer.WINDOW_SIZE, "2048");
        sshd.getProperties().put(SshServer.MAX_PACKET_SIZE, "256");
        sshd.setPort(sshPort);
        sshd.setKeyPairProvider(Utils.createTestHostKeyProvider());
        sshd.setShellFactory(new EchoShellFactory());
        sshd.setPasswordAuthenticator(new BogusPasswordAuthenticator());
        sshd.setTcpipForwardingFilter(new BogusForwardingFilter());
        sshd.start();

        NioSocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.setHandler(new IoHandlerAdapter() {
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                IoBuffer recv = (IoBuffer) message;
                IoBuffer sent = IoBuffer.allocate(recv.remaining());
                sent.put(recv);
                sent.flip();
                session.write(sent);
            }
        });
        acceptor.setReuseAddress(true);
        acceptor.bind(new InetSocketAddress(echoPort));
        this.acceptor = acceptor;
	}
	
	public void testForwardingChannel() throws Exception {
        ClientSession session = createNativeSession();

        int forwardedPort = getFreePort();
        SshdSocketAddress local = new SshdSocketAddress("", forwardedPort);
        SshdSocketAddress remote = new SshdSocketAddress("localhost", echoPort);

        ChannelDirectTcpip channel = session.createDirectTcpipChannel(local, remote);
        channel.open().await();

        channel.getInvertedIn().write("Hello".getBytes());
        channel.getInvertedIn().flush();
        byte[] buf = new byte[1024];
        int n = channel.getInvertedOut().read(buf);
        String res = new String(buf, 0, n);
        System.out.println("Hello  receive:"+ res);
        channel.close(false);

        session.close(false).await();
    }
	
	public static void main(String[] args) throws Exception {
		KServer ks = new KServer();
		ks.init();
		ks.testForwardingChannel();
	}

}
