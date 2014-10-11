package com.kkk.netconf.server.behaviour;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author khuang
 *
 */
public class NetconfMsgTest {
    NetconfLogFile nf;

    @Before
    public void setUp() throws Exception {
	nf = NetconfLogFile.getInstance();
	nf.StudyNetconfMsg("netconfLogFile.txt");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGenResponseMessage() {
	List<NetconfMsg> ls = nf.allMsg;
	boolean hasResponse = false;
	for (NetconfMsg nm : ls) {
	    if (nm.isResponse()) {
		hasResponse = true;
		System.out.println(nm.genResponseMessage("kkkkkk", "34"));
		System.out.println(nm.genResponseMessage("kkkkkk", "34").length());
		String tmp = nm.genResponseMessage("kkkkkk", "34");
		Assert.assertTrue("The new netconf msg has the right length info.", tmp.contains(tmp.length()+"\""));
	    }
	}
	if (!hasResponse) {
	    fail("No response message!");
	}
    }

}
