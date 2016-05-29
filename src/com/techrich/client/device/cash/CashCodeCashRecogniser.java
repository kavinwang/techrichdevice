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
	private final static byte RESET 											= 0x30;//重置设备
	private final static byte GETSTATUS 							= 0x31;//读取开机状态
	private final static byte POLL 												= 0x33;
	private final static byte ENABLEBILLTYPE 		= 0x34;
	private final static byte STACK 											= 0x35;
	private final static byte IDENTIFY									= 0x37;


	private final static class MODULES {
		public final static byte NONE = 0x00;
		public final static byte BILL_VALIDATOR = 0x03;
	}

	private byte CURR_DEVICE = MODULES.BILL_VALIDATOR; //当前的设备，通过suite方法取得并设置,缺省为识币器模块
	protected final static HashMap<Integer, Integer> BILL_TABLE = new HashMap<Integer, Integer>();

	private final static HashMap<Byte, String> cashRejectedErrors = new HashMap<Byte, String>();
	static{
		cashRejectedErrors.put((byte)0x60, "[1C]插入方法不正确");
		cashRejectedErrors.put((byte)0x61, "[1C]磁电信息检测错误");
		cashRejectedErrors.put((byte)0x62, "[1C]识币头里币不能再次插入钱币");
		cashRejectedErrors.put((byte)0x63, "[1C]叠加因子或叠加补偿错误");
		cashRejectedErrors.put((byte)0x64, "[1C]传送投入的钱币错误");
		cashRejectedErrors.put((byte)0x65, "[1C]不能识别投入的钱币");
		cashRejectedErrors.put((byte)0x66, "[1C]投入的钱币不能通过验证");
		cashRejectedErrors.put((byte)0x67, "[1C]光检测错误");
		cashRejectedErrors.put((byte)0x68, "[1C]不接受的钱币面值");
		cashRejectedErrors.put((byte)0x69, "[1C]容量太小不能继续投币");
		cashRejectedErrors.put((byte)0x6A, "[1C]操作方法不正确");
		cashRejectedErrors.put((byte)0x6C, "[1C]钱币长度不正确");
		cashRejectedErrors.put((byte)0x6D, "[1C]钱币属性不符合");
		cashRejectedErrors.put((byte)0x50, "[47]叠钞马达错误");
		cashRejectedErrors.put((byte)0x51, "[47]传送马达速度异常");
		cashRejectedErrors.put((byte)0x52, "[47]传送马达错误");
		cashRejectedErrors.put((byte)0x53, "[47]整理马达错误");
		cashRejectedErrors.put((byte)0x54, "[47]初始化钱箱状态错误");
		cashRejectedErrors.put((byte)0x55, "[47]光探头错误");
		cashRejectedErrors.put((byte)0x56, "[47]磁探头错误");
		cashRejectedErrors.put((byte)0x5F, "[47]容量探测头错误");
	}

	CashCheckingManager cashCheckThread = null;

	//固件信息
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

		//本模块支持识币器（无论是否带钱箱）、支持硬币找零机
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
		if(CURR_DEVICE == MODULES.NONE) throw new DeviceInitException("没有找到识币器设备");
		
		try{Thread.sleep(2000);}catch(Exception e){}
		try{
			ByteBuffer data = new ByteBuffer(sendCmd(IDENTIFY, null));
			partNum = new String(data.getValueN(0, 15));
			serialNum = new String(data.getValueN(15, 12));
			assetNum = data.getValueN(27);
	
			LogManager.logInfo(Activator.PLUGIN_ID, "检测到CashCode设备：" + this.partNum + " 序列号：" + serialNum + " 资产编号:" + Tools.bytes2hex(assetNum));
	
			String[] parts = partNum.split("-");
			if(parts.length < 2) throw new Exception("不是CASHCODE 识币器！");
			if(!parts[1].startsWith("CN")) throw new Exception("CASH CODE NOT FOR CHINA"); 

			//XXX 如果识币器有退钞功能是否需要增加掉电恢复功能
			getSettings();
			setAcceptNoteValue(new int[]{}, false);//disable bill types
			poll();
		}catch(Exception e){
			throw new DeviceInitException("没有找到识币器设备",e); 
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
		return deviceName!=null?deviceName:"CASHCODE识币器";
	}
	
	private void setAcceptNoteValue(int[] values, boolean autoStack) throws Exception {
		if(values != null) acceptableNoteValues = values.clone();
		else{
			acceptableNoteValues = new int[]{};
			LogManager.logInfo(Activator.PLUGIN_ID, "没有设置可以接受的钱币的面值");
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
		for(int i = 0; i < 24; i++){//共支持24中币种，参考cashcode的文档
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
		throw new Exception("识币器错误：激活识币器失败，识币器无反应！");
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

		// 接收数据
		bb = new ByteBuffer();
		bb.append(recieveData(3, 300));// SYNC,ADDR,LNG
		int len = (bb.getByteAt(2) & 0x00ff) - 3 - 2;
		if(len > 0) bb.append(recieveData(len, 100));// 数据部分

		crc = recieveData(2, 100);// crc部分
		byte[] crcv = getCrc(bb.getValue());
		bb.append(crc);

		if(crcv[0] != crc[0] || crcv[1] != crc[1]){ throw new Exception("数据验证失败！"); }

		if(bb.getByteAt(2) == 0x06){
			byte errorCode = bb.getByteAt(3);
			if(errorCode == 0xff) throw new Exception("Negative Acknowledgment");	
			if(errorCode == 0x30) throw new Exception("Illegal command");
		}

		//回应识币器成功接收数据
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
		if(polling) throw new Exception("识币器正在忙!不能重新开始");
		if(operating) throw new Exception("识币器正在忙!不能开始识币");
		this.noteCheckListener = cashReaderListener;
		this.noteWaitingTimeOut = timeOut;
		if(this.noteWaitingTimeOut <= 0)this.noteWaitingTimeOut = 5 * 60 *1000;//缺省5分钟
		this.maxAcceptMoney = maxAcceptMoney;
		if(cashTypes == null){
			LogManager.logError(Activator.PLUGIN_ID, "没有设置可以接受的钱币的面值");
			cashTypes = new int[]{};
		}
		
		
		this.isCashAutoStack = true;

		LogManager.logInfo(Activator.PLUGIN_ID, "本次允许接收的纸币为：" + getAcceptStr(cashTypes));
		setAcceptNoteValue(cashTypes, this.isCashAutoStack);

		totalRecogValue = 0;
		currNoteValue = 0;
		if(CURR_DEVICE == MODULES.BILL_VALIDATOR) totalCashCount = 0;//只有在纯识币器的情况下才清计数器

		LogManager.logInfo(Activator.PLUGIN_ID, "识币过程开始");
		startStopTime = 0;
		polling = true;
		synchronized(checkerLocker){
			checkerLocker.notify();
		}
	}

	@Override
	public void stopNoteCheck() throws Exception {
		LogManager.logInfo(Activator.PLUGIN_ID, "接收到停止识币的指令.... ...");
		if(!polling) throw new Exception("识币器还没有被打开并进行识币");
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
				if((System.currentTimeMillis() - startStopTime) > (waitCloseTime + 500)) throw new Exception("关闭识币器超时");
			}
		}catch(Exception e){
			throw new Exception("停止识币过程非常困难，可能识币器有卡币或者其它不正常的操作");
		}finally{
			LogManager.logInfo(Activator.PLUGIN_ID, "识币过程结束");
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
									sendCmd(STACK, null);//自动叠炒情况下不需要
									Thread.sleep(200);//防备后续的poll过快而出错
								}
							}else if(data[0] == (byte)0x81){
								currNoteValue = getCashNoteValue(data[1]);

								//清除里面的先前残余数据，因为之前周兴国报告机器启动后会自动存在一个币值而造成错误，经查询日志后发现打开识币器后就会读到数据表而被解析成投入的币值
								if(canAcceptCash(currNoteValue) && transfering){//允许的币值并且先前传送过才可以
									totalRecogValue += currNoteValue;
									totalCashCount++;
									transfering = false;//计算后设置为没有传送
									LogManager.logInfo(Activator.PLUGIN_ID, "成功叠钞：" + currNoteValue + " 当前总额：" + totalRecogValue);
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
											LogManager.logInfo(Activator.PLUGIN_ID, "设置面额错误");
										}
									}
								}
							}
							continue;
						}
						if(data[0] == (byte)0x1C || data[0] == (byte)0x47){
							String message = getRejectMessage(data[1]);
							if(message == null) message = "无定义错误:[" + Tools.bytes2hex(data) + "]";
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
							if(oldS != 0x15) statusMessage = "正在检查投入的钱币，并确定其面值";
						}else if(data[0] == (byte)0x17){
							transfering = true;//设置正在传送币值，只是说明有币值进入，只用存在此状态，才允许计算叠钞的结果
							if(oldS != 0x17) statusMessage = "正在传送投入的币,以便进行叠钞";
						}else if(data[0] == (byte)0x18){
							if(oldS != 0x18) statusMessage = "正在回退投入的钱币，并等待用户取走";
						}else if(data[0] == (byte)0x19){//这个需要警告客户
							if(oldS != 0x19) try{ if(noteCheckListener != null) noteCheckListener.recogniseWarn("[19]请取走识币器口的物品"); }catch(Exception e){ }
						}else if(data[0] == (byte)0x1b){
							waittime = (int)data[1] * 100;
							if(oldS != 0x1b) statusMessage = "识币器忙，需等待:" + waittime + "毫秒";
						}else if(data[0] == 0x41){
							if(oldS != 0x41) statusMessage = "检测到识币器满信号，可能识币器叠钞堵塞";
							waittime = 1000;
						}else if(data[0] == (byte)0x42){
							if(oldS != 0x42) statusMessage = "检测到钞箱未安装到位";
						}else if(data[0] == (byte)0x43){
							if(oldS != 0x43) statusMessage = "检测到有钱币卡在识币器内部";
						}else if(data[0] == (byte)0x44){
							if(oldS != 0x44) statusMessage = "检测到有钱币卡在识币器内部";
						}else if(data[0] == (byte)0x45){
							if(oldS != 0x45) statusMessage = "检测到可能存在的假钞";
							isCheat = true;
						}else if(data[0] == (byte)0x46){
							if(oldS != 0x46) statusMessage = "识币器中有币，请等待叠钞后再插入钱币";
						}else if(data[0] == (byte)0x47){
							if(oldS != 0x47) statusMessage = "识币器运行错误，识币结束，请处理！错误代码:" + data[1];
							break;
						}else if(data[0] == (byte)0x14){
							if(oldS != 0x14) statusMessage = "识币器空闲，并等待用户投币";
							waittime = 300;
						}else if(data[0] == (byte)0x13){
							statusMessage = "收到识币器被迫初始化的信息：" + Tools.bytes2hex(data) + " 如果在识币过程中，这个状态需要警觉！！！！！";
							forceInited = true;
							//waittime = 3500;//现在强制终止投币过程
						}else{
							statusMessage = "收到不能识别的状态数据：" + Tools.bytes2hex(data);
						}
						if(data[0] != (byte)0x14) start = System.currentTimeMillis();//只要不空闲，就重新计时//XXX 后来加的，需要验证
						if(data[0] != (byte)0x13){
							if(forceInited){
								try{
									LogManager.logInfo(Activator.PLUGIN_ID, "识币器被强制初始化后重新设置失败面额");
									setAcceptNoteValue(acceptableNoteValues, isCashAutoStack);
									Thread.sleep(300);//后面poll的太快可能会出错呀！
								}catch(Throwable e){
									LogManager.logError(Activator.PLUGIN_ID, "识币器被强制初始化后重新设置失败面额失败", e);
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
					LogManager.logError(Activator.PLUGIN_ID, "识币过程出现错误，停止识币", e);
					try{ if(noteCheckListener != null) noteCheckListener.recogniseError(e.getMessage()); }catch(Exception ee){ }
				}finally{
					LogManager.logInfo(Activator.PLUGIN_ID, "识币循环结束，当前接收到的金额为:" + totalRecogValue);
					try{ setAcceptNoteValue(new int[]{}, false); }catch(Exception e){ LogManager.logError(Activator.PLUGIN_ID, "关闭识币器识币面额错误，识币器可能会不经意接收未经启动的投币", e); }
					noteWaitingTimeOut = 5 * 60 * 1000;
					polling = false;
					hasStoped = true;
					try{ synchronized(checkerWaiter){ checkerWaiter.notify(); } }catch(Throwable e){ LogManager.logError(Activator.PLUGIN_ID, "通知停止方法已经完成停止过程", e); }

					try{
						if(noteCheckListener != null){
							LogManager.logInfo(Activator.PLUGIN_ID, "通知应用程序识币已经结束");
							noteCheckListener.recogniseFinished(totalRecogValue);
						}else{
							LogManager.logInfo(Activator.PLUGIN_ID, "由于没有关联到应用程序，因此不能通知应用程序识币结束");
						}
					}catch(Exception e){
						LogManager.logError(Activator.PLUGIN_ID, "通知应用程序识币已经结束失败", e);
					}finally{
						noteCheckListener = null;//清除当前的listener，避免后面的问题出现
					}
				}
			}
		}
	}
}
