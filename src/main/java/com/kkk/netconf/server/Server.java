package com.kkk.netconf.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.i2cat.netconf.rpc.RPCElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Channel;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Service;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerConnectionService;
import org.apache.sshd.server.session.ServerUserAuthService;

import com.kkk.netconf.server.behaviour.NetconfLogFile;
import com.kkk.netconf.server.exceptions.ServerException;
import com.kkk.netconf.server.netconf.NetconfIoHandler;
import com.kkk.netconf.server.netconf.NetconfSubsystem;
import com.kkk.netconf.server.ssh.AlwaysTruePasswordAuthenticator;
import com.kkk.netconf.server.ssh.CTcpipServerChannel;

/**
 * Netconf server class allowing to create a test Netconf server with the
 * ability of:
 * <ul>
 * <li>Store the received Netconf messages</li>
 * <li>Configure behaviors</li>
 * </ul>
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class Server implements MessageStore, BehaviourContainer {

	public static final int NETCONF_SERVER_PORT_OFFSET = 1000;
	public static Charset CHARSET = Charset.forName("UTF-8");
	public static String LINE_DELIMITER = LineDelimiter.NUL.getValue();
	private static final Log log = LogFactory.getLog(Server.class);

	private static SshServer sshd;

	// stored messages
	private boolean storeMessages = false;
	private List<RPCElement> messages;

	// behaviours
	private List<Behaviour> behaviours;

	private NioSocketAcceptor acceptor;

	// hide default constructor, forcing using factory method
	private Server() {
	}
	
	public static SshServer getSSHServer() {
		return sshd;
	}

	/**
	 * Creates an standard server listening in loopback interface
	 * 
	 * @param listeningPort
	 *            where the server will listen for SSH connections
	 */
	public static Server createServer(int listeningPort) {
		Server server = new Server();
		server.storeMessages = false;

		server.initializeServer("127.0.0.1", listeningPort);

		return server;
	}

	/**
	 * Creates an standard server
	 * 
	 * @param host
	 *            host name (use null to listen in all interfaces)
	 * @param listeningPort
	 *            TPC port where the server will listen for SSH connections
	 */
	public static Server createServer(String host, int listeningPort) {
		Server server = new Server();
		server.storeMessages = false;

		server.initializeServer(host, listeningPort);
		
		server.initializeNetconfServer(listeningPort + NETCONF_SERVER_PORT_OFFSET);
		
		log.info("Server configured.");

		return server;
	}

	/**
	 * Creates a server listening in loopback interface and store all received
	 * messages
	 * 
	 * @param listeningPort
	 *            where the server will listen for SSH connections
	 * 
	 */
	public static Server createServerStoringMessages(int listeningPort) {
		Server server = new Server();
		server.messages = new ArrayList<RPCElement>();
		server.storeMessages = true;

		// server.initializeServer("192.168.37.84,127.0.0.1", listeningPort);
		server.initializeServer("0.0.0.0", listeningPort);
		
		server.initializeNetconfServer(listeningPort + NETCONF_SERVER_PORT_OFFSET);
		
		log.info("Server configured.");

		return server;
	}

	/**
	 * Creates a server and store all received messages
	 * 
	 * @param host
	 *            host name (use null to listen in all interfaces)
	 * @param listeningPort
	 *            where the server will listen for SSH connections
	 * 
	 */
	public static Server createServerStoringMessages(String host,
			int listeningPort) {
		Server server = new Server();
		server.messages = new ArrayList<RPCElement>();
		server.storeMessages = true;

		server.initializeServer(host, listeningPort);
		
		server.initializeNetconfServer(listeningPort + NETCONF_SERVER_PORT_OFFSET);
		
		log.info("Server configured.");

		return server;
	}

	private void initializeNetconfServer(int listeningPort) {
		NioSocketAcceptor acceptor = new NioSocketAcceptor();
		TextLineCodecFactory lineCodec = new TextLineCodecFactory(CHARSET, LINE_DELIMITER,  
			LINE_DELIMITER);
	        lineCodec.setDecoderMaxLineLength(2*1024*1024);  
	        lineCodec.setEncoderMaxLineLength(2*1024*1024);  
	        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(lineCodec));  
