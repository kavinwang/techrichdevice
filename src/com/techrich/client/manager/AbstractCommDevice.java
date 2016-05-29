package com.techrich.client.manager;

import java.util.HashMap;

import com.techrich.client.device.SerialController;

/**
 * 缺省的串口类设备，
 * 初始化或者打开的过程：
 * 		生成实例
 * 		设置硬件参数
 * 		打开设备
 * 		设置软件参数
 * 		对设备进行初始化,并确定是否匹配
 * 		如果匹配则根据类的类型确定设备类型并进行注册
 * 		打开完成
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
		if(ports.length==0)throw new RuntimeException("设备必须设置至少一个端口");
		if(ports.length == 1){//防备多次调用
			System.out.println("-----------------单端口系统--------------------");
			this.serialCtrl = new SerialController();
		}else{
			System.out.println("-----------------多端口系统--------------------");
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
			
			if(!params.containsKey(S_BAUD_RATE))throw new Exception("没有设置波特率");
//			else{
				String portInfo = ports[i];//如果端口配置了波特率，则使用
				String[] portParams = portInfo.split(":");
				
				int bandRate = Integer.parseInt(params.get(S_BAUD_RATE));
				if(portParams.length>1)bandRate = Integer.parseInt(portParams[1]);
				System.out.println("设置波特率为："+bandRate);
				pp.setComSpeed(bandRate);
//			}
			//如果需要，下面的参数可以参照上面来以配置优先的方式处理
			if(!params.containsKey(S_DATABITS))throw new Exception("没有设置数据位");
			else{
				System.out.println("设置数据位为："+Integer.parseInt(params.get(S_DATABITS)));
				pp.setDataBit(Integer.parseInt(params.get(S_DATABITS)));
			}

			if(!params.containsKey(S_PARITY))throw new Exception("没有设置奇偶校验位");
			else{
				System.out.println("设置校验位为："+Integer.parseInt(params.get(S_PARITY)));
				pp.setParity(Integer.parseInt(params.get(S_PARITY)));
			}

			if(!params.containsKey(S_STOPBITS))throw new Exception("没有设置停止位");
			else{
				System.out.println("设置停止位为："+Integer.parseInt(params.get(S_STOPBITS)));
				pp.setStopBit(Integer.parseInt(params.get(S_STOPBITS)));
			}
			
		}
		
	}
	
	public String getDeviceDesc(){
		if(ports.length>1)return ports[0]+","+ports[1];
		else return ports[0];
	}

	public void openDevice()throws Exception{
		System.out.println("打开设备!");
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
				System.out.println("打开端口:"+portInfo[0]);
				serialCtrl.OpenPort(portInfo[0]);
			}
			
		}else{
			System.out.println("设备已经打开!");
		}
	}
	
	 /**
	 * 关闭设备，如果没有打开，则不做任何操作
	 */
	public void closeDevice(){//外部调用 3合一 改为public
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
		if(!multiPorts)return;//只有一个端口，就不切换了
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
	 * 直接发送提供的内容
	 * @param content
	 */
	public void sendDirect(String content)throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
		serialCtrl.sendDirect(content.getBytes());
	}
	public void sendDirect(byte[] content)throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
		serialCtrl.sendDirect(content);
	}
	
	public void sendCheck(String content)throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
		serialCtrl.sendCheck(content.getBytes());
	}
	public void sendCheck(byte[] content)throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
		serialCtrl.sendCheck(content);
	}
	public boolean hasDataInBuffer()throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
		return serialCtrl.hasDataInBuffer();
	}
	/**
	 * 接收指定长度的数据
	 * @param length
	 * @param timeOut
	 * @return
	 * @throws Exception
	 */
	public byte[] recieveData(int length,int timeOut)throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
			return serialCtrl.recieve(length,timeOut);
	}
	public byte[] recieveData(int length)throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
		return serialCtrl.recieve(length,2000);
	}
	/**
	 * 接收以指定字符结尾的数据包
	 * @param endCodes
	 * @param timeOut
	 * @return
	 * @throws Exception
	 */
	public byte[] recieveData(byte[]endCodes,int maxCheck,int timeOut)throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
		return serialCtrl.recieve(endCodes, maxCheck,timeOut);
	}
	/**
	 * 接收指定字符开始到指定字符结束的数据包
	 * @param startCode
	 * @param endCodes
	 * @param timeOut
	 * @return
	 * @throws Exception
	 */
	public byte[] recieveData(byte startCode,byte[]endCodes,int maxCheck,int timeOut)throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
		return serialCtrl.recieve(startCode, endCodes, maxCheck, timeOut);
	}
	public void clearBuffer()throws Exception{
		if(serialCtrl == null)throw new Exception("多端口设备需要选择端口才能通讯");
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
