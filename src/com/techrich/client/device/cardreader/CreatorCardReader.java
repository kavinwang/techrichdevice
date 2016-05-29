package com.techrich.client.device.cardreader;

import org.apache.velocity.util.StringUtils;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.IntegratedCardReader;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;

public class CreatorCardReader extends IntegratedCardReader {
	private int cardStatus=0xFF;
	private int canAcceptCard = 0;//0x00:�����������0x01:��˽��� 0x02��˽��� 0x03��������
	private boolean frontDoorStatus = false;//�ر�
	private boolean backDoorStatus = false;//�ر�
	private int CardType = CARD_TYPE_NOCARD;//��ǰ�Ŀ�����
	
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"���ڴ��Զ�����";
	}
	
	public boolean canFrontCardInsert(){
		return frontDoorStatus;
	}
	public boolean canBackCardInsert(){
		return backDoorStatus;
	}
	public CreatorCardReader() {
		this.deviceId = "CARDREADER-CZ-3IN1";
	}

	@Override
	public String getDeviceIdentifer() throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x30,0x3A});//�����к���Ϣ
		return new String(bb.getValueN(3));
	}

	@Override
	public void initDevice() throws DeviceInitException {
		try {
			//������λ
			ByteBuffer bb = commToCardReader(new byte[]{0x30,0x31});//������λ�����ض������汾��Ϣ��ǰ�˵������ֿ�
			//���ý�����ʽ
			bb = commToCardReader(new byte[]{0x2F,0x33,0x31});//����ſ���IC�����ǽӴ�����˫���濨������ʽ��ֻ�����ǰ�˿�բ�Ž���
			if(bb.getByteAt(3)=='N')throw new Exception("���ý�����ʽʧ��");
			//����ͣ����ʽ
			bb = commToCardReader(new byte[]{0x2E,0x31});//������ͣ��ǰ�˲��ֿ�
			if(bb.getByteAt(2)=='N')throw new Exception("����ͣ����ʽʧ��");
			//����
			bb = commToCardReader(new byte[]{0x2F,0x31,0x31});
			if(bb.getByteAt(3)=='N')throw new Exception("���ö���������ʧ��");
			//ȡ״̬������
			bb = commToCardReader(new byte[]{0x31,0x30});
			byte[] st = bb.getValueN(2);
			cardStatus = st[0];//��״̬,�μ��ĵ����н���
			if(st[1]==0x4E)frontDoorStatus=false;//ǰ�Ž�ֹ����
			else frontDoorStatus = true;//ǰ���������
			if(st[2]==0x4E)backDoorStatus = false;//���Ž�ֹ����
			else backDoorStatus = true;//�����������
			
			//ȷ����ǰ�Ķ�����֧�ֵ�ģ��
			bb = commToCardReader(new byte[]{0x47,0x30,0x04,0x04});
			if(bb.getByteAt(2)=='N')throw new Exception("�ж϶�����ģ�����");
			byte[] pre = bb.getValueN(5, 4);
			if(pre[0]==0x30)canReadMagnetCard=false;
			else canReadMagnetCard=true;
			if(pre[1]==0x31)canReadRFCard=true;
			if(pre[2]==0x31){
				canReadMemoryCard = true;
				canReadCPUCard = true;
				canReadSimCard=true;
			}
			if(pre[3]==0x31)canReadSamCard=true;

			StringBuffer sb = new StringBuffer("��ǰ���Զ�������");
			sb.append(canReadMagnetCard?"�ſ�֧��":"�ſ���֧��").append("|");
			sb.append(canReadRFCard?"�ǽӴ���֧��":"�ǽӴ�����֧��").append("|");
			sb.append(canReadMemoryCard?"IC��֧��":"IC����֧��").append("|");
			sb.append(canReadCPUCard?"CPU��֧��":"CPU����֧��").append("|");
			sb.append(canReadSimCard?"SIM��֧��":"SIM����֧��").append("|");
			sb.append(canReadSamCard?"SAM��֧��":"SAM����֧��").append("|");
			LogManager.logInfo(sb.toString());
		} catch (Exception e) {
			throw new DeviceInitException(e);
		}
	}
	
	
	/**
	 * �򿪶������ţ�����忨�Ͷ���
	 * @param type 0:�ſ���ʽ 1�����ط�ʽ��֧���������͵Ŀ� 2�����źŷ�ʽ��֧�ֱ���
	 * @param location �ֿ�λ�� 0��ͣ��ǰ�˲��ֿ���1��ͣ��ǰ�˳ֿ� 2��RF��λ�� 3��IC��λ�� 4��ͣ�ں�˳ֿ� 5��ͣ�ں�˲��ֿ�
	 * @throws Exception
	 */
	@Override
	public void openDoor(int type,int loc) throws Exception {
		byte location =(byte)(0x30+loc);
		byte doorMode = 0x32;
		if(type==1)doorMode=0x33;
		else if(type==2)doorMode=0x34;
		//���гֿ��ͺ�ֿ�ʱ�����ط�ʽ����ʹ�ܣ�����ſ���IC����Mefare 1��Ƶ����˫���濨��ǰ�˿�բ�Ž�����
		//����ֻ���Ǵſ���ǰ�Ž�����ʽ
		if(location<0x32)doorMode=0x32;
		ByteBuffer bb = commToCardReader(new byte[]{0x2F,doorMode,0x31});//0x32 0x33
		if(bb.getByteAt(3)=='N')throw new Exception("���ö���������ʧ��");
		bb = commToCardReader(new byte[]{0x2E,location});
		if(bb.getByteAt(2)=='N')throw new Exception("���ö������ֿ�λ�ô���");
		getCardReaderRuntimeStatus();
		if(canAcceptCard <2)throw new Exception("�򿪶�������ʧ�ܣ�");//��λΪǰ��
	}
	
	@Override
	public void closeDoor() throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x2F,0x31,0x31});
		if(bb.getByteAt(3)=='N')throw new Exception("���ö���������ʧ��");
	}

	/**
	 * ���ò��߿���ָ��λ��
	 * @param loc �߿���λ�� 0�� �ߵ�ǰ�˲��ֿ� 1���ߵ�ǰ�˳ֿ� 2���ߵ���˳ֿ� 
	 * 						3���ߵ���˲��ֿ� 4��󵯳��� 5���гֿ� 6���ߵ�������IC��λ
	 * @throws Exception
	 */

	@Override
	public void moveCard(int loc) throws Exception {
		byte location = 0x30;
		if(loc>=0&&loc<=4)location +=loc;
		else{
			if(loc==5)location=0x2E;
			else if(loc==6)location=0x2F;
		}
		ByteBuffer bb = commToCardReader(new byte[]{0x32,location});
		if(bb.getByteAt(2)=='N')throw new Exception("���ö���������ʧ��");
		//XXX ����������Ϣ��Ҫ����
	}

	/**
	 * ȡ�����ĵ�ǰ��״̬
	 * 0���������޿� 
	 * 1���������г��� 2���������ж̿� 3������ǰ�˲��ֿ�λ���п� 4������ǰ�˳ֿ�λ���п�
	 * 5��������ͣ��λ���п� 6��������IC��λ���п� 7��������˳ֿ�λ���п� 8��������˲��ֿ�λ���п�
	 * @return
	 * @throws Exception
	 */
	@Override
	public int getCardReaderRuntimeStatus()throws Exception{
		ByteBuffer bb = commToCardReader(new byte[]{0x31,0x30});
		byte[] st = bb.getValueN(2);
		cardStatus = st[0];//��״̬,�μ��ĵ����н���
		canAcceptCard = 0x00;
		if(st[1] != 0x4E)canAcceptCard+=0x02;
		if(st[2] != 0x4E)canAcceptCard+=0x01;
		getCardStatusInfo();
		if(cardStatus==0x4E)return 0;
		else return cardStatus-0x45;
	}
	
	private int parseCommResult(byte bb) throws Exception {
		if(bb=='0')throw new Exception("Ѱ������Ƶ��");
		if(bb=='1')throw new Exception("���������Ŵ�");
		if(bb=='2')throw new Exception("�����Ŀ����кŴ���");
		if(bb=='3')throw new Exception("�������");
		if(bb=='4')throw new Exception("�����ݴ���");
		if(bb=='N')throw new Exception("����ʧ��");
		if(bb=='E')throw new Exception("�������޿�");
		if(bb=='W')throw new Exception("���������������λ����");
		if(bb=='Y')return 0;
		return 1;
	}
	
	public String getCardStatusInfo(){
		if(cardStatus==0x46)return "�������г���";
		else if(cardStatus==0x47)return "�������ж̿�";
		else if(cardStatus==0x48)return "����ǰ�˲��ֿ�λ���п�";
		else if(cardStatus==0x49)return "����ǰ�˳ֿ�λ���п�";
		else if(cardStatus==0x4A)return "������ͣ��λ���п�";
		else if(cardStatus==0x4B)return "������IC������λ���п�";
		else if(cardStatus==0x4C)return "������˳ֿ�λ���п�";
		else if(cardStatus==0x4D)return "������˲��ֿ�λ���п�(û�տ�)";
		else if(cardStatus==0x4E)return "�������޿�";
		else return "��������";
	}
	
	//������
	@Override
	public String[] readTrackDatas(boolean track1, boolean track2, boolean track3) throws Exception {
		this.CardType  = CreatorCardReader.CARD_TYPE_MAGNET;
//		byte mode = (byte)(0x30 | (track1?0x01:0x00) | (track2?0x02:0x00) | (track3?0x04:0x00));
//
//		ByteBuffer bb = commToCardReader(new byte[] { 0x45, 0x30, 0x30, mode });
//		for (int i = 0; i < bb.length(); i++)if (bb.getByteAt(i) == US)bb.replace(i, (byte) '$');
//		String validTrackData = new String(bb.getValueN(4), "iso8859-1");
//		String[] result = StringUtils.split(validTrackData, "$");
//		if (result.length < 3)throw new Exception("δ�ܶ���3���ŵ�����");
//		for (int i = 0; i < result.length; i++) {
//			System.out.println("�ŵ� " + i + " ������Ϊ��"	+ (result[i] == null ? "" : result[i]));
//		}
//		// ������Ĵŵ����߲����Ĵŵ����ݲ�����
//		String[] trackDatas = new String[3];
//		if (result[0] != null && result[0].length() > 0	&& result[0].charAt(0) == 'Y')trackDatas[0] = result[0].substring(1);
//		if (result[1] != null && result[1].length() > 0	&& result[1].charAt(0) == 'Y')trackDatas[1] = result[1].substring(1);
//		if (result[2] != null && result[2].length() > 0	&& result[2].charAt(0) == 'Y')trackDatas[2] = result[2].substring(1);
//		return trackDatas;
		byte mode = 0x30;
		if (track1 == true && track2 == false && track3 == false)mode = 0x31;
		else if (track1 == false && track2 == true && track3 == false)mode = 0x32;
		else if (track1 == false && track2 == false && track3 == true)mode = 0x33;
		else if (track1 == true && track2 == true && track3 == false)mode = 0x34;
		else if (track1 == false && track2 == true && track3 == true)mode = 0x35;
		else if (track1 == true && track2 == false && track3 == true)mode = 0x36;
		else if (track1 == true && track2 == true && track3 == true)mode = 0x37;

		ByteBuffer bb = commToCardReader(new byte[] { 0x45, 0x30, 0x30, mode });
		System.out.println("�����Ŀ���ϢΪ��" + Tools.bytes2hex(bb.getValue()));
		for (int i = 0; i < bb.length(); i++)if (bb.getByteAt(i) == 0x1F)bb.replace(i, (byte) '$');
		String validTrackData = new String(bb.getValueN(4), "iso8859-1");
		String[] result = StringUtils.split(validTrackData, "$");
		if (result.length < 3)throw new Exception("δ�ܶ���3���ŵ�����");
		for (int i = 0; i < result.length; i++) {
			System.out.println("�ŵ� " + i + " ������Ϊ��"	+ (result[i] == null ? "" : result[i]));//�ͻ���Ϣ��ȫԭ�򣬲������¼��־
		}
		// ������Ĵŵ����߲����Ĵŵ����ݲ�����
		String[] trackDatas = new String[3];
		if (result[0] != null && result[0].length() > 0	&& result[0].charAt(0) == 'Y')trackDatas[0] = result[0].substring(1);
		if (result[1] != null && result[1].length() > 0	&& result[1].charAt(0) == 'Y')trackDatas[1] = result[1].substring(1);
		if (result[2] != null && result[2].length() > 0	&& result[2].charAt(0) == 'Y')trackDatas[2] = result[2].substring(1);
		return trackDatas;

	}
  
	
	//IC��
	@Override
  public byte[] openICCard() throws Exception {
		this.CardType  = CreatorCardReader.CARD_TYPE_NOCARD;
    ByteBuffer bb = commToCardReader(new byte[] { 0x32, 0x2F});//�ߵ�IC��λ��
    bb = commToCardReader(new byte[] { 0x33, 0x30 });//�ϵ�
    bb = commToCardReader(new byte[] { 0x37, 0x30 });//�临λ 0x2FΪ�ȸ�λ
    this.CardType  = CreatorCardReader.CARD_TYPE_ICCARD;
    return bb.getValue();
  }

	/**
	 * ��IC������apdu����
	 * @param data apdu����
	 * @throws Exception
	 */
	@Override
	public byte[] apdu(byte[] data)throws Exception{
		if(this.CardType == CreatorCardReader.CARD_TYPE_ICCARD){
			byte[] len = Tools.htons((short)data.length);
			ByteBuffer bb = new ByteBuffer(new byte[]{0x37,0x31});
			bb.append(len).append(data);
			bb = commToCardReader(bb.getValue());
			parseCommResult(bb.getByteAt(2));
			String results = Tools.bytes2hex(bb.getValueN(5));
	
			int resuLen = results.length();
			String sw = results.substring(resuLen - 4, resuLen);
			if(!sw.equalsIgnoreCase("9000")){ throw new Exception(sw); }
			return Tools.hex2bytes(results.substring(0, resuLen - 4));
		}else if(this.CardType == CreatorCardReader.CARD_TYPE_SAMCARD){
			//XXX :��SAM������apdu����,��һ���ֽ��ǿ�����
			ByteBuffer bb = new ByteBuffer(new byte[]{0x3D,(byte)0x31,data[0]});//ֻ��T=0�� T=1�Ĳ���
			bb.append(Tools.htons((short)(data.length-1))).append(data,1,data.length-1);
			bb = commToCardReader(bb.getValue());
			parseCommResult(bb.getByteAt(2));
			return bb.getValueN(6);
		}else throw new Exception("����ȷ����Ƭ������,�����ϵ���ٲ���!");
	}
	
	/**
	 * IC ���µ�
	 * @return
	 * @throws Exception
	 */
	@Override
	public void powerDownICCard()throws Exception{
		//ʹ��Sim�����µ�ָ��
		ByteBuffer bb = commToCardReader(new byte[]{0X33,0x31});
		parseCommResult(bb.getByteAt(2));
		this.CardType  = CreatorCardReader.CARD_TYPE_NOCARD;
	}
	
	//��SIM/SAM���Ķ�д,�û���Ȩ��
	
	/**
	 * ��SAM�������ϵ�ͳ�ʼ��
	 * @param type ����ѹ���ͣ�������,����18��30��50
	 * @param socket ������
	 * @return ���صĿ�������ʽ�������Ķ�SIM���������븽��������ؽ��
	 * @throws Exception
	 */
	@Override
	public int powerUpSamCard(int socket)throws Exception{
//		byte t = 0x2E; //1.8v
//		if(type==30)t = 0x2F;//3.0v
//		else if(type==50)t = 0x30; //5.0v
		this.CardType  = CreatorCardReader.CARD_TYPE_NOCARD;
		ByteBuffer bb = commToCardReader(new byte[]{0x3D,0x2F,(byte)socket});//ֻ��3v��
		int status =  parseCommResult(bb.getByteAt(3));
		this.CardType  = CreatorCardReader.CARD_TYPE_SAMCARD;
		return status;
	}
	
	/**
	 * ��SIM�������µ����
	 * @throws Exception
	 */
	@Override
	public void powerDownSamCard()throws Exception{
		ByteBuffer bb = commToCardReader(new byte[]{0x4A,0x31});
		parseCommResult(bb.getByteAt(2));
		this.CardType  = CreatorCardReader.CARD_TYPE_NOCARD;
	}

	
	//mifare ��
	@Override
	public void findRFCard() throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x35,0x30});
		parseCommResult(bb.getByteAt(2));
		this.CardType  = CreatorCardReader.CARD_TYPE_RFCARD;
	}

	@Override
	public String getRFCardSerialNo() throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x35,0x31});
		parseCommResult(bb.getByteAt(2));
		return Tools.bytes2hex(bb.getValueN(3));
	}
	@Override
	public void authRFCardKeyA(int secNo, byte[] password) throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x32,(byte)secNo});
		for(int i=0;i<6;i++)bb.append(password[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(3));
	}


	@Override
	public String readRFCardData(int secNo, int blockNo) throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x35,0x33,(byte)secNo,(byte)blockNo});
		parseCommResult(bb.getByteAt(4));
		return Tools.bytes2hex(bb.getValueN(5));
	}

	@Override
	public void writeRFCardData(int secNo, int blockNo, byte[] data)throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x34,(byte)secNo,(byte)blockNo});
		for(int i=0;i<16;i++)bb.append(data[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(4));
		
	}
	
	@Override
	public String addRFCardValue(int secNo, int blockNo, int value)
			throws Exception {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (value & 0x000000ff);
		bytes[1] = (byte) ((value & 0x0000ff00) >> 8);
		bytes[2] = (byte) ((value & 0x00ff0000) >> 16);
		bytes[3] = (byte) ((value & 0xff000000) >> 24);
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x37,(byte)secNo,(byte)blockNo});
		for(int i=0;i<4;i++)bb.append(bytes[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(4));
		return new String(bb.getValue());
	}


	@Override
	public String decreaseRFCardValue(int secNo, int blockNo, int value)
			throws Exception {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (value & 0x000000ff);
		bytes[1] = (byte) ((value & 0x0000ff00) >> 8);
		bytes[2] = (byte) ((value & 0x00ff0000) >> 16);
		bytes[3] = (byte) ((value & 0xff000000) >> 24);
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x38,(byte)secNo,(byte)blockNo});
		for(int i=0;i<4;i++)bb.append(bytes[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(4));
		return new String(bb.getValue());
	}

	@Override
	public void authRFCardKeyB(int secNo, byte[] password) throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x39,(byte)secNo});
		for(int i=0;i<6;i++)bb.append(password[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(3));
		
	}


	/**
	 * �Ͷ���������ͨѶ
	 * @param commandData Ϊ�����������+�������
	 * @return ͨѶ�Ľ�����ݣ�������STX�����ȡ�ETX��BCC��Ϣ
	 * @throws Exception
	 */
