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
	public static native int SDT_StartFindIDCard(int iPort, byte[] publicManagerInfo, int openPortInside);//0x9f:成功 0x80:失败
	public static native int SDT_SelectIDCard(int iPort, byte[] publicManagerMsg, int openPortInside);//0x90:成功 0x81:失败
	public static native int SDT_ReadMngInfo(int iPort, byte[] publicManagerMsg, int openPortInside);//0x90:成功 其它:失败，见宏定义
	public static native int SDT_ReadBaseMsg(int iPort, byte[] pubCHMsg, int[] pubCHMsgLen, byte[] pubPHMsg, int[] pubPHMsgLen, int openPortInside);//0x90:成功 其它:失败
	public static native int SDT_ReadNewAppMsg(int iPort, byte[] pubAppMsg, int[] pubAppMsgLen, int openPortInside);//0x90:成功 其它:失败

	public HuaXuIDCardReader(){
		this.deviceId = "IDCARD-HUAXU";
	}
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"华旭身份证读卡器";
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
			if(suc != 0x90) throw new Exception("打开身份证识别器失败：comm:" + port);
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
			LogManager.logInfo(Activator.PLUGIN_ID, "巡卡失败：" + Tools.bytes2hex(new byte[]{(byte)ret}));
			return 0;
		}
		ret = SDT_SelectIDCard(port, publicManagerMsg, OPEN_PORT_INSIDE);
		if((ret & 0x00ff) == 0x90) return 1;
		LogManager.logInfo(Activator.PLUGIN_ID, "选卡失败:" + Tools.bytes2hex(new byte[]{(byte)ret}));
		return 0;//认证失败
	}

	@Override
	public Map<String, String> read() throws Exception {
		cardInfoMap=new HashMap<String, String>();

		if(port <= 0) throw new Exception("请设置正确的端口");
		int rv = Authenticate();
		if(rv != 1){
			LogManager.logInfo(Activator.PLUGIN_ID, "身份证没放置好:" + rv);
			throw new Exception("身份证没放置好:" + rv);
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
			cardInfoMap.put("Sex", new String(data, "UTF-16").trim().equals("2") ? "女" : "男");//这个和国腾的不太一样//memcpy(&wt_Sex[0], &pucCHMsg[30], 2);
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
				LogManager.logError("设置波特率成功："+bands[i]);
				setBandDone = true;
				break;
			}
			else if(res == 0x1)LogManager.logError("端口打开失败/端口号不合法："+bands[i]);
			else if(res == 0x2)LogManager.logError(" 超时，设置不成功："+bands[i]);
			else if(res == 3)LogManager.logError( "设置波特率时数据传输错误："+bands[i]);
			else if(res == 0x21)LogManager.logError(" 输入参数数值错误："+bands[i]);
			else LogManager.logError(" 设置波特率错误："+bands[i]+"  ret:"+res );
		}
		if(!setBandDone)throw new DeviceInitException("设置波特率失败："+res);
		res = SDT_GetCOMBaud(port, baud);
		if(res == 0x90)LogManager.logError( "身份证读卡器使用的波特率为：" + baud[0]);
		else if(res == 1)throw new DeviceInitException( "端口打开失败/端口号不合法 ");
		else if(res == 3)throw new DeviceInitException( "数据传输错误 ");
		else if(res == 5)throw new DeviceInitException( "无法获得该 SAM_V 的波特率，该 SAM_V 串口不可用。 ");
		else throw new DeviceInitException( "未解析错误: "+res);


		res =  SDT_GetSAMStatus (port,OPEN_PORT_INSIDE);
		if(res == 0x90)LogManager.logError( "s身份证SAM状态ok！");
		else if(res == 0x60)throw new DeviceInitException("自检失败，不能接收命令 ");
		else throw new DeviceInitException("自检失败，其它错误： "+res);
		
		byte[] modelSerialNo = new byte[255];
		res = SDT_GetSAMIDToStr(port, modelSerialNo, OPEN_PORT_INSIDE);//OPEN_PORT_INSIDE
		if(res == 0x90)LogManager.logError( "身份证读卡器安全模块序列号:" + new String(modelSerialNo).trim());
		else  throw new DeviceInitException("取安全模块序列号失败:" + res);
	}

	@Override
	public String getDeviceIdentifer() throws Exception {
		int[] baud = {0};
		int res = -1;
		try{
			res = SDT_GetCOMBaud(port, baud);
		}catch(Exception e){
			throw new Exception("没有提供自动检测功能");
		}
		try{
			byte[] modelSerialNo = new byte[255];
			res = SDT_GetSAMIDToStr(port, modelSerialNo, OPEN_PORT_INSIDE);
			if(res != 144) throw new DeviceInitException("取安全模块序列号失败:" + res);
			return new String(modelSerialNo).trim();
		}catch(Exception e){
			throw new Exception("没有提供自动检测功能");
		}

	}

	@Override
	public void deviceCheck() throws Exception {

	}

	@Override
	public void setDeviceAppParams(HashMap<String, String> params) throws Exception {
	}

}
