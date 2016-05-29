package com.techrich.client.manager;

import java.util.HashMap;

import com.techrich.client.device.DeviceInitException;

/**
 * @author kavinwang
 */
public abstract class AbstractTechrichDevice {
	protected StringBuffer statusInfo = new StringBuffer();
	protected byte[] errorCode=new byte[]{'0','0','0','0'};
	protected HashMap<String, String> appParams = new HashMap<String,String>();
	
	protected String deviceId = "unknown";
	protected String deviceName=null;
	
	public AbstractTechrichDevice(){
	}
	
	public String getDeviceId(){
		return deviceId;
	}
	
	/**
	 * 本设备是否具有状态检测功能
	 * @return
	 */
	public boolean canCheckDeviceStatus(){
		return false;
	}
	
	public abstract String getDeviceName();
	public void setDeviceName(String name){
		this.deviceName = name;
	}
	/**
	 * 设置设备的描述符，比如comm1 lpt1等 设备硬件信息，比如端口，插板的port pci的序号等
	 * 此方法只在设备不进行自动检测时生效
	 * @param desc
	 */
	public abstract void setDeviceDesc(String desc);
	public abstract String getDeviceDesc();

	/**
	 * 设备设备的硬件参数
	 * @param params 硬件参数，在实现中对这个参数列表进行转换和实现
	 * @throws Exception 如果参数不存在或者设置错误或者其它错误
	 */
	public abstract void setDeviceParams(HashMap<String,String> params)throws Exception;

	/**
	 * 打开设备，如果已经打开，此方法不做任何操作
	 * @throws Exception
	 */
	protected abstract void openDevice()throws Exception;
	 /**
	 * 关闭设备，如果没有打开，则不做任何操作
	 */
	protected abstract void closeDevice();
	/**
	 * 检查设备是否已经打开
	 * @return
	 */
	public abstract boolean isDeviceOpened();
	
	/**
	 * 设备设备的应用参数
	 * @param params 硬件参数，在实现中对这个参数列表进行转换和实现
	 * @throws Exception 如果参数不存在或者设置错误或者其它错误
	 */
	public void setDeviceAppParams(HashMap<String,String> params)throws Exception{
		appParams.putAll(params);
	}
	
	/**
	 * 当被设置了应用参数后，调用此方法进行设备的初始化
	 * 初始化包括：
	 * 	
	 * 	1，如果一致则使用设置的应用数据对设备进行初始化
	 *  2，取得设备的当前状态并设置（或者缓存)
	 * @throws Exception 设备不匹配时抛出
	 * @throws DeviceInitException 设备初始化失败时抛出
	 */
	public abstract void initDevice()throws DeviceInitException;
	public String getDeviceIdentifer()throws Exception{
		return "TECHRICH_DEVICE";
	}
	
	public final String getErrorCode(){
		return new String(errorCode);
	}
	public final String getErrorInfo(){
		return statusInfo.toString();
	}
	
	/**
	 * 检查设备状态
	 */
	public final void checkDeviceStatus(){
		statusInfo = new StringBuffer();
		errorCode=new byte[]{'0','0','0','0'};
		try {
			deviceCheck();
			if(new String(errorCode).equals("0000"))statusInfo.append("正常");
		} catch (Exception e) {
			errorCode=new byte[]{'1','0','0','0'};
			statusInfo = new StringBuffer();
			statusInfo.append("检查设备状态错误："+e.getMessage());
		}
	}
	/**
	 * 进行设备状态检查工作
	 * @throws Exception
	 */
	public abstract void deviceCheck()throws Exception;


}
