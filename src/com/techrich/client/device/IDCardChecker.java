package com.techrich.client.device;

import java.util.HashMap;
import java.util.Map;

import com.techrich.client.manager.AbstractCommDevice;

public abstract class IDCardChecker extends AbstractCommDevice {

	protected static HashMap<String,String> NATIONS = new HashMap<String,String>(){{
		put("01","����");
		put("02","�ɹ���");
		put("03","����");
		put("04","����");
		put("05","ά�����");
		put("06","����");
		put("07","����");
		put("08","׳��");
		put("09","������");
		put("10","������");
		put("11","����");
		put("12","����");
		put("13","����");
		put("14","����");
		put("15","������");
		put("16","������");
		put("17","��������");
		put("18","����");
		put("19","����");
		put("20","������");
		put("21","����");
		put("22","���");
		put("23","��ɽ��");
		put("24","������");
		put("25","ˮ��");
		put("26","������");
		put("27","������");
		put("28","������");
		put("29","�¶����� ");
		put("30","����");
		put("31","���Ӷ���");
		put("32","������");
		put("33","Ǽ��");
		put("34","������");
		put("35","������");
		put("36","ë����");
		put("37","������");
		put("38","������");
		put("39","������");
		put("40","������");
		put("41","��������");
		put("42","ŭ��");
		put("43","���α��");
		put("44","����˹��");
		put("45","���¿���");
		put("46","������");
		put("47","������");
		put("48","ԣ����");
		put("49","����");
		put("50","��������");
		put("51","������");
		put("52","���״���");
		put("53","������");
		put("54","�Ű���");
		put("55","�����");
		put("56","��ŵ��");
		put("57","���� ");
		put("58","���Ѫͳ�й�����ʿ");
	}};
	/**
	 * ��������������
	 * @throws Exception
	 */
	public abstract Map<String, String> read() throws Exception;

	public IDCardChecker(){
		this.deviceId = "IdCardChecker";
	}
}
