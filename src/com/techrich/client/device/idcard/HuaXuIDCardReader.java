package com.techrich.client.device.idcard;

import java.util.HashMap;
import java.util.Map;

import com.techrich.client.Activator;
import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.IDCardChecker;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.JNALoader;
import com.techrich.tools.Tools;

public class HuaXuIDCardReader extends IDCardChecker {
	public static final String libname = JNALoader.load(new String[]{"reposity\\extlibs\\huaxu"}, new String[]{"sdtapi", "WltRS"});

	public static native int SDT_OpenPort(int iPort);
	public static native int SDT_GetCOMBaud(int iPort, int[] band);
	public static native int SDT_SetCOMBaud(int iPort, int curband, int newBaud);
	public static native int SDT_ClosePort(int iPort);
	public static native int SDT_ResetSAM(int iPort, int openPortInside);
	public static native int SDT_SetMaxRFByte(int iPort, int length, int openPortInside);
	public static native int SDT_GetSAMStatus(int iPort, int openPortInside);
	public static native int SDT_GetSAMIDToStr(int iPort, byte[] modelSerialNo, int openPortInside);
	public static native int SDT_StartFindIDCard(int iPort, byte[] publicManagerInfo, int openPortInside);//0x9f:�ɹ� 0x80:ʧ��
	public static native int SDT_SelectIDCard(int iPort, byte[] publicManagerMsg, int openPortInside);//0x90:�ɹ� 0x81:ʧ��
	public static native int SDT_ReadMngInfo(int iPort, byte[] publicManagerMsg, int openPortInside);//0x90:�ɹ� ����:ʧ�ܣ����궨��
	public static native int SDT_ReadBaseMsg(int iPort, byte[] pubCHMsg, int[] pubCHMsgLen, byte[] pubPHMsg, int[] pubPHMsgLen, int openPortInside);//0x90:�ɹ� ����:ʧ��
	public static native int SDT_ReadNewAppMsg(int iPort, byte[] pubAppMsg, int[] pubAppMsgLen, int openPortInside);//0x90:�ɹ� ����:ʧ��

	public HuaXuIDCardReader(){
		this.deviceId = "IDCARD-HUAXU";
	}
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"�������֤������";
	}
	
	private HashMap<String, String> cardInfoMap = new HashMap<String, String>();
	int port = -1;
	private static int OPEN_PORT_INSIDE = 1;
//	byte[] publicManagerInfo = new byte[4];
//	byte[] publicManagerMsg = new byte[8];

	public void openDevice() throws Exception {
		String commport = getDeviceDesc();
		if(commport.toLowerCase().startsWith("com")){
			port = Integer.parseInt(commport.substring(3));
		}else port = Integer.parseInt(commport);

		if(OPEN_PORT_INSIDE == 0){
			int suc = SDT_OpenPort(port);
			if(suc != 0x90) throw new Exception("�����֤ʶ����ʧ�ܣ�comm:" + port);
		}
	}

	public void closeDevice() {
		if(OPEN_PORT_INSIDE == 0){
			if(port == -1) return;
			SDT_ClosePort(port);
		}
	}

	private int Authenticate() {
		byte[] publicManagerInfo = new byte[255];
		byte[] publicManagerMsg = new byte[255];
		int ret = SDT_StartFindIDCard(port, publicManagerInfo, OPEN_PORT_INSIDE);
		if((ret & 0x00ff) != 0x9f){
			LogManager.logInfo(Activator.PLUGIN_ID, "Ѳ��ʧ�ܣ�" + Tools.bytes2hex(new byte[]{(byte)ret}));
			return 0;
		}
		ret = SDT_SelectIDCard(port, publicManagerMsg, OPEN_PORT_INSIDE);
		if((ret & 0x00ff) == 0x90) return 1;
		LogManager.logInfo(Activator.PLUGIN_ID, "ѡ��ʧ��:" + Tools.bytes2hex(new byte[]{(byte)ret}));
		return 0;//��֤ʧ��
	}

	@Override
	public Map<String, String> read() throws Exception {
		cardInfoMap=new HashMap<String, String>();

		if(port <= 0) throw new Exception("��������ȷ�Ķ˿�");
		int rv = Authenticate();
		if(rv != 1){
			LogManager.logInfo(Activator.PLUGIN_ID, "���֤û���ú�:" + rv);
			throw new Exception("���֤û���ú�:" + rv);
		}

		byte[] pucCHMsg = new byte[256];
		int[] puiCHMsgLen = new int[1];
		byte[] pucPHMsg = new byte[1024];
		int[] puiPHMsgLen = new int[1];

		rv = SDT_ReadBaseMsg(port, pucCHMsg, puiCHMsgLen, pucPHMsg, puiPHMsgLen, OPEN_PORT_INSIDE);
		if((rv & 0x00ff) == 0x90){
			pucCHMsg = reverseWord(pucCHMsg);
			java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(256);
			bb.put(pucCHMsg);
			bb.flip();
			byte[] data = null;
			data = new byte[30];
			bb.get(data);
			cardInfoMap.put("Name", new String(data, "UTF16").trim());
			data = new byte[2];
			bb.get(data);
			cardInfoMap.put("Sex", new String(data, "UTF-16").trim().equals("2") ? "Ů" : "��");//����͹��ڵĲ�̫һ��//memcpy(&wt_Sex[0], &pucCHMsg[30], 2);
			data = new byte[4];
			bb.get(data);
			cardInfoMap.put("Nation", new String(data, "UTF-16").trim());
			data = new byte[16];
			bb.get(data);
			cardInfoMap.put("Birthday", new String(data, "UTF-16").trim()); 
			data = new byte[70];
			bb.get(data);
			cardInfoMap.put("Address", new String(data, "UTF-16").trim()); 
			data = new byte[36];
			bb.get(data);
			cardInfoMap.put("IdCode", new String(data, "UTF-16").trim()); 
			data = new byte[30];
			bb.get(data);
			cardInfoMap.put("Department", new String(data, "UTF-16").trim());
			data = new byte[16];
			bb.get(data);
			cardInfoMap.put("StartDate", new String(data, "UTF-16").trim());  
			data = new byte[16];
			bb.get(data);
			cardInfoMap.put("EndDate", new String(data, "UTF-16").trim()); 
			return cardInfoMap;
		}else{
			throw new Exception("��ȡ���֤����ʧ��!");
		}
	}

	private byte[] reverseWord(byte[] orgs) {
		byte[] result;
		if(orgs.length % 2 != 0){
			result = new byte[orgs.length + 1];
			System.arraycopy(orgs, 0, result, 0, orgs.length);
			result[result.length - 1] = 0x0;
		}else{
			result = new byte[orgs.length];
			System.arraycopy(orgs, 0, result, 0, orgs.length);
		}

		byte temp = 0;
		for(int i = 0; i < result.length; i += 2){
			temp = result[i];
			result[i] = result[i + 1];
			result[i + 1] = temp;
		}
		return result;
	}

	@Override
	public void initDevice() throws DeviceInitException {
		int[] baud = {0};
		int res = -1;
		int[] bands = new int[]{9600,19200,38400,57600,115200};
		Boolean setBandDone = false;
		
		for(int i = 0;i< bands.length;i++){
			res = SDT_SetCOMBaud(port, bands[i], 115200);
			try{Thread.sleep(2);}catch(Exception e){}
			if(res == 0x90){
				LogManager.logError("���ò����ʳɹ���"+bands[i]);
				setBandDone = true;
				break;
			}
			else if(res == 0x1)LogManager.logError("�˿ڴ�ʧ��/�˿ںŲ��Ϸ���"+bands[i]);
			else if(res == 0x2)LogManager.logError(" ��ʱ�����ò��ɹ���"+bands[i]);
			else if(res == 3)LogManager.logError( "���ò�����ʱ���ݴ������"+bands[i]);
			else if(res == 0x21)LogManager.logError(" ���������ֵ����"+bands[i]);
			else LogManager.logError(" ���ò����ʴ���"+bands[i]+"  ret:"+res );
		}
		if(!setBandDone)throw new DeviceInitException("���ò�����ʧ�ܣ�"+res);
		res = SDT_GetCOMBaud(port, baud);
		if(res == 0x90)LogManager.logError( "���֤������ʹ�õĲ�����Ϊ��" + baud[0]);
		else if(res == 1)throw new DeviceInitException( "�˿ڴ�ʧ��/�˿ںŲ��Ϸ� ");
		else if(res == 3)throw new DeviceInitException( "���ݴ������ ");
		else if(res == 5)throw new DeviceInitException( "�޷���ø� SAM_V �Ĳ����ʣ��� SAM_V ���ڲ����á� ");
		else throw new DeviceInitException( "δ��������: "+res);


		res =  SDT_GetSAMStatus (port,OPEN_PORT_INSIDE);
		if(res == 0x90)LogManager.logError( "s���֤SAM״̬ok��");
		else if(res == 0x60)throw new DeviceInitException("�Լ�ʧ�ܣ����ܽ������� ");
		else throw new DeviceInitException("�Լ�ʧ�ܣ��������� "+res);
		
		byte[] modelSerialNo = new byte[255];
		res = SDT_GetSAMIDToStr(port, modelSerialNo, OPEN_PORT_INSIDE);//OPEN_PORT_INSIDE
		if(res == 0x90)LogManager.logError( "���֤��������ȫģ�����к�:" + new String(modelSerialNo).trim());
		else  throw new DeviceInitException("ȡ��ȫģ�����к�ʧ��:" + res);
	}

	@Override
	public String getDeviceIdentifer() throws Exception {
		int[] baud = {0};
		int res = -1;
		try{
			res = SDT_GetCOMBaud(port, baud);
		}catch(Exception e){
			throw new Exception("û���ṩ�Զ���⹦��");
		}
		try{
			byte[] modelSerialNo = new byte[255];
			res = SDT_GetSAMIDToStr(port, modelSerialNo, OPEN_PORT_INSIDE);
			if(res != 144) throw new DeviceInitException("ȡ��ȫģ�����к�ʧ��:" + res);
			return new String(modelSerialNo).trim();
		}catch(Exception e){
			throw new Exception("û���ṩ�Զ���⹦��");
		}

	}

	@Override
	public void deviceCheck() throws Exception {

	}

	@Override
	public void setDeviceAppParams(HashMap<String, String> params) throws Exception {
	}

}
