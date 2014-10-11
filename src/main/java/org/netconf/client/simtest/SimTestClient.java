package org.netconf.client.simtest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

/**
 * 
 * @author khuang
 *
 */
public class SimTestClient {

    public static void main(String[] args) {
	// TODO Auto-generated method stub

	NioSocketConnector connector = new NioSocketConnector();
	connector.getFilterChain().addLast(
	        "codec",
	        new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"),
	                LineDelimiter.NUL.getValue(), LineDelimiter.NUL.getValue()))); // 设置文本编码过滤器
	connector.getFilterChain().addLast("logger", new LoggingFilter());
	// connector.getFilterChain().addLast("codec",new
	// ProtocolCodecFilter(new ObjectSerializationCodecFactory())); //
	// 传输java对象
	connector.setConnectTimeoutMillis(3000);
	connector.setHandler(new SimClientHandler());// 设置事件处理器
	ConnectFuture cf = connector.connect(new InetSocketAddress("127.0.0.1", 1830));// 建立连接
	cf.awaitUninterruptibly();// 等待连接创建完成
	// cf.getSession().write("hello");// 发送消息
	// boolean fag = true;
	// while (fag) {
	// Scanner sc = new Scanner(System.in);
	// String str = sc.nextLine();
	// System.out.println(str);
	// cf.getSession().write(str);// 发送消息
	// if (str.equals("quit"))
	// fag = false;
	// }
	cf.getSession().getCloseFuture().awaitUninterruptibly();// 等待连接断开
	connector.dispose();
    }

}
