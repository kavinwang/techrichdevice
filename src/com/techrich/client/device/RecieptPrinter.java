package com.techrich.client.device;

import java.util.HashMap;

import org.json.JSONObject;

import com.techrich.client.manager.AbstractCommDevice;
import com.techrich.tools.Tools;




/**
 * 票据类打印机
 * @author Wyi
 *
 */
public abstract class RecieptPrinter extends AbstractCommDevice {
	protected HashMap<String,String> templates = new HashMap<String,String>();
	
	/**
	 * 通过给定的脚本和提供的数据进行打印
	 * @param content
	 * @throws Exception
	 */
	public abstract void printScript(HashMap<String,String>context,String content) throws Exception;
	
	public void printScript(JSONObject context,String templateNameOrContent) throws Exception{
		HashMap<String,String> cc = new HashMap<String,String>();
		
		
		for(String key:JSONObject.getNames(context)) cc.put(key, context.get(key).toString());
		
		printScript(cc,templateNameOrContent);
	}

	public RecieptPrinter(){
		this.deviceId = "RecieptPrinter";
	}
	
//	public final void registerTemplate(String templateName,String script) throws Exception{
//		if(templateName!= null && script!=null&&!templateName.trim().equals("")&&!script.trim().equals("")){
//			if(templates.containsKey(templateName)){
//				throw new Exception("模板："+templateName+" 已经存在！！");
//			}
//			templates.put(templateName, script);
//		}
//	}
//	
//	public final boolean hasTemplate(String templateName){
//		return templates.containsKey(templateName);
//	}


	/**
	 * 直接发送打印机命令，此命令必须使用HEX方式进行，内部会转换成正常的格式
	 * @param command
	 * @return
	 */
	public String perfomDirectCommand(String command){
		byte[] data = Tools.hex2bytes(command);
		return new String(data);
	}

}