//	        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ProtocolCodecFactory() {
//		    
//		    @Override
//		    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
//			// TODO Auto-generated method stub
//			return new ProtocolEncoder() {
//			    
//			    @Override
//			    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
//				// TODO Auto-generated method stub
//				
//			    }
//			    
//			    @Override
//			    public void dispose(IoSession session) throws Exception {
//				// TODO Auto-generated method stub
//				
//			    }
//			};
//		    }
//		    
//		    @Override
//		    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
//			// TODO Auto-generated method stub
//			return new ProtocolDecoder() {
//			    
//			    @Override
//			    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
//				// TODO Auto-generated method stub
//				
//			    }
//			    
//			    @Override
//			    public void dispose(IoSession session) throws Exception {
//				// TODO Auto-generated method stub
//				
//			    }
//			    
//			    @Override
//			    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
//				// TODO Auto-generated method stub
//				
//			    }
//			};
//		    }
//		}));  
		acceptor.getFilterChain().addLast("logger", new LoggingFilter());
		acceptor.setHandler(new NetconfIoHandler(this, this));
		acceptor.setReuseAddress(true);
		acceptor.setDefaultLocalAddress(new InetSocketAddress(listeningPort));
		this.acceptor = acceptor;
		log.info("Netconf Server listenig port: " + listeningPort);
	}

	private void initializeServer(String host, int listeningPort) {
		log.info("Configuring server...");
		sshd = SshServer.setUpDefaultServer();
		sshd.setHost(host);
		sshd.setPort(listeningPort);

		log.info("SSH Host: '" + host + "', listenig port: " + listeningPort);

		sshd.setPasswordAuthenticator(new AlwaysTruePasswordAuthenticator());
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
				"hostkey.ser"));

		List<NamedFactory<Command>> subsystemFactories = new ArrayList<NamedFactory<Command>>();
		subsystemFactories.add(NetconfSubsystem.Factory.createFactory(this,
				this));
		sshd.setSubsystemFactories(subsystemFactories);

		sshd.setServiceFactories(Arrays.asList(
				new ServerUserAuthService.Factory() {
					@Override
					public Service create(Session session) throws IOException {
						return new ServerUserAuthService(session) {
							@Override
							public void process(byte cmd, Buffer buffer)
									throws Exception {
								// log.info(cmd + " buffer "+
								// buffer.toString());
								super.process(cmd, buffer);
							}
						};
					}
				}, new ServerConnectionService.Factory()));

		sshd.setTcpipForwardingFilter(new ForwardingFilter() {
			public boolean canForwardAgent(Session session) {
				log.info("canForwardAgent.=====================");
				return true;
			}

			public boolean canForwardX11(Session session) {
				log.info("canForwardX11..=====================");
				return false;
			}

			public boolean canListen(SshdSocketAddress address, Session session) {
				log.info("canListen..=====================");
				return true;
			}

			public boolean canConnect(SshdSocketAddress address, Session session) {
				log.info("canConnect.=====================.");
				log.info("direct tcpip..=====================");
				return true;
			}
		});

		// sshd.setSessionFactory(new SessionFactory() {
		// @Override
		// protected AbstractSession doCreateSession(IoSession ioSession) throws
		// Exception {
		// return new CServerSession(server, ioSession);
		// }
		// });

		sshd.setChannelFactories(Arrays.<NamedFactory<Channel>> asList(
				new ChannelSession.Factory(),
				new CTcpipServerChannel.DirectTcpipFactory()));

//		sshd.setTcpipForwarderFactory(new CTcpipForwarderFactory());
	}

	@Override
	public void defineBehaviour(Behaviour behaviour) {
		if (behaviours == null) {
			behaviours = new ArrayList<Behaviour>();
		}
		synchronized (behaviours) {
			behaviours.add(behaviour);
		}
	}

	@Override
	public List<Behaviour> getBehaviours() {
		if (behaviours == null) {
			return null;
		}
		synchronized (behaviours) {
			return behaviours;
		}
	}

	public void startServer() throws ServerException {
		log.info("Starting server...");
		try {
			sshd.start();
		} catch (IOException e) {
			log.error("Error starting server!", e);
			throw new ServerException("Error starting server", e);
		}
		
		try {
			acceptor.bind();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Server started.");
	}

	public void stopServer() {
		log.info("Stopping server...");
		try {
			sshd.stop();
			log.info("SSH Server stopped.");
		} catch (InterruptedException e) {
			log.error("Error stopping server!");
			throw new ServerException("Error stopping server", e);
		}
		
		try {  
            acceptor.unbind(); 
            acceptor.dispose();
			log.info("Netconf Server stopped.");  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
		
	}

	@Override
	public void storeMessage(RPCElement message) {
		if (messages != null) {
			synchronized (messages) {
				log.info("Storing message");
				messages.add(message);
			}
		}
	}

	@Override
	public List<RPCElement> getStoredMessages() {
		if (storeMessages) {
			synchronized (messages) {
				return Collections.unmodifiableList(messages);
			}
		} else {
			throw new ServerException(new UnsupportedOperationException(
					"Server is configured to not store messages!"));
		}
	}

	public static void main(String[] args) throws IOException {

		System.setProperty("org.apache.commons.logging.simplelog.defaultlog",
				"warn");

		Server server = Server.createServerStoringMessages(830);
		server.startServer();
		
		NetconfLogFile.getInstance().importMsg("kkk.txt");
		NetconfLogFile.getInstance().StudyNetconfMsg("netconfLogFile.txt");

		// read lines form input
		BufferedReader buffer = new BufferedReader(new InputStreamReader(
				System.in));

		while (true) {
			if (buffer.readLine().equalsIgnoreCase("EXIT")) {
				break;
			}

			log.info("Messages received(" + server.getStoredMessages().size()
					+ "):");
			for (RPCElement rpcElement : server.getStoredMessages()) {
				log.info("#####  BEGIN message #####\n" + rpcElement.toXML()
						+ '\n' + "#####   END message  #####");
			}
		}

		log.info("Exiting");
		System.exit(0);
	}

}
