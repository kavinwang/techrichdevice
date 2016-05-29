package com.techrich.client.device;

public class DeviceInitException extends DeviceException {
	private static final long serialVersionUID = -5838644903924525662L;
	public DeviceInitException(){
		super();
	}
	public DeviceInitException(String msg){
		super(msg);
	}
	public DeviceInitException(String msg,Throwable t){
		super(msg,t);
	}
	public DeviceInitException(Throwable t){
		super(t);
	}

}
