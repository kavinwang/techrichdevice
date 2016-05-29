package com.techrich.client.device.cardreader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONObject;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.IntegratedCardReader;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;

public class SuoTianCTKRreader extends IntegratedCardReader {
	// ���������ն�ͷ��ʶ
	public static final byte IN_HEADER = (byte)0xBD;
	// �ն˵�������ͷ��ʶ
	public static final byte OUT_HEADER = (byte)0xBA;

	public static final byte MF_FindCard = (byte)0x0F;
	public static final byte MF_GetSNR = (byte)0x21;
	public static final byte MF_Auth_KEYA = (byte)0x22;
	public static final byte MF_Auth_KEYB = (byte)0x23;
	public static final byte MF_ReadBlk = (byte)0x24;
	public static final byte MF_WriteBlk = (byte)0x25;
	public static final byte MF_ReadVal = (byte)0x26;
	public static final byte MF_WriteVal = (byte)0x27;
	public static final byte MF_AddValue = (byte)0x28;
	public static final byte MF_DescValue = (byte)0x10;
	public static final byte CTK_READBALANCE = (byte)0x11;
	public static final byte MFPRO_APDU = (byte)0x0E;

	public static final byte SAM_PowerUp = (byte)0x30;
	public static final byte SAM_PowerDown = (byte)0x31;
	public static final byte SAM_ExchangeApdu = (byte)0x32;

	public static final byte Version = (byte)0x60;
	private String samSNR;
	
	private int CardType = IntegratedCardReader.CARD_TYPE_NOCARD;
	public SuoTianCTKRreader(){
		this.deviceId = "CARDREADER-SUOTIAN-CTK";
	}
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"����CTK������";
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
		try{
//			this.powerUpSamCard(1, 1);
//			this.samSNR = this.samCardApdu(0, 1, Tools.hex2bytes("00B095000C"));
		}catch(Exception e){
			// TODO Auto-generated catch block
			throw new DeviceInitException("SAM����ʼ��ʧ��", e);
		}
	}

	@Override
	public void findRFCard() throws Exception {
		ByteBuffer bb = comm2reader(new byte[]{MF_FindCard});//Ѱ��
		CardType = IntegratedCardReader.CARD_TYPE_RFCARD;
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
		bb.append(CTK_READBALANCE);
		ByteBuffer buf = comm2reader(bb.getValue());
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("AppDate", Tools.bytes2hex(buf.getValueN(20, 4)));//��������
		map.put("ApplicationFlag", String.valueOf(buf.getByteAt(8)));//Ӧ�ñ�־ 
		map.put("AppSerialNo", Tools.bytes2hex(buf.getValueN(10, 10)).substring(0, 19));//Ӧ�ÿ��� 
		map.put("AppVer", String.valueOf(buf.getByteAt(9)));//Ӧ�ð汾 
		map.put("CardTypeFlag", String.valueOf(buf.getByteAt(30)));//��Ӧ�ñ�ʾ;
		map.put("Emprolee", String.valueOf(buf.getByteAt(31)));//Ա����ʶ;
		map.put("ExpDate", Tools.bytes2hex(buf.getValueN(24, 4)));//��Ч��ֹ����
		map.put("Fic", Tools.bytes2hex(buf.getValueN(28, 2)));//��Ч��ֹ����
		map.put("IdType", String.valueOf(buf.getByteAt(84)));//֤������
		map.put("IssueFlag", Tools.bytes2hex(buf.getValueN(0, 8)));//��������ʶ
		map.put("UserIdNum", Tools.bytes2hex(buf.getValueN(52, 32)).trim());//���֤��
		map.put("UserName", Tools.bytes2hex(buf.getValueN(32, 20)).trim());//����
		map.put("Balance", String.valueOf(Integer.parseInt(Tools.bytes2hex(buf.getValueN(85)), 16)));//����
		map.put("SAMSNR", this.samSNR);
		return new JSONObject(map).toString();
//		return JSONUtil.serialize(map);
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
		bb.append(Tools.htonl(value));
		bb.append(Tools.hex2bytes(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())));
		ByteBuffer buf = comm2reader(bb.getValue());
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("CardTrace", Tools.bytes2hex(buf.getValueN(0, 2)));//�û�����ˮ��
		map.put("OverDraft", String.valueOf(Integer.parseInt(Tools.bytes2hex(buf.getValueN(2, 3)), 16)));//͸֧���
		map.put("Account", String.valueOf(Integer.parseInt(Tools.bytes2hex(buf.getValueN(5, 4)), 16)));//���׽��
		map.put("Transtype", String.valueOf(buf.getByteAt(9)));//��������
		map.put("SamId", Tools.bytes2hex(buf.getValueN(10, 6)));//SAM ID
		map.put("TransTime", Tools.bytes2hex(buf.getValueN(16, 7)));//��������ʱ��
		map.put("Balance", String.valueOf(Integer.parseInt(Tools.bytes2hex(buf.getValueN(23, 4)), 16) - value));//�������
		map.put("TAC", Tools.bytes2hex(buf.getValueN(27, 4)));//TAC
		map.put("CardNum", Tools.bytes2hex(buf.getValueN(31, 10)).substring(0, 19));//����
		map.put("SamTrace", String.valueOf(Integer.parseInt(Tools.bytes2hex(buf.getValueN(41, 4)), 16)));//SAM����ˮ��
		map.put("SAMSNR", this.samSNR);
		return new JSONObject(map).toString();
