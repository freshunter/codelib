package com.kkk.netconf.server.behaviour;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ch.ethz.ssh2.crypto.Base64;

public class NetconfLogFile {
    static final String REQUEST = "Southbound Netconf Request XML:";
    static final String RESPONSE = "Southbound Netconf Response XML:";

    File file;
    
    List<NetconfMsg> allMsg = new ArrayList<NetconfMsg>(100);

//    Map<String,String> request = new HashMap<String, String>(50);
    Map<String,NetconfMsg> request = new HashMap<String, NetconfMsg>(50);
    Map<String,NetconfMsg> response = new HashMap<String, NetconfMsg>(50);
    DocumentBuilder builder;
//    MessageDigest md;
	

	
    public NetconfLogFile(File logFile) {
	super();
	this.file = logFile;
	try {
	    builder = DocumentBuilderFactory   
	                .newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
        }
//	try {
//	    md = MessageDigest.getInstance("MD5");
//        } catch (NoSuchAlgorithmException e) {
//	    // TODO Auto-generated catch block
//	    e.printStackTrace();
//        }
    }
    
    public void exportResult() {
	allMsg
    }
    
    public void importResult() {
//	allMsg
    }

    public void LearnNetconfMsg() {
	LineIterator it = null;
//	ArrayList<String> al = new ArrayList<String>(50);
	try {
	    it = FileUtils.lineIterator(file, "UTF-8");
	    while (it.hasNext()) {
		String line = it.nextLine();
		NetconfMsg nm;
		if(line.contains(REQUEST)) {
		    nm = procMsg(line, REQUEST);
		    request.put(nm.getMd5(), nm);
		} else if(line.contains(RESPONSE)) {
		    nm = procMsg(line, RESPONSE);
		    response.put(nm.getResponseKey(), nm);
		} else {
		    //do nothing.
		}
	    }
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} finally {
	    LineIterator.closeQuietly(it);
	}
    }

    private NetconfMsg procMsg(String line, String type) throws IOException {
	int i = line.indexOf(type);
	String msg = line.substring(i + type.length());
	NetconfMsg nm = null;
	try {
	    Document doc = builder   
	             .parse(new InputSource(new StringReader(msg)));
	    
	    Element root = doc.getDocumentElement();
	    String mi = root.getAttribute("message-id");
	    String name = root.getAttribute("nodename");
//	    if(!root.getAttribute("length").isEmpty()) {
//		System.out.println(root.getAttribute("length") + " " + msg.trim().length());
//	    }
	    nm = new NetconfMsg(name, mi, msg.trim(), type.equals(RESPONSE));
	    allMsg.add(nm);
		System.out.println(nm.toString());
		System.out.println(nm.genResponseMessage("kkkkkk", "34"));
	    
	} catch (SAXException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return nm;
    }
    
    public static void main(String[] args) {
	new NetconfLogFile(new File("netconfLogFile.txt")).LearnNetconfMsg();
    }

}
