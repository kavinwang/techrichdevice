package com.techrich.client.device;

import java.util.HashMap;

import org.json.JSONObject;

import com.techrich.client.manager.AbstractCommDevice;
import com.techrich.tools.Tools;




/**
 * Ʊ�����ӡ��
 * @author Wyi
 *
 */
public abstract class RecieptPrinter extends AbstractCommDevice {
	protected HashMap<String,String> templates = new HashMap<String,String>();
	
	/**
	 * ͨ�������Ľű����ṩ�����ݽ��д�ӡ
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
//				throw new Exception("ģ�壺"+templateName+" �Ѿ����ڣ���");
//			}
//			templates.put(templateName, script);
//		}
//	}
//	
//	public final boolean hasTemplate(String templateName){
//		return templates.containsKey(templateName);
//	}


	/**
	 * ֱ�ӷ��ʹ�ӡ��������������ʹ��HEX��ʽ���У��ڲ���ת���������ĸ�ʽ
	 * @param command
	 * @return
	 */
	public String perfomDirectCommand(String command){
		byte[] data = Tools.hex2bytes(command);
		return new String(data);
	}

}
