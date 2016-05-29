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
		
	public static final float UNIT_DOT_MM=0.125f;//�㵥λ��һ����ӡ��Ϊ0.125����
	public static final float DEFAULT_LINE_SPACE=3.75f;//Ĭ���м�࣬30�㼴3.75mm
	public static final int ALIGN_LEFT=0;//�����
	public static final int ALIGN_CENTER=1;//����
	public static final int ALIGN_RIGHT=2;//�Ҷ���

	Hashtable<String,String> status = new Hashtable<String,String>();
	VelocityEngine ve =  new VelocityEngine();
	public YanKeRecieptPrinter(){
		this.deviceId = "RECIEPT-PRINTER-YK";
	}
	
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"�����п��վݴ�ӡ��";
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
//				LogManager.logInfo(Activator.PLUGIN_ID, "��ʼ����ӡ��ͼƬ");
//				ByteBuffer bb = new ByteBuffer(new byte[]{FS,'q',1});//ֻ����һ�ţ�����ж��ŵĻ�����Ҫ������
//				bb.append(ImagePixelUtils.getNVImageData(YanKeRecieptPrinter.class.getResourceAsStream("/images/powerrcode.bmp")));
//				sendDirect(bb.getValue());
//				Thread.sleep(3000);//��ѽ��
//				LogManager.logInfo(Activator.PLUGIN_ID, "��ʼ����ӡ��ͼƬ��ɣ�");				
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
			if((data[0]&0x90)!=0)throw new Exception("�����վݴ�ӡ��");//���صĵ���λ,����λһ����0��

			//ȡ��ӡ������
			byte[] sendOut = new byte[]{GS,'I','1'};
			sendDirect(sendOut);
			data = recieveData(1, 3000);
			if(data[0]!=(byte)0x87)throw new Exception("�����пƴ�ӡ��");
			
			//ȡ��ӡ���汾
			sendOut = new byte[]{GS,'I','3'};
			sendDirect(sendOut);
			data = recieveData(1, 3000);
			return new String(Tools.bytes2hex(data));//��ǰΪ00
		} catch (Exception e) {
			throw new Exception(e);
		}
	}
	
	public void loadImage(String[] images)throws Exception{
		if(images==null||images.length ==0)return;
		int imageCount = images.length;
		
		ByteBuffer bb = new ByteBuffer(new byte[]{0x1C,'q',(byte)imageCount});
		
		for(int i = 0;i< imageCount;i++){
			System.out.println( "����ͼƬ��"+images[i]);
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
				bb.append(ImagePixelUtils.getNVImageData(in,scale));//�Ŵ�8��
			}
		}
		System.out.println( "ͼƬ��������"+bb.length());
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
				
//				LogManager.logInfo("��Ҫ��ӡ�����ݣ�"+sw.toString());
				sendDirect(sw.toString().getBytes());
				LogManager.logInfo("��Ҫ��ӡ�����ݷ�����ɣ��ȴ���ӡ���...");

		}

		try{
			sendDirect(new byte[]{GS, 'r', '1'});
			recieveData(1, 10000);
			LogManager.logInfo("��ӡ����Ѿ���ɣ�");
		}catch(Exception e){
			LogManager.logError("�ȴ���ӡ���ʧ�ܣ�",e);
		}

	}

	
	/**
	 * ��ӡ�����У����ڵ�һ���ַ�λ��
	 * @return
	 */
	public String feedLine(){
		return new String(new byte[]{LF});
	}
	/**
	 * ��ӡ����ֽ 
	 * @param n ����
	 * @return
	 */
	public String printFeed(int n){
		return new String(new byte[]{ESC,'J',(byte)(n*8)});
	}
	
	/**
	 * ��ӡ����ֽ 
	 * @param n ��
	 * @return
	 */
	public String printFeedLine(int n){
		return new String(new byte[]{ESC,'d',(byte)n});
	}
	
	/**
	 * ���þ���λ�ã����п�ʼλ������
	 * @param x ����
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
	 * ��ֽ���ڱ괦
	 * @return
	 * @throws Exception
	 */
	public String formfeedToBM() throws Exception{
		return new String(new byte[]{GS,FF});
	}
	/**
	 * ���ô�ӡģʽ 
	 * @param normal true ���� false ����
	 * @bold �Ƿ�Ӵ�
	 * @ul �»���
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
	 * ��ֽ
	 * @return
	 * @throws Exception
	 */
	public String cutPaper() throws Exception {
		return new String(new byte[]{GS,'V',66,'1'});		
	}

	/**
	 * ���ú���ģʽ
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
	 * ȡ������ģʽ
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
	 * ���ö��뷽ʽ
	 * @param align 0 ��0�� ����� 1 '1' ���� 2 '2' �Ҷ���
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
			addDeviceErrorInfo("�ѻ�");
			setErrorLevel(4);
		}
		//----�ѻ�״̬----
		if((status[1]&0x04)>0){
			addDeviceErrorInfo("�ǰ��Ѵ�");
			setErrorLevel(4);
		}
		if((status[1]&0x08)>0){
			addDeviceErrorInfo("ͨ����ֽ����ֽ");
			setErrorLevel(3);

		}
		if((status[1]&0x20)>0){
			addDeviceErrorInfo("��ӡֽ����,ֹͣ��ӡ");
			setErrorLevel(4);
		}
		//----����״̬----
		if((status[2]&0x04)>0){
			addDeviceErrorInfo("������е����");
			setErrorLevel(4);
		}
		if((status[2]&0x08)>0){
			addDeviceErrorInfo("�����Զ���ֽ����");
			setErrorLevel(3);
		}
		if((status[2]&0x80)>0){
			addDeviceErrorInfo("���ֿ�ֽ����");
			setErrorLevel(4);
		}
		//----������ֽ������״̬----
		if((status[3]&0x0C)>0){
			addDeviceErrorInfo("ֽ����");
			setErrorLevel(1);
		}
		if((status[3]&0x60)>0){
			addDeviceErrorInfo("ȱֽ");
			setErrorLevel(4);
		}
	}
}
