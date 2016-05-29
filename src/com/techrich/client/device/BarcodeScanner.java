package com.techrich.client.device;

import com.techrich.client.manager.AbstractCommDevice;


public abstract class BarcodeScanner extends AbstractCommDevice {
	
	public BarcodeScanner(){
		this.deviceId = "BarcodeScaner";
	}
	/**
	 * 条码扫描器扫描过程
	 * @param timeout 超时时间(毫秒)
	 * @throws DeviceException
	 */
	public abstract String scan(int timeout) throws Exception;
	public abstract boolean startScan() ;
	public abstract void stopScan() ;
}
