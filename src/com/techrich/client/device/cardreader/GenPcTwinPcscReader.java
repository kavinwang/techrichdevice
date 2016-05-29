package com.techrich.client.device.cardreader;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.IntegratedCardReader;
import com.techrich.tools.Tools;

public class GenPcTwinPcscReader extends IntegratedCardReader {
	CardTerminal terminal = null;
	CardChannel cardChannel = null;
	private Integer sw;
	private String name;
	private byte[] lastResponse;

	public GenPcTwinPcscReader(){
		this.deviceId = "CARDREADER-GEMPLUS-PCSC";
	}
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"Gemplus PCSC 读卡器";
	}
	
	@Override
	public void openDevice(){
		
	}
	public void initDevice() throws DeviceInitException {
		System.setProperty("sun.security.smartcardio.t0GetResponse", "false");
		try{
			TerminalFactory factory = TerminalFactory.getDefault();
			for(CardTerminal t : factory.terminals().list()){
				System.out.println("发现PC/SC读写器：" + t.getName());
				if(t.getName().indexOf("Gemplus") >= 0){
					terminal = t;
					break;
				}
			}
			if((terminal == null) && (!factory.terminals().list().isEmpty())){
				terminal = ((CardTerminal)factory.terminals().list().get(0));
				System.out.println("找不着GEMPLUS，随便用一个其他的代替" + terminal.getName());
			}
			
			if(terminal == null){ throw new Exception("没有GEMPLUS读卡器"); }
		}catch(Exception e){
			throw new DeviceInitException(e);
		}
		canReadMemoryCard = true;
		canReadCPUCard = true;
		canReadSimCard=true;
	}
	
	public byte[] apdu(byte[] apdu)throws Exception{
		System.out.println("[APDU CMD]" + Tools.bytes2hex(apdu));
		ResponseAPDU r_apdu = cardChannel.transmit(new CommandAPDU(apdu));
		System.out.println("[APDU RSP]" + Tools.bytes2hex(r_apdu.getBytes()));
		int sw = Integer.valueOf(r_apdu.getSW());
		String errorCode = Integer.toHexString(sw).toUpperCase();
		if(errorCode.startsWith("61")){
			int len = Integer.parseInt(errorCode.substring(2), 16);
			if(len == 0)return new byte[]{};
			return apdu(new byte[]{0x00, (byte)0xC0, 0x00, 0x00, (byte)len});
		}else if(errorCode.startsWith("6C")){
			int len = Integer.parseInt(errorCode.substring(2), 16);
			apdu[apdu.length-1] = (byte)len;
			return apdu(apdu);
		}else if(sw == 0x6E00 || sw == 0x6700){
			throw new Exception("返回6E00或者6700啦,需要考虑init中的设置问题了!");
    }else{
			if(sw != 0x9000) throw new Exception("APDU错误:"+errorCode); 
			r_apdu.getBytes();
			return r_apdu.getData();
		}
		
	}

	@Override
	public void powerDownICCard() throws Exception {
	}
	@Override
	public int powerUpSamCard(int socket) throws Exception {
		Card card = this.terminal.connect("T=0");
		this.cardChannel = card.getBasicChannel();
		this.lastResponse = null;
		byte[] res = this.cardChannel.getCard().getATR().getBytes();
		return res.length > 0? res[0]:0;
	}
	@Override
	public void powerDownSamCard() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] openICCard() throws Exception{
		Card card = this.terminal.connect("T=0");
		this.cardChannel = card.getBasicChannel();
		this.lastResponse = null;
		return this.cardChannel.getCard().getATR().getBytes();
	}

	@Override
	public void deviceCheck() throws Exception {
		
	}
	@Override
	public void openDoor(int type, int location) throws Exception {
		
	}
	@Override
	public void closeDoor() throws Exception {
		
	}
	@Override
	public void moveCard(int loc) throws Exception {
		
	}
	@Override
	public int getCardReaderRuntimeStatus() throws Exception {
		return 0;
	}
	@Override
	public String[] readTrackDatas(boolean track1, boolean track2, boolean track3) throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}
	@Override
	public void findRFCard() throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}
	@Override
	public String getRFCardSerialNo() throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}
	@Override
	public void authRFCardKeyA(int secNo, byte[] password) throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}
	@Override
	public void authRFCardKeyB(int secNo, byte[] password) throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}
	@Override
	public String readRFCardData(int secNo, int blockNo) throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}
	@Override
	public void writeRFCardData(int secNo, int blockNo, byte[] data) throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}
	@Override
	public String addRFCardValue(int secNo, int blockNo, int value) throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}
	@Override
	public String decreaseRFCardValue(int secNo, int blockNo, int value) throws Exception {
		throw new RuntimeException("本读卡器不支持此操作!");
	}

}
