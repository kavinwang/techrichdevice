package com.techrich.client.device;

import com.techrich.client.manager.AbstractCommDevice;

public abstract class QueueCallerScreen extends AbstractCommDevice {

	public QueueCallerScreen(){
		this.deviceId = "QueueCallerScreen";
	}
	
	public abstract void welcome(int counterNum);

	public abstract void freeCounter(int counterNum,String ticketNum);

	public abstract void service(int counterNum, String num);

	public abstract void pauseCounter(int counterNum);

	public abstract void callNum(int counterNum, String num);


}
