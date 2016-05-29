package com.techrich.client.device;

import com.techrich.client.manager.AbstractCommDevice;

/**
 * ��ʵ�����б�������������������ͣ�����ص���������Ϊtrue�����ܶ��Ŀ�����Ϊfalse
 */
public abstract class IntegratedCardReader extends AbstractCommDevice {
	protected static final int CARD_TYPE_NOCARD = -1;
	protected static final int CARD_TYPE_MAGNET = 0;
	protected static final int CARD_TYPE_ICCARD = 1;
	protected static final int CARD_TYPE_SAMCARD = 2;
	protected static final int CARD_TYPE_RFCARD = 3;
	
	protected boolean canReadMagnetCard=false;//������
	protected boolean canReadRFCard=false;//�ǽӴ���,��Ƶ��,mifare��
	protected boolean canReadMemoryCard = false;//�Ӵ���,������ͨ��memory��,IC��
	protected boolean canReadCPUCard=false;//��CPU���Ķ�д����ͨCPU���ܿ�
	protected boolean canReadSimCard=false;//��SIM���Ķ�д,�û���Ȩ��
	
	// ��ʱû���ṩAPI
	protected boolean canReadSamCard=false;//��SAM���Ķ�д,��ȫ���ܿ�
	
	public IntegratedCardReader(){
		this.deviceId = "CardReader";
	}
	public final boolean canReadMagnetCard(){
		return canReadMagnetCard;
	}
	public final boolean canReadRFCard(){
		return canReadRFCard;
	}
	public final boolean canReadMemoryCard(){
		return canReadMemoryCard;
	}
	public final boolean canReadSimCard(){
		return canReadSimCard;
	}
	public final boolean canReadSamCard(){
		return canReadSamCard;
	}
	public final boolean canReadCPUCard(){
		return canReadCPUCard;
	}
	/**
	 * �򿪶������ţ�����忨�Ͷ���
	 * @param type 0:�ſ���ʽ 1�����ط�ʽ��֧���������͵Ŀ� 2�����źŷ�ʽ��֧�ֱ���
	 * @param location �ֿ�λ�� 0��ͣ��ǰ�˲��ֿ���1��ͣ��ǰ�˳ֿ� 2��RF��λ�� 3��IC��λ�� 4��ͣ�ں�˳ֿ� 5��ͣ�ں�˲��ֿ�
	 * @throws Exception
	 */
	public abstract void openDoor(int type,int location) throws Exception;

	/**
	 * �رն������ţ�������忨�Ͷ���
	 * @throws Exception
	 */
	public abstract void closeDoor() throws Exception;
	
	/**
	 * ���ò��߿���ָ��λ��
	 * @param loc �߿���λ�� 0�� �ߵ�ǰ�˲��ֿ� 1���ߵ�ǰ�˳ֿ� 2���ߵ���˳ֿ� 
	 * 						3���ߵ���˲��ֿ� 4��󵯳��� 5���гֿ� 6���ߵ�������IC��λ
	 * @throws Exception
	 */
	public abstract void moveCard(int loc) throws Exception;
	
	/**
	 * ȡ�����ĵ�ǰ��״̬
	 * 0���������޿� 
	 * 1���������г��� 2���������ж̿� 3������ǰ�˲��ֿ�λ���п� 4������ǰ�˳ֿ�λ���п�
	 * 5��������ͣ��λ���п� 6��������IC��λ���п� 7��������˳ֿ�λ���п� 8��������˲��ֿ�λ���п�
	 * @return
	 * @throws Exception
	 */
	public abstract int getCardReaderRuntimeStatus()throws Exception;

	
	//��������д
	/**
	 * ��ȡ�ſ��ŵ���Ϣ
	 * @param track1 �Ƿ��1�ŵ�
	 * @param track2 �Ƿ�����ŵ�
	 * @param track3 �Ƿ�����ŵ�
	 * @return ���شŵ���Ϣ����,��Ҫ���ϳ�3���ŵ���û�д��ڵĴŵ�����Ϊ���ַ���
	 * @throws Exception
	 */
	public abstract String[] readTrackDatas(boolean track1,boolean track2,boolean track3) throws Exception;
	


	//�ǽӴ���,��Ƶ��,mifare��
	/**
	 * Ѱ��
	 * @throws Exception Ѱ�����ɹ��׳�
	 */
	public abstract void findRFCard()throws Exception;
	
	
	/**
	 * ȡ�ǽӴ����Ŀ����к�
	 * @return ���ΪHEX��ʽ����
	 * @throws Exception
	 */
	public abstract String getRFCardSerialNo()throws Exception;
	

	/**
	 * ��֤KEY-A
	 * @param secNo ������
	 * @param password ����
	 * @throws Exception
	 */
	public abstract void authRFCardKeyA(int secNo,byte[] password)throws Exception;
	/**
	 * ��֤KEY-B
	 * @param secNo ������
	 * @param password ����
	 * @throws Exception
	 */
	public abstract void authRFCardKeyB(int secNo,byte[] password)throws Exception;
	
	/**
	 * ������������
	 * @param secNo
	 * @param blockNo
	 * @return
	 * @throws Exception
	 */
	public abstract String readRFCardData(int secNo,int blockNo)throws Exception;
	
	/**
	 * д����������
	 * @param secNo
	 * @param blockNo
	 * @param data
	 * @throws Exception
	 */
	public abstract void writeRFCardData(int secNo,int blockNo,byte[] data)throws Exception;
	
	/**
	 * �ԷǽӴ���������ֵ����
	 * @param secNo
	 * @param blockNo
	 * @param value
	 * @throws Exception
	 */
	public abstract String addRFCardValue(int secNo,int blockNo,int value)throws Exception;
	/**
	 * �ԷǽӴ������м�ֵ����
	 * @param secNo
	 * @param blockNo
	 * @param value
	 * @throws Exception
	 */
	public abstract String decreaseRFCardValue(int secNo,int blockNo,int value)throws Exception;

	//��CPU���Ķ�д����ͨCPU���ܿ�
	
	public abstract byte[] openICCard() throws Exception;
	
	/**
	 * CPU ���µ�
	 * @throws Exception
	 */
	public abstract void powerDownICCard()throws Exception;
	
	/**
	 * �Կ�Ƭ����apdu����
	 * @param data apdu����,���ΪSAM��,���һ���ֽ�Ϊ������
	 * @throws Exception
	 */
	public abstract byte[] apdu(byte[] data)throws Exception;
	
	//��SIM/SAM���Ķ�д,�û���Ȩ��
	
	/**
	 * ��SIM�������ϵ�ͳ�ʼ��
	 * @param socket ������
	 * @return ���صĿ�������ʽ�������Ķ�SIM���������븽��������ؽ��
	 * @throws Exception
	 */
	public abstract int powerUpSamCard(int socket)throws Exception;
	
	/**
	 * ��SIM�������µ����
	 * @throws Exception
	 */
	public abstract void powerDownSamCard()throws Exception;
	
}
