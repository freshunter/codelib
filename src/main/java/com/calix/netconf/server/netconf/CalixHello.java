package com.calix.netconf.server.netconf;

import java.util.ArrayList;

import net.i2cat.netconf.rpc.Capability;
import net.i2cat.netconf.rpc.RPCElement;

public class CalixHello extends RPCElement {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	ArrayList<Capability>	capabilities;
	String					sessionId;
	String					messageId;

	public CalixHello() {
		messageId = "0";
	}

	public String toXML() {

		String xml = "<hello xmlns=\"" + Capability.BASE + "\">";

		xml += "\t<capabilities>";

		for (Capability capability : capabilities) {
			xml += "\t\t<capability>" + capability + "</capability>";
		}

		xml += "\t</capabilities>";

		if (sessionId != null)
			xml += "\t<session-id>" + sessionId + "</session-id>";

//		<session-timeout>1860</session-timeout>
//		<request-timeout>20</request-timeout>
		xml += "\t<session-timeout>1860</session-timeout><request-timeout>20</request-timeout>";

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
}