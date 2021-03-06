package org.netconf.server.netconf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.netconf.server.BehaviourContainer;
import org.netconf.server.MessageStore;

/**
 * Netconf Subsystem
 * 
 * @author Julio Carlos Barrera
 * 
 */
public class NetconfSubsystem implements Command, SessionAware {

	private static final Log	log					= LogFactory.getLog(NetconfSubsystem.class);

	// subsystem fields
	private ExitCallback		callback;
	private InputStream			in;
	private OutputStream		out;
	private OutputStream		err;
	private Environment			env;

	private ServerSession		session;

	private MessageStore		messageStore;
	private BehaviourContainer	behaviourContainer	= null;

	private SessionChannelNetconfProcessor	netconfProcessor;
	
	private static NetconfSubsystem ns;
	
	public synchronized static NetconfSubsystem getInstance(MessageStore messageStore, BehaviourContainer behaviourContainer) {
		if (ns == null) {
			ns = new NetconfSubsystem(messageStore, behaviourContainer);
		}
		return ns;
	}
	
	public static NetconfSubsystem getInstance() {
		return ns;
	}
	
	public AbstractNetconfProcessor getNetconfProcessor() {
		return netconfProcessor;
	}

	private Thread				clientThread;

	public NetconfSubsystem(MessageStore messageStore, BehaviourContainer behaviourContainer) {
		this.messageStore = messageStore;
		this.behaviourContainer = behaviourContainer;
	}

	public InputStream getInputStream() {
		return in;
	}

	@Override
	public void setInputStream(InputStream in) {
		this.in = in;
	}

	public OutputStream getOutputStream() {
		return out;
	}

	@Override
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

	public OutputStream getErrorStream() {
		return err;
	}

	@Override
	public void setErrorStream(OutputStream err) {
		this.err = err;
	}

	@Override
	public void setExitCallback(ExitCallback callback) {
		this.callback = callback;
	}

	@Override
	public void start(Environment env) throws IOException {
		this.env = env;

		// initialize Netconf processor
		netconfProcessor = new SessionChannelNetconfProcessor(in, out, err, callback);
		netconfProcessor.setMessageStore(messageStore);
		netconfProcessor.setBehaviors(behaviourContainer);

		log.info("Starting session client thread...");
		(clientThread = new Thread(netconfProcessor, "Session client thread")).start();
	}

	@Override
	public void destroy() {
		netconfProcessor.waitAndInterruptThreads();
		try {
			clientThread.join(2000);
		} catch (InterruptedException e) {
			log.warn("Error joining Client thread" + e.getMessage());
		}
		clientThread.interrupt();
		log.info("Netconf Subsystem destroyed");
	}

	@Override
	public void setSession(ServerSession session) {
		this.session = session;
	}

	/**
	 * Netconf Subsystem Factory
	 * 
	 * @author Julio Carlos Barrera
	 * 
	 */
	public static class Factory implements NamedFactory<Command> {
		private static final Log	log					= LogFactory.getLog(Factory.class);

		private MessageStore		messageStore		= null;
		private BehaviourContainer	behaviourContainer	= null;

		private Factory(MessageStore messageStore, BehaviourContainer behaviourContainer) {
			this.messageStore = messageStore;
			this.behaviourContainer = behaviourContainer;
		}

		public static Factory createFactory(MessageStore messageStore, BehaviourContainer behaviourContainer) {
			return new Factory(messageStore, behaviourContainer);
		}

		public Command create() {
			log.info("Creating Netconf Subsystem Factory");
			return new NetconfSubsystem(messageStore, behaviourContainer);
//			return NetconfSubsystem.getInstance(messageStore, behaviourContainer);
		}

		public String getName() {
			return "netconf";
		}
	}

}
