package com.techrich.client.device.idcard;

import java.util.HashMap;
import java.util.Map;

import com.sun.jna.ptr.IntByReference;
import com.techrich.client.Activator;
import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.IDCardChecker;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.JNALoader;

public class WHJLIDCardChecker extends IDCardChecker {
	public static final String libname = JNALoader.load(new String[]{"reposity\\extlibs\\whjl"}, new String[]{  "sdtapi" });//"JpgDll", "SavePhoto","Dewlt",  
	public static native int InitComm(int iPort);
	public static native int CloseComm();
	public static native int Authenticate();
	public static native int ReadBaseMsg(byte[] publicManagerInfo, IntByReference len);
	
	private HashMap<String, String> cardInfoMap=new HashMap<String, String>();

	public WHJLIDCardChecker() {
		this.deviceId = "IDCARD-WHJL";
	}

	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"武汉经纶身份证读卡器";
	}
	
	@Override
	public void openDevice() throws Exception {
			int ret = InitComm(1001);
			if (ret != 1)
				throw new Exception();
	}

	@Override
	public void closeDevice() {
		CloseComm();
	}

	// ------------------

	public int AuthenticateInner() {
		int ret = Authenticate();
		return ret;// 0 认证失败，1成功。
	}

	@Override
	public Map<String, String> read() throws Exception {
		cardInfoMap=new HashMap<String, String>();
		byte[] publicManagerInfo = new byte[256];
		try {
			int ret = AuthenticateInner();
			if (ret != 1)
				throw new Exception("身份证没放置好:"+ret);
			IntByReference len = new IntByReference();
			ret = ReadBaseMsg(publicManagerInfo, len);
			
			LogManager.logInfo(Activator.PLUGIN_ID, " set Dll File lenlenlen=" + len.getValue());
			if(ret != 1)
				throw new Exception("武汉金轮--获取身份证信息失败");
			parseBytes(publicManagerInfo,len.getValue());
			System.out.println(ret);
			return cardInfoMap;
		} catch (Exception e) {
			throw new Exception(e.toString());
		}
	}
	
	private void parseBytes(byte[] data,int len) throws Exception{
		final String EnCode="GBK";
		
		byte byteInfo[]=new byte[len + 1];
		System.arraycopy(data, 0, byteInfo, 0, len);
		
		ByteBuffer bb=new ByteBuffer(byteInfo);
		
		cardInfoMap.put("Name",new String(bb.getValueN(0, 31),EnCode).trim());
		cardInfoMap.put("Sex",new String(bb.getValueN(31, 3),EnCode).trim());
		cardInfoMap.put("Nation",new String(bb.getValueN(34, 10),EnCode).trim());
		cardInfoMap.put("Birthday",new String(bb.getValueN(44, 9),EnCode).trim());
		cardInfoMap.put("Address",new String(bb.getValueN(53, 71),EnCode).trim());
		cardInfoMap.put("IdCode",new String(bb.getValueN(124, 19),EnCode).trim());
		cardInfoMap.put("Department",new String(bb.getValueN(143, 31),EnCode).trim());
		cardInfoMap.put("StartDate",new String(bb.getValueN(174, 9),EnCode).trim());
		cardInfoMap.put("EndDate",new String(bb.getValueN(183, 9),EnCode).trim());
		System.out.println(cardInfoMap);		
		
	}

	public byte[] getDeviceStatus() throws Exception {
		return null;
	}

	@Override
	public void setDeviceDesc(String desc) {
	}

	@Override
	public void initDevice() throws DeviceInitException {
		try {
			LogManager.logInfo(Activator.PLUGIN_ID, "武汉金轮 ---身份证读卡器初始化");
		} catch (Exception e) {
		}

	}

	@Override
	public String getDeviceIdentifer() throws Exception {
		return "";
	}

	@Override
	public void deviceCheck() throws Exception {

	}
	
	@Override
	public String getDeviceDesc() {
		return "USB";
	}
}
