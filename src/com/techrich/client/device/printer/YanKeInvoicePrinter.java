package com.techrich.client.device.printer;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.techrich.client.Activator;
import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.InvoicePrinter;
import com.techrich.client.manager.LogManager;

public class YanKeInvoicePrinter extends InvoicePrinter {

	public static final byte LF = 0x0A;
	public static final byte DLE = 0x10;
	public static final byte ESC = 0x1B;
	public static final byte FS = 0x1C;
	public static final byte GS = 0x1D;

	public static final float UNIT_DOT_MM = 0.125f;//点单位，一个打印点为0.125毫米
	public static final float DEFAULT_LINE_SPACE = 3.75f;//默认行间距，30点即3.75mm
	public static final int ALIGN_LEFT = 0;//左对齐
	public static final int ALIGN_CENTER = 1;//居中
	public static final int ALIGN_RIGHT = 2;//右对齐
	public static final double DEFAULT_CUT_LEN = 5;//距离黑标中心 mm毫米
	Hashtable<String, String> status = new Hashtable<String, String>();
	VelocityEngine ve = new VelocityEngine();
	double cutLen = DEFAULT_CUT_LEN;
	boolean halfCut = true;

	public YanKeInvoicePrinter() {
		this.deviceId = "INVOICE-PRINTER-YK";
	}
	
	public String getDeviceName() {
		return deviceName!=null?deviceName:"深圳研科发票打印机";
	}
	
	@Override
	public boolean canCheckDeviceStatus() {
		return true;
	}

