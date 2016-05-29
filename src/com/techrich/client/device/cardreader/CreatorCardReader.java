package com.techrich.client.device.cardreader;

import org.apache.velocity.util.StringUtils;

import com.techrich.client.device.DeviceInitException;
import com.techrich.client.device.IntegratedCardReader;
import com.techrich.client.manager.LogManager;
import com.techrich.tools.ByteBuffer;
import com.techrich.tools.Tools;

public class CreatorCardReader extends IntegratedCardReader {
	private int cardStatus=0xFF;
	private int canAcceptCard = 0;//0x00:不允许进卡，0x01:后端进卡 0x02后端进卡 0x03：都允许
	private boolean frontDoorStatus = false;//关闭
	private boolean backDoorStatus = false;//关闭
	private int CardType = CARD_TYPE_NOCARD;//当前的卡类型
	
	@Override
	public String getDeviceName() {
		return deviceName!=null?deviceName:"深圳创自读卡器";
	}
	
	public boolean canFrontCardInsert(){
		return frontDoorStatus;
	}
	public boolean canBackCardInsert(){
		return backDoorStatus;
	}
	public CreatorCardReader() {
		this.deviceId = "CARDREADER-CZ-3IN1";
	}

	@Override
	public String getDeviceIdentifer() throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x30,0x3A});//读序列号信息
		return new String(bb.getValueN(3));
	}

	@Override
	public void initDevice() throws DeviceInitException {
		try {
			//卡机复位
			ByteBuffer bb = commToCardReader(new byte[]{0x30,0x31});//卡机复位，返回读卡器版本信息，前端弹卡不持卡
			//设置进卡方式
			bb = commToCardReader(new byte[]{0x2F,0x33,0x31});//允许磁卡，IC卡，非接触卡，双界面卡进卡方式，只允许从前端开闸门进卡
			if(bb.getByteAt(3)=='N')throw new Exception("设置进卡方式失败");
			//设置停卡方式
			bb = commToCardReader(new byte[]{0x2E,0x31});//进卡后停在前端并持卡
			if(bb.getByteAt(2)=='N')throw new Exception("设置停卡方式失败");
			//关门
			bb = commToCardReader(new byte[]{0x2F,0x31,0x31});
			if(bb.getByteAt(3)=='N')throw new Exception("设置读卡器关门失败");
			//取状态并设置
			bb = commToCardReader(new byte[]{0x31,0x30});
			byte[] st = bb.getValueN(2);
			cardStatus = st[0];//卡状态,参见文档进行解析
			if(st[1]==0x4E)frontDoorStatus=false;//前门禁止进卡
			else frontDoorStatus = true;//前门允许进卡
			if(st[2]==0x4E)backDoorStatus = false;//后门禁止进卡
			else backDoorStatus = true;//后门允许进卡
			
			//确定当前的读卡器支持的模块
			bb = commToCardReader(new byte[]{0x47,0x30,0x04,0x04});
			if(bb.getByteAt(2)=='N')throw new Exception("判断读卡器模块错误");
			byte[] pre = bb.getValueN(5, 4);
			if(pre[0]==0x30)canReadMagnetCard=false;
			else canReadMagnetCard=true;
			if(pre[1]==0x31)canReadRFCard=true;
			if(pre[2]==0x31){
				canReadMemoryCard = true;
				canReadCPUCard = true;
				canReadSimCard=true;
			}
			if(pre[3]==0x31)canReadSamCard=true;

			StringBuffer sb = new StringBuffer("当前创自读卡器：");
			sb.append(canReadMagnetCard?"磁卡支持":"磁卡不支持").append("|");
			sb.append(canReadRFCard?"非接触卡支持":"非接触卡不支持").append("|");
			sb.append(canReadMemoryCard?"IC卡支持":"IC卡不支持").append("|");
			sb.append(canReadCPUCard?"CPU卡支持":"CPU卡不支持").append("|");
			sb.append(canReadSimCard?"SIM卡支持":"SIM卡不支持").append("|");
			sb.append(canReadSamCard?"SAM卡支持":"SAM卡不支持").append("|");
			LogManager.logInfo(sb.toString());
		} catch (Exception e) {
			throw new DeviceInitException(e);
		}
	}
	
	
	/**
	 * 打开读卡器门，允许插卡和读卡
	 * @param type 0:磁卡方式 1：开关方式，支持所有类型的卡 2：磁信号方式，支持薄卡
	 * @param location 持卡位置 0：停在前端不持卡，1：停在前端持卡 2：RF卡位置 3：IC卡位置 4：停在后端持卡 5：停在后端不持卡
	 * @throws Exception
	 */
	@Override
	public void openDoor(int type,int loc) throws Exception {
		byte location =(byte)(0x30+loc);
		byte doorMode = 0x32;
		if(type==1)doorMode=0x33;
		else if(type==2)doorMode=0x34;
		//当中持卡和后持卡时允许开关方式进卡使能，允许磁卡，IC卡，Mefare 1射频卡，双界面卡从前端开闸门进卡，
		//否则只能是磁卡从前门进卡方式
		if(location<0x32)doorMode=0x32;
		ByteBuffer bb = commToCardReader(new byte[]{0x2F,doorMode,0x31});//0x32 0x33
		if(bb.getByteAt(3)=='N')throw new Exception("设置读卡器开门失败");
		bb = commToCardReader(new byte[]{0x2E,location});
		if(bb.getByteAt(2)=='N')throw new Exception("设置读卡器持卡位置错误");
		getCardReaderRuntimeStatus();
		if(canAcceptCard <2)throw new Exception("打开读卡器门失败！");//高位为前门
	}
	
	@Override
	public void closeDoor() throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x2F,0x31,0x31});
		if(bb.getByteAt(3)=='N')throw new Exception("设置读卡器关门失败");
	}

	/**
	 * 设置并走卡到指定位置
	 * @param loc 走卡的位置 0： 走到前端不持卡 1：走到前端持卡 2：走到后端持卡 
	 * 						3：走到后端不持卡 4向后弹出卡 5：中持卡 6：走到卡机内IC卡位
	 * @throws Exception
	 */

	@Override
	public void moveCard(int loc) throws Exception {
		byte location = 0x30;
		if(loc>=0&&loc<=4)location +=loc;
		else{
			if(loc==5)location=0x2E;
			else if(loc==6)location=0x2F;
		}
		ByteBuffer bb = commToCardReader(new byte[]{0x32,location});
		if(bb.getByteAt(2)=='N')throw new Exception("设置读卡器关门失败");
		//XXX 其他错误信息需要解析
	}

	/**
	 * 取卡机的当前卡状态
	 * 0：卡机内无卡 
	 * 1：卡机内有长卡 2：卡机内有短卡 3：卡机前端不持卡位置有卡 4：卡机前端持卡位置有卡
	 * 5：卡机内停卡位置有卡 6：卡机内IC卡位置有卡 7：卡机后端持卡位置有卡 8：卡机后端不持卡位置有卡
	 * @return
	 * @throws Exception
	 */
	@Override
	public int getCardReaderRuntimeStatus()throws Exception{
		ByteBuffer bb = commToCardReader(new byte[]{0x31,0x30});
		byte[] st = bb.getValueN(2);
		cardStatus = st[0];//卡状态,参见文档进行解析
		canAcceptCard = 0x00;
		if(st[1] != 0x4E)canAcceptCard+=0x02;
		if(st[2] != 0x4E)canAcceptCard+=0x01;
		getCardStatusInfo();
		if(cardStatus==0x4E)return 0;
		else return cardStatus-0x45;
	}
	
	private int parseCommResult(byte bb) throws Exception {
		if(bb=='0')throw new Exception("寻不到射频卡");
		if(bb=='1')throw new Exception("操作扇区号错");
		if(bb=='2')throw new Exception("操作的卡序列号错误");
		if(bb=='3')throw new Exception("密码错误");
		if(bb=='4')throw new Exception("读数据错误");
		if(bb=='N')throw new Exception("操作失败");
		if(bb=='E')throw new Exception("卡机内无卡");
		if(bb=='W')throw new Exception("卡不在允许操作的位置上");
		if(bb=='Y')return 0;
		return 1;
	}
	
	public String getCardStatusInfo(){
		if(cardStatus==0x46)return "卡机内有长卡";
		else if(cardStatus==0x47)return "卡机内有短卡";
		else if(cardStatus==0x48)return "卡机前端不持卡位置有卡";
		else if(cardStatus==0x49)return "卡机前端持卡位置有卡";
		else if(cardStatus==0x4A)return "卡机内停卡位置有卡";
		else if(cardStatus==0x4B)return "卡机内IC卡操作位置有卡";
		else if(cardStatus==0x4C)return "卡机后端持卡位置有卡";
		else if(cardStatus==0x4D)return "卡机后端不持卡位置有卡(没收卡)";
		else if(cardStatus==0x4E)return "卡机内无卡";
		else return "卡机正常";
	}
	
	//磁条卡
	@Override
	public String[] readTrackDatas(boolean track1, boolean track2, boolean track3) throws Exception {
		this.CardType  = CreatorCardReader.CARD_TYPE_MAGNET;
//		byte mode = (byte)(0x30 | (track1?0x01:0x00) | (track2?0x02:0x00) | (track3?0x04:0x00));
//
//		ByteBuffer bb = commToCardReader(new byte[] { 0x45, 0x30, 0x30, mode });
//		for (int i = 0; i < bb.length(); i++)if (bb.getByteAt(i) == US)bb.replace(i, (byte) '$');
//		String validTrackData = new String(bb.getValueN(4), "iso8859-1");
//		String[] result = StringUtils.split(validTrackData, "$");
//		if (result.length < 3)throw new Exception("未能读出3个磁道内容");
//		for (int i = 0; i < result.length; i++) {
//			System.out.println("磁道 " + i + " 的内容为："	+ (result[i] == null ? "" : result[i]));
//		}
//		// 有问题的磁道或者不读的磁道内容不设置
//		String[] trackDatas = new String[3];
//		if (result[0] != null && result[0].length() > 0	&& result[0].charAt(0) == 'Y')trackDatas[0] = result[0].substring(1);
//		if (result[1] != null && result[1].length() > 0	&& result[1].charAt(0) == 'Y')trackDatas[1] = result[1].substring(1);
//		if (result[2] != null && result[2].length() > 0	&& result[2].charAt(0) == 'Y')trackDatas[2] = result[2].substring(1);
//		return trackDatas;
		byte mode = 0x30;
		if (track1 == true && track2 == false && track3 == false)mode = 0x31;
		else if (track1 == false && track2 == true && track3 == false)mode = 0x32;
		else if (track1 == false && track2 == false && track3 == true)mode = 0x33;
		else if (track1 == true && track2 == true && track3 == false)mode = 0x34;
		else if (track1 == false && track2 == true && track3 == true)mode = 0x35;
		else if (track1 == true && track2 == false && track3 == true)mode = 0x36;
		else if (track1 == true && track2 == true && track3 == true)mode = 0x37;

		ByteBuffer bb = commToCardReader(new byte[] { 0x45, 0x30, 0x30, mode });
		System.out.println("读到的卡信息为：" + Tools.bytes2hex(bb.getValue()));
		for (int i = 0; i < bb.length(); i++)if (bb.getByteAt(i) == 0x1F)bb.replace(i, (byte) '$');
		String validTrackData = new String(bb.getValueN(4), "iso8859-1");
		String[] result = StringUtils.split(validTrackData, "$");
		if (result.length < 3)throw new Exception("未能读出3个磁道内容");
		for (int i = 0; i < result.length; i++) {
			System.out.println("磁道 " + i + " 的内容为："	+ (result[i] == null ? "" : result[i]));//客户信息安全原因，不允许记录日志
		}
		// 有问题的磁道或者不读的磁道内容不设置
		String[] trackDatas = new String[3];
		if (result[0] != null && result[0].length() > 0	&& result[0].charAt(0) == 'Y')trackDatas[0] = result[0].substring(1);
		if (result[1] != null && result[1].length() > 0	&& result[1].charAt(0) == 'Y')trackDatas[1] = result[1].substring(1);
		if (result[2] != null && result[2].length() > 0	&& result[2].charAt(0) == 'Y')trackDatas[2] = result[2].substring(1);
		return trackDatas;

	}
  
	
	//IC卡
	@Override
  public byte[] openICCard() throws Exception {
		this.CardType  = CreatorCardReader.CARD_TYPE_NOCARD;
    ByteBuffer bb = commToCardReader(new byte[] { 0x32, 0x2F});//走到IC卡位置
    bb = commToCardReader(new byte[] { 0x33, 0x30 });//上电
    bb = commToCardReader(new byte[] { 0x37, 0x30 });//冷复位 0x2F为热复位
    this.CardType  = CreatorCardReader.CARD_TYPE_ICCARD;
    return bb.getValue();
  }

	/**
	 * 对IC卡进行apdu操作
	 * @param data apdu数据
	 * @throws Exception
	 */
	@Override
	public byte[] apdu(byte[] data)throws Exception{
		if(this.CardType == CreatorCardReader.CARD_TYPE_ICCARD){
			byte[] len = Tools.htons((short)data.length);
			ByteBuffer bb = new ByteBuffer(new byte[]{0x37,0x31});
			bb.append(len).append(data);
			bb = commToCardReader(bb.getValue());
			parseCommResult(bb.getByteAt(2));
			String results = Tools.bytes2hex(bb.getValueN(5));
	
			int resuLen = results.length();
			String sw = results.substring(resuLen - 4, resuLen);
			if(!sw.equalsIgnoreCase("9000")){ throw new Exception(sw); }
			return Tools.hex2bytes(results.substring(0, resuLen - 4));
		}else if(this.CardType == CreatorCardReader.CARD_TYPE_SAMCARD){
			//XXX :对SAM卡进行apdu操作,第一个字节是卡座号
			ByteBuffer bb = new ByteBuffer(new byte[]{0x3D,(byte)0x31,data[0]});//只做T=0的 T=1的不做
			bb.append(Tools.htons((short)(data.length-1))).append(data,1,data.length-1);
			bb = commToCardReader(bb.getValue());
			parseCommResult(bb.getByteAt(2));
			return bb.getValueN(6);
		}else throw new Exception("不能确定卡片的类型,请先上电后再操作!");
	}
	
	/**
	 * IC 卡下电
	 * @return
	 * @throws Exception
	 */
	@Override
	public void powerDownICCard()throws Exception{
		//使用Sim卡的下电指令
		ByteBuffer bb = commToCardReader(new byte[]{0X33,0x31});
		parseCommResult(bb.getByteAt(2));
		this.CardType  = CreatorCardReader.CARD_TYPE_NOCARD;
	}
	
	//对SIM/SAM卡的读写,用户鉴权卡
	
	/**
	 * 对SAM卡进行上电和初始化
	 * @param type 卡电压类型，卡类型,包括18，30，50
	 * @param socket 卡座号
	 * @return 返回的卡操作方式，后续的对SIM卡操作必须附带这个返回结果
	 * @throws Exception
	 */
	@Override
	public int powerUpSamCard(int socket)throws Exception{
//		byte t = 0x2E; //1.8v
//		if(type==30)t = 0x2F;//3.0v
//		else if(type==50)t = 0x30; //5.0v
		this.CardType  = CreatorCardReader.CARD_TYPE_NOCARD;
		ByteBuffer bb = commToCardReader(new byte[]{0x3D,0x2F,(byte)socket});//只做3v的
		int status =  parseCommResult(bb.getByteAt(3));
		this.CardType  = CreatorCardReader.CARD_TYPE_SAMCARD;
		return status;
	}
	
	/**
	 * 对SIM卡进行下电操作
	 * @throws Exception
	 */
	@Override
	public void powerDownSamCard()throws Exception{
		ByteBuffer bb = commToCardReader(new byte[]{0x4A,0x31});
		parseCommResult(bb.getByteAt(2));
		this.CardType  = CreatorCardReader.CARD_TYPE_NOCARD;
	}

	
	//mifare 卡
	@Override
	public void findRFCard() throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x35,0x30});
		parseCommResult(bb.getByteAt(2));
		this.CardType  = CreatorCardReader.CARD_TYPE_RFCARD;
	}

	@Override
	public String getRFCardSerialNo() throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x35,0x31});
		parseCommResult(bb.getByteAt(2));
		return Tools.bytes2hex(bb.getValueN(3));
	}
	@Override
	public void authRFCardKeyA(int secNo, byte[] password) throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x32,(byte)secNo});
		for(int i=0;i<6;i++)bb.append(password[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(3));
	}


	@Override
	public String readRFCardData(int secNo, int blockNo) throws Exception {
		ByteBuffer bb = commToCardReader(new byte[]{0x35,0x33,(byte)secNo,(byte)blockNo});
		parseCommResult(bb.getByteAt(4));
		return Tools.bytes2hex(bb.getValueN(5));
	}

	@Override
	public void writeRFCardData(int secNo, int blockNo, byte[] data)throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x34,(byte)secNo,(byte)blockNo});
		for(int i=0;i<16;i++)bb.append(data[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(4));
		
	}
	
	@Override
	public String addRFCardValue(int secNo, int blockNo, int value)
			throws Exception {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (value & 0x000000ff);
		bytes[1] = (byte) ((value & 0x0000ff00) >> 8);
		bytes[2] = (byte) ((value & 0x00ff0000) >> 16);
		bytes[3] = (byte) ((value & 0xff000000) >> 24);
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x37,(byte)secNo,(byte)blockNo});
		for(int i=0;i<4;i++)bb.append(bytes[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(4));
		return new String(bb.getValue());
	}


	@Override
	public String decreaseRFCardValue(int secNo, int blockNo, int value)
			throws Exception {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (value & 0x000000ff);
		bytes[1] = (byte) ((value & 0x0000ff00) >> 8);
		bytes[2] = (byte) ((value & 0x00ff0000) >> 16);
		bytes[3] = (byte) ((value & 0xff000000) >> 24);
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x38,(byte)secNo,(byte)blockNo});
		for(int i=0;i<4;i++)bb.append(bytes[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(4));
		return new String(bb.getValue());
	}

	@Override
	public void authRFCardKeyB(int secNo, byte[] password) throws Exception {
		ByteBuffer bb = new ByteBuffer(new byte[]{0x35,0x39,(byte)secNo});
		for(int i=0;i<6;i++)bb.append(password[i]);
		bb = commToCardReader(bb.getValue());
		parseCommResult(bb.getByteAt(3));
		
	}


	/**
	 * 和读卡器进行通讯
	 * @param commandData 为纯粹的命令字+命令参数
	 * @return 通讯的结果内容，不保护STX、长度、ETX和BCC信息
	 * @throws Exception
	 */
