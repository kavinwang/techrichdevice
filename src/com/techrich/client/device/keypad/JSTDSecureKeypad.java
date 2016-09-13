package com.techrich.client.device.keypad;

import java.util.HashMap;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.SecureKeypad;
import com.techrich.client.manager.ConfigManager;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;

//10058F9F1418F590
public class JSTDSecureKeypad extends SecureKeypad {
	private static final int CMD_SUCCESS = 0x04;
	private static final int PROTOCOL_HEADER = 0x02;

	private static final byte SAM_MKEY = 0x00;
	private static final byte SAM_PINKEY = 0x00;
	private static final byte SAM_MACKEY = 0x01;
	
	boolean logComm = true;
	public JSTDSecureKeypad() {
		this.deviceId = "SECURE-KEYPAD-JSTD";
	}
	
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"��˼̩���������";
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
//		logComm = Boolean.parseBoolean(params.get("KEYPAD_LOG_COMM"));
		logComm = Boolean.parseBoolean(ConfigManager.getDefault().getConfigElementDef("keypad.log", params.get("KEYPAD_LOG_COMM")));
	}
	boolean tdesEnabled = true;
	
	@Override
	public void initDevice() throws DeviceInitException {
		//reset
		try{
			String version = new String(comm2keypad(new byte[]{0x30}));
		//todo:�жϰ汾�������Ƿ�֧��TDES
			tdesEnabled = true;
			LogManager.logInfo("������̹̼��汾��"+version);
			comm2keypad(new byte[]{0x31});
		}catch(Exception e){
			throw new DeviceInitException(e);
		}
		try{
			comm2keypad(new byte[]{0x46, 0x00, 0x40});//���ع�����Կ����SAM�����㷨,����Կ����
			comm2keypad(new byte[]{0x46, 0x01, 0x30});//��������PIN��������SAM�����㷨,������Կ����
			comm2keypad(new byte[]{0x46, 0x02, (byte)0xFF});//�������� PIN ��ʱ����<F>ֵ��� PIN �ұ�ֱ�� 8 �ֽ�
			comm2keypad(new byte[]{0x46, 0x04, 0x10});//�������� PIN ����ʽΪISO9564-1��ʽ0(ANSI ��ʽ) 
			comm2keypad(new byte[]{0x46, 0x05, 0x01});//�ڼ���״̬�����뵽Լ������ʱ�Զ����ͻس���ֵ

//			comm2keypad(new byte[]{0x46, 0x00, 0x40});//���ع�����Կ����SAM�����㷨,����Կ����
//			comm2keypad(new byte[]{0x46, 0x01, 0x40});//��������PIN��������SAM�����㷨,������Կ����
//			comm2keypad(new byte[]{0x46, 0x04, 0x10});//�������� PIN ����ʽΪISO9564-1��ʽ0(ANSI ��ʽ) 
//			comm2keypad(new byte[]{0x46, 0x05, 0x01});//�ڼ���״̬�����뵽Լ������ʱ�Զ����ͻس���ֵ
//			comm2keypad(new byte[]{0x46, 0x02, (byte)0xFF});//�������� PIN ��ʱ����<F>ֵ��� PIN �ұ�ֱ�� 8 �ֽ�
		}catch(Exception e){
			throw new DeviceInitException(e);
		}
		
//		try{
//			selectSocket(KEYPAD_SAM_SOCKET, true);//��֤ʹ����ȷ��socket
//		}catch(Exception e){
//			throw new DeviceInitException("��������п���SAM����װ����ȷ",e);
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
//		selectSocket(KEYPAD_SAM_SOCKET, true);
	comm2keypad(new byte[]{(byte)0x59, (byte)KEYPAD_SAM_SOCKET, (byte)0x88});//ѡ��IC����
	comm2keypad(new byte[]{0x5B});//�ϵ�
	try{Thread.sleep(1000);}catch(Exception e){}
		System.out.println("ATR:"+Tools.bytes2hex(comm2keypad(new byte[]{0x49})));//�ϵ�
		
		try{
			apdu(Tools.hex2bytes("00A40200020011"));
		}catch(Exception e){
			apdu(Tools.hex2bytes("00A40000020011"));
		}
		
		byte[] datas= apdu(Tools.hex2bytes("00B000002E"));
		return new String(datas).substring(datas.length-15-8);//		"00000000000000000000000" + posShop + posTerminal;
	}
	
	public void loadWorkKey(byte[] pinKeyE, byte[] macKeyE) throws Exception {
		if(keyPadOpened)closeKeypadWithSound(false);
		selectSocket(KEYPAD_SAM_SOCKET, true);
		
		if(pinKeyE!=null){
			ByteBuffer bb = new ByteBuffer(pinKeyE);
			byte[] pinKey = bb.getValueN(0, 16);
			String pinKeyCv = Tools.bytes2hex(bb.getValueN(16));
			
			apdu(Tools.hex2bytes("0020000003" + KEYPAD_SAM_PWD));
			
			try{
				apdu(Tools.hex2bytes("801A090100"));
			}catch(Exception e){
				apdu(Tools.hex2bytes("801A0C0100"));
			}
			
			//�˴�������ִ��80FAָ���ѽ�������ڲ�����GET_PINʹ��
			ByteBuffer kb = new ByteBuffer(new byte[]{0x33, SAM_MKEY, SAM_PINKEY});
			kb.append(pinKey);
			comm2keypad(kb.getValue());
			
			//����CHECKVALUE
			if(pinKeyCv!=null){
				selectWorkKey(SAM_MKEY,SAM_PINKEY);
	      comm2keypad(new byte[]{0x46, 0x01, 0x30});
		    bb = new ByteBuffer();
		    bb.append(new byte[]{0x36});
		    bb.append(Tools.hex2bytes("0000000000000000"));
		    byte[] res = comm2keypad(bb.getValue());
		    if(!Tools.bytes2hex(res).startsWith(pinKeyCv))throw new Exception("ǩ��PINKEY��֤����");
			}
	    
			apdu(Tools.hex2bytes("0020000003" + KEYPAD_SAM_PWD));
			try{
				apdu(Tools.hex2bytes("801A090100"));
			}catch(Exception e){
				apdu(Tools.hex2bytes("801A0C0100"));
			}
				
			ByteBuffer pinApdu = new ByteBuffer();
			pinApdu.append(Tools.hex2bytes("80FA0000")).append((byte)pinKey.length).append(pinKey);
			byte[] pinKeyResult = apdu(pinApdu.getValue());
			if(pinKeyResult!=null){
				System.out.println("PinKeyClear:"+Tools.bytes2hex(pinKeyResult));
				ConfigManager.getDefault().setSystemPinKey(pinKeyResult);
			}else	throw new Exception("�޷��õ�ϵͳPINKEY");
		}
		
		if(macKeyE!=null){
			ByteBuffer bb = new ByteBuffer(macKeyE);
//			byte[] macKey = bb.getValueN(0, 8);
			int len = bb.length()>16?16:8;
			byte[] macKey = bb.getValueN(0, len);
			String macKeyCv = Tools.bytes2hex(bb.getValueN(len));

			apdu(Tools.hex2bytes("0020000003" + KEYPAD_SAM_PWD));
			try{
				apdu(Tools.hex2bytes("801A090100"));
			}catch(Exception e){
				apdu(Tools.hex2bytes("801A0C0100"));
			}
			
			ByteBuffer kb = new ByteBuffer(new byte[]{0x33, SAM_MKEY, SAM_MACKEY});
			kb.append(macKey);
			comm2keypad(kb.getValue());
			
			//����CHECKVALUE
			if(macKeyCv!=null){
				selectWorkKey(SAM_MKEY,SAM_MACKEY);
	      comm2keypad(new byte[]{0x46, 0x01, 0x20});
		    bb = new ByteBuffer();
		    bb.append(new byte[]{0x36});
		    bb.append(Tools.hex2bytes("0000000000000000"));
		    byte[] res = comm2keypad(bb.getValue());
		    if(!Tools.bytes2hex(res).startsWith(macKeyCv))throw new Exception("ǩ��PINKEY��֤����");
			}
			apdu(Tools.hex2bytes("0020000003" + KEYPAD_SAM_PWD));
			try{
				apdu(Tools.hex2bytes("801A090100"));
			}catch(Exception e){
				apdu(Tools.hex2bytes("801A0C0100"));
			}
			
			ByteBuffer macApdu = new ByteBuffer(Tools.hex2bytes("80FA0000"));
			macApdu.append((byte)macKey.length).append(macKey);
			kb.append(macApdu.getValue());
			byte[] macKeyResult = apdu(macApdu.getValue());
			if(macKeyResult!=null){
				System.out.println("MacKeyClear:"+Tools.bytes2hex(macKeyResult));
				ConfigManager.getDefault().setSystemMacKey(macKeyResult);
			}else throw new Exception("�޷��õ�ϵͳMACKEY");
		}
		closeKeypadWithSound(true);
	}
	
	private void selectWorkKey(int mKeyNo, int wKeyNo) throws Exception {
		if(keyPadOpened)closeKeypadWithSound(false);
		comm2keypad(new byte[]{0x43, (byte)mKeyNo, (byte)wKeyNo});
	}
	
	/**
	 * �������Կ���Ⱥ͵ȴ�������Կ�����ʱ��
	 */
