package com.techrich.client.device;

import java.util.HashMap;

import com.techrich.client.manager.AbstractCommDevice;

/**
 * @author kavinwang
 *
 */
public abstract class SecureKeypad extends AbstractCommDevice {
	protected static final HashMap<Byte, String> errorMap = new HashMap<Byte, String>();
	static{
		errorMap.put((byte)0x15, "���������");
		errorMap.put((byte)0x80, "��ʱ����");
		errorMap.put((byte)0xa4, "�������ִ��,������Կ��Ч");
		errorMap.put((byte)0xb5, "������Ч,������Կ��Ч");
		errorMap.put((byte)0xc4, "�������ִ��,����ؿ�����");
		errorMap.put((byte)0xd5, "������Ч,�ҵ�ؿ�����");
		errorMap.put((byte)0xe0, "��Ч����");
		errorMap.put((byte)0xf0, "�Լ����:CPU��");
		errorMap.put((byte)0xf1, "�Լ����:SRAM��");
		errorMap.put((byte)0xf2, "�Լ����:�����ж�·��");
		errorMap.put((byte)0xf3, "�Լ����:���ڵ�ƽ��");
		errorMap.put((byte)0xf4, "�Լ����:CPU������");
		errorMap.put((byte)0xf5, "�Լ����:��ؿ�����");
		errorMap.put((byte)0xf6, "�Լ����:����ԿʧЧ");
		errorMap.put((byte)0xf7, "�Լ����:�����");
		errorMap.put((byte)0x21, "�Զ�����󣬹���û���ṩSAM��");
	}
	
	public SecureKeypad(){
		this.deviceId = "SecureKeypad";
	}
	public boolean tdesEnabled(){
		return true;
	}
	/**
	 * ��װ������Կ
	 * @param keyValue
	 * @throws Exception
	 */
	public abstract void loadWorkKey( byte[] pinKey, byte[]macKey) throws Exception;
	/**
	 * ��������ģʽ��׼��������������ģʽ��ɺ��Զ���Ϊ��ͨ����ģʽ
	 * ����������볤��С��ָ�������볤��ʱ���谴��ȷ�ϡ�������ɣ����򵱴ﵽָ������ʱ�Զ����
	 * ��ʱ���ش���
	 * @param passwdLen ��Ҫ�����볤�ȣ�
	 * @param timeOut  ��ʱʱ�䣨�룩
	 * @throws Exception
	 */
	public abstract void startPinMode(int passwdLen,String trackData,int timeOut) throws Exception;
	/**
	 * ��ָ����ʱ���ڷ��ذ�����ֵ�����������Ӧ�ó����п���ѭ����������������г���
	 * @return
	 * @throws Exception
	 */
	public abstract byte getPressKey(int time) throws Exception;
	/**
	 * ȡ�ü��ܺ��PIN
	 * ȡ�ô������ݣ���֯���û�������ܵ�PAD1 ���ڲ����ݽṹȡ��������Ϣ������2�����3��
	 * �����½ӿڸĳ���ȡ2�ŵ������2�ŵ�ȡ������ȡ3�ŵ�
	 *String asTrack;
	 *int ilen = track2.length();
     *   if(ilen < 13) {
     *       ilen = track3.length();
     *      if(ilen < 13)throw new Exception("Not Track Data");
     *       asTrack = track3;
     *   }else{
     *   	asTrack = track2;
      *  }
	 * @return
	 * @throws Exception
	 */
	public abstract byte[] getPin(String track) throws Exception;
	

	public abstract byte[] getMac(byte[] macdata) throws Exception ;
	
	public abstract void closeKeypad()throws Exception;
	public abstract String getPosInfo() throws Exception ;
}
