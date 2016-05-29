package com.techrich.client.device;

public class DeviceException extends Exception {
	private static final long serialVersionUID = 457182845138709438L;
	public DeviceException(){
		super();
	}
	public DeviceException(String msg){
		super(msg);
	}
	public DeviceException(String msg,Throwable t){
		super(msg,t);
	}
	public DeviceException(Throwable t){
		super(t);
	}

}
