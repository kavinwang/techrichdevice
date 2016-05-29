package com.techrich.client.device.barcode;

import com.techrich.client.device.BarcodeScanner;
import com.techrich.client.device.DeviceInitException;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;
public class UE988Barcode extends BarcodeScanner {

	public UE988Barcode(){
		this.deviceId = "UE988BARCODER";
	}
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"UE988Barcode";
	}
	
	@Override
	public String scan(int timeout) throws Exception {
		//取消扫描指令:E50400
		long start = System.currentTimeMillis();
		while(true){
			if(System.currentTimeMillis() - start > timeout)throw new Exception("扫描失败!");
			try{
				comm2bardecoder(Tools.hex2bytes("E40400"));//开始扫描并解码
				byte[] datas = recieveData(8);
				return new String(datas);
			}catch(Exception e){
				continue;
			}
		}
	}

	@Override
	public boolean startScan() {
		try{
			comm2bardecoder(Tools.hex2bytes("E90400"));//缺省上电就处于激活状态,如果被下面的指令关闭的话,可以使用本指令进行激活
			return true;
		}catch(Exception e){
			return false;
		}
	}

	@Override
	public void stopScan() {
		try{
			comm2bardecoder(Tools.hex2bytes("E50400"));//取消当前的扫描,
		}catch(Exception e){
		}
		try{
			comm2bardecoder(Tools.hex2bytes("EA0400"));//关闭扫描器,
		}catch(Exception e){
		}
	}

	@Override
	public void initDevice() throws DeviceInitException {
		try{
			comm2bardecoder(Tools.hex2bytes("C60408008A08"));//80A8 主机模式
			comm2bardecoder(Tools.hex2bytes("C6040800BFB7"));//BFB7 47度宽扫描
			comm2bardecoder(Tools.hex2bytes("C6040800EB00"));//没有前缀和后缀
		}catch(Exception e){
			throw new DeviceInitException("扫描器初始化失败!");
		}
	}

	@Override
	public void deviceCheck() throws Exception {

	}

	// ================================================================
	private void comm2bardecoder(byte[] sends) throws Exception {
		ByteBuffer bb = new ByteBuffer(sends);
		bb.insert(0, new byte[]{(byte)(sends.length + 1)}); // 加长度 1
		bb.append(getCRCCode(bb.getValue()));
		
		clearBuffer();
		sendDirect(new byte[]{0x00}); // 唤醒
		Thread.sleep(100); // 等待15ms
		sendDirect(bb.getValue()); // 发送命令；
		Thread.sleep(300); // 等待15ms
		
		bb.reset();
		byte[] len = recieveData(1);
		bb.append(len);
		byte[] data = recieveData((int)(len[0]&0xff)+1);
		bb.append(data);
		System.out.println(Tools.bytes2hex(bb.getValue()));
	}

	private byte[] getCRCCode(byte[] value) {
		int crc = 0;
		for(int i = 0; i < value.length; i++) crc += (value[i] & 0xff);
		crc = ~crc + 0x01;
		return Tools.htons((short)crc);
	}

}
