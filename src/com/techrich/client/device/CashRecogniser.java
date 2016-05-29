package com.techrich.client.device;

import com.techrich.client.manager.AbstractCommDevice;

public abstract class CashRecogniser extends AbstractCommDevice {
	protected final static int[] notesArray = new int[] { 1, 2, 5, 10, 20, 50, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	public CashRecogniser(){
		this.deviceId = "CashRecogniser";
	}
	
	@Override
	public boolean canCheckDeviceStatus(){
		return true;
	}
	
	protected static int getCashNoteValue(int index) {
		return notesArray[index];
	}
	public abstract void startNoteCheck(int[] cashTypes, int maxAcceptMoney, int timeOut,NoteRecogniseListener listener) throws Exception;
	public abstract void stopNoteCheck() throws Exception;


	public abstract String getRejectMessage(byte code);
	
	public interface NoteRecogniseListener{

		public void cashStacked(int currCashValue, int totalInsertCash);

		public void recogniseWarn(String message);

		public void statusChanged(String statusMessage);

		public void cheatedCash();

		public void recogniseExpireTime(long l);

		public void recogniseError(String message);

		public void recogniseFinished(int totalInsertCash);
		
	}
}
