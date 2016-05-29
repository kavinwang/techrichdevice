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
		errorMap.put((byte)0x15, "命令参数错");
		errorMap.put((byte)0x80, "超时错误");
		errorMap.put((byte)0xa4, "命令可以执行,但主密钥无效");
		errorMap.put((byte)0xb5, "命令无效,且主密钥无效");
		errorMap.put((byte)0xc4, "命令可以执行,但电池可能损坏");
		errorMap.put((byte)0xd5, "命令无效,且电池可能损坏");
		errorMap.put((byte)0xe0, "无效命令");
		errorMap.put((byte)0xf0, "自检出错:CPU错");
		errorMap.put((byte)0xf1, "自检出错:SRAM错");
		errorMap.put((byte)0xf2, "自检出错:键盘有短路错");
		errorMap.put((byte)0xf3, "自检出错:串口电平错");
		errorMap.put((byte)0xf4, "自检出错:CPU卡出错");
		errorMap.put((byte)0xf5, "自检出错:电池可能损坏");
		errorMap.put((byte)0xf6, "自检出错:主密钥失效");
		errorMap.put((byte)0xf7, "自检出错:杂项错");
		errorMap.put((byte)0x21, "自定义错误，估计没有提供SAM卡");
	}
	
	public SecureKeypad(){
		this.deviceId = "SecureKeypad";
	}
	public boolean tdesEnabled(){
		return true;
	}
	/**
	 * 下装工作密钥
	 * @param keyValue
	 * @throws Exception
	 */
	public abstract void loadWorkKey( byte[] pinKey, byte[]macKey) throws Exception;
	/**
	 * 启动加密模式，准备按键，当加密模式完成后自动变为普通按键模式
	 * 当输入的密码长度小于指定的密码长度时，需按“确认”进行完成，否则当达到指定长度时自动完成
	 * 超时返回错误
	 * @param passwdLen 想要的密码长度，
	 * @param timeOut  超时时间（秒）
	 * @throws Exception
	 */
	public abstract void startPinMode(int passwdLen,String trackData,int timeOut) throws Exception;
	/**
	 * 在指定的时间内返回按键键值，否则出错，在应用程序中可以循环调用这个方法进行尝试
	 * @return
	 * @throws Exception
	 */
	public abstract byte getPressKey(int time) throws Exception;
	/**
	 * 取得加密后的PIN
	 * 取得磁条数据，组织成用户密码加密的PAD1 从内部数据结构取出磁条信息（磁条2或磁条3）
	 * 银联新接口改成先取2磁道，如果2磁道取不到再取3磁道
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