//	private ByteBuffer commToCardReader(byte[] commandData)throws Exception{
	public ByteBuffer commToCardReader(byte[] commandData)throws Exception{//�ⲿ3��һ���� ��Ϊpublic
		if(commandData==null)commandData=new byte[]{};
		ByteBuffer bb = new ByteBuffer();
		bb.append(STX).append(Tools.htons((short)commandData.length)).append(commandData).append(ETX);
		byte bcc = 0x00;
		for(int i=0;i<bb.length();i++)bcc^=bb.getByteAt(i);
		bb.append(bcc);
		
		for(int i=0;i<3;i++){//����3��
			sendDirect(bb.getValue());
			byte ack = recieveData(1, 5000)[0];
			if(ack==ACK){//�ɹ�
				sendDirect(new byte[]{ENQ});
				break;
			}else if(ack==NAK){//bcc���󣬽������·���
				if(i==2)throw new Exception("�Ͷ�����ͨѶ���ܽ���");
			}
			
		}
		//��ʼ���ս������
		bb.reset();
//		while(true){
		byte[] stxs = recieveData(1,5000);
		if(stxs[0]!=STX){
//				break;
			throw new Exception("�յ������ݰ�����STX��ͷ");
		}
//		}
		bb.append(STX);
		byte[] len = recieveData(2);
		bb.append(len);
		int length = Tools.ntohs(len, 0);
		
		bb.append(recieveData(length));
		byte etx = recieveData(1)[0];
		if(etx!=ETX)throw new Exception("���������ص����ݰ���ʽ����");
		bb.append(ETX);
		
		byte bccr = recieveData(1)[0];
		//У��bcc
		bcc = 0x00;
		for(int i=0;i<bb.length();i++)bcc^=bb.getByteAt(i);
		if(bccr==bcc){
			System.out.println("�յ������ݣ�"+Tools.bytes2hex(bb.getValue()));
			if(bb.getByteAt(3)=='N')throw new Exception("�յ����������ص�����");
			return new ByteBuffer(bb.getValueN(3, length));
		}else{
			throw new Exception("�յ������������ݼ���BCC����");
		}
	}
	@Override
	public void deviceCheck() throws Exception {
		
	}

}
