package com.kkk.netconf.server.ssh;

import org.apache.sshd.common.TcpipForwarder;
import org.apache.sshd.common.TcpipForwarderFactory;
import org.apache.sshd.common.session.ConnectionService;

public class CalixTcpipForwarderFactory implements TcpipForwarderFactory
{
	   public TcpipForwarder create( ConnectionService service )
	   {
	      return new CalixTcpipForwarder( service );
	   }
	}
