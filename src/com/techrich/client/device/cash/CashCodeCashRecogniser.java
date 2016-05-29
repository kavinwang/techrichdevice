package com.techrich.client.device.cash;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import com.techrich.client.Activator;
import com.techrich.client.device.CashRecogniser;
import com.techrich.client.device.DeviceInitException;
import com.techrich.client.manager.ConfigManager;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;

public class CashCodeCashRecogniser extends CashRecogniser {
	private final static byte SYNC 											= 0x02;
	private final static byte RESET 											= 0x30;//�����豸
	private final static byte GETSTATUS 							= 0x31;//��ȡ����״̬
	private final static byte POLL 												= 0x33;
	private final static byte ENABLEBILLTYPE 		= 0x34;
	private final static byte STACK 											= 0x35;
	private final static byte IDENTIFY									= 0x37;


	private final static class MODULES {
		public final static byte NONE = 0x00;
		public final static byte BILL_VALIDATOR = 0x03;
	}

	private byte CURR_DEVICE = MODULES.BILL_VALIDATOR; //��ǰ���豸��ͨ��suite����ȡ�ò�����,ȱʡΪʶ����ģ��
	protected final static HashMap<Integer, Integer> BILL_TABLE = new HashMap<Integer, Integer>();

	private final static HashMap<Byte, String> cashRejectedErrors = new HashMap<Byte, String>();
	static{
		cashRejectedErrors.put((byte)0x60, "[1C]���뷽������ȷ");
		cashRejectedErrors.put((byte)0x61, "[1C]�ŵ���Ϣ������");
		cashRejectedErrors.put((byte)0x62, "[1C]ʶ��ͷ��Ҳ����ٴβ���Ǯ��");
		cashRejectedErrors.put((byte)0x63, "[1C]�������ӻ���Ӳ�������");
		cashRejectedErrors.put((byte)0x64, "[1C]����Ͷ���Ǯ�Ҵ���");
		cashRejectedErrors.put((byte)0x65, "[1C]����ʶ��Ͷ���Ǯ��");
		cashRejectedErrors.put((byte)0x66, "[1C]Ͷ���Ǯ�Ҳ���ͨ����֤");
		cashRejectedErrors.put((byte)0x67, "[1C]�������");
		cashRejectedErrors.put((byte)0x68, "[1C]�����ܵ�Ǯ����ֵ");
		cashRejectedErrors.put((byte)0x69, "[1C]����̫С���ܼ���Ͷ��");
		cashRejectedErrors.put((byte)0x6A, "[1C]������������ȷ");
		cashRejectedErrors.put((byte)0x6C, "[1C]Ǯ�ҳ��Ȳ���ȷ");
		cashRejectedErrors.put((byte)0x6D, "[1C]Ǯ�����Բ�����");
		cashRejectedErrors.put((byte)0x50, "[47]����������");
		cashRejectedErrors.put((byte)0x51, "[47]��������ٶ��쳣");
		cashRejectedErrors.put((byte)0x52, "[47]����������");
		cashRejectedErrors.put((byte)0x53, "[47]����������");
		cashRejectedErrors.put((byte)0x54, "[47]��ʼ��Ǯ��״̬����");
		cashRejectedErrors.put((byte)0x55, "[47]��̽ͷ����");
		cashRejectedErrors.put((byte)0x56, "[47]��̽ͷ����");
		cashRejectedErrors.put((byte)0x5F, "[47]����̽��ͷ����");
	}

	CashCheckingManager cashCheckThread = null;

	//�̼���Ϣ
	private String partNum;
	private String serialNum;
	private byte[] assetNum;
	private BitSet typeEnable = new BitSet(24);
	private BitSet securityEnable = new BitSet(24);

	private int[] acceptableNoteValues = new int[]{1, 2, 5, 10, 20, 50, 100};
	private boolean isCashAutoStack = false;

//	private boolean logCommDatas = false;
	
	private boolean hasStoped = true;

	public CashCodeCashRecogniser() {
		super();
	}

