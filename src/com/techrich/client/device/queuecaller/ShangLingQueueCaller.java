package com.techrich.client.device.queuecaller;

import java.util.ArrayList;
import java.util.List;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.QueueCallerScreen;
import com.techrich.client.manager.ConfigManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;

public class ShangLingQueueCaller extends QueueCallerScreen {
	public static final byte DISPLAY_SPEED_FAST = 0x0B;
	public static final byte DISPLAY_SPEED_NORMAL = 0x0F;
	public static final byte DISPLAY_SPEED_SLOW = 0x0E;
	private static final int DEFAULT_DISPLAY_LINE = 3;

	
	private static final int MAIN_SCREEN = 0; //����Ϊ��һ���˿�
	private static final int COUNTER_SCREEN = 1; //��̨��Ϊ�ڶ����˿�
	
	private List<String> displayer = new ArrayList<String>();
	
	int maxMainScreenDisplayLines = DEFAULT_DISPLAY_LINE;
	public ShangLingQueueCaller(){
		this.deviceId = "CALLER-DISPLAY-SL";
	}
	
	public String getDeviceName() {
		return deviceName!=null?deviceName:"���������";
	}
	
	@Override
	public void initDevice() throws DeviceInitException {
		
		maxMainScreenDisplayLines = Integer.parseInt(ConfigManager.getDefault().getConfigElementDef("queue.maindisplay.lines", String.valueOf(DEFAULT_DISPLAY_LINE)));

		try{
			displayCounterScreen(0, "��ӭ���٣�");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void deviceCheck() throws Exception {
		
	}

	@Override
	public void welcome(int counterNum) {
		try{
			displayCounterScreen(counterNum, "��ӭ���٣�");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void callNum(int counterNum, String num) {
		String display = "��"+Tools.adjustString(num, '0', 4, true)+"��";
		try{
			displayCounterScreen(counterNum, display);
		}catch(Exception e){
			e.printStackTrace();
		}

		String mainDisplay = "��" +Tools.adjustString(num, '0', 4, true) +"��" +Tools.adjustString(String.valueOf(counterNum), '0', 2, true) +"�Ŵ���";

		try{
			System.out.println("========================================");
			displayMainScreen(mainDisplay);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void pauseCounter(int counterNum) {
		try{
			displayCounterScreen(counterNum, "��ͣ����");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void service(int counterNum, String num) {
		try{
			//String cName = Tools.adjustString(String.valueOf(counterNum), '0', 2, true);
			
			displayCounterScreen(counterNum, /*cName+*/"����"+num);
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	@Override
	public void freeCounter(int counterNum,String ticketNum) {
		try{
			displayCounterScreen(counterNum,Tools.adjustString(String.valueOf(counterNum), '0', 2, true)+"�Ŵ���");
		}catch(Exception e){
			e.printStackTrace();
		}
		if(ticketNum != null){
			String mainDisplay = "��" +Tools.adjustString(ticketNum, '0', 4, true) +"��" +Tools.adjustString(String.valueOf(counterNum), '0', 2, true) +"�Ŵ���";
			displayer.remove(mainDisplay);
			
			try{
				displayMainScreen(null);
			}catch(Exception e){
			}
		}
	}

	/**
	 * ��infoΪnullʱ��ֻ��ˢ����Ļ
	 * @param info
	 * @throws Exception
	 */
	private void displayMainScreen(String info) throws Exception {
		changePort(MAIN_SCREEN);
		
		System.out.println( "����ʾ����ʾ:" + info==null?"":info);
		if(info != null && !info.trim().equals("")){
			if(!displayer.contains(info)){
				displayer.add(info);
				while(displayer.size() > maxMainScreenDisplayLines) displayer.remove(0);
			}else{
				System.out.println( "����ʾ���Ѿ�����:[" + info+"]������ʾͬ������Ϣ�� ");
				return;
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < maxMainScreenDisplayLines; i++){
			String line = displayer.size() > i ? displayer.get(i) : "";
			sb.append(line);
		}
		
		System.out.println( "��Ҫ���͵�����ʾ��������:" + sb.toString());
		try{
			
			byte[] data = sb.toString().getBytes("GBK");
			sendCmd2Display((byte)0x00, new byte[]{(byte)0xaa, (byte)0xaa, (byte)0xaa, 0x4e}, new byte[]{'S', 0x00, 0x64}, data);
			sendCmd2Display((byte)0x01, new byte[]{(byte)0xaa, (byte)0xaa, (byte)0xaa, 0x4e}, new byte[]{'S', 0x00, 0x64}, data);
		}catch(Exception e){
			System.out.println("�������ݵ��������ִ���:");
			e.printStackTrace();
		}
//		ByteBuffer bb = new ByteBuffer();
//		bb.append((byte)0x01).append(new byte[]{'S', 0x00, 0x64}).append(data);
//		int byteLen = bb.length() + 4;
//		bb.insert(0, new byte[]{(byte)byteLen});
//		bb.append(getCrc(bb.getValue()));
//		bb.append((byte)0x1a);
//		bb.insert(0, new byte[]{(byte)0xaa, (byte)0xaa, (byte)0xaa, 0x4e});
//		sendDirect(bb.getValue());
	}

	private void displayCounterScreen(int winNum, String info) throws Exception {
		changePort(COUNTER_SCREEN);
		
		if(info == null) return;
		System.out.println("��ʾ��[" + winNum + "] ��ʾ:" + info);

		byte[] data = info.getBytes("GBK");
		ByteBuffer bb = new ByteBuffer();
		bb.append((byte)0x1D);
		if(winNum >= 10){
			bb.append((byte)(winNum / 10));
			bb.append((byte)(winNum % 10));
		}else{
			bb.append((byte)0x00);
			bb.append((byte)winNum);
		}
		
		bb.append(DISPLAY_SPEED_SLOW); //����ȱʡʹ������

		bb.append(data).append((byte)0x10);
		sendDirect(bb.getValue());
		Thread.sleep(20);
		sendDirect(bb.getValue());
	}
	
	private byte[] getCrc(byte[] datas) {
		byte crc = 0x00;
		for(int i = 0; i < datas.length; i++)	crc ^= datas[i];
		return new byte[]{(byte)((crc & 0xf0) >> 4), (byte)(crc & 0x0f)};
	}

	private void sendCmd2Display(byte address, byte[] protocol, byte[] cmd, byte[] data) throws Exception {
		ByteBuffer bb = new ByteBuffer();
		bb.append(address).append(cmd).append(data);
		int byteLen = bb.length() + 4;
		bb.insert(0, new byte[]{(byte)byteLen});
		bb.append(getCrc(bb.getValue()));
		bb.append((byte)0x1a);
		bb.insert(0, protocol);
		byte[] datas = bb.getValue();
		System.out.println("���͵����������ݣ�"+Tools.bytes2hex(datas));
		try{Thread.sleep(100);}catch(Exception e){}//�ӳ�100����,�п��������������ط�
		sendDirect(datas);
		try{Thread.sleep(50);}catch(Exception e){}//�ӳ�100����,�п��������������ط�
	}

}
