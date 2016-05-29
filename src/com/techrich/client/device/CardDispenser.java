/**
 * 
 * @author kavinwang
 * @created Oct 30, 2007 9:43:46 AM
 */
package com.techrich.client.device;

import com.techrich.client.manager.AbstractCommDevice;


/**
 *
 */
public abstract class CardDispenser extends AbstractCommDevice {
	public CardDispenser(){
		this.deviceId = "CardDispenser";
	}
	
	/**
	 * ��λ������
	 */
	public abstract void reset(int address)throws Exception;
	
	/**
	 * ����Ԥ����״̬
	 */
	public abstract void setPreDispenserCard(int address,boolean enabled)throws Exception;
		
	/**
	 * ���������ص�ǰ�����ĵ�ַ��
	 * @return
	 */
	public abstract int dispenserCard(int address)throws Exception;
	/**
	 * ������ֵ������,�˷����Զ����������dispenserCard����
	 * @param address
	 * @return
	 * @throws Exception
	 */
	public abstract int dispenserCardByValue(int address)throws Exception;
	
	
	//��⿨���Ƿ����
	public abstract boolean isCardTrackExits(int address,boolean checkDeep);

	/**
	 * ��װ�ṩ����ֵ����Ƿ��ܹ�����
	 * @param value
	 * @return
	 */
	public abstract boolean checkTrackByValue(int value) ;
}