	@Override
	public void initDevice() throws DeviceInitException {
		for(int i = 0; i < notesArray.length; i++) if(notesArray[i] != 0) BILL_TABLE.put(notesArray[i], i);

		//��ģ��֧��ʶ�����������Ƿ��Ǯ�䣩��֧��Ӳ�������
		byte[] deviceTypes = new byte[]{/*MODULES.BILL_TO_BILL,*/ /* MODULES.COIN_CHANGER  , */MODULES.BILL_VALIDATOR/* ,MODULES . CARD_READER */}; 
		for(int i = 0; i < deviceTypes.length; i++){
			this.CURR_DEVICE = deviceTypes[i];
			try{
				sendCmd(RESET, null);
				break;
			}catch(Exception e){
				this.CURR_DEVICE = MODULES.NONE;
			}
		}
		if(CURR_DEVICE == MODULES.NONE) throw new DeviceInitException("û���ҵ�ʶ�����豸");
		
		try{Thread.sleep(2000);}catch(Exception e){}
		try{
			ByteBuffer data = new ByteBuffer(sendCmd(IDENTIFY, null));
			partNum = new String(data.getValueN(0, 15));
			serialNum = new String(data.getValueN(15, 12));
			assetNum = data.getValueN(27);
	
			LogManager.logInfo(Activator.PLUGIN_ID, "��⵽CashCode�豸��" + this.partNum + " ���кţ�" + serialNum + " �ʲ����:" + Tools.bytes2hex(assetNum));
	
			String[] parts = partNum.split("-");
			if(parts.length < 2) throw new Exception("����CASHCODE ʶ������");
			if(!parts[1].startsWith("CN")) throw new Exception("CASH CODE NOT FOR CHINA"); 

			//XXX ���ʶ�������˳������Ƿ���Ҫ���ӵ���ָ�����
			getSettings();
			setAcceptNoteValue(new int[]{}, false);//disable bill types
			poll();
		}catch(Exception e){
			throw new DeviceInitException("û���ҵ�ʶ�����豸",e); 
		}
			cashCheckThread = new CashCheckingManager();
			cashCheckThread.setDaemon(true);
			cashCheckThread.setName("WaitForCashThread");
			cashCheckThread.start();
	}

	@Override
	public String getRejectMessage(byte code) {
		return cashRejectedErrors.get(code);
	}

