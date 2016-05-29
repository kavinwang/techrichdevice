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
	 * 复位发卡机
	 */
	public abstract void reset(int address)throws Exception;
	
	/**
	 * 设置预发卡状态
	 */
	public abstract void setPreDispenserCard(int address,boolean enabled)throws Exception;
		
	/**
	 * 发卡，返回当前操作的地址号
	 * @return
	 */
	public abstract int dispenserCard(int address)throws Exception;
	/**
	 * 根据面值来发卡,此方法自动调用上面的dispenserCard方法
	 * @param address
	 * @return
	 * @throws Exception
	 */
	public abstract int dispenserCardByValue(int address)throws Exception;
	
	
	//检测卡道是否存在
	public abstract boolean isCardTrackExits(int address,boolean checkDeep);

	/**
	 * 安装提供的面值检查是否能够发卡
	 * @param value
	 * @return
	 */
	public abstract boolean checkTrackByValue(int value) ;
}
