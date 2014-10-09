package com.kkk.netconf.server.netconf;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

import net.i2cat.netconf.messageQueue.MessageQueue;
import net.i2cat.netconf.messageQueue.MessageQueueListener;
import net.i2cat.netconf.rpc.Hello;
import net.i2cat.netconf.rpc.Operation;
import net.i2cat.netconf.rpc.Query;
import net.i2cat.netconf.rpc.RPCElement;
import net.i2cat.netconf.rpc.Reply;
import net.i2cat.netconf.rpc.ReplyFactory;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.kkk.netconf.server.Behaviour;
import com.kkk.netconf.server.BehaviourContainer;
import com.kkk.netconf.server.MessageStore;
import com.kkk.netconf.server.behaviour.NetconfLogFile;
import com.kkk.netconf.server.behaviour.NetconfMsg;
import com.kkk.netconf.server.transport.ServerTransportContentParser;

public abstract class AbstractNetconfProcessor implements MessageQueueListener {
    public static final String PING_MSG = "<hello type=\"cms\"></hello>";
    protected Logger log = LoggerFactory.getLogger(getClass());
    private int messageCounter = 100;
    private MessageStore messageStore;
    private BehaviourContainer behaviourContainer;
    protected Status status;
    protected XMLReader xmlParser;
    protected ServerTransportContentParser xmlHandler;
    protected MessageQueue messageQueue;
    private Thread messageProcessorThread;

    private NetconfLogFile nfb = NetconfLogFile.getInstance();

    String sessionId;

    /**
     * Netconf session status
     * 
     */
    protected enum Status {
	INIT(0), HELLO_RECEIVED(1), CLOSING_SESSION(99), SESSION_CLOSED(100);

	private int index;

	private Status(int index) {
	    this.index = index;
	}

	public int getIndex() {
	    return index;
	}
    }

    public abstract void send(String reply) throws IOException;

    public void setMessageStore(MessageStore messageStore) {
	this.messageStore = messageStore;
    }

    public void setBehaviors(BehaviourContainer behaviourContainer) {
	this.behaviourContainer = behaviourContainer;
    }

    public void processCms(String message) throws IOException, SAXException {
	if(message.startsWith("<hello")) {
	    if(message.equals(PING_MSG)) {
		//do nothing.
		return;
	    }
	    int index = message.indexOf(">");
	    String cmshello = message.substring(0, index +1);
	    log.warn("Receive CMS hello message:" + cmshello);
	    message = message.substring(index +1);
	    if(message.isEmpty()) {
		return;
	    }
	}
	
	NetconfMsg nm = nfb.procMsg(message);
	NetconfMsg req = nfb.getRequest(nm.getMd5());
	if (req == null) {
	    log.warn("---------------not support this request:");
	    log.warn(message);
	} else {
	    NetconfMsg res = nfb.getResponse(req.getResponseKey());
	    if (res == null) {
		log.warn("---------------only have request, not have response in the map:");
		log.warn(message);
	    } else {
		send(res.genResponseMessage(nm.getNodeName(), nm.getMessageId()));
	    }
	}

    }

    public void process(final String message) throws IOException, SAXException {
	log.debug("Starting parser..");
	try {
	    log.trace("Parsing message:\n" + message);
	    xmlParser.parse(new InputSource(new StringReader(message)));
	} catch (SAXException e) {
	    if (e.getMessage().contentEquals("Content is not allowed in trailing section.")) {
		// Using shitty non-xml delimiters forces us to detect
		// end-of-frame by a SAX error.
		// Do nothing will just restart the parser.
		// Blame netconf
	    } else {
		log.error("Error parsing. Message: \n" + message, e);
		status = Status.SESSION_CLOSED;
	    }
	    log.info("End of parsing.");
	} catch (Exception e) {
	    log.error("Error parsing message", e);
	    status = Status.SESSION_CLOSED;
	}
    }

    protected void sendHello() throws IOException {
	// create a server hello message
	/**
	 * CalixHello serverHello = new CalixHello(); // generate a random
	 * session ID serverHello.setSessionId(sessionId);
	 * 
	 * // add only base capability ArrayList<Capability> capabilities = new
	 * ArrayList<Capability>(); capabilities.add(Capability.BASE);
	 * capabilities.add(Capability.WRITABLE_RUNNING); capabilities.add(new
	 * CalixCapability("http://calix.com/e7-2/2.3/config"));
	 * capabilities.add(new
	 * CalixCapability("http://calix.com/e7-2/2.3/stats"));
	 * capabilities.add(new
	 * CalixCapability("http://calix.com/e7-2/2.3/admin"));
	 * serverHello.setCapabilities(capabilities); CompositeConfiguration ctx
	 * = new CompositeConfiguration();
	 */
	// ctx.addProperty("session-id", sessionId);
	// ctx.addProperty("session-timeout", 1860);
	// ctx.addProperty("request-timeout", 20);

	// Configuration cc = new BaseConfiguration();
	// cc.addProperty("session-id", sessionId);
	// cc.addProperty("session-timeout", 1860);
	// cc.addProperty("request-timeout", 20);
	// ctx.addConfiguration(cc);
	// serverHello.setCtx( ctx);

	send(CalixHello.genHello(sessionId).toXML());
    }

