package com.techrich.client.device.cardreader;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.IntegratedCardReader;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;

public class SuoTianCardReader extends IntegratedCardReader {
	// ���������ն�ͷ��ʶ
	public static final byte IN_HEADER = (byte)0xBD;
	// �ն˵�������ͷ��ʶ
	public static final byte OUT_HEADER = (byte)0xBA;

	public static final byte MF_FindCard = (byte)0x20;
	public static final byte MF_GetSNR = (byte)0x21;
	public static final byte MF_Auth_KEYA = (byte)0x22;
	public static final byte MF_Auth_KEYB = (byte)0x23;
	public static final byte MF_ReadBlk = (byte)0x24;
	public static final byte MF_WriteBlk = (byte)0x25;
	public static final byte MF_ReadVal = (byte)0x26;
	public static final byte MF_WriteVal = (byte)0x27;
	public static final byte MF_AddValue = (byte)0x28;
	public static final byte MF_DescValue = (byte)0x29;

	public static final byte Version = (byte)0x60;
	
	public SuoTianCardReader(){
		this.deviceId = "CARDREADER-SUOTIAN-RF";
	}
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"����RF������";
	}
	
	@Override
	public String getDeviceIdentifer() throws Exception {
		try{
			ByteBuffer bb = comm2reader(new byte[]{Version});//�����к���Ϣ
			return new String(bb.getValue());
		}catch(Exception e){
			throw new Exception(e);
		}

	}

	@Override
	public void initDevice() throws DeviceInitException {
		canReadRFCard = true;
	}

	@Override
	public void findRFCard() throws Exception {
		ByteBuffer bb = comm2reader(new byte[]{MF_FindCard});//Ѱ��
	}

	@Override
	public String getRFCardSerialNo() throws Exception {
		ByteBuffer bb = comm2reader(new byte[]{MF_GetSNR});//�����к���Ϣ
		return Tools.bytes2hex(bb.getValue());
	}

	@Override
	public void authRFCardKeyA(int secNo, byte[] password) throws Exception {
		ByteBuffer bb = new ByteBuffer();
		bb.append(MF_Auth_KEYA);
		bb.append((byte)secNo);
		bb.append(password);
		comm2reader(bb.getValue());
	}

	@Override
	public void authRFCardKeyB(int secNo, byte[] password) throws Exception {
		ByteBuffer bb = new ByteBuffer();
		bb.append(MF_Auth_KEYB);
		bb.append((byte)secNo);
		bb.append(password);
		comm2reader(bb.getValue());
	}

	@Override
	public String readRFCardData(int secNo, int blockNo) throws Exception {
		ByteBuffer bb = new ByteBuffer();
		bb.append(MF_ReadBlk);
		bb.append((byte)secNo);
		bb.append((byte)blockNo);
		bb = comm2reader(bb.getValue());
		return Tools.bytes2hex(bb.getValue());
	}

	@Override
	public void writeRFCardData(int secNo, int blockNo, byte[] data) throws Exception {
		ByteBuffer bb = new ByteBuffer();
		bb.append(MF_ReadBlk);
		bb.append((byte)secNo);
		bb.append((byte)blockNo);
		bb.append(data);
		comm2reader(bb.getValue());
	}

	@Override
	public String addRFCardValue(int secNo, int blockNo, int value) throws Exception {
		ByteBuffer bb = new ByteBuffer();
		bb.append(MF_AddValue);
		bb.append((byte)secNo);
		bb.append((byte)blockNo);
		bb.append(Tools.htonl(value));
		comm2reader(bb.getValue());
		return null;
	}

	@Override
	public String decreaseRFCardValue(int secNo, int blockNo, int value) throws Exception {
		ByteBuffer bb = new ByteBuffer();
		bb.append(MF_DescValue);
		bb.append((byte)secNo);
		bb.append((byte)blockNo);
		bb.append(Tools.htonl(value));
		comm2reader(bb.getValue());
		return null;
	}

	protected ByteBuffer comm2reader(byte[] data) throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{OUT_HEADER});
		bb.append((byte)(data.length + 1));
		bb.append(data);
		bb.append(bb.getCheckSum());
		//		System.out.println("�������ݣ�"+Tools.bytes2hex(bb.getValue()));
		sendDirect(bb.getValue());
		bb.reset();
		byte head = recieveData(1)[0];
		bb.append(head);
		byte len = recieveData(1)[0];
		bb.append(len);
		bb.append(recieveData(len - 1));
		//		System.out.println("�������ݣ�"+Tools.bytes2hex(bb.getValue()));
		byte bcc = recieveData(1)[0];
		if(bcc != bb.getCheckSum()) throw new Exception("����֤����");
		if(head != IN_HEADER) throw new Exception("������Ӧ��");
		if(bb.getByteAt(3) != 0x00) throw new Exception("ͨѶ���ش���");
		if(bb.length() > 4) return new ByteBuffer(bb.getValueN(4));
		return new ByteBuffer();
	}

	@Override
	public void closeDoor() throws Exception {
	}

	@Override
	public void deviceCheck() throws Exception {

	}

	@Override
	public void openDoor(int type, int location) throws Exception {
	}

	@Override
	public void moveCard(int loc) throws Exception {
		
	}

	@Override
	public int getCardReaderRuntimeStatus() throws Exception {
		return 0;
	}

	@Override
	public String[] readTrackDatas(boolean track1, boolean track2, boolean track3) throws Exception {
		throw new RuntimeException("����������֧�ִ˲���!");
	}

	@Override
	public byte[] openICCard() throws Exception {
		throw new RuntimeException("����������֧�ִ˲���!");
	}

	@Override
	public void powerDownICCard() throws Exception {
		throw new RuntimeException("����������֧�ִ˲���!");
	}

	@Override
	public byte[] apdu(byte[] data) throws Exception {
		throw new RuntimeException("����������֧�ִ˲���!");
	}

	@Override
	public int powerUpSamCard(int socket) throws Exception {
		throw new RuntimeException("����������֧�ִ˲���!");
	}

	@Override
	public void powerDownSamCard() throws Exception {
		throw new RuntimeException("����������֧�ִ˲���!");
	}



}
