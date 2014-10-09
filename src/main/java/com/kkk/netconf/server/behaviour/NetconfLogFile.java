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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.kkk.netconf.server.Server;

public class NetconfLogFile {
    static final String REQUEST = "Southbound Netconf Request XML:";
    static final String RESPONSE = "Southbound Netconf Response XML:";
    protected Logger log = LoggerFactory.getLogger(getClass());

    // File file;

    List<NetconfMsg> allMsg = new ArrayList<NetconfMsg>(100);

    // Map<String,String> request = new HashMap<String, String>(50);
    Map<String, NetconfMsg> request = new HashMap<String, NetconfMsg>(50);
    Map<String, NetconfMsg> response = new HashMap<String, NetconfMsg>(50);
    DocumentBuilder builder;
    // MessageDigest md;

    public static NetconfLogFile instance = null;

    public NetconfMsg getRequest(String md5) {
        return request.get(md5);
    }

    public NetconfMsg getResponseByRequest(String md5) {
        return response.get(request.get(md5).getResponseKey());
    }

    public NetconfMsg getResponse(String key) {
        return response.get(key);
    }

    //
    public synchronized static NetconfLogFile getInstance() {
	if (instance == null) {
	    instance = new NetconfLogFile();
	}
	return instance;
    }

    public void StudyNetconfMsg(String file) {
	log.info("study msg from log file:" + file);
	this.StudyNetconfMsg(new File(file));
    }

    public NetconfLogFile() {
	super();
	try {
	    builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public boolean exportMsg(String file) {
	log.info("export msg to file:" + file);
	List<String> l = new ArrayList<String>();
	l.add("#---Note when importting: one line one message. Reply and response message-id and nodename right by pair.");
	for (NetconfMsg nm : allMsg) {
	    l.add(nm.getMessage());
	}
	try {
	    FileUtils.writeLines(new File(file), Server.CHARSET.toString(), l);
	    return true;
        } catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
        }
    }

    public void importMsg(String file) {
	log.info("import msg from file:" + file);
	allMsg.clear();
	LineIterator it = null;
	try {
	    it = FileUtils.lineIterator(new File(file), Server.CHARSET.toString());
	    while (it.hasNext()) {
		String msg = it.nextLine();
		if(!msg.startsWith("#")) {
		    allMsg.add(procMsg(msg));
		}
	    }
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} finally {
	    LineIterator.closeQuietly(it);
	}
	updateMap();
    }

    public void StudyNetconfMsg(File file) {
	log.info("study msg from log file:" + file);
	LineIterator it = null;
	try {
	    it = FileUtils.lineIterator(file, Server.CHARSET.toString());
	    while (it.hasNext()) {
		String line = it.nextLine();
		NetconfMsg nm;
		String msg;
		int i;
		if (line.contains(REQUEST)) {
		    i = line.indexOf(REQUEST);
		    msg = line.substring(i + REQUEST.length());
		    allMsg.add(procMsg(msg));
		} else if (line.contains(RESPONSE)) {
		    i = line.indexOf(RESPONSE);
		    msg = line.substring(i + RESPONSE.length());
		    allMsg.add(procMsg(msg));
		} else {
		    // do nothing.
		}
	    }
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} finally {
	    LineIterator.closeQuietly(it);
	}
	updateMap();
    }

    private void updateMap() {
	for (NetconfMsg nm : allMsg) {
	    if (nm.isResponse()) {
		response.put(nm.getResponseKey(), nm);
	    } else {
		request.put(nm.getMd5(), nm);
	    }
	}
    }

    public NetconfMsg procMsg(String msg) {
	NetconfMsg nm = null;
	try {
	    Document doc = builder.parse(new InputSource(new StringReader(msg)));

	    Element root = doc.getDocumentElement();
	    String mi = root.getAttribute("message-id");
	    String name = root.getAttribute("nodename");
	    // if(!root.getAttribute("length").isEmpty()) {
	    // System.out.println(root.getAttribute("length") + " " +
	    // msg.trim().length());
	    // }
	    nm = new NetconfMsg(name, mi, msg.trim());
	    // System.out.println(nm.toString());
	    // System.out.println(nm.genResponseMessage("kkkkkk", "34"));
	    // System.out.println(nm.genResponseMessage("kkkkkk",
	    // "34").length());

	} catch (SAXException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return nm;
    }

     public static void main(String[] args) {
	NetconfLogFile nf = NetconfLogFile.getInstance();
	nf.StudyNetconfMsg("netconfLogFile.txt");
	nf.exportMsg("kkk.txt");
     }

}