    public void sendFakeConfig(Query configQuery) throws IOException {
	InputStream configFileIs = this.getClass().getResourceAsStream("/router_configs/router_config_A.xml");
	Reply reply = ReplyFactory.newGetConfigReply(configQuery, null, IOUtils.toString(configFileIs));
	sendReply(reply);
    }

    private void sendCloseSession() throws IOException {
	log.debug("Sending close session.");
	Query query = new Query();
	query.setMessageId("" + messageCounter++);
	query.setOperation(Operation.CLOSE_SESSION);
	sendQuery(query);
    }

    private void sendOk(Query query) throws IOException {
	log.debug("Sending OK.");
	sendReply(ReplyFactory.newOk(query, null));
    }

    public void sendQuery(Query query) throws IOException {
	send(query.toXML());
    }

    public void sendReply(Reply reply) throws IOException {
	send(reply.toXML());
    }

    public AbstractNetconfProcessor() {
	super();
	try {
	    messageQueue = new MessageQueue();

	    xmlHandler = new ServerTransportContentParser();
	    xmlHandler.setMessageQueue(messageQueue);
	    messageQueue.addListener(this);

	    xmlParser = XMLReaderFactory.createXMLReader();
	    xmlParser.setContentHandler(xmlHandler);
	    xmlParser.setErrorHandler(xmlHandler);
	} catch (SAXException e) {
	    log.error("Cannot instantiate XML parser", e);
	    return;
	}
	status = Status.INIT;
	startMessageProcessor();
    }

    protected void startMessageProcessor() {
	log.info("Creating new message processor...");
	messageProcessorThread = new Thread("Message processor") {
	    @Override
	    public void run() {
		while (status.getIndex() < Status.SESSION_CLOSED.getIndex()) {

		    RPCElement message = messageQueue.blockingConsume();

		    if (message == null) {
			continue;
		    }

		    log.trace("Message body:\n" + message.toXML() + '\n');

		    // store message if necessary
		    if (messageStore != null) {
			messageStore.storeMessage(message);
		    }

		    // avoid message processing when session is already closed
		    if (status == Status.SESSION_CLOSED) {
			log.warn("Session is closing or is already closed, message will not be processed");
			return;
		    }

		    // process message
		    try {
			// user defined behaviours
			if (message instanceof Query) {
			    Query query = (Query) message;
			    List<Behaviour> behaviours = behaviourContainer.getBehaviours();
			    if (behaviours != null) {
				Behaviour behaviour = null;
				for (Behaviour b : behaviours) {
				    if (b.getQuery().getOperation().equals(query.getOperation())) {
					behaviour = b;
					break;
				    }
				}
				if (behaviour != null) {
				    log.info("Behaviour matched.");
				    if (behaviour.isConsume()) {
					log.info("Behaviour matched. Sending reply...");
					behaviours.remove(behaviour);
				    }
				    log.info("Sending matched reply...");
				    behaviour.getReply().setMessageId(query.getMessageId());
				    sendReply(behaviour.getReply());
				    // next iteration
				    continue;
				}
			    }
			}

			// default message processing
			if (message instanceof Hello) {
			    if (status.getIndex() < Status.HELLO_RECEIVED.getIndex()) {
				status = Status.HELLO_RECEIVED;
				// send hello
				log.debug("Sending hello...");
				sendHello();
			    } else {
				log.error("Hello already received. Aborting");
				sendCloseSession();
				status = Status.CLOSING_SESSION;
			    }
			} else if (message instanceof Query) {
			    Query query = (Query) message;
			    Operation operation = query.getOperation();

			    if (operation.equals(Operation.CLOSE_SESSION)) {
				log.info("Close-session received.");
				status = Status.CLOSING_SESSION;
				sendOk(query);
				status = Status.SESSION_CLOSED;
				log.info("Session closed.");
				// next iteration
				continue;
			    } else if (operation.equals(Operation.GET_CONFIG)) {
				log.info("Get-config received.");
				sendFakeConfig(query);
				// next iteration
				continue;
			    } else {
				log.info("Unknown query received, replying OK");
				sendOk(query);
				// next iteration
				continue;
			    }
			} else if (message instanceof Reply) {
			    if (status == Status.CLOSING_SESSION) {
				log.info("Client confirms the close session request.");
				status = Status.SESSION_CLOSED;
				// next iteration
				continue;
			    } else {
				log.error("Unknown reply received!");
				// next iteration
				continue;
			    }
			} else {
			    log.warn("Unknown message: " + message.toXML());
			    // next iteration
			    continue;
			}
		    } catch (IOException e) {
			log.error("Error sending reply", e);
			break;
		    }
		}
		log.trace("Message processor ended");
	    }
	};
	messageProcessorThread.start();
	log.info("Message processor started.");
    }

    public void waitAndInterruptThreads() {
	// wait for thread
	try {
	    messageProcessorThread.join(2000);
	} catch (InterruptedException e) {
	    log.error("Error waiting for thread end", e);
	}

	// kill thread if it don't finish naturally
	if (messageProcessorThread != null && messageProcessorThread.isAlive()) {
	    log.debug("Killing message processor thread");
	    messageProcessorThread.interrupt();
	}
    }

    @Override
    public void receiveRPCElement(RPCElement element) {
	log.info("------kkk----------Message received");
    }

}