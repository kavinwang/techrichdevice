package com.techrich.client.device.idcard;

import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Structure;
import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.IDCardChecker;
import com.techrich.tools.JNALoader;
import com.techrich.tools.Tools;

public class SynjonesIDCardReader extends IDCardChecker {
	public static final String libname = JNALoader.load(new String[]{"reposity\\extlibs\\xzx"}, new String[]{"SynIDCardAPI", "sdtapi", "WltRS"});
	private static String preamble = "AAAAAA9669";//new byte[]{(byte)0xAA,(byte)0xAA,(byte)0xAA,(byte)0x96,0x69};

	//SAM端口函数
	public static native int Syn_SetMaxRFByte(int iPort, byte ucByte, int bIfOpen);
	public static native int Syn_GetCOMBaudEx(int iPort);
	public static native int Syn_SetCOMBaud(int iPort, int uiCurrBaud, int uiSetBaud);
	public static native int Syn_OpenPort(int iPort);
	public static native int Syn_ClosePort(int iPort);

	//SAM类函数
	public static native int Syn_ResetSAM(int iPort, int iIfOpen);
	public static native int Syn_GetSAMStatus(int iPort, int iIfOpen);
	public static native int Syn_GetSAMID(int iPort, byte[] pucSAMID, int iIfOpen);

	//身份证卡类函数
	public static native int Syn_StartFindIDCard(int iPort, byte[] pucIIN, int iIfOpen);
	public static native int Syn_SelectIDCard(int iPort, byte[] pucSN, int iIfOpen);
	public static native int Syn_ReadMsg(int iPort, int iIfOpen, IDCardData.ByReference pINCardData);
//	public static native int Syn_ReadBaseMsg(int iPort, byte[] pucCHMsg, IntByReference puiCHMsgLen, byte[] pucPHMsg, IntByReference puiPHMsgLen, int iIfOpen);
	public static native int Syn_ReadBaseMsg(int iPort, byte[] pucCHMsg, int[] puiCHMsgLen, byte[] pucPHMsg, int[] puiPHMsgLen, int iIfOpen);
	public static native int Syn_FindReader();

	//设置附加功能函数
	public static native int Syn_SetPhotoPath(int iOption, String cPhotopath);
	public static native int Syn_SetPhotoType(int iType);
	public static native int Syn_SetPhotoName(int iType);
	public static native int Syn_SetSexType(int iType);
	public static native int Syn_SetNationType(int iType);
	public static native int Syn_SetBornType(int iType);
	public static native int Syn_SetUserLifeBType(int iType);
	public static native int Syn_SetUserLifeEType(int iTyp, int iOption);
	private HashMap<String, String> cardInfoMap = new HashMap<String, String>();

	public SynjonesIDCardReader() {
		this.deviceId = "IDCARD-SYNJONES";
	}

	@Override
	public String getDeviceName() {
		return deviceName != null ? deviceName : "新中新身份证读卡器";
	}

	int port = -1;
	private static int OPEN_PORT_INSIDE = 1;
	byte[] publicManagerInfo = new byte[4];
	byte[] publicManagerMsg = new byte[8];

	@Override
	public void initDevice() throws DeviceInitException {
		int nRet = Syn_GetSAMStatus(port, 0);
		if(nRet == 0) System.out.println("读取SAM模块成功");
		else{
			nRet = Syn_ResetSAM(port, 0);
			if(nRet == 0) System.out.println("复位SAM模块成功");
			else{
				nRet = Syn_GetSAMStatus(port, 0);
				if(nRet != 0) throw new DeviceInitException("复位SAM模块失败");
			}
		}

		if(Syn_SetMaxRFByte(port, (byte)80, 0) == 0) System.out.println("设置最大通讯字节数成功!");
		else throw new DeviceInitException("设置最大通讯字节数出错!");

		byte[] strSAMID = new byte[16];
		if(Syn_GetSAMID(port, strSAMID, 0) == 0){
			System.out.println(Tools.bytes2hex(strSAMID));
		}else{
			throw new DeviceInitException("读取SAM ID号错误!");
		}

	}

	public void openDevice() throws Exception {
		String commport = getDeviceDesc();
		if(commport.toLowerCase().startsWith("com")){
			port = Integer.parseInt(commport.substring(3));
		}else port = Integer.parseInt(commport);

		int nRet = Syn_OpenPort(port);
		if(nRet != 0){ throw new Exception("打开端口错误"); }
	}

	public void closeDevice() {
		if(port == -1) return;
		Syn_ClosePort(port);
	}

	@Override
	public Map<String, String> read() throws Exception {
		cardInfoMap = new HashMap<String, String>();

		if(port <= 0) throw new Exception("请设置正确的端口");
		
		byte[] pucIIN = new byte[8];
		byte[] pucSN = new byte[8];
		int nRet = Syn_StartFindIDCard(port, pucIIN, 0);
		if(nRet !=0 )throw new Exception("身份证没放置好");
		nRet = Syn_SelectIDCard(port, pucSN, 0);
		if(nRet !=0 )throw new Exception("身份证没放置好");
		
//		IDCardData.ByReference idcardData = new IDCardData.ByReference();
//		Syn_SetSexType(1);
//		Syn_SetNationType(1);
//		Syn_SetBornType(2);
//		Syn_SetUserLifeBType(3);
//		Syn_SetUserLifeEType(4,1);
//		if(Syn_ReadMsg(port, 0, idcardData) == 0){
//			System.out.println("读取身份证信息成功!");
//			try{
//				cardInfoMap.put("Name", new String(idcardData.Name, "GBK"));
//				cardInfoMap.put("Sex", new String(idcardData.Sex, "GBK"));
//				cardInfoMap.put("Nation", new String(idcardData.Nation, "GBK"));
//				cardInfoMap.put("Birthday",new String(idcardData.Born, "GBK"));
//				cardInfoMap.put("Address",new String(idcardData.Address, "GBK"));
//				cardInfoMap.put("IdCode",new String(idcardData.IDCardNo, "GBK"));
//				cardInfoMap.put("Department", new String(idcardData.GrantDept, "GBK"));
//				cardInfoMap.put("StartDate", new String(idcardData.UserLifeBegin, "GBK"));
//				cardInfoMap.put("EndDate", new String(idcardData.UserLifeEnd, "GBK"));
////				 new String(idcardData.PhotoFileName, "GBK");
//				return cardInfoMap;
//			}catch(Exception une){
//				throw new Exception("解析身份证信息错误!");
//			}
//		}else{
//			throw new Exception("读取身份证信息错误!");
//		}
		
		byte[] pucCHMsg = new byte[256];
		int[] puiCHMsgLen = new int[1];
		byte[] pucPHMsg = new byte[1024];
		int[] puiPHMsgLen = new int[1];


		if(Syn_ReadBaseMsg(port, pucCHMsg, puiCHMsgLen, pucPHMsg, puiPHMsgLen, OPEN_PORT_INSIDE)==0){
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
			cardInfoMap.put("Sex", new String(data, "UTF-16").trim().equals("2") ? "女" : "男");
			data = new byte[4];
			bb.get(data);
			cardInfoMap.put("Nation", NATIONS.get(new String(data, "UTF-16").trim()));
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
			throw new Exception("读取身份证内容失败!");
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
	public static class IDCardData extends Structure {
		public byte[] Name = new byte[32];
		public byte[] Sex = new byte[6];
		public byte[] Nation = new byte[20];
		public byte[] Born = new byte[18];
		public byte[] Address = new byte[72];
		public byte[] IDCardNo = new byte[38];
		public byte[] GrantDept = new byte[32];
		public byte[] UserLifeBegin = new byte[18];
		public byte[] UserLifeEnd = new byte[18];
		public byte[] reserved = new byte[38];
		public byte[] PhotoFileName = new byte[255];

		public IDCardData() {
			
		}

		public static class ByReference extends IDCardData implements Structure.ByReference { }
		public static class ByValue extends IDCardData implements Structure.ByValue { }
	}

	@Override
	public void deviceCheck() throws Exception {
	}

	@Override
	public String getDeviceIdentifer() throws Exception {
		byte[] strSAMID = new byte[8];
		int nRet = Syn_GetSAMID(port, strSAMID, 0);
		if(nRet == 0){
			return (Tools.bytes2hex(strSAMID));
		}else{
			System.out.println("读取SAM ID号错误:" + nRet);
			return null;
		}

	}

}
