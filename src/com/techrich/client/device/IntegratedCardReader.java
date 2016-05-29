package com.techrich.client.device;

import com.techrich.client.manager.AbstractCommDevice;

/**
 * 在实现类中必须设置允许读卡的类型，把相关的类型设置为true，不能读的卡设置为false
 */
public abstract class IntegratedCardReader extends AbstractCommDevice {
	protected static final int CARD_TYPE_NOCARD = -1;
	protected static final int CARD_TYPE_MAGNET = 0;
	protected static final int CARD_TYPE_ICCARD = 1;
	protected static final int CARD_TYPE_SAMCARD = 2;
	protected static final int CARD_TYPE_RFCARD = 3;
	
	protected boolean canReadMagnetCard=false;//磁条卡
	protected boolean canReadRFCard=false;//非接触卡,射频卡,mifare卡
	protected boolean canReadMemoryCard = false;//接触卡,包括普通的memory卡,IC卡
	protected boolean canReadCPUCard=false;//对CPU卡的读写，普通CPU功能卡
	protected boolean canReadSimCard=false;//对SIM卡的读写,用户鉴权卡
	
	// 暂时没有提供API
	protected boolean canReadSamCard=false;//对SAM卡的读写,安全加密卡
	
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
	 * 打开读卡器门，允许插卡和读卡
	 * @param type 0:磁卡方式 1：开关方式，支持所有类型的卡 2：磁信号方式，支持薄卡
	 * @param location 持卡位置 0：停在前端不持卡，1：停在前端持卡 2：RF卡位置 3：IC卡位置 4：停在后端持卡 5：停在后端不持卡
	 * @throws Exception
	 */
	public abstract void openDoor(int type,int location) throws Exception;

	/**
	 * 关闭读卡器门，不允许插卡和读卡
	 * @throws Exception
	 */
	public abstract void closeDoor() throws Exception;
	
	/**
	 * 设置并走卡到指定位置
	 * @param loc 走卡的位置 0： 走到前端不持卡 1：走到前端持卡 2：走到后端持卡 
	 * 						3：走到后端不持卡 4向后弹出卡 5：中持卡 6：走到卡机内IC卡位
	 * @throws Exception
	 */
	public abstract void moveCard(int loc) throws Exception;
	
	/**
	 * 取卡机的当前卡状态
	 * 0：卡机内无卡 
	 * 1：卡机内有长卡 2：卡机内有短卡 3：卡机前端不持卡位置有空 4：卡机前端持卡位置有卡
	 * 5：卡机内停卡位置有卡 6：卡机内IC卡位置有空 7：卡机后端持卡位置有卡 8：卡机后端不持卡位置有卡
	 * @return
	 * @throws Exception
	 */
	public abstract int getCardReaderRuntimeStatus()throws Exception;

	
	//磁条卡读写
	/**
	 * 读取磁卡磁道信息
	 * @param track1 是否读1磁道
	 * @param track2 是否读二磁道
	 * @param track3 是否读三磁道
	 * @return 返回磁道信息数组,需要整合成3个磁道，没有存在的磁道内容为空字符串
	 * @throws Exception
	 */
	public abstract String[] readTrackDatas(boolean track1,boolean track2,boolean track3) throws Exception;
	


	//非接触卡,射频卡,mifare卡
	/**
	 * 寻卡
	 * @throws Exception 寻卡不成功抛出
	 */
	public abstract void findRFCard()throws Exception;
	
	
	/**
	 * 取非接触卡的卡序列号
	 * @return 结果为HEX方式返回
	 * @throws Exception
	 */
	public abstract String getRFCardSerialNo()throws Exception;
	

	/**
	 * 验证KEY-A
	 * @param secNo 扇区号
	 * @param password 密码
	 * @throws Exception
	 */
	public abstract void authRFCardKeyA(int secNo,byte[] password)throws Exception;
	/**
	 * 验证KEY-B
	 * @param secNo 扇区号
	 * @param password 密码
	 * @throws Exception
	 */
	public abstract void authRFCardKeyB(int secNo,byte[] password)throws Exception;
	
	/**
	 * 读扇区块数据
	 * @param secNo
	 * @param blockNo
	 * @return
	 * @throws Exception
	 */
	public abstract String readRFCardData(int secNo,int blockNo)throws Exception;
	
	/**
	 * 写扇区块数据
	 * @param secNo
	 * @param blockNo
	 * @param data
	 * @throws Exception
	 */
	public abstract void writeRFCardData(int secNo,int blockNo,byte[] data)throws Exception;
	
	/**
	 * 对非接触卡进行增值操作
	 * @param secNo
	 * @param blockNo
	 * @param value
	 * @throws Exception
	 */
	public abstract String addRFCardValue(int secNo,int blockNo,int value)throws Exception;
	/**
	 * 对非接触卡进行减值操作
	 * @param secNo
	 * @param blockNo
	 * @param value
	 * @throws Exception
	 */
	public abstract String decreaseRFCardValue(int secNo,int blockNo,int value)throws Exception;

	//对CPU卡的读写，普通CPU功能卡
	
	public abstract byte[] openICCard() throws Exception;
	
	/**
	 * CPU 卡下电
	 * @throws Exception
	 */
	public abstract void powerDownICCard()throws Exception;
	
	/**
	 * 对卡片进行apdu操作
	 * @param data apdu数据,如果为SAM卡,则第一个字节为卡座号
	 * @throws Exception
	 */
	public abstract byte[] apdu(byte[] data)throws Exception;
	
	//对SIM/SAM卡的读写,用户鉴权卡
	
	/**
	 * 对SIM卡进行上电和初始化
	 * @param socket 卡座号
	 * @return 返回的卡操作方式，后续的对SIM卡操作必须附带这个返回结果
	 * @throws Exception
	 */
	public abstract int powerUpSamCard(int socket)throws Exception;
	
	/**
	 * 对SIM卡进行下电操作
	 * @throws Exception
	 */
	public abstract void powerDownSamCard()throws Exception;
	
}
