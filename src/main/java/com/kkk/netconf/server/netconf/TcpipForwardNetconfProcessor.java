package com.kkk.netconf.server.netconf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.i2cat.netconf.messageQueue.MessageQueue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

import com.kkk.netconf.server.transport.ServerTransportContentParser;


public class TcpipForwardNetconfProcessor  extends AbstractNetconfProcessor{

	private IoSession session;

	public TcpipForwardNetconfProcessor(IoSession session) {
		super();
		this.session = session;
		this.sessionId = Long.toString(session.getId());
	}

	public void send(String reply) throws IOException {
		log.info("Sending message:\n" + reply);
		IoBuffer message = IoBuffer.wrap(reply.getBytes("UTF-8"));
		session.write(message);
	}
	
//	public void receiveMSG(String msg) {
//		
//		// start message processor
//		
//		// wait for message processor to continue
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			log.warn("Error waiting for message processor thread.", e);
//		}
//
//		// process messages
//		try {
//			StringBuilder message = new StringBuilder();
//
//			while (status != Status.SESSION_CLOSED) {
////				BufferedReader br = new BufferedReader(new InputStreamReader(in));
//
//				log.debug("Start reading new message...");
//
//				// read message
//				String line;
////				while ((line = br.readLine()) != null) {
//					log.debug("----------kkk----------Line read: '" + line + "'");
//					if (line.endsWith(END_CHAR_SEQUENCE)) {
//						log.trace("Detected end message.");
//						// remove end char sequence from message
//						line = line.replace(END_CHAR_SEQUENCE, "");
//						message.append(line + '\n');
//						// process data
//						process(message.toString());
//						// reset message
//						message.setLength(0);
////					}
//					message.append(line + '\n');
//				}
//				// exit loop if stream closed
//				break;
//			}
//		} catch (Exception e) {
//			log.error("Exception caught in Netconf subsystem", e);
//		} finally {
//			waitAndInterruptThreads();
//		}
//	}
	
}
