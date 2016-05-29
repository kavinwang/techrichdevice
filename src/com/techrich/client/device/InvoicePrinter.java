package com.techrich.client.device;

import java.util.HashMap;

import org.json.JSONObject;

import com.techrich.client.manager.AbstractCommDevice;



public abstract class InvoicePrinter extends AbstractCommDevice {
	protected HashMap<String,String> templates = new HashMap<String,String>();

	/**
	 * ͨ�������Ľű����ṩ�����ݽ��д�ӡ,���û���ṩcontext��ֱ�Ӵ�ӡscript���ݳ���
	 * @param context
	 * @param templateNameOrContent
	 * @throws Exception
	 */
	public abstract void printScript(HashMap<String,String>context,String templateNameOrContent) throws Exception;
	
	public void printScript(JSONObject context,String templateNameOrContent) throws Exception{
		HashMap<String,String> cc = new HashMap<String,String>();
		for(String key:JSONObject.getNames(context)) cc.put(key, context.get(key).toString());
		
		printScript(cc,templateNameOrContent);
	}
	
	public InvoicePrinter(){
		this.deviceId = "InvoicePrinter";
	}
	
//	public final void registerTemplate(String templateName,String script) throws Exception{
//		if(templateName!= null && script!=null&&!templateName.trim().equals("")&&!script.trim().equals("")){
//			if(templates.containsKey(templateName)){
//				throw new Exception("ģ�壺"+templateName+" �Ѿ����ڣ���");
//			}
//			templates.put(templateName, script);
//		}
//	}
//	public final boolean hasTemplate(String templateName){
//		return templates.containsKey(templateName);
//	}
}
