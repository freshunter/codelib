package org.netconf.server.ssh;

import org.apache.sshd.common.TcpipForwarder;
import org.apache.sshd.common.TcpipForwarderFactory;
import org.apache.sshd.common.session.ConnectionService;

/**
 * 
 * @author khuang
 *
 */
public class CTcpipForwarderFactory implements TcpipForwarderFactory
{
	   public TcpipForwarder create( ConnectionService service )
	   {
	      return new CTcpipForwarder( service );
	   }
	}
