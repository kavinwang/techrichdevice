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
		//ȡ��ɨ��ָ��:E50400
		long start = System.currentTimeMillis();
		while(true){
			if(System.currentTimeMillis() - start > timeout)throw new Exception("ɨ��ʧ��!");
			try{
				comm2bardecoder(Tools.hex2bytes("E40400"));//��ʼɨ�貢����
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
			comm2bardecoder(Tools.hex2bytes("E90400"));//ȱʡ�ϵ�ʹ��ڼ���״̬,����������ָ��رյĻ�,����ʹ�ñ�ָ����м���
			return true;
		}catch(Exception e){
			return false;
		}
	}

	@Override
	public void stopScan() {
		try{
			comm2bardecoder(Tools.hex2bytes("E50400"));//ȡ����ǰ��ɨ��,
		}catch(Exception e){
		}
		try{
			comm2bardecoder(Tools.hex2bytes("EA0400"));//�ر�ɨ����,
		}catch(Exception e){
		}
	}

	@Override
	public void initDevice() throws DeviceInitException {
		try{
			comm2bardecoder(Tools.hex2bytes("C60408008A08"));//80A8 ����ģʽ
			comm2bardecoder(Tools.hex2bytes("C6040800BFB7"));//BFB7 47�ȿ�ɨ��
			comm2bardecoder(Tools.hex2bytes("C6040800EB00"));//û��ǰ׺�ͺ�׺
		}catch(Exception e){
			throw new DeviceInitException("ɨ������ʼ��ʧ��!");
		}
	}

	@Override
	public void deviceCheck() throws Exception {

	}

	// ================================================================
	private void comm2bardecoder(byte[] sends) throws Exception {
		ByteBuffer bb = new ByteBuffer(sends);
		bb.insert(0, new byte[]{(byte)(sends.length + 1)}); // �ӳ��� 1
		bb.append(getCRCCode(bb.getValue()));
		
		clearBuffer();
		sendDirect(new byte[]{0x00}); // ����
		Thread.sleep(100); // �ȴ�15ms
		sendDirect(bb.getValue()); // �������
		Thread.sleep(300); // �ȴ�15ms
		
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