	@Override
	public void setDeviceAppParams(HashMap<String, String> params) throws Exception {
		super.setDeviceAppParams(params);;
	
		if(params.containsKey("AcceptCashType")){
			String valuestr = params.get("AcceptCashType").trim();
			String[] values = StringUtils.splitPreserveAllTokens(valuestr, ",");
			ArrayList<Integer> allCash = new ArrayList<Integer>();
			for(String value : values){
				if(value == null || value.trim().equals("")) continue;
				try{
					int v = Integer.parseInt(value);
					allCash.add(v);
				}catch(Exception e){
				}
			}
			acceptableNoteValues = new int[allCash.size()];
			for(int i = 0; i < allCash.size(); i++)
				acceptableNoteValues[i] = allCash.get(i);
		}
	}
	@Override
	public void deviceCheck() throws Exception {
	}

	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"CASHCODEʶ����";
	}
	
	private void setAcceptNoteValue(int[] values, boolean autoStack) throws Exception {
		if(values != null) acceptableNoteValues = values.clone();
		else{
			acceptableNoteValues = new int[]{};
			LogManager.logInfo(Activator.PLUGIN_ID, "û�����ÿ��Խ��ܵ�Ǯ�ҵ���ֵ");
		}
		byte cashType = 0x00;

		for(int k = 0; k < acceptableNoteValues.length; k++){
			byte temp = 0x00;
			for(int i = 0; i < 24; i++){
				if(notesArray[i] == acceptableNoteValues[k]){
					temp = (byte)(1 << i);
					break;
				}
			}
			cashType |= temp;
		}

		byte[] cmdData = new byte[]{0x00, 0x00, cashType, 0x00, 0x00, 0x00};
		if(!autoStack) cmdData[5] = cashType;
		sendCmd(ENABLEBILLTYPE, cmdData);
		Thread.sleep(150);
	}

	/**
	 * @throws Exception
	 */
	private void getSettings() throws Exception {
		byte[] data = sendCmd(GETSTATUS, null);

		int bits = ((data[0] & 0x0000ff) << 16) | ((data[1] & 0x0000ff) << 8) | data[2];
		for(int i = 0; i < 24; i++){//��֧��24�б��֣��ο�cashcode���ĵ�
			if((bits & (1 << i)) != 0) typeEnable.set(i);
			else typeEnable.clear(i);
		}

		bits = ((data[3] & 0x0000ff) << 16) | ((data[4] & 0x0000ff) << 8) | data[5];
		for(int i = 0; i < 24; i++){
			if((bits & (1 << i)) != 0) securityEnable.set(i);
			else securityEnable.clear(i);
		}
	}

	private byte[] poll() throws Exception {
		for(int i = 0; i < 5; i++){
			try{
				return sendCmd(POLL, null);
			}catch(Exception e){
				Thread.sleep(200);
			}
		}
		throw new Exception("ʶ�������󣺼���ʶ����ʧ�ܣ�ʶ�����޷�Ӧ��");
	}

	private byte[] sendCmd(byte cmd, byte[] data) throws Exception {
		ByteBuffer bb = new ByteBuffer();
		bb.append(SYNC).append(CURR_DEVICE);
		if(data != null) bb.append((byte)(data.length + 6));
		else bb.append((byte)6);
		bb.append(cmd);
		if(data != null) bb.append(data);
		byte[] crc = getCrc(bb.getValue());
		bb.append(crc);
		sendDirect(bb.getValue());

		// ��������
		bb = new ByteBuffer();
		bb.append(recieveData(3, 300));// SYNC,ADDR,LNG
		int len = (bb.getByteAt(2) & 0x00ff) - 3 - 2;
		if(len > 0) bb.append(recieveData(len, 100));// ���ݲ���

		crc = recieveData(2, 100);// crc����
		byte[] crcv = getCrc(bb.getValue());
		bb.append(crc);

		if(crcv[0] != crc[0] || crcv[1] != crc[1]){ throw new Exception("������֤ʧ�ܣ�"); }

		if(bb.getByteAt(2) == 0x06){
			byte errorCode = bb.getByteAt(3);
			if(errorCode == 0xff) throw new Exception("Negative Acknowledgment");	
			if(errorCode == 0x30) throw new Exception("Illegal command");
		}

		//��Ӧʶ�����ɹ���������
		ByteBuffer answer = new ByteBuffer(new byte[]{0x02, CURR_DEVICE, 0x06, 0x00});
		answer.append(getCrc(answer.getValue()));
		sendDirect(answer.getValue());

		return bb.getValueN(3, bb.length() - 5);
	}

	private static byte[] getCrc(byte[] datas) {
		byte[] crc = new byte[]{0x00, 0x00};
		int temp_crc = 0;
		for(int i = 0; i < datas.length; i++){
			temp_crc = (temp_crc & 0xff00) | (((temp_crc & 0x00ff) ^ datas[i]) & 0x00ff);
			for(int k = 0; k < 8; k++){
				if((temp_crc & 0x0001) != 0x00){
					temp_crc >>= 1;
					temp_crc ^= 0x08408;
				}else{
					temp_crc >>= 1;
				}
			}
		}
		crc[1] = (byte)((temp_crc & 0xff00) >> 8);
		crc[0] = (byte)(temp_crc & 0x00ff);
		return crc;
	}

	private Object checkerLocker = new Object();
	private Object checkerWaiter = new Object();
	private NoteRecogniseListener noteCheckListener;
	private int noteWaitingTimeOut = 66000;
	private int totalRecogValue = 0;
	private int currNoteValue = 0;
	private boolean polling = false;
	private boolean operating = false;
	private int totalCashCount = 0;

	private long startStopTime = 0;
	private long waitCloseTime = 0;
	private int maxAcceptMoney = 0;



	private boolean canAcceptCash(int cash) {
		if(this.acceptableNoteValues == null || this.acceptableNoteValues.length == 0) return false;
		for(int i = 0; i < this.acceptableNoteValues.length; i++){
			if(this.acceptableNoteValues[i] == cash) return true;
		}
		return false;
	}
	
	public int getTotalInsertMoney() {
		return totalRecogValue;
	}

	public int getTotalNotesCount() {
		return totalCashCount;
	}

	public boolean isWorking() {
		return polling;
	}

	private String getAcceptStr(int[] cashTypes) {
		if(cashTypes == null || cashTypes.length == 0) return "";
		StringBuilder sb = new StringBuilder();
		int len = cashTypes.length;
		for(int i = 0; i < len;){
			sb.append(cashTypes[i]);
			i++;
			if(i < len) sb.append(",");
		}
		return sb.toString();
	}
	
	@Override
	public void startNoteCheck(int[] cashTypes, int maxAcceptMoney, int timeOut,NoteRecogniseListener cashReaderListener) throws Exception {
		if(polling) throw new Exception("ʶ��������æ!�������¿�ʼ");
		if(operating) throw new Exception("ʶ��������æ!���ܿ�ʼʶ��");
		this.noteCheckListener = cashReaderListener;
		this.noteWaitingTimeOut = timeOut;
		if(this.noteWaitingTimeOut <= 0)this.noteWaitingTimeOut = 5 * 60 *1000;//ȱʡ5����
		this.maxAcceptMoney = maxAcceptMoney;
		if(cashTypes == null){
			LogManager.logError(Activator.PLUGIN_ID, "û�����ÿ��Խ��ܵ�Ǯ�ҵ���ֵ");
			cashTypes = new int[]{};
		}
		
		
		this.isCashAutoStack = true;

		LogManager.logInfo(Activator.PLUGIN_ID, "����������յ�ֽ��Ϊ��" + getAcceptStr(cashTypes));
		setAcceptNoteValue(cashTypes, this.isCashAutoStack);

		totalRecogValue = 0;
		currNoteValue = 0;
		if(CURR_DEVICE == MODULES.BILL_VALIDATOR) totalCashCount = 0;//ֻ���ڴ�ʶ����������²��������

		LogManager.logInfo(Activator.PLUGIN_ID, "ʶ�ҹ��̿�ʼ");
		startStopTime = 0;
		polling = true;
		synchronized(checkerLocker){
			checkerLocker.notify();
		}
	}

	@Override
	public void stopNoteCheck() throws Exception {
		LogManager.logInfo(Activator.PLUGIN_ID, "���յ�ֹͣʶ�ҵ�ָ��.... ...");
		if(!polling) throw new Exception("ʶ������û�б��򿪲�����ʶ��");
		this.waitCloseTime = 15000;
		
		polling = false;
		startStopTime = System.currentTimeMillis();
		try{
			hasStoped = false;
			while(true){
				synchronized(checkerWaiter){
					checkerWaiter.wait(500);
				}
				if(hasStoped) break;
				if((System.currentTimeMillis() - startStopTime) > (waitCloseTime + 500)) throw new Exception("�ر�ʶ������ʱ");
			}
		}catch(Exception e){
			throw new Exception("ֹͣʶ�ҹ��̷ǳ����ѣ�����ʶ�����п��һ��������������Ĳ���");
		}finally{
			LogManager.logInfo(Activator.PLUGIN_ID, "ʶ�ҹ��̽���");
		}

	}

	class CashCheckingManager extends Thread {
		public void run() {
			while(ConfigManager.getDefault().isSystemRunning()){
				if(!polling){ synchronized(checkerLocker){ try{ checkerLocker.wait(300); }catch(InterruptedException e){ } } }
				if(!ConfigManager.getDefault().isSystemRunning()) return;
				if(!polling) continue;
				long start = System.currentTimeMillis();
				byte oldS = 0x00;
				byte oldE = 0x00;
				boolean transfering = false;
				boolean forceInited = false;

				try{
					while(ConfigManager.getDefault().isSystemRunning()){
						Thread.sleep(150);
						byte[] data = poll();
						if(!polling && !forceInited){
							if(startStopTime != 0) if(System.currentTimeMillis() - startStopTime > (waitCloseTime / 2)) break;
							if(data[0] == 0x14 || data[0] == 0x1C) break;
						}
						if(data[0] == (byte)0x80 || data[0] == (byte)0x81 || data[0] == (byte)0x82){
							if(data[1] > 0x23){
								continue;
							}
							if(data[0] == (byte)0x82){
								int cash = getCashNoteValue(data[1]);
							}else if(data[0] == (byte)0x80){
								int cash = getCashNoteValue(data[1]);
								if(!isCashAutoStack){
									sendCmd(STACK, null);//�Զ���������²���Ҫ
									Thread.sleep(200);//����������poll���������
								}
							}else if(data[0] == (byte)0x81){
								currNoteValue = getCashNoteValue(data[1]);

								//����������ǰ�������ݣ���Ϊ֮ǰ���˹����������������Զ�����һ����ֵ����ɴ��󣬾���ѯ��־���ִ�ʶ������ͻ�������ݱ����������Ͷ��ı�ֵ
								if(canAcceptCash(currNoteValue) && transfering){//����ı�ֵ������ǰ���͹��ſ���
									totalRecogValue += currNoteValue;
									totalCashCount++;
									transfering = false;//���������Ϊû�д���
									LogManager.logInfo(Activator.PLUGIN_ID, "�ɹ�������" + currNoteValue + " ��ǰ�ܶ" + totalRecogValue);
									try{ if(noteCheckListener != null) noteCheckListener.cashStacked(currNoteValue, totalRecogValue); }catch(Exception e){ }
								}else{
									//what the fuck!!!
								}
								start = System.currentTimeMillis();
								if(maxAcceptMoney > 0){
									if(totalRecogValue >= maxAcceptMoney){
										break;
									}else{
										int currRest = maxAcceptMoney - totalRecogValue; 
										ArrayList<Integer> nowAcceptValues = new ArrayList<Integer>();
										for(int i = 0; i < acceptableNoteValues.length; i++) if(currRest >= acceptableNoteValues[i]) nowAcceptValues.add(acceptableNoteValues[i]); 
										int[] acceptMoney = new int[nowAcceptValues.size()];
										for(int i = 0; i < nowAcceptValues.size(); i++) acceptMoney[i] = nowAcceptValues.get(i);
										try{
											setAcceptNoteValue(acceptMoney, isCashAutoStack);
											Thread.sleep(200);
										}catch(Exception e){
											LogManager.logInfo(Activator.PLUGIN_ID, "����������");
										}
									}
								}
							}
							continue;
						}
						if(data[0] == (byte)0x1C || data[0] == (byte)0x47){
							String message = getRejectMessage(data[1]);
							if(message == null) message = "�޶������:[" + Tools.bytes2hex(data) + "]";
							if(oldE != data[1]){
								LogManager.logInfo(Activator.PLUGIN_ID, message);
								try{ if(noteCheckListener != null) noteCheckListener.recogniseWarn(message); }catch(Exception e){ }
							}
							oldE = data[1];
							continue;
						}

						oldE = 0x00;
						String statusMessage = null;
						int waittime = 0;
						boolean isCheat = false;
						if(data[0] == (byte)0x15){
							transfering = true;
							if(oldS != 0x15) statusMessage = "���ڼ��Ͷ���Ǯ�ң���ȷ������ֵ";
						}else if(data[0] == (byte)0x17){
							transfering = true;//�������ڴ��ͱ�ֵ��ֻ��˵���б�ֵ���룬ֻ�ô��ڴ�״̬���������������Ľ��
							if(oldS != 0x17) statusMessage = "���ڴ���Ͷ��ı�,�Ա���е���";
						}else if(data[0] == (byte)0x18){
							if(oldS != 0x18) statusMessage = "���ڻ���Ͷ���Ǯ�ң����ȴ��û�ȡ��";
						}else if(data[0] == (byte)0x19){//�����Ҫ����ͻ�
							if(oldS != 0x19) try{ if(noteCheckListener != null) noteCheckListener.recogniseWarn("[19]��ȡ��ʶ�����ڵ���Ʒ"); }catch(Exception e){ }
						}else if(data[0] == (byte)0x1b){
							waittime = (int)data[1] * 100;
							if(oldS != 0x1b) statusMessage = "ʶ����æ����ȴ�:" + waittime + "����";
						}else if(data[0] == 0x41){
							if(oldS != 0x41) statusMessage = "��⵽ʶ�������źţ�����ʶ������������";
							waittime = 1000;
						}else if(data[0] == (byte)0x42){
							if(oldS != 0x42) statusMessage = "��⵽����δ��װ��λ";
						}else if(data[0] == (byte)0x43){
							if(oldS != 0x43) statusMessage = "��⵽��Ǯ�ҿ���ʶ�����ڲ�";
						}else if(data[0] == (byte)0x44){
							if(oldS != 0x44) statusMessage = "��⵽��Ǯ�ҿ���ʶ�����ڲ�";
						}else if(data[0] == (byte)0x45){
							if(oldS != 0x45) statusMessage = "��⵽���ܴ��ڵļٳ�";
							isCheat = true;
						}else if(data[0] == (byte)0x46){
							if(oldS != 0x46) statusMessage = "ʶ�������бң���ȴ��������ٲ���Ǯ��";
						}else if(data[0] == (byte)0x47){
							if(oldS != 0x47) statusMessage = "ʶ�������д���ʶ�ҽ������봦���������:" + data[1];
							break;
						}else if(data[0] == (byte)0x14){
							if(oldS != 0x14) statusMessage = "ʶ�������У����ȴ��û�Ͷ��";
							waittime = 300;
						}else if(data[0] == (byte)0x13){
							statusMessage = "�յ�ʶ�������ȳ�ʼ������Ϣ��" + Tools.bytes2hex(data) + " �����ʶ�ҹ����У����״̬��Ҫ��������������";
							forceInited = true;
							//waittime = 3500;//����ǿ����ֹͶ�ҹ���
						}else{
							statusMessage = "�յ�����ʶ���״̬���ݣ�" + Tools.bytes2hex(data);
						}
						if(data[0] != (byte)0x14) start = System.currentTimeMillis();//ֻҪ�����У������¼�ʱ//XXX �����ӵģ���Ҫ��֤
						if(data[0] != (byte)0x13){
							if(forceInited){
								try{
									LogManager.logInfo(Activator.PLUGIN_ID, "ʶ������ǿ�Ƴ�ʼ������������ʧ�����");
									setAcceptNoteValue(acceptableNoteValues, isCashAutoStack);
									Thread.sleep(300);//����poll��̫����ܻ����ѽ��
								}catch(Throwable e){
									LogManager.logError(Activator.PLUGIN_ID, "ʶ������ǿ�Ƴ�ʼ������������ʧ�����ʧ��", e);
								}
							}
							forceInited = false;
						}

						if(statusMessage != null){
							LogManager.logInfo(Activator.PLUGIN_ID, statusMessage);
							try{ if(noteCheckListener != null) noteCheckListener.statusChanged(statusMessage); }catch(Exception e){ }
						}
						if(isCheat){
							try{ if(noteCheckListener != null) noteCheckListener.cheatedCash(); }catch(Exception e){ }
						}

						if(waittime > 0) Thread.sleep(waittime);

						oldS = data[0];
						if(noteWaitingTimeOut > 0){
							long splash = System.currentTimeMillis() - start;
							if(splash > noteWaitingTimeOut) break;
							else{
								try{ if(noteCheckListener != null) noteCheckListener.recogniseExpireTime(noteWaitingTimeOut - splash); }catch(Exception e){ }
							}
						}
					}
				}catch(Throwable e){
					LogManager.logError(Activator.PLUGIN_ID, "ʶ�ҹ��̳��ִ���ֹͣʶ��", e);
					try{ if(noteCheckListener != null) noteCheckListener.recogniseError(e.getMessage()); }catch(Exception ee){ }
				}finally{
					LogManager.logInfo(Activator.PLUGIN_ID, "ʶ��ѭ����������ǰ���յ��Ľ��Ϊ:" + totalRecogValue);
					try{ setAcceptNoteValue(new int[]{}, false); }catch(Exception e){ LogManager.logError(Activator.PLUGIN_ID, "�ر�ʶ����ʶ��������ʶ�������ܻ᲻�������δ��������Ͷ��", e); }
					noteWaitingTimeOut = 5 * 60 * 1000;
					polling = false;
					hasStoped = true;
					try{ synchronized(checkerWaiter){ checkerWaiter.notify(); } }catch(Throwable e){ LogManager.logError(Activator.PLUGIN_ID, "ֹ֪ͨͣ�����Ѿ����ֹͣ����", e); }

					try{
						if(noteCheckListener != null){
							LogManager.logInfo(Activator.PLUGIN_ID, "֪ͨӦ�ó���ʶ���Ѿ�����");
							noteCheckListener.recogniseFinished(totalRecogValue);
						}else{
							LogManager.logInfo(Activator.PLUGIN_ID, "����û�й�����Ӧ�ó�����˲���֪ͨӦ�ó���ʶ�ҽ���");
						}
					}catch(Exception e){
						LogManager.logError(Activator.PLUGIN_ID, "֪ͨӦ�ó���ʶ���Ѿ�����ʧ��", e);
					}finally{
						noteCheckListener = null;//�����ǰ��listener�����������������
					}
				}
			}
		}
	}
}
