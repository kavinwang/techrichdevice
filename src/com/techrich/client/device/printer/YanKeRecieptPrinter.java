package com.techrich.client.device.printer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.RecieptPrinter;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.ImagePixelUtils;
import com.techrich.tools.Tools;


public class YanKeRecieptPrinter extends RecieptPrinter {
	private static final byte LF=0x0A;
	private static final byte FF=0x0C;
	private static final byte DLE=0x10;
	private static final byte ESC=0x1B;
	private static final byte FS=0x1C;
	private static final byte GS=0x1D;
		
	public static final float UNIT_DOT_MM=0.125f;//点单位，一个打印点为0.125毫米
	public static final float DEFAULT_LINE_SPACE=3.75f;//默认行间距，30点即3.75mm
	public static final int ALIGN_LEFT=0;//左对齐
	public static final int ALIGN_CENTER=1;//居中
	public static final int ALIGN_RIGHT=2;//右对齐

	Hashtable<String,String> status = new Hashtable<String,String>();
	VelocityEngine ve =  new VelocityEngine();
	public YanKeRecieptPrinter(){
		this.deviceId = "RECIEPT-PRINTER-YK";
	}
	
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"深圳研科收据打印机";
	}
	@Override
	public boolean canCheckDeviceStatus(){
		return true;
	}

	@Override
	public void initDevice() throws DeviceInitException {
		try {
			sendDirect(new byte[]{ESC,'@'});
			
//			Boolean initPrintLogo = Boolean.parseBoolean(ConfigManager.getDefault().getConfigElementDef("imagelogo.init", "false"));
//			
//			if(initPrintLogo){
//				Thread.sleep(3000);
//				LogManager.logInfo(Activator.PLUGIN_ID, "初始化打印机图片");
//				ByteBuffer bb = new ByteBuffer(new byte[]{FS,'q',1});//只加载一张，如果有多张的话，需要改数量
//				bb.append(ImagePixelUtils.getNVImageData(YanKeRecieptPrinter.class.getResourceAsStream("/images/powerrcode.bmp")));
//				sendDirect(bb.getValue());
//				Thread.sleep(3000);//慢呀！
//				LogManager.logInfo(Activator.PLUGIN_ID, "初始化打印机图片完成！");				
//			}
			
			ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this);
			ve.init();
		} catch (Exception e) {
			throw new DeviceInitException(e);
		}
	}
	
	@Override
	public void setDeviceAppParams(HashMap<String, String> params)throws Exception {
		
	}

	@Override
	public String getDeviceIdentifer() throws Exception {
		try {
			clearBuffer();
			sendDirect(new byte[]{GS,'r',1});
			byte[] data = recieveData(1, 3000);
			if((data[0]&0x90)!=0)throw new Exception("不是收据打印机");//返回的第四位,第七位一定是0；

			//取打印机名称
			byte[] sendOut = new byte[]{GS,'I','1'};
			sendDirect(sendOut);
			data = recieveData(1, 3000);
			if(data[0]!=(byte)0x87)throw new Exception("不是研科打印机");
			
			//取打印机版本
			sendOut = new byte[]{GS,'I','3'};
			sendDirect(sendOut);
			data = recieveData(1, 3000);
			return new String(Tools.bytes2hex(data));//当前为00
		} catch (Exception e) {
			throw new Exception(e);
		}
	}
	
	public void loadImage(String[] images)throws Exception{
		if(images==null||images.length ==0)return;
		int imageCount = images.length;
		
		ByteBuffer bb = new ByteBuffer(new byte[]{0x1C,'q',(byte)imageCount});
		
		for(int i = 0;i< imageCount;i++){
			System.out.println( "加载图片："+images[i]);
			int scale = 1;
			String[] segs = images[i].split("~");
			if(segs.length>1){
				scale = Integer.parseInt(segs[1]);
			}
			String file = segs[0];
			System.out.println(file);

			if(file.startsWith("http://")){
				URL url = new URL(file);
				HttpURLConnection conn =  (HttpURLConnection)url.openConnection();
				int httpResult = conn.getResponseCode(); 
				if(httpResult == HttpURLConnection.HTTP_OK) {
					InputStream in = conn.getInputStream();
					bb.append(ImagePixelUtils.getNVImageData(in,scale));
				}
			}else{
				InputStream in = new FileInputStream(file);
				bb.append(ImagePixelUtils.getNVImageData(in,scale));//放大8倍
			}
		}
		System.out.println( "图片总数量："+bb.length());
		sendDirect(bb.getValue());
	}
	
	@Override
	public void printScript(HashMap<String, String> context, String printScript) throws Exception {
		if(context == null || context.size() == 0) sendDirect(printScript);
		else{

				VelocityContext vc = new VelocityContext(context);
				vc.put("printer", this);
				StringWriter sw = new StringWriter();
				
				String[] scripts = StringUtils.split(printScript, "\n");
				StringBuffer sb = new StringBuffer();
				for(String s:scripts){
					if(s.startsWith("##"))continue;
					sb.append(s);
				}
				
				ve.evaluate(vc, sw, "recieptPrinter", sb.toString());
				
//				LogManager.logInfo("将要打印的内容："+sw.toString());
				sendDirect(sw.toString().getBytes());
				LogManager.logInfo("将要打印的内容发送完成！等待打印完成...");

		}

		try{
			sendDirect(new byte[]{GS, 'r', '1'});
			recieveData(1, 10000);
			LogManager.logInfo("打印完成已经完成！");
		}catch(Exception e){
			LogManager.logError("等待打印完成失败：",e);
		}

	}

	
	/**
	 * 打印并换行，放在第一个字符位置
	 * @return
	 */
	public String feedLine(){
		return new String(new byte[]{LF});
	}
	/**
	 * 打印并进纸 
	 * @param n 毫米
	 * @return
	 */
	public String printFeed(int n){
		return new String(new byte[]{ESC,'J',(byte)(n*8)});
	}
	
	/**
	 * 打印并进纸 
	 * @param n 行
	 * @return
	 */
	public String printFeedLine(int n){
		return new String(new byte[]{ESC,'d',(byte)n});
	}
	
	/**
	 * 设置绝对位置，从行开始位置算起
	 * @param x 毫米
	 * @return
	 * @throws Exception
	 */
	public String setAbsolutePos(int dist) throws Exception{
		int v = dist*8;
		byte x = (byte)(v&0x00ff);
		byte y = (byte)((v&0xff00)>>8);
		return new String(new byte[]{ESC,'$',x,y});
	}

	
	/**
	 * 进纸到黑标处
	 * @return
	 * @throws Exception
	 */
	public String formfeedToBM() throws Exception{
		return new String(new byte[]{GS,FF});
	}
	/**
	 * 设置打印模式 
	 * @param normal true 正常 false 反白
	 * @bold 是否加粗
	 * @ul 下划线
	 * @return
	 */
	public String setPrintMode(boolean bold,boolean dw,boolean dh,boolean ul){
		byte mode = 0x00;
		if(bold)mode|=0x08;
		if(dh)mode|=0x10;
		if(dw)mode|=0x20;
		if(ul)mode|=0x80;
		return new String(new byte[]{ESC,'!',mode});
	}
	
	
	public String printLogo(int which){
		return new String(new byte[]{FS,'p',(byte)which,0x00});
	}
	
	/**
	 * 切纸
	 * @return
	 * @throws Exception
	 */
	public String cutPaper() throws Exception {
		return new String(new byte[]{GS,'V',66,'1'});		
	}

	/**
	 * 设置汉字模式
	 * @param dW
	 * @param dH
	 * @param underLine
	 * @return
	 * @throws Exception
	 */
	public String setChineseMode(boolean dW,boolean dH) throws Exception{
		byte data = 0x00;
		if(dW)data|=0x04;
		if(dH)data|=0x08;
		return new String(new byte[]{FS,'&',FS,'!',data});
	}

	/**
	 * 取消汉字模式
	 * @return
	 * @throws Exception
	 */	
	public String setEnglishMode(boolean dW,boolean dH) throws Exception{
		byte mode = 0x00;
		if(dW)mode|=1<<5;	//32
		if(dH)mode|=1<<4;	//16
		return new String(new byte[]{FS,'.',ESC,'!',mode});
	}
	public byte getPrinterStatus(int stateType) throws Exception{
		sendDirect(new byte[]{DLE,EOT,(byte)stateType});
		return recieveData(1, 3000)[0];
	}
	/**
	 * 设置对齐方式
	 * @param align 0 ‘0’ 左对齐 1 '1' 居中 2 '2' 右对齐
	 * @return
	 * @throws Exception
	 */
	public String setAlign(int align) throws Exception{
		return new String(new byte[]{ESC,'a',(byte)align});
	}


	@Override
	public void deviceCheck()throws Exception{
		sendDirect(new byte[]{DLE,EOT,1,DLE,EOT,2,DLE,EOT,3,DLE,EOT,4});
		byte[] status = recieveData(4, 3000);
		if((status[0]&0x08) > 0){
			addDeviceErrorInfo("脱机");
			setErrorLevel(4);
		}
		//----脱机状态----
		if((status[1]&0x04)>0){
			addDeviceErrorInfo("盖板已打开");
			setErrorLevel(4);
		}
		if((status[1]&0x08)>0){
			addDeviceErrorInfo("通过进纸键进纸");
			setErrorLevel(3);

		}
		if((status[1]&0x20)>0){
			addDeviceErrorInfo("打印纸用完,停止打印");
			setErrorLevel(4);
		}
		//----错误状态----
		if((status[2]&0x04)>0){
			addDeviceErrorInfo("发生机械错误");
			setErrorLevel(4);
		}
		if((status[2]&0x08)>0){
			addDeviceErrorInfo("发生自动切纸错误");
			setErrorLevel(3);
		}
		if((status[2]&0x80)>0){
			addDeviceErrorInfo("出现卡纸错误");
			setErrorLevel(4);
		}
		//----连续用纸传感器状态----
		if((status[3]&0x0C)>0){
			addDeviceErrorInfo("纸将尽");
			setErrorLevel(1);
		}
		if((status[3]&0x60)>0){
			addDeviceErrorInfo("缺纸");
			setErrorLevel(4);
		}
	}
}