//	private ByteBuffer commToCardReader(byte[] commandData)throws Exception{
	public ByteBuffer commToCardReader(byte[] commandData)throws Exception{//外部3合一调用 改为public
		if(commandData==null)commandData=new byte[]{};
		ByteBuffer bb = new ByteBuffer();
		bb.append(STX).append(Tools.htons((short)commandData.length)).append(commandData).append(ETX);
		byte bcc = 0x00;
		for(int i=0;i<bb.length();i++)bcc^=bb.getByteAt(i);
		bb.append(bcc);
		
		for(int i=0;i<3;i++){//发送3次
			sendDirect(bb.getValue());
			byte ack = recieveData(1, 5000)[0];
			if(ack==ACK){//成功
				sendDirect(new byte[]{ENQ});
				break;
			}else if(ack==NAK){//bcc错误，进行重新发送
				if(i==2)throw new Exception("和读卡器通讯不能进行");
			}
			
		}
		//开始接收结果数据
		bb.reset();
//		while(true){
		byte[] stxs = recieveData(1,5000);
		if(stxs[0]!=STX){
//				break;
			throw new Exception("收到的数据包不是STX开头");
		}
//		}
		bb.append(STX);
		byte[] len = recieveData(2);
		bb.append(len);
		int length = Tools.ntohs(len, 0);
		
		bb.append(recieveData(length));
		byte etx = recieveData(1)[0];
		if(etx!=ETX)throw new Exception("读卡器返回的数据包格式错误");
		bb.append(ETX);
		
		byte bccr = recieveData(1)[0];
		//校验bcc
		bcc = 0x00;
		for(int i=0;i<bb.length();i++)bcc^=bb.getByteAt(i);
		if(bccr==bcc){
			System.out.println("收到的数据："+Tools.bytes2hex(bb.getValue()));
			if(bb.getByteAt(3)=='N')throw new Exception("收到不正常返回的数据");
			return new ByteBuffer(bb.getValueN(3, length));
		}else{
			throw new Exception("收到读卡器的数据检验BCC错误");
		}
	}
	@Override
	public void deviceCheck() throws Exception {
		
	}

}
