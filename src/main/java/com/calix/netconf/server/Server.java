package com.calix.netconf.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.i2cat.netconf.rpc.RPCElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Service;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerConnectionService;
import org.apache.sshd.server.session.ServerUserAuthService;

import com.calix.netconf.server.exceptions.ServerException;
import com.calix.netconf.server.netconf.NetconfSubsystem;
import com.calix.netconf.server.ssh.AlwaysTruePasswordAuthenticator;
import com.calix.netconf.server.ssh.CalixTcpipForwarderFactory;

/**
 * Netconf server class allowing to create a test Netconf server with the ability of:
 * <ul>
 * <li>Store the received Netconf messages</li>
 * <li>Configure behaviors</li>
 * </ul>
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class Server implements MessageStore, BehaviourContainer {

	private static final Log	log				= LogFactory.getLog(Server.class);

	private SshServer			sshd;

	// stored messages
	private boolean				storeMessages	= false;
	private List<RPCElement>	messages;

	// behaviours
	private List<Behaviour>		behaviours;

	// hide default constructor, forcing using factory method
	private Server() {
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

		server.initializeServer("192.168.37.84", listeningPort);

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

		return server;
	}

	/**
	 * Creates a server listening in loopback interface and store all received messages
	 * 
	 * @param listeningPort
	 *            where the server will listen for SSH connections
	 * 
	 */
	public static Server createServerStoringMessages(int listeningPort) {
		Server server = new Server();
		server.messages = new ArrayList<RPCElement>();
		server.storeMessages = true;

//		server.initializeServer("192.168.37.84", listeningPort);
		server.initializeServer("0.0.0.0", listeningPort);

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
	public static Server createServerStoringMessages(String host, int listeiningPort) {
		Server server = new Server();
		server.messages = new ArrayList<RPCElement>();
		server.storeMessages = true;

		server.initializeServer(host, listeiningPort);

		return server;
	}

	private void initializeServer(String host, int listeningPort) {
		log.info("Configuring server...");
		sshd = SshServer.setUpDefaultServer();
		sshd.setHost(host);
		sshd.setPort(listeningPort);

		log.info("Host: '" + host + "', listenig port: " + listeningPort);

		sshd.setPasswordAuthenticator(new AlwaysTruePasswordAuthenticator());
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));

		List<NamedFactory<Command>> subsystemFactories = new ArrayList<NamedFactory<Command>>();
		subsystemFactories.add(NetconfSubsystem.Factory.createFactory(this, this));
		sshd.setSubsystemFactories(subsystemFactories);
		
		sshd.setServiceFactories(Arrays.asList(
                new ServerUserAuthService.Factory() {
                    @Override
                    public Service create(Session session) throws IOException {
                        return new ServerUserAuthService(session) {
                            @Override
                            public void process(byte cmd, Buffer buffer) throws Exception {
                            	log.info(cmd + " buffer "+ buffer.toString());
                                super.process(cmd, buffer);
                            }
                        };
                    }
                },
                new ServerConnectionService.Factory()
        ));
		
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
                return true;
            }
        });
		
		sshd.setTcpipForwarderFactory(new CalixTcpipForwarderFactory());

		log.info("Server configured.");
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
		log.info("Server started.");
	}

	public void stopServer() {
		log.info("Stopping server...");
		try {
			sshd.stop();
		} catch (InterruptedException e) {
			log.error("Error stopping server!");
			throw new ServerException("Error stopping server", e);
		}
		log.info("Server stopped.");
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
			throw new ServerException(new UnsupportedOperationException("Server is configured to not store messages!"));
		}
	}

	public static void main(String[] args) throws IOException {

		System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "trace");
		
		Server server = Server.createServerStoringMessages(830);
		server.startServer();

		// read lines form input
		BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			if (buffer.readLine().equalsIgnoreCase("EXIT")) {
				break;
			}

			log.info("Messages received(" + server.getStoredMessages().size() + "):");
			for (RPCElement rpcElement : server.getStoredMessages()) {
				log.info("#####  BEGIN message #####\n" +
						rpcElement.toXML() + '\n' +
						"#####   END message  #####");
			}
		}

		log.info("Exiting");
		System.exit(0);
	}

}
