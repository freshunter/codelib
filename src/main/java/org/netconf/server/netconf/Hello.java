package org.netconf.server.netconf;

import java.util.ArrayList;

import net.i2cat.netconf.rpc.Capability;
import net.i2cat.netconf.rpc.RPCElement;

/**
 * 
 * @author khuang
 *
 */
public class Hello extends RPCElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	ArrayList<Capability>	capabilities;
	String					sessionId;
	String					messageId;

	public Hello() {
		messageId = "0";
	}

	public String toXML() {

		String xml = "<hello length=\"000530\" xmlns=\"" + Capability.BASE + "\">";

		xml += "<capabilities>";

		for (Capability capability : capabilities) {
			xml += "<capability>" + capability + "</capability>";
		}

		xml += "</capabilities>";

		if (sessionId != null)
			xml += "<session-id>" + sessionId + "</session-id>";

//		<session-timeout>1860</session-timeout>
//		<request-timeout>20</request-timeout>
		xml += "<session-timeout>1860</session-timeout><request-timeout>20</request-timeout>";

		xml += "</hello>";

		return xml;
	}

	public ArrayList<Capability> getCapabilities() {
		return capabilities;
	}

	public void setCapabilities(ArrayList<Capability> capabilities) {
		this.capabilities = capabilities;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public static Hello genHello(String sessionId) {
		Hello serverHello = new Hello();
		// generate a random session ID
		serverHello.setSessionId(sessionId);

		// add only base capability
		ArrayList<Capability> capabilities = new ArrayList<Capability>();
		capabilities.add(Capability.BASE);
		capabilities.add(Capability.WRITABLE_RUNNING);
		capabilities.add(new KCapability("http://calix.com/e7-2/2.3/config"));
		capabilities.add(new KCapability("http://calix.com/e7-2/2.3/stats"));
		capabilities.add(new KCapability("http://calix.com/e7-2/2.3/admin"));
		serverHello.setCapabilities(capabilities);
		return serverHello;
	}
}