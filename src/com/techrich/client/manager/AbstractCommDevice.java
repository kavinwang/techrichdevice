package com.techrich.client.manager;

import java.util.HashMap;

import com.techrich.client.device.SerialController;

/**
 * ȱʡ�Ĵ������豸��
 * ��ʼ�����ߴ򿪵Ĺ��̣�
 * 		����ʵ��
 * 		����Ӳ������
 * 		���豸
 * 		�����������
 * 		���豸���г�ʼ��,��ȷ���Ƿ�ƥ��
 * 		���ƥ��������������ȷ���豸���Ͳ�����ע��
 * 		�����
 * @author kavinwang
 *
 */
public abstract class AbstractCommDevice extends AbstractTechrichDevice {
	public static final byte STX=0x02;
	public static final byte ETX=0x03;
	public static final byte ENQ=0x05;
	public static final byte ACK=0x06;
	public static final byte NAK=0x15;
	public static final byte EOT=0x04;
	public static final byte CAN=0x18;

	public static final String S_BAUD_RATE="BaudRate";
	public static final String S_DATABITS="DataBits";
	public static final String S_STOPBITS="StopBits";
	public static final String S_PARITY="Parity";
	
	private SerialController serialCtrl;
	private SerialController[] serialCtrls;
	
	private String[] ports=null;
	
	private boolean multiPorts = false;
	
	public void setDeviceDesc(String desc){
		ports = desc.split(",");
		if(ports.length==0)throw new RuntimeException("�豸������������һ���˿�");
		if(ports.length == 1){//������ε���
			System.out.println("-----------------���˿�ϵͳ--------------------");
			this.serialCtrl = new SerialController();
		}else{
			System.out.println("-----------------��˿�ϵͳ--------------------");
			this.serialCtrls = new SerialController[ports.length];
			
			for(int i = 0;i< ports.length;i++)this.serialCtrls[i] = new SerialController();
			multiPorts = true;
		}
	}
	
	public final String[] getPorts(){
		return ports;
	}
	
	public final void setDeviceParams(HashMap<String,String> params)throws Exception{
		SerialController[] p = serialCtrls;
		if(!multiPorts)p = new SerialController[]{this.serialCtrl};
		
		
		for(int i = 0; i< p.length;i++){
			SerialController pp = p[i];
			
			if(!params.containsKey(S_BAUD_RATE))throw new Exception("û�����ò�����");
//			else{
				String portInfo = ports[i];//����˿������˲����ʣ���ʹ��
				String[] portParams = portInfo.split(":");
				
				int bandRate = Integer.parseInt(params.get(S_BAUD_RATE));
				if(portParams.length>1)bandRate = Integer.parseInt(portParams[1]);
				System.out.println("���ò�����Ϊ��"+bandRate);
				pp.setComSpeed(bandRate);
//			}
			//�����Ҫ������Ĳ������Բ������������������ȵķ�ʽ����
			if(!params.containsKey(S_DATABITS))throw new Exception("û����������λ");
			else{
				System.out.println("��������λΪ��"+Integer.parseInt(params.get(S_DATABITS)));
				pp.setDataBit(Integer.parseInt(params.get(S_DATABITS)));
			}

			if(!params.containsKey(S_PARITY))throw new Exception("û��������żУ��λ");
			else{
				System.out.println("����У��λΪ��"+Integer.parseInt(params.get(S_PARITY)));
				pp.setParity(Integer.parseInt(params.get(S_PARITY)));
			}

			if(!params.containsKey(S_STOPBITS))throw new Exception("û������ֹͣλ");
			else{
				System.out.println("����ֹͣλΪ��"+Integer.parseInt(params.get(S_STOPBITS)));
				pp.setStopBit(Integer.parseInt(params.get(S_STOPBITS)));
			}
			
		}
		
	}
	
	public String getDeviceDesc(){
		if(ports.length>1)return ports[0]+","+ports[1];
		else return ports[0];
	}

	public void openDevice()throws Exception{
		System.out.println("���豸!");
		if(!isDeviceOpened()){
			
			if(multiPorts){
				for(int i = 0;i< serialCtrls.length;i++){
					if(serialCtrls[i]!=null){
						String[] portInfo = ports[i].split(":");
						serialCtrls[i].OpenPort(portInfo[0]);
					}
				}
			}else{
				String[] portInfo = ports[0].split(":");
				System.out.println("�򿪶˿�:"+portInfo[0]);
				serialCtrl.OpenPort(portInfo[0]);
			}
			
		}else{
			System.out.println("�豸�Ѿ���!");
		}
	}
	
	 /**
	 * �ر��豸�����û�д򿪣������κβ���
	 */
	public void closeDevice(){//�ⲿ���� 3��һ ��Ϊpublic
		if(isDeviceOpened()){
			if(multiPorts){
				for(int i = 0;i< serialCtrls.length;i++){
					if(serialCtrls[i]!=null)
						serialCtrls[i].closePort();
				}
			}else{
				serialCtrl.closePort();
			}
		}
	}
	
	protected void changePort(int port){
		if(!multiPorts)return;//ֻ��һ���˿ڣ��Ͳ��л���
		if(port >= 0 && port < serialCtrls.length)serialCtrl = serialCtrls[port];
	}
	
	public final boolean isDeviceOpened() {
		if(multiPorts){
			for(int i = 0;i< serialCtrls.length;i++) if(!serialCtrls[i].isOpened())return false;
			return true;
		}else{
			return serialCtrl.isOpened();
		}
	}
	/**
	 * ֱ�ӷ����ṩ������
	 * @param content
	 */
	public void sendDirect(String content)throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		serialCtrl.sendDirect(content.getBytes());
	}
	public void sendDirect(byte[] content)throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		serialCtrl.sendDirect(content);
	}
	
	public void sendCheck(String content)throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		serialCtrl.sendCheck(content.getBytes());
	}
	public void sendCheck(byte[] content)throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		serialCtrl.sendCheck(content);
	}
	public boolean hasDataInBuffer()throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		return serialCtrl.hasDataInBuffer();
	}
	/**
	 * ����ָ�����ȵ�����
	 * @param length
	 * @param timeOut
	 * @return
	 * @throws Exception
	 */
	public byte[] recieveData(int length,int timeOut)throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
			return serialCtrl.recieve(length,timeOut);
	}
	public byte[] recieveData(int length)throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		return serialCtrl.recieve(length,2000);
	}
	/**
	 * ������ָ���ַ���β�����ݰ�
	 * @param endCodes
	 * @param timeOut
	 * @return
	 * @throws Exception
	 */
	public byte[] recieveData(byte[]endCodes,int maxCheck,int timeOut)throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		return serialCtrl.recieve(endCodes, maxCheck,timeOut);
	}
	/**
	 * ����ָ���ַ���ʼ��ָ���ַ����������ݰ�
	 * @param startCode
	 * @param endCodes
	 * @param timeOut
	 * @return
	 * @throws Exception
	 */
	public byte[] recieveData(byte startCode,byte[]endCodes,int maxCheck,int timeOut)throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		return serialCtrl.recieve(startCode, endCodes, maxCheck, timeOut);
	}
	public void clearBuffer()throws Exception{
		if(serialCtrl == null)throw new Exception("��˿��豸��Ҫѡ��˿ڲ���ͨѶ");
		serialCtrl.clearReadBuffer();
	}
	
	public void addDeviceErrorInfo(String info){
		if(statusInfo.length()>0)statusInfo.append("~");
		statusInfo.append(info);
	}
	
	protected final void setErrorLevel(int level){
		if(level==0)errorCode=new byte[]{'0','0','0','0'};
		else{
			if(level>4)return;
			errorCode[4-level]='1';
		}
	}
}