//		return JSONUtil.serialize(map);
	}

	protected ByteBuffer comm2reader(byte[] data) throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{OUT_HEADER});
		int len = data.length + 1;
		byte[] bLen = new byte[2];
		bLen[0] = (byte)((len & 0x0000ff00) >> 8);
		bLen[1] = (byte)(len & 0x000000ff);
		bb.append(bLen);
		bb.append(data);
		bb.append(bb.getCheckSum());
		LogManager.logInfo("CTK Reader", "�������ݣ�" + Tools.bytes2hex(bb.getValue()));
		sendDirect(bb.getValue());
		bb.reset();
		byte head = recieveData(1)[0];
		bb.append(head);
		bLen = recieveData(2);
		len = bLen[0] * 256 + bLen[1];
		bb.append(bLen);
		bb.append(recieveData(len - 1));
		LogManager.logInfo("CTK Reader", "�������ݣ�" + Tools.bytes2hex(bb.getValue()));
		byte bcc = recieveData(1)[0];
		if(bcc != bb.getCheckSum()) throw new Exception("����֤����");
		if(head != IN_HEADER) throw new Exception("������Ӧ��");
		if(bb.getByteAt(4) != 0x00) throw new Exception("ͨѶ���ش���");
		if(bb.length() > 5) return new ByteBuffer(bb.getValueN(5));
		return new ByteBuffer();
	}


	@Override
	public int powerUpSamCard(int socket) throws Exception {
		CardType = IntegratedCardReader.CARD_TYPE_SAMCARD;
		ByteBuffer bb = new ByteBuffer(new byte[]{this.SAM_PowerUp});
		bb.append((byte)socket);
		bb = comm2reader(bb.getValue());
		if(bb.length()>0)return bb.getByteAt(0);
		else return 0;
	}

	@Override
	public byte[] apdu(byte[] data) throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{this.SAM_ExchangeApdu});
		bb.append((byte)data[0]);
		bb.append((byte)(data.length-1));
		bb.append(data,1,data.length-1);

		ByteBuffer buf = comm2reader(bb.getValue());
		String sw = Tools.bytes2hex(buf.getValueN(buf.length() - 2));
		if(sw.startsWith("61")){
			System.out.println("��Ӧ��:" + sw);
			bb = new ByteBuffer(new byte[]{this.SAM_ExchangeApdu});
			bb.append((byte)data[0]);
			bb.append((byte)0x05);
			bb.append(new byte[]{(byte)0x00, (byte)0xC0, (byte)0x00, (byte)0x00});
			bb.append(buf.getByteAt(buf.length() - 1));
			buf = comm2reader(bb.getValue());
			sw = Tools.bytes2hex(buf.getValueN(buf.length() - 2));
			if(!sw.equals("9000")){ throw new Exception("APDU����:" + sw); }
		}else if(!sw.equals("9000")){ throw new Exception("APDU����:" + sw); }
		return buf.getValueN(0, buf.length() - 2);
	}

	@Override
	public void openDoor(int type, int location) throws Exception {
		
	}

	@Override
	public void closeDoor() throws Exception {
		
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
		throw new RuntimeException("�˲�����û��ʵ��!");
	}

	@Override
	public void powerDownICCard() throws Exception {
		throw new RuntimeException("�˲�����û��ʵ��!");
	}

	@Override
	public void powerDownSamCard() throws Exception {
	}

	@Override
	public void deviceCheck() throws Exception {
	}

//	@Override
//	public String rfCardApdu(byte[] data) throws Exception {
//		// TODO Auto-generated method stub
//		ByteBuffer bb = new ByteBuffer(new byte[]{MFPRO_APDU});
//		bb.append(data);
//
//		ByteBuffer buf = comm2reader(bb.getValue());
//		String sw = Tools.bytes2hex(buf.getValueN(buf.length() - 2));
//		if(sw.startsWith("61")){
//			System.out.println("��Ӧ��:" + sw);
//		}else if(!sw.equals("9000")){ throw new Exception("APDU����:" + sw); }
//		return Tools.bytes2hex(buf.getValueN(0, buf.length() - 2));
//	}
//
//	@Override
//	public String rfInitForLoad(int money) throws Exception {
//		// TODO Auto-generated method stub
//		HashMap map = new HashMap();
//		map.put("CardRandom", "ABCDEF12");
//		map.put("MAC1", "88AA22BB");
//		map.put("KeyVersion", "01");
//		map.put("Algorithm", "00");
//		map.put("CardTrace", "000000A3");
//		map.put("NewBalance", "00008FFF");
//		return JSONUtil.serialize(map);
//	}

}
