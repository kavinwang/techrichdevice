package com.techrich.client.device;

import com.techrich.client.manager.AbstractCommDevice;

public abstract class DoorControl extends AbstractCommDevice {
	public DoorControl(){
		this.deviceId = "DoorControl";
	}

	public abstract void openDoor() throws Exception;
	public abstract boolean checkMainSupply() throws Exception;
	public abstract int getRemainPower() throws Exception;
	//是否启动红外开门
	public abstract void openInfrared(boolean activation) throws Exception;
}
