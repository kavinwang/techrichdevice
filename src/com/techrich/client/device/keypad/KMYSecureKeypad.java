package com.techrich.client.device.keypad;

import java.util.HashMap;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.SecureKeypad;
import com.techrich.client.manager.ConfigManager;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;

public class KMYSecureKeypad extends SecureKeypad {
	private static final int CMD_SUCCESS = 0x04;
	private static final int PROTOCOL_HEADER = 0x02;

	private static final byte SAM_MKEY = 0x00;
	private static final byte LOC_PINKEY = 0x00; //放在主秘钥0号位置
	private static final byte LOC_MACKEY = 0x02; //放在主秘钥2号位置
	
	boolean logComm = true;
	public KMYSecureKeypad() {
		this.deviceId = "SECURE-KEYPAD-KMY";
	}
	
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"凯明杨密码键盘";
	}

	private String KEYPAD_SAM_PWD = "333888";
	private int KEYPAD_PIN_MAXLEN = 6;
	private int KEYPAD_PIN_TIMEOUT = 120;
	private int KEYPAD_SAM_SOCKET = 1;

	@Override
	public void setDeviceAppParams(HashMap<String, String> params) throws Exception {
		super.setDeviceAppParams(params);
		if(params.containsKey("KEYPAD_SAM_PWD")){
			KEYPAD_SAM_PWD = params.get("KEYPAD_SAM_PWD");
		}
		if(params.containsKey("KEYPAD_SAM_SOCKET")){
			KEYPAD_SAM_SOCKET = Integer.parseInt(params.get("KEYPAD_SAM_SOCKET"));
		}
		String psamSlot = ConfigManager.getDefault().getConfigElement("psam.slot");
		if(psamSlot!=null){
			KEYPAD_SAM_SOCKET = Integer.parseInt(psamSlot);
		}
		if(params.containsKey("KEYPAD_PIN_MAXLEN")){
			KEYPAD_PIN_MAXLEN = Integer.parseInt(params.get("KEYPAD_PIN_MAXLEN"));
		}
		if(params.containsKey("KEYPAD_PIN_TIMEOUT")){
			KEYPAD_PIN_TIMEOUT = Integer.parseInt(params.get("KEYPAD_PIN_TIMEOUT"));
		}
		logComm = Boolean.parseBoolean(ConfigManager.getDefault().getConfigElementDef("keypad.log", params.get("KEYPAD_LOG_COMM")));
	}
	boolean tdesEnabled = true;
	
	boolean newKMY = false;
	@Override
	public void initDevice() throws DeviceInitException {
		//reset
		try{
			String version = new String(comm2keypad(new byte[]{0x30}));
			
			if(version.startsWith("STC2"))newKMY = true;//旧款：S1A1YX LXMZV005  新款：STC2UDYJ ZV104
			LogManager.logInfo("密码键盘固件版本："+version);
			
			comm2keypad(new byte[]{0x31});//0x31,0x38会重置主秘钥和工作秘钥
		}catch(Exception e){
			throw new DeviceInitException(e);
		}
		try{
//			comm2keypad(new byte[]{0x46, 0x00, 0x40});//下载工作密钥采用SAM卡内算法,主密钥解密
			comm2keypad(new byte[]{0x46, 0x02, (byte)0xFF});//键盘输入 PIN 短时，用<F>值填充 PIN 右边直至 8 字节
//			comm2keypad(new byte[]{0x46, 0x01, 0x70});//
			comm2keypad(new byte[]{0x46, 0x04, 0x10});//键盘输入 PIN 处理方式为ISO9564-1格式0(ANSI 格式) 
			comm2keypad(new byte[]{0x46, 0x05, 0x01});//在 PIN 输入时，达到指定长度时自动加送回车键值
//			comm2keypad(new byte[]{0x46, 0x05, 0x03});//功能键*
			comm2keypad(new byte[]{0x46, 0x05, 0x04});//下载密钥不返回验证码*
			
			
		}catch(Exception e){
			throw new DeviceInitException(e);
		}
		
//		try{
//			selectSocket(KEYPAD_SAM_SOCKET, true);//保证使用正确的socket
//		}catch(Exception e){
//			throw new DeviceInitException("密码键盘有可能SAM卡安装不正确",e);
//		}
	}
	
	@Override
	public boolean tdesEnabled() {
		return tdesEnabled;
	}
	
	@Override
	public String getDeviceIdentifer() throws Exception {
		try{
			byte[] retData = comm2keypad(new byte[]{0x30});
			return new String(retData);
		}catch(Exception e){
			throw new Exception(e);
		}
	}

	@Override
	public String getPosInfo()throws Exception{
		
		for(int i = 0;i< 5;	i++){
			try{
				comm2keypad(new byte[]{0x5B});//断电
				try{Thread.sleep(2000);}catch(Exception e){}
				selectSocket(KEYPAD_SAM_SOCKET, true);
				break;
			}catch(Exception e){
				e.printStackTrace();
				try{Thread.sleep(1000);}catch(Exception ee){}
			}
		
			if(i >= 5)throw new Exception("不能读取SAM，系统初始化错误！");
		}
		
		try{apdu(Tools.hex2bytes("00A40200020011"));}catch(Exception e){/* 凯明杨的某些版本键盘此处无返回，但选文件已经成功！*/}
		
		for(int i = 0;i<3;i++){
			try{
				Thread.sleep(300);
				this.clearBuffer();
				byte[] datas= apdu(Tools.hex2bytes("00B0001717"));//00B000002E
				return new String(datas).substring(datas.length-15-8);//		"00000000000000000000000" + posShop + posTerminal;
			}catch(Exception e){
				if(i < 2)continue;
				else throw e;
			}
		}
		return null;
	}
	
	public void loadWorkKey(byte[] pinKeyE, byte[] macKeyE) throws Exception {
		if(keyPadOpened)closeKeypadWithSound(false);
		selectSocket(KEYPAD_SAM_SOCKET, true);
		
		byte[] pinKeyResult = null, macKeyResult = null;
		if(pinKeyE!=null){
			ByteBuffer bb = new ByteBuffer(pinKeyE);
			byte[] pinKey = bb.getValueN(0, 16);
//			String pinKeyCv = Tools.bytes2hex(bb.getValueN(16));
			
			apdu(Tools.hex2bytes("0020000003" + KEYPAD_SAM_PWD));
			
			try{
				apdu(Tools.hex2bytes("801A090100"));
			}catch(Exception e){
				apdu(Tools.hex2bytes("801A0C0100"));
			}
			
			ByteBuffer pinApdu = new ByteBuffer();
			pinApdu.append(Tools.hex2bytes("80FA0000")).append((byte)pinKey.length).append(pinKey);
			pinKeyResult = apdu(pinApdu.getValue());
			LogManager.logInfo("PINKEY解密前[:"+Tools.bytes2hex(pinKey)+"]后["+Tools.bytes2hex(pinKeyResult)+"]");
		}
		
		if(macKeyE!=null){
			ByteBuffer bb = new ByteBuffer(macKeyE);
			
			int len = bb.length()>16?16:8;
			byte[] macKey = bb.getValueN(0, len);
//			String macKeyCv = Tools.bytes2hex(bb.getValueN(len));

//			String macKeyCv = Tools.bytes2hex(bb.getValueN(8));

			apdu(Tools.hex2bytes("0020000003" + KEYPAD_SAM_PWD));
			try{
				apdu(Tools.hex2bytes("801A090100"));
			}catch(Exception e){
				apdu(Tools.hex2bytes("801A0C0100"));
			}
			ByteBuffer macApdu = new ByteBuffer();
			macApdu.append(Tools.hex2bytes("80FA0000")).append((byte)macKey.length).append(macKey);
			macKeyResult = apdu(macApdu.getValue());
			
		}
		
		
		if(pinKeyResult!=null){
			System.out.println("PinKeyClear:"+Tools.bytes2hex(pinKeyResult));
			//下载到密码键盘
			ByteBuffer loadKeyBuff = new ByteBuffer(pinKeyResult);
			loadKeyBuff.insert(0, LOC_PINKEY);
			loadKeyBuff.insert(0, (byte)0x32);
			comm2keypad(loadKeyBuff.getValue());
			ConfigManager.getDefault().setSystemPinKey(pinKeyResult);
		}else	throw new Exception("无法得到系统PINKEY");
		
		if(macKeyResult!=null){
			System.out.println("MacKeyClear:"+Tools.bytes2hex(macKeyResult));
			ByteBuffer loadKeyBuff = new ByteBuffer(macKeyResult);
			loadKeyBuff.insert(0, LOC_MACKEY);
			loadKeyBuff.insert(0, (byte)0x32);
			comm2keypad(loadKeyBuff.getValue());
			ConfigManager.getDefault().setSystemMacKey(macKeyResult);
		}else throw new Exception("无法得到系统MACKEY");
		
		closeKeypadWithSound(true);
	}
	
	private void selectWorkKey(int mKeyNo, int wKeyNo) throws Exception {
		if(keyPadOpened)closeKeypadWithSound(false);
		comm2keypad(new byte[]{0x43, (byte)mKeyNo, (byte)wKeyNo});//如果使用的是主秘钥模式，则忽略工作密钥
	}
	
	/**
	 * 输入的密钥长度和等待输入密钥的最大时间
	 */
	@Override
	public void startPinMode(int passwdLen,String trackData, int timeOut) throws Exception {
		clearBuffer();
		openKeypadWithSound(true);
//		comm2keypad(new byte[]{0x46, 0x01, 0x70});//键盘采用 3DES 密码算法，主密钥加密
//		selectWorkKey(LOC_PINKEY, LOC_PINKEY);//选择LOC_PINKEY主秘钥

//		ByteBuffer bb = new ByteBuffer(new byte[]{0x34});//设置卡号
//		bb.append(trackData.getBytes());
//		comm2keypad(bb.getValue());
		
		byte pinLen = (byte)KEYPAD_PIN_MAXLEN;
		byte pinTimeOut = (byte)KEYPAD_PIN_TIMEOUT;
		if(passwdLen > 0 && passwdLen < 10) pinLen = (byte)passwdLen;
		if(timeOut > 0) pinTimeOut = (byte)timeOut;
		ByteBuffer bbSend = new ByteBuffer(new byte[]{0x35, pinLen, 0x01, 0x00, 0x00, pinTimeOut});
		comm2keypad(bbSend.getValue());
		Thread.sleep(500);
		comm2keypad(bbSend.getValue());//发两次看看效果
//		openKeypadWithSound(true);

	}
	
	public byte[] getPin(String trackData) throws Exception {
		if(trackData == null || trackData.length() != 12) throw new Exception("不正确的Track信息");
		this.clearBuffer();
		if(keyPadOpened)closeKeypadWithSound(false);
//		closeKeypadWithSound(false);
//		clearBuffer();
		
		comm2keypad(new byte[]{0x46, 0x01, 0x70});//键盘采用 3DES 密码算法，主密钥加密
		selectWorkKey(LOC_PINKEY, LOC_PINKEY);//选择LOC_PINKEY主秘钥
		
		ByteBuffer bb = new ByteBuffer(new byte[]{0x34});
		bb.append(trackData.getBytes());
		comm2keypad(bb.getValue());

		bb = new ByteBuffer(new byte[]{0x42});
		byte[] aa = comm2keypad(bb.getValue(),300);
		byte[] cc = new byte[8];
		System.arraycopy(aa, 0, cc, 0, 8);
		
//		closeKeypadWithSound(false);
		
		return cc;
	}
	
	public byte[] getMac(byte[] macdata) throws Exception {
		if(macdata == null) throw new Exception("不正确的MACDATA信息");

		comm2keypad(new byte[]{0x46, 0x01, 0x60});//键盘采用 DES 密码算法，主密钥加密
		selectWorkKey(LOC_MACKEY, LOC_MACKEY);//使用MACKEY进行加密
    ByteBuffer bb = new ByteBuffer(macdata);
    for (; bb.length() % 8 != 0; )bb.append( (byte) 0x00);
    
    if(bb.length()<=200){
    	bb.insert(0, new byte[]{0x41});
      return comm2keypad(bb.getValue());
    }else{
    	byte[] mid = new byte[8];
    	int group = bb.length()%200 == 0 ?bb.length() / 200 : bb.length()/200 + 1;
    	
    	for(int i =0; i < group; i++){
    		ByteBuffer sbb = new ByteBuffer();
    		if(i == group -1)sbb.append(bb.getValueN(i*200));
    		else sbb.append(bb.getValueN(i*200, 200));
    		for(int k=0;k<8;k++)sbb.replace(k, (byte)(mid[k]^sbb.getByteAt(k)));
    		sbb.insert(0, new byte[]{0x41});
    		mid = comm2keypad(sbb.getValue());
    	}
    	
      return mid;
    }
  }
	
	private boolean keyPadOpened = false;

	/**
	 * 打开密码键盘
	 * 
	 * @param openSound
	 *          false:打开但关闭按键声音;true:打开且打开按键声音
	 * @throws Exception
	 */
	private void openKeypadWithSound(boolean openSound) throws Exception {
//	if(newKMY){
//		byte[] data = new byte[]{0x45, 0x03};
//		comm2keypad(data);
//		
//		data[1] = openSound?(byte)0x01:(byte)0x02;
////		comm2keypad(data);
//	}else{
		byte[] data = new byte[]{0x45, 0x01};
		if(openSound) data[1] = 0x03;
		comm2keypad(data);
//	}
	keyPadOpened = true;
}