	@Override
	public void initDevice() throws DeviceInitException {
		try{
			sendDirect(new byte[]{ESC, '@'});

			//			sendDirect(new byte[]{ESC,'N',1,1});//带切刀 0：没有切刀
			//			sendDirect(new byte[]{ESC,'N',2,0});//没有收纸器  1：有收纸器
			sendDirect(new byte[]{ESC, 'N', 3, halfCut ? (byte)1 : 0});
			//			sendDirect(new byte[]{ESC,'N',4,0});//每行40字  1：每行42字
			//			sendDirect(new byte[]{ESC,'N',5,0});//9600  1：19200
			sendDirect(new byte[]{ESC, 'N', 6, 0});//双向打印  1：单向打印
			sendDirect(new byte[]{ESC, 'N', 7, 1});//选择黑标模式  0:非黑标模式
			sendDirect(new byte[]{ESC, 'N', 8, 1});//选择汉字模式  0：ascii模式
			//			sendDirect(new byte[]{ESC,'N',9,1});//纸宽76mm  0：纸宽58mm 

			double cutLen = DEFAULT_CUT_LEN;
			try{
				byte forward = cutLen > 0 ? (byte)0 : (byte)1; //大于0 向前算 小于0向后算
				cutLen = Math.abs(cutLen);

				//				(nL+nHx256)*0.176mm  固有撕纸线位置在黑标后54mm处
				int num = (int)((cutLen + 54) / 0.176);
				int nL = num % 256;
				int nH = num / 256;
				sendDirect(new byte[]{GS, '(', 'F', 4, 0, 2, forward, (byte)nL, (byte)nH});//90,1

			}catch(Exception e){
			}

			ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this);
			ve.init();
		}catch(Exception e){
			throw new DeviceInitException(e);
		}
	}

	@Override
	public String getDeviceIdentifer() throws Exception {
		try{
			sendDirect(new byte[]{DLE, EOT, 1, DLE, EOT, 2, DLE, EOT, 3, DLE, EOT, 4});
			byte[] data = recieveData(4, 3000);
			for(int i = 0; i < 4; i++)
				if((data[i] & 0x12) != 0x12) throw new Exception("不是研科打印机");
			clearBuffer();
			sendDirect(new byte[]{GS, 'I', '1'});
			data = recieveData(1, 3000);
			return "YankeInvoicePrinter";
		}catch(Exception e){
			throw new Exception(e);
		}

	}

	public void setDeviceAppParams(HashMap<String, String> params) throws Exception {
		super.setDeviceAppParams(params);
		try{
			String p = params.get("InvoiceCutPaperPosition");
			if(p != null) cutLen = Double.parseDouble(p);
		}catch(Exception e){
		}
		halfCut = Boolean.parseBoolean(params.get("InvoiceHalfCut"));
	}

	@Override
	public void printScript(HashMap<String, String> context, String printScript) throws Exception {
		if(context == null || context.size() == 0) sendDirect(printScript);//没有环境数据就直接打印
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
			
			ve.evaluate(vc, sw, "invoicePrinter", sb.toString());
			sendDirect(sw.toString().getBytes());
		}

		try{
			sendDirect(new byte[]{GS, 'r', '1'});
			recieveData(1, 10000);
		}catch(Exception e){
		}

	}

	public String feedLine() {
		return new String(new byte[]{LF});
	}

	public String setAbsolutePos(float mmPos) throws Exception {
		int r = Math.round(mmPos / UNIT_DOT_MM);
		byte l = (byte)(r % 256);
		byte h = (byte)(r / 256);
		return new String(new byte[]{ESC, '$', l, h});
	}

	public String setRelativePos(float mmPos) throws Exception {
		int r = Math.round(mmPos / UNIT_DOT_MM);
		byte l = (byte)(r % 256);
		byte h = (byte)(r / 256);
		return new String(new byte[]{ESC, '\\', l, h});
	}

	public String setEnglishMode() throws Exception {
		return new String(new byte[]{FS, '.'});
	}

	public String setChineseMode() throws Exception {
		return new String(new byte[]{FS, '&'});
	}

	public String setCharSpace(int mmSpace) throws Exception {
		return new String(new byte[]{ESC, 0x20, (byte)mmSpace});
	}

	public String setLineSpace(float mmSpace) throws Exception {
		byte space = (byte)Math.round(mmSpace / UNIT_DOT_MM);
		return new String(new byte[]{ESC, '3', space});
	}

	public String setChineseMode(boolean dW, boolean dH) throws Exception {
		byte data = 0x00;
		if(dW) data |= 0x04;
		if(dH) data |= 0x08;
		return new String(new byte[]{FS, '&', FS, '!', data});
	}

	public String setEnglishMode(boolean dW, boolean dH) throws Exception {
		byte mode = 0x00;
		if(dW) mode |= 1 << 5; //32
		if(dH) mode |= 1 << 4; //16
		return new String(new byte[]{FS, '.', ESC, '!', mode});
	}

	public String setPrintMode(boolean bold, boolean dw, boolean dh, boolean ul) {
		byte mode = 0x00;
		if(bold) mode |= 0x08;
		if(dh) mode |= 0x10;
		if(dw) mode |= 0x20;
		if(ul) mode |= 0x80;
		return new String(new byte[]{ESC, '!', mode});
	}

	public String setFontStyle(boolean doubleWidth, boolean doubleHeight, boolean bold, int underlineWeight,
			boolean gbChars) throws Exception {
		int styleBits = 0;
		StringBuffer sb = new StringBuffer();
		if(gbChars){
			if(doubleWidth) styleBits |= 1 << 2; //4
			if(doubleHeight) styleBits |= 1 << 3; //8
			if(underlineWeight != 0) styleBits |= 1 << 7; //128
			sb.append(new String(new byte[]{FS, '!', (byte)styleBits}));
			if(underlineWeight != 0) sb.append(new String(new byte[]{FS, '-', (byte)underlineWeight}));
			if(bold) sb.append(new String(new byte[]{ESC, 'E', 1}));
			else sb.append(new String(new byte[]{ESC, 'E', 0}));
		}else{
			if(bold) styleBits |= 1 << 3; //8
			if(doubleWidth) styleBits |= 1 << 5; //32
			if(doubleHeight) styleBits |= 1 << 4; //16
			if(underlineWeight != 0) styleBits |= 1 << 7; //128
			sb.append(new String(new byte[]{ESC, '!', (byte)styleBits}));
		}
		return sb.toString();
	}

	public String setAlign(int align) throws Exception {
		return new String(new byte[]{ESC, 'a', (byte)align});
	}

	public String setUnderLine(int dot) throws Exception {
		return new String(new byte[]{FS, '-', (byte)dot});
	}

	public String formfeedpoint(int point) throws Exception {
    return new String(new byte[]{ESC, 'J', (byte)point});
	}
	
	public String formfeed(float distMM) throws Exception {
		byte dd = (byte)Math.round(distMM / UNIT_DOT_MM);
		return new String(new byte[]{ESC, 'J', dd});
	}

	public String formfeed(int lineCount) throws Exception {
		return new String(new byte[]{ESC, 'd', (byte)lineCount});
	}

	public String formfeedToBM() throws Exception {
		return new String(new byte[]{GS, 0x0C});
	}

	public String setShadow(int n) throws Exception {
		return new String(new byte[]{ESC, 'G', (byte)n});
	}

	public String cutPaper() throws Exception {
		return new String(new byte[]{ESC,'N',3,0,GS,'V',66,1});		
//		return new String(new byte[]{GS, 'V', 66, 1});
	}

	//	//下面的方法可能没有什么用
		public String cutPaper(boolean fullCut) throws Exception {
			return new String(new byte[]{ESC,'N',3,fullCut?(byte)0:(byte)1,GS,'V',66,1});		
		}

	@Override
	public void deviceCheck() throws Exception {
		byte[] status = new byte[]{0x08, 0x00, 0x00, 0x00};
		for(int i = 0; i < 4; i++){
			try{
				sendDirect(new byte[]{DLE, EOT, (byte)(i + 1)});
				byte[] data = recieveData(1, 3000);
				status[i] = data[0];
			}catch(Exception e){
				LogManager.logError(Activator.PLUGIN_ID, "发票取打印机状态错误：DLE EOT " + (i + 1));
			}
		}

		if((status[0] & 0x08) > 0){
			addDeviceErrorInfo("脱机");
			setErrorLevel(4);
		}else{
			if((status[1] & 0x08) > 0){
				addDeviceErrorInfo("通过进纸键进纸");
				setErrorLevel(3);
			}
			if((status[1] & 0x20) > 0){
				addDeviceErrorInfo("打印纸用完,停止打印");
				setErrorLevel(4);
			}
			if((status[1] & 0x40) > 0){
				addDeviceErrorInfo("打印机错误");
				setErrorLevel(4);
			}
			//----错误状态----
			if((status[2] & 0x04) > 0){
				addDeviceErrorInfo("发生机械错误");
				setErrorLevel(4);
			}
			if((status[2] & 0x08) > 0){
				addDeviceErrorInfo("发生自动切纸错误");
				setErrorLevel(3);
			}
			if((status[2] & 0x20) > 0){
				addDeviceErrorInfo("不可恢复故障");
				setErrorLevel(3);
			}
			if((status[2] & 0x40) > 0){
				addDeviceErrorInfo("可恢复故障");
				setErrorLevel(1);
			}
			if((status[2] & 0x80) > 0){
				addDeviceErrorInfo("出现卡纸错误");
				setErrorLevel(4);
			}
			//----连续用纸传感器状态----
			if((status[3] & 0x0C) > 0){
				addDeviceErrorInfo("纸将尽");
				setErrorLevel(1);
			}
			if((status[3] & 0x60) > 0){
				addDeviceErrorInfo("缺纸");
				setErrorLevel(4);
			}
		}
	}

}