//	@Override
//	public void startPinMode(int passwdLen, int timeOut) throws Exception {
//		if(!keyPadOpened)openKeypadWithSound(true);
//
//		byte pinLen = (byte)KEYPAD_PIN_MAXLEN;
//		byte pinTimeOut = (byte)KEYPAD_PIN_TIMEOUT;
//		if(passwdLen > 0 && passwdLen < 10) pinLen = (byte)passwdLen;
//		if(timeOut > 0) pinTimeOut = (byte)timeOut;
//		ByteBuffer bbSend = new ByteBuffer(new byte[]{0x35, pinLen, 0x01, 0x00, 0x00, pinTimeOut});
//		comm2keypad(bbSend.getValue());
//	}

	@Override
	public void startPinMode(int passwdLen,String trackData, int timeOut) throws Exception {
		clearBuffer();
		openKeypadWithSound(true);
		comm2keypad(new byte[]{0x46, 0x00, 0x40});
		
		byte pinLen = (byte)KEYPAD_PIN_MAXLEN;
		byte pinTimeOut = (byte)KEYPAD_PIN_TIMEOUT;
		if(passwdLen > 0 && passwdLen < 10) pinLen = (byte)passwdLen;
		if(timeOut > 0) pinTimeOut = (byte)timeOut;
		ByteBuffer bbSend = new ByteBuffer(new byte[]{0x35, pinLen, 0x01, 0x00, 0x00, pinTimeOut});
		comm2keypad(bbSend.getValue());
		
		ByteBuffer bb = new ByteBuffer(new byte[]{0x34});
		bb.append(trackData.getBytes());
		comm2keypad(bb.getValue());
	}
	
	public byte[] getPin(String trackData) throws Exception {
		if(trackData == null || trackData.length() != 12) throw new Exception("����ȷ��Track��Ϣ");

		if(!keyPadOpened) openKeypadWithSound(true);
		clearBuffer();
		
		//3:������Կ��ָ��ʹ�õ���Կ�������� ��Կ�Ŷ�Ϊ1		
		selectWorkKey(SAM_MKEY, SAM_PINKEY);
		comm2keypad(new byte[]{0x46, 0x01, 0x30});//�������� PIN �������� 3DES �����㷨��������Կ����
		
		ByteBuffer bb = new ByteBuffer(new byte[]{0x34});

		bb.append(trackData.getBytes());
		comm2keypad(bb.getValue());

		bb = new ByteBuffer(new byte[]{0x42});
		byte[] aa = comm2keypad(bb.getValue());
		byte[] cc = new byte[8];
		System.arraycopy(aa, 0, cc, 0, 8);
		return cc;
	}
	
	public byte[] getMac(byte[] macdata) throws Exception {
		if(macdata == null) throw new Exception("����ȷ��MACDATA��Ϣ");

		selectWorkKey(SAM_MKEY, SAM_MACKEY);//ʹ��MACKEY���м���
		comm2keypad(new byte[]{0x46, 0x01, 0x20});//ʹ��DES�㷨
		
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
	 * ���������
	 * 
	 * @param openSound
	 *          false:�򿪵��رհ�������;true:���Ҵ򿪰�������
	 * @throws Exception
	 */
	private void openKeypadWithSound(boolean openSound) throws Exception {
		byte[] data = new byte[]{0x45, 0x01};
		if(openSound) data[1] = 0x03;
		comm2keypad(data);
		keyPadOpened = true;
	}

	/**
	 * �ر��������
	 * 
	 * @param openSound
	 *          false:�ر��ҹرհ�������;true:�رյ��򿪰�������
	 * @throws Exception
	 */
	//	@Override
	private void closeKeypadWithSound(boolean openSound) throws Exception {
		byte[] data = new byte[]{0x45, 0x00};
		if(openSound) data[1] = 0x02;
		comm2keypad(data);
		keyPadOpened = false;
	}

	

	@Override
	public byte getPressKey(int time) throws Exception {
		if(!keyPadOpened)openKeypadWithSound(true);
		byte[] recvs = recieveData(1, time);
		//�������Э��ͷ����˵�������ڲ����ܳ���(��������PIN��ȴ��û����볬ʱ)����˰�Э��ȡ������
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
			if(bcc != bccr[0]) throw new Exception("���ݸ�ʽbcc��");
			byte errorCode = data[0];
			String msg;
			if(!errorMap.containsKey(errorCode)){
				msg = "δ֪������:" + String.format("0x%H", errorCode) + "";
			}else msg = errorMap.get(errorCode);
			throw new Exception(msg);
		}
		return recvs[0];
	}

	protected byte[] comm2keypad(byte[] sends) throws Exception {

		ByteBuffer bb = new ByteBuffer(sends);
		bb.insert(0, new byte[]{(byte)sends.length});

		byte bcc = getBccCode(bb.getValue());
		bb.append(bcc);

		String content = Tools.bytes2hex(bb.getValue());
		bb.reset();
		bb.append((byte)PROTOCOL_HEADER).append(content.getBytes());

		if(logComm) LogManager.logInfo("�������ݣ�" + Tools.bytes2hex(bb.getValue()));
		clearBuffer();//��ʱ����ܴ���������������ɺ�����ͨ�Ų�����
		sendDirect(bb.getValue());
		
//		if(timeOut>0)try{Thread.sleep(timeOut);}catch(Exception ee){};
		
		bb.reset();
		byte[] header = recieveData(1,1000);
		if(header[0] != PROTOCOL_HEADER){
			System.out.println("���Ƿ��صİ���X:"+Tools.bytes2hex(header));
			if(header[0] == 0x0D){
				header = recieveData(1);
				if(header[0] != PROTOCOL_HEADER)throw new Exception("���Ƿ��صİ�����:"+Tools.bytes2hex(header));
			}else	throw new Exception("���Ƿ��صİ�����:"+Tools.bytes2hex(header));
		}
		byte[] len = recieveData(2);
		int length = Tools.hex2bytes(new String(len))[0];
		bb.append((byte)length);
		byte[] data1 = recieveData(length * 2);
		
		byte[] data = Tools.hex2bytes(new String(data1));
		bb.append(data);
		byte[] bccr = recieveData(2);
		if(logComm){
			LogManager.logInfo("�������ݣ�" + Tools.bytes2hex(new ByteBuffer().append(header).append(len).append(data1).append(bccr).getValue()));
		}
		bccr = Tools.hex2bytes(new String(bccr));
		bcc = getBccCode(bb.getValue());
		if(bcc != bccr[0]) throw new Exception("���ݸ�ʽbcc��:"+Tools.bytes2hex(bb.getValue()));

		if(data[0] != CMD_SUCCESS){
			byte errorCode = data[0];
			String msg;
			if(!errorMap.containsKey(errorCode)){
				msg = "δ֪������:" + String.format("0x%H", errorCode) + "";
			}else msg = errorMap.get(errorCode);
			throw new Exception(msg);
		}

		//		System.out.println("receive:"+Tools.bytes2hex(bb.getValue()));
		return bb.getValueN(2);//�������ݲ��֣��������Ⱥ�״̬
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
			Thread.sleep(200);//cpuæ���ȴ�һ��
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