/**
 * 关闭密码键盘
 * 
 * @param openSound
 *          false:关闭且关闭按键声音;true:关闭但打开按键声音
 * @throws Exception
 */
//	@Override
private void closeKeypadWithSound(boolean openSound) throws Exception {
//	if(newKMY){
//		byte[] data = new byte[]{0x45, 0x00};
//		comm2keypad(data);
//		data[1] = openSound?(byte)0x01:(byte)0x02;
//		comm2keypad(data);			
//	}else{
		byte[] data = new byte[]{0x45, 0x00};
		if(openSound) data[1] = 0x02;
		comm2keypad(data);
//	}

	keyPadOpened = false;
}

	@Override
	public byte getPressKey(int time) throws Exception {
//		if(!keyPadOpened)openKeypadWithSound(true);
		byte[] recvs = recieveData(1, time);
		//如果返回协议头，则说明键盘内部可能出错(比如启动PIN后等待用户输入超时)，因此按协议取回数据
		if(recvs[0] == PROTOCOL_HEADER){
			ByteBuffer bb = new ByteBuffer();
			byte[] len = recieveData(2);
			int length = Tools.hex2bytes(new String(len))[0];
			bb.append((byte)length);
			byte[] data = recieveData(length * 2);
			data = Tools.hex2bytes(new String(data));
			bb.append(data);
			byte[] bccr = Tools.hex2bytes(new String(recieveData(2)));
			int bcc = getBccCode(bb.getValue());
			if(bcc != bccr[0]) throw new Exception("数据格式bcc错");
			byte errorCode = data[0];
			String msg;
			if(!errorMap.containsKey(errorCode)){
				msg = "未知错误码:" + String.format("0x%H", errorCode) + "";
			}else msg = errorMap.get(errorCode);
			throw new Exception(msg);
		}
		return recvs[0];
	}

	protected byte[] comm2keypad(byte[] sends) throws Exception {
		return comm2keypad(sends,-1);
	}
	protected byte[] comm2keypad(byte[] sends,long timeOut) throws Exception {

		ByteBuffer bb = new ByteBuffer(sends);
		bb.insert(0, new byte[]{(byte)sends.length});

		byte bcc = getBccCode(bb.getValue());
		bb.append(bcc);

		String content = Tools.bytes2hex(bb.getValue());
		bb.reset();
		bb.append((byte)PROTOCOL_HEADER).append(content.getBytes());

		if(logComm) LogManager.logInfo("发送数据：" + Tools.bytes2hex(bb.getValue()));
		clearBuffer();//有时候可能错包，如果错包则会造成后续的通信部正常
		sendDirect(bb.getValue());
		
		if(timeOut>0)try{System.out.println("Wait");Thread.sleep(timeOut);}catch(Exception ee){};
		
		bb.reset();
		byte[] header = recieveData(1,1000);
		if(header[0] != PROTOCOL_HEADER){
			System.out.println("不是返回的包数X:"+Tools.bytes2hex(header));
			if(header[0] == 0x0D){
				header = recieveData(1);
				if(header[0] != PROTOCOL_HEADER)throw new Exception("不是返回的包数据:"+Tools.bytes2hex(header));
			}else	throw new Exception("不是返回的包数据:"+Tools.bytes2hex(header));
		}
		byte[] len = recieveData(2);
		int length = Tools.hex2bytes(new String(len))[0];
		bb.append((byte)length);
		byte[] data1 = recieveData(length * 2);
		
		byte[] data = Tools.hex2bytes(new String(data1));
		bb.append(data);
		byte[] bccr = recieveData(2);
		if(logComm){
			LogManager.logInfo("返回数据：" + Tools.bytes2hex(new ByteBuffer().append(header).append(len).append(data1).append(bccr).getValue()));
		}
		bccr = Tools.hex2bytes(new String(bccr));
		bcc = getBccCode(bb.getValue());
		if(bcc != bccr[0]) throw new Exception("数据格式bcc错:"+Tools.bytes2hex(bb.getValue()));

		if(data[0] != CMD_SUCCESS){
			byte errorCode = data[0];
			String msg;
			if(!errorMap.containsKey(errorCode)){
				msg = "未知错误码:" + String.format("0x%H", errorCode) + "";
			}else msg = errorMap.get(errorCode);
			throw new Exception(msg);
		}

		//		System.out.println("receive:"+Tools.bytes2hex(bb.getValue()));
		return bb.getValueN(2);//返回数据部分，跳过长度和状态
	}

	private byte getBccCode(byte[] datas) {
		byte bcc = 0x00;
		for(int i = 0; i < datas.length; i++){
			bcc ^= datas[i];
		}
		return bcc;
	}

	@Override
	public void closeKeypad() throws Exception {
		closeKeypadWithSound(false);
	}

	@Override
	public void deviceCheck() throws Exception {

	}

	public void selectSocket(int socket, boolean poweron) throws Exception {
		comm2keypad(new byte[]{(byte)0x59, (byte)socket, (byte)0x88});
		if(poweron){
			System.out.println("ATR:"+Tools.bytes2hex(comm2keypad(new byte[]{0x49})));
		}
	}
	public byte[] apdu(byte[] requestData) throws Exception {
		ByteBuffer bf = new ByteBuffer(new byte[]{0x48});
		bf.append(requestData);

		bf = new ByteBuffer(comm2keypad(bf.getValue()));
		byte[] sw = bf.getValueN(bf.length() - 2);
		if((sw[0] == (byte)0x90) && (sw[1] == (byte)0x00)){
			if(bf.length() > 2) return bf.getValueN(0, (bf.length() - 2));
			else return new byte[]{};
		}else if(sw[0] == (byte)0x61){
			Thread.sleep(200);//cpu忙，等待一下
			ByteBuffer bf1 = new ByteBuffer(new byte[]{0x48, 0x00, (byte)0xC0, 0x00, 0x00});
			bf1.append(sw[1]);
			bf = new ByteBuffer(comm2keypad(bf.getValue()));
			sw = bf.getValueN(bf.length() - 2);
			if((sw[0] == (byte)0x90) && (sw[1] == (byte)0x00)){
				return bf.getValueN(0, (bf.length() - 2));
			}else{
				throw new Exception("GET RESPONSE ERROR! SW1,SW2 = " + Tools.bytes2hex(sw));
			}
		}else{
			throw new Exception("APDU ERROR! SW1,SW2 = " + Tools.bytes2hex(sw));
		}
	}

	public byte[] directCommToDevice(byte[] data) throws Exception {
		return comm2keypad(data);
	}

}
