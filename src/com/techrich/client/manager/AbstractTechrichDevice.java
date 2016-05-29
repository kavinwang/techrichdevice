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
	 * ���豸�Ƿ����״̬��⹦��
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
	 * �����豸��������������comm1 lpt1�� �豸Ӳ����Ϣ������˿ڣ�����port pci����ŵ�
	 * �˷���ֻ���豸�������Զ����ʱ��Ч
	 * @param desc
	 */
	public abstract void setDeviceDesc(String desc);
	public abstract String getDeviceDesc();

	/**
	 * �豸�豸��Ӳ������
	 * @param params Ӳ����������ʵ���ж���������б����ת����ʵ��
	 * @throws Exception ������������ڻ������ô��������������
	 */
	public abstract void setDeviceParams(HashMap<String,String> params)throws Exception;

	/**
	 * ���豸������Ѿ��򿪣��˷��������κβ���
	 * @throws Exception
	 */
	protected abstract void openDevice()throws Exception;
	 /**
	 * �ر��豸�����û�д򿪣������κβ���
	 */
	protected abstract void closeDevice();
	/**
	 * ����豸�Ƿ��Ѿ���
	 * @return
	 */
	public abstract boolean isDeviceOpened();
	
	/**
	 * �豸�豸��Ӧ�ò���
	 * @param params Ӳ����������ʵ���ж���������б����ת����ʵ��
	 * @throws Exception ������������ڻ������ô��������������
	 */
	public void setDeviceAppParams(HashMap<String,String> params)throws Exception{
		appParams.putAll(params);
	}
	
	/**
	 * ����������Ӧ�ò����󣬵��ô˷��������豸�ĳ�ʼ��
	 * ��ʼ��������
	 * 	
	 * 	1�����һ����ʹ�����õ�Ӧ�����ݶ��豸���г�ʼ��
	 *  2��ȡ���豸�ĵ�ǰ״̬�����ã����߻���)
	 * @throws Exception �豸��ƥ��ʱ�׳�
	 * @throws DeviceInitException �豸��ʼ��ʧ��ʱ�׳�
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
	 * ����豸״̬
	 */
	public final void checkDeviceStatus(){
		statusInfo = new StringBuffer();
		errorCode=new byte[]{'0','0','0','0'};
		try {
			deviceCheck();
			if(new String(errorCode).equals("0000"))statusInfo.append("����");
		} catch (Exception e) {
			errorCode=new byte[]{'1','0','0','0'};
			statusInfo = new StringBuffer();
			statusInfo.append("����豸״̬����"+e.getMessage());
		}
	}
	/**
	 * �����豸״̬��鹤��
	 * @throws Exception
	 */
	public abstract void deviceCheck()throws Exception;


}
