package com.kkk.netconf.server.behaviour;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NetconfLogFileTest {
    NetconfLogFile nf;
    String str1 = "<rpc message-id=\"33\" nodename=\"NTWK-kkk\"><action><action-type>test</action-type></action></rpc>";
    String str2 = "<rpc-reply length=\"002281\" message-id=\"33\" nodename=\"NTWK-kkk\"><ok/></rpc-reply>";
    String key = "NTWK-kkk-33"; 

    @Before
    public void setUp() throws Exception {
	nf = NetconfLogFile.getInstance();
	nf.StudyNetconfMsg("netconfLogFile.txt");
    }
    
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testImportMsg() throws IOException {
	String file = "testimport.txt";
	nf.exportMsg(file);
	int reqlen = nf.request.size();
	int reslen = nf.response.size();
	
	List<String> l = new ArrayList<String>();	
	l.add("#---Note when importting: one line one message. Reply and response message-id and nodename right by pair.");
	l.add(str1);
	l.add(str2);
	FileUtils.writeLines(new File(file), "UTF-8", l, true);
	
	nf.importMsg(file);

	Assert.assertEquals("Request should add one count", reqlen + 1, nf.request.size());
	Assert.assertEquals("Response should add one count", reslen + 1, nf.response.size());
	Assert.assertTrue("Response should contain new msg", nf.response.containsKey(key));

	NetconfMsg nm = null;
	for (Map.Entry<String,NetconfMsg> p : nf.request.entrySet()) {
	    if (p.getValue().getResponseKey().equals(key)) {
		nm = p.getValue();
	    }
        }
	Assert.assertNotNull("Request should contain new msg", nm);
	Assert.assertEquals("Request msg is the same", str1, nm.getMessage());
	Assert.assertEquals("Response msg is the same", str2, nf.response.get(nm.getResponseKey()).getMessage());
    }

}
