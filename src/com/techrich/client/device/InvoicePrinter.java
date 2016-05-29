package com.techrich.client.device;

import java.util.HashMap;

import org.json.JSONObject;

import com.techrich.client.manager.AbstractCommDevice;



public abstract class InvoicePrinter extends AbstractCommDevice {
	protected HashMap<String,String> templates = new HashMap<String,String>();

	/**
	 * 通过给定的脚本和提供的数据进行打印,如果没有提供context则直接打印script内容出来
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
//				throw new Exception("模板："+templateName+" 已经存在！！");
//			}
//			templates.put(templateName, script);
//		}
//	}
//	public final boolean hasTemplate(String templateName){
//		return templates.containsKey(templateName);
//	}
}
