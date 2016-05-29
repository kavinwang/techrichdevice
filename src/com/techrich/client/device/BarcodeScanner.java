package com.techrich.client.device;

import com.techrich.client.manager.AbstractCommDevice;


public abstract class BarcodeScanner extends AbstractCommDevice {
	
	public BarcodeScanner(){
		this.deviceId = "BarcodeScaner";
	}
	/**
	 * ����ɨ����ɨ�����
	 * @param timeout ��ʱʱ��(����)
	 * @throws DeviceException
	 */
	public abstract String scan(int timeout) throws Exception;
	public abstract boolean startScan() ;
	public abstract void stopScan() ;
}
