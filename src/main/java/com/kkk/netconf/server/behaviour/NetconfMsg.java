package com.kkk.netconf.server.behaviour;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import ch.ethz.ssh2.crypto.Base64;

public class NetconfMsg {
    static final int LEN_STR_LEN = 6;
    static final int REPLY_HEAD_STR_LEN = 53;
    static MessageDigest md;
    static {
	try {
	    md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
        }
    }
    
    private String messageId;
    private String nodeName;

    private String message;
    private String messageBodyNoHead;
    private String md5;
    private int length;
    private boolean isResponse = false;

    public NetconfMsg(String name, String mi, String msg) {
	setNodeName(name);
	setMessageId(mi);
	setMessage(msg);
	setResponse(msg.startsWith("<rpc-reply"));
    }

    public String genResponseMessage(String name, String messageid) {
        return isResponse?genResponseHeader(name, messageid) + this.messageBodyNoHead:"";
    }

    private String genResponseHeader(String name, String messageid) {
        return "<rpc-reply length=\"" + genLength(name, messageid)
        	+ "\" message-id=\"" + messageid
        	+ "\" nodename=\"" + name
        	+ "\">";
    }
    
    private String genLength(String name, String messageid) {
//	int len = this.length + getDiffer(name, messageid);
	int len = this.messageBodyNoHead.length() + REPLY_HEAD_STR_LEN + name.length() + messageid.length();
	String lenStr = Integer.toString(len);
	for (int i = LEN_STR_LEN, l = lenStr.length(); i > l ; i--) {
	    lenStr = "0" + lenStr;
        }
	return lenStr;
    }
    
    private int getDiffer(String name, String messageid) {
	return (name.length() + messageid.length()) - (this.nodeName.length() + this.messageId.length());
    }
    
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((md5 == null) ? 0 : md5.hashCode());
	return result;
    }
    
    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	NetconfMsg other = (NetconfMsg) obj;
	if (md5 == null) {
	    if (other.md5 != null)
		return false;
	} else if (!md5.equals(other.md5))
	    return false;
	return true;
    }
    
    @Override
    public String toString() {
	return "NetconfMsg [messageId=" + messageId + ", nodeName=" + nodeName + ", message=" + message
	        + ", messageBodyNoHead=" + messageBodyNoHead + ", md5=" + md5 + ", length=" + length + ", isResponse="
	        + isResponse + "]";
    }

    public String getResponseKey() {
	return nodeName + "-" + messageId;
    }
    
    public boolean isResponse() {
        return isResponse;
    }

    public void setResponse(boolean isResponse) {
        this.isResponse = isResponse;
    }
    
    public String getMessageId() {
        return messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    public String getNodeName() {
        return nodeName;
    }
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
        this.length = message.length();
        int index = message.indexOf(">");
        setMessageBodyNoHead(message.substring(index + 1));
    }
    public String getMessageBodyNoHead() {
        return messageBodyNoHead;
    }
    private void setMessageBodyNoHead(String messageBodyNoHead) {
        this.messageBodyNoHead = messageBodyNoHead;
        this.md5 = new String(Base64.encode(md.digest(messageBodyNoHead.getBytes())));
    }
    public String getMd5() {
        return md5;
    }
    public int getLength() {
        return length;
    }
    
    public static void main(String[] args) throws NoSuchAlgorithmException {
	System.out.println("<rpc-reply length=\"000346\" message-id=\"\" nodename=\"\">".length());
    }

}
