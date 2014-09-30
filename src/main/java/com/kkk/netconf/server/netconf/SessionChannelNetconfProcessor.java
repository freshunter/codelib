package com.kkk.netconf.server.netconf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sshd.server.ExitCallback;


public class SessionChannelNetconfProcessor  extends AbstractNetconfProcessor implements Runnable{

	// client streams
	private InputStream						in;
	public InputStream getIn() {
		return in;
	}

	public OutputStream getOut() {
		return out;
	}

	private OutputStream					out;
	private OutputStream					err;

	// callback
	private ExitCallback					callback;

    protected final AtomicInteger nextSessionId = new AtomicInteger(0);

	public SessionChannelNetconfProcessor(InputStream in, OutputStream out, OutputStream err, ExitCallback callback) {
		super();
		this.in = in;
		this.out = out;
		this.err = err;
		this.callback = callback;
	}

	protected int getNextSessionId() {
        return nextSessionId.getAndIncrement();
    }

	@Override
	public void run() {
		// initialize XML parser & handler and message queue

		sessionId = "" + getNextSessionId();

		try {
			sendHello();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			log.warn("Error waiting for message processor thread.", e);
		}

		// process messages
		try {
			StringBuilder message = new StringBuilder();

			while (status != Status.SESSION_CLOSED) {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));

				log.debug("Start reading new message...");

				// read message
				String line;
				while ((line = br.readLine()) != null) {
					log.debug("----------kkk----------Line read: '" + line + "'");
					if (line.endsWith(END_CHAR_SEQUENCE)) {
						log.trace("Detected end message.");
						// remove end char sequence from message
						line = line.replace(END_CHAR_SEQUENCE, "");
						message.append(line + '\n');
						// process data
						process(message.toString());
						// reset message
						message.setLength(0);
					}
					message.append(line + '\n');
				}
				// exit loop if stream closed
				break;
			}
		} catch (Exception e) {
			log.error("Exception caught in Netconf subsystem", e);
		} finally {
			waitAndInterruptThreads();
			callback.onExit(0);
		}
	}

	public void send(String xmlMessage) throws IOException {
		log.trace("Sending message:\n" + xmlMessage);
		out.write(xmlMessage.getBytes("UTF-8"));
		// send final sequence
		out.write((END_CHAR_SEQUENCE + "\n").getBytes("UTF-8"));
		out.flush();
	}
}
