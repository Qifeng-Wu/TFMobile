package com.modules.customerService;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jeewx.api.core.exception.WexinReqException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SQLHelper;
import com.modules.db.SaleSqlServerHelper;
import com.modules.entity.message.Textcard;
import com.modules.entity.message.TextcardMessage;
import com.modules.utils.AjaxJson;
import com.modules.utils.ConfigurationFileHelper;
import com.modules.utils.WxCommonAPI;

import net.sf.json.JSONObject;

/**
 * 发送预警通知消息
 * @author Caroline
 */
@RestController
@RequestMapping(value = "tf-api/customerService/Waring")
public class FaultWaring {
	
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getSaleQualitySecret();
	private static String AgentId = ConfigurationFileHelper.getSaleQualityAgentId();
	
	/**
	 * 1、推送文本卡片消息(发送预警提醒)
	 * @param ProductId,AreaName,ServerStationName,count
	 * @throws WexinReqException
	 * @throws UnsupportedEncodingException 
	 */
	@RequestMapping(value = "MultiSNWaringCard")
	@Transactional
	@ResponseBody
	public AjaxJson alarmReminder(HttpServletRequest request) throws WexinReqException, UnsupportedEncodingException{
		String productId = request.getParameter("ProductId");//sn号
		String ProductType = URLDecoder.decode(request.getParameter("ProductType"), "GBK");//产品类型
		String MonitorNo=request.getParameter("MonitorNo");
		if(MonitorNo!=null&&!MonitorNo.isEmpty()) {			
			MonitorNo = URLDecoder.decode(request.getParameter("MonitorNo"), "GBK");//产品类型
		}

		String AreaName = URLDecoder.decode(request.getParameter("AreaName"), "GBK");//大区
		String ServerStationName = URLDecoder.decode(request.getParameter("ServerStationName"), "GBK");//服务站
		String count = request.getParameter("count");//SN被服务的次数
		AjaxJson ajax = new AjaxJson();	
		String ProductTypeCode =  URLEncoder.encode(URLEncoder.encode(ProductType, "UTF-8"),"UTF-8");
		//发送预警消息
		String access_token = "";
		try {
			access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
			if(access_token!=null && !"".equals(access_token)) {
								
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();
				//textcard.setTitle("预警通知   ( "+serialNumber+" )");
				textcard.setTitle("多次维修预警通知 ");
				textcard.setUrl(serviceUrl+"/TFMobile/webpage/customerService/MultiSNWaring.html?productId="+productId+"&ProductType="+ProductTypeCode+"&MonitorNo="+MonitorNo);
				textcard.setDescription(df.format(new Date())+"\nSN号："+productId+"\n产品类型："+ProductType+"\n大区："+AreaName+"\n服务站："+ServerStationName+"\n维修次数："+count+" ");
				textcardMessage.setTouser("WuQiFeng|WuJingJing");
				textcardMessage.setToparty("5");
				textcardMessage.setMsgtype("textcard");
				textcardMessage.setAgentid(Integer.parseInt(AgentId));
				textcardMessage.setTextcard(textcard);
				JSONObject result = WxCommonAPI.sendMssage(access_token, textcardMessage);
				if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
					ajax.setSuccess(true);
					ajax.setMessage("消息发送成功！");
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("消息发送失败！");
					ajax.setErrorCode("-1");
				}			
			}
		
			} catch (Exception e) {
				ajax.setErrorCode("-1");
				ajax.setSuccess(false);
				ajax.setMessage("消息发送失败！");
			}
		return ajax;
	}
	
	/**
	 * 根据部门编号获取该部门下所有成员的memberId字符串
	 * @param toparty
	 * @return memberIdStr
	 */
	@SuppressWarnings("unchecked")
	public static String getsendMessageUserIds() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select userId from tf_member where isleader = 0 and (department = '[2]' or department = '[3]' or department = '[4]' or department = '[5]')";				
		List<Object> userIdList = sqlhe.query(sql);//获取下一流程部门成员的memberId集合	
		String userIdStr = "";
		if(userIdList!=null && userIdList.size()>0) {
			for(int i=0;i<userIdList.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) userIdList.get(i);
				String id = String.valueOf(map.get("userId"));
				userIdStr += id+"|";					
			  }
			if(userIdStr.substring(userIdStr.length()-1).equals("|")){
				userIdStr = userIdStr.substring(0,userIdStr.length()-1);
			}
		}
		return userIdStr;
	}
	
	
	/**
	 * 2、抓取预警SN的详细维修信息(多次维修预警通知)
	 * @param ordermaking,warningTime,number,description
	 * @throws WexinReqException
	 * @throws UnsupportedEncodingException 
	 */
	@RequestMapping(value = "MultiSNWaringDetail")
	@Transactional
	@ResponseBody
	public AjaxJson MultiSNWaringDetail(HttpServletRequest request) throws WexinReqException, UnsupportedEncodingException{
		String productId = request.getParameter("productId");
		String ProductType = request.getParameter("ProductType");
		String MonitorId = request.getParameter("MonitorNo");

		if("".equals(MonitorId)) {
			MonitorId = "null";
		}
		List<String> DetailList=new ArrayList<String>();
		AjaxJson ajax = new AjaxJson();	

		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		String sql="";
		sql="select distinct b.LATE_WARING_DATE as LateWaringDate,\r\n" + 
				"				               b.SERV_COUNT as ServCount, \r\n" + 
				"								 a.PRODUCT_ID AS SN, \r\n" + 
				"								 a.MAKE_ROLE_NAME AS HostType,   \r\n" + 
				"								 a.PRODUCT_TYPE_CODE AS ServProductType,  \r\n" + 
				"								 a.MONITOR_NO AS MonitorNo,  \r\n" + 
				"								 a.SERV_DOC_ID as LateServID, \r\n" + 
				"								 a.FAULT_SERV_STATION_DESC as LateFaultDesc, \r\n" + 
				"				                a.MATERIEL_DESC as LateMaterialDesc, \r\n" + 
				"				                a.AREA_NAME as LateServAreaName, \r\n" + 
				"				                a.FIX_STATION_NAME as LateServStationName,   \r\n" + 
				"				                a.BUY_DATE as BuyDate,  \r\n" + 
				"				                a.CREATION_DATE as LateServCreationDate,  \r\n" + 
				"				                DATEDIFF ( Day, a.BUY_DATE, a.CREATION_DATE ) as HostUsingDay, \r\n" + 
				"				                a.SERVDOC_COMPLETE_DATE as LateServCompleteDate \r\n" + 
				"				FROM  tb_CSS_Data a,tbWaring_SN b  \r\n" + 
				"				where  a.PRODUCT_TYPE_CODE not in ('笔记本') and a.PRODUCT_ID=b.PRODUCT_ID  and a.PRODUCT_TYPE_CODE=b.PRODUCT_TYPE_CODE and a.MONITOR_NO=b.MONITOR_NO\r\n" + 
				"				and  a.SERV_DOC_ID=b.SERV_DOC_ID \r\n" + 
				"				and  a.PRODUCT_ID='"+productId+"'\r\n" + 
				"				and  b.PRODUCT_TYPE_CODE='"+ProductType+"'\r\n" + 
				"				and  b.MONITOR_NO='"+MonitorId+"' ";
		System.out.println(sql);
		List<Object> list1=sqlServerHelper.query(sql);
		
		//LateWaringDate	ServCount	SN	HostType	LateServID	LateFaultDesc	LateMaterialDesc	LateServAreaName	LateServStationName	BuyDate	LateServCreationDate	HostUsingDay	LateServCompleteDate
		for(int i=0;i<list1.size();i++) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) list1.get(i);
			String LateWaringDate = String.valueOf(map.get("LateWaringDate"));
			String ServCount = String.valueOf(map.get("ServCount"));
			String SN = String.valueOf(map.get("SN"));
			String HostType = String.valueOf(map.get("HostType"));
			String ServProductType = String.valueOf(map.get("ServProductType"));
			String MonitorNo = String.valueOf(map.get("MonitorNo"));
			String LateServID = String.valueOf(map.get("LateServID"));
			String LateFaultDesc = String.valueOf(map.get("LateFaultDesc"));
			String LateMaterialDesc = String.valueOf(map.get("LateMaterialDesc"));
			String LateServAreaName = String.valueOf(map.get("LateServAreaName"));
			String LateServStationName = String.valueOf(map.get("LateServStationName"));
			String BuyDate = String.valueOf(map.get("BuyDate"));
			String LateServCreationDate = String.valueOf(map.get("LateServCreationDate"));
			String HostUsingDay = String.valueOf(map.get("HostUsingDay"));
			String LateServCompleteDate = String.valueOf(map.get("LateServCompleteDate"));
			
			DetailList.add(LateWaringDate);
			DetailList.add(ServCount);
			DetailList.add(SN);
			DetailList.add(HostType);
			DetailList.add(ServProductType);
			DetailList.add(MonitorNo);
			DetailList.add(LateServID);
			DetailList.add(LateFaultDesc);
			DetailList.add(LateMaterialDesc);
			DetailList.add(LateServAreaName);
			DetailList.add(LateServStationName);
			DetailList.add(BuyDate);
			DetailList.add(LateServCreationDate);
			DetailList.add(HostUsingDay);
			DetailList.add(LateServCompleteDate);
		}
		System.out.println(DetailList);
		//System.out.println(ResultList);

		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("DetailList", DetailList);
		ajax.setBody(body);
		return ajax;

	}
	
	
	//得到本月的第一天
	public String getMonthFirstDay() {
	Calendar calendar = Calendar.getInstance();
	calendar.set(Calendar.DAY_OF_MONTH,
	calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
	SimpleDateFormat firstDay= new SimpleDateFormat("yyyy-MM-dd");
	return firstDay.format(calendar.getTime());
	}
	//得到本月的最后一天
	public String getMonthLastDay() {
	Calendar calendar = Calendar.getInstance();
	calendar.set(Calendar.DAY_OF_MONTH,
	calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
	SimpleDateFormat lastDay= new SimpleDateFormat("yyyy-MM-dd");
	return lastDay.format(calendar.getTime());
	}
	/** 
	* 根据当前日期获得上周周一和周日日期 
	*  
	* @return 
	* @author Caroline 
	*/  
	public static String getLastMonday() {  
		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	     Calendar calendar1 = Calendar.getInstance();      
	     int dayOfWeek = calendar1.get(Calendar.DAY_OF_WEEK) - 1;  
	     int offset1 = 1 - dayOfWeek;  
	     calendar1.add(Calendar.DATE, offset1 - 7);    
	     // System.out.println(sdf.format(calendar1.getTime()));// last Monday  
	     String lastBeginDate = sdf.format(calendar1.getTime());  
	     return lastBeginDate ;  
	} 
	public static String getLastSunday() {  
		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	     Calendar calendar1 = Calendar.getInstance();  
	     int dayOfWeek = calendar1.get(Calendar.DAY_OF_WEEK) - 1;  
	     int offset2 = 7 - dayOfWeek;  
	     calendar1.add(Calendar.DATE, offset2 - 7);  
	     // System.out.println(sdf.format(calendar2.getTime()));// last Sunday  
	     String lastEndDate = sdf.format(calendar1.getTime());  
	     return  lastEndDate;  
	} 
	/** 
	* 根据上月月份和上上月月份 ，以及上月的第一天和最后一天
	*  
	* @return 
	* @author Caroline 
	*/  
	public static String getLastMonth() {  
		Calendar c=Calendar.getInstance();
		c.add(Calendar.MONTH, -1);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
		String lastMonth = sdf.format(c.getTime()); //上月
		//System.out.println(lastMonth);
	     return lastMonth ;  
	} 
	public static String getLastBeforeMonth() {  
		Calendar c=Calendar.getInstance();
		c.add(Calendar.MONTH, -2);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
		String lastBeforeMonth = sdf.format(c.getTime()); //上月
		//System.out.println(lastMonth);
	     return lastBeforeMonth ;  
	} 

	/**
	 * 3、获取预警信息列表
	 * @param type,page
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findList(HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		System.out.println(page);
		String type = request.getParameter("type");
		AjaxJson ajax = new AjaxJson();			
		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		String sql = "";
		
		Calendar cal=Calendar.getInstance();    
	    int y=cal.get(Calendar.YEAR);    
	    int m=cal.get(Calendar.MONTH)+1;    
	    int d=cal.get(Calendar.DATE);  	    
		String today=y+"-"+m+"-"+d;
		
		String LastMonday = getLastMonday();
		String LastSunday = getLastSunday();
		
		String LastMonth = getLastMonth();
		String LastBeforeMonth = getLastBeforeMonth();
	
		if("1".equals(type)){//查询当天预警
			sql = " select top 10 PRODUCT_ID, PRODUCT_TYPE_CODE,MONITOR_NO, SERV_DOC_ID, AREA_NAME, FIX_STATION_NAME, SERV_COUNT, LATE_WARING_DATE \r\n" + 
					"FROM tbWaring_SN\r\n" + 
					"where PRODUCT_ID not in (\r\n" + 
					"	  SELECT top "+page+" PRODUCT_ID from tbWaring_SN\r\n" + 
					"	  WHERE LATE_WARING_DATE  BETWEEN '"+today+" 00:00:00.000' AND '"+today+" 23:59:59.000')\r\n" + 
					"AND LATE_WARING_DATE BETWEEN '"+today+" 00:00:00.000' AND '"+today+" 23:59:59.000' ORDER BY LATE_WARING_DATE DESC";
		}else if("2".equals(type)) {//查询当月预警
			sql = " select top 10 PRODUCT_ID, PRODUCT_TYPE_CODE,MONITOR_NO, SERV_DOC_ID, AREA_NAME, FIX_STATION_NAME, SERV_COUNT, LATE_WARING_DATE \r\n" + 
					"FROM tbWaring_SN\r\n" + 
					"where PRODUCT_ID not in (\r\n" +  
					"	  SELECT top "+page+" PRODUCT_ID from tbWaring_SN\r\n" + 
					"	  WHERE LATE_WARING_DATE  BETWEEN '"+getMonthFirstDay()+" 00:00:00.000' AND '"+getMonthLastDay()+" 23:59:59.000')\r\n" + 
					"AND LATE_WARING_DATE BETWEEN '"+getMonthFirstDay()+" 00:00:00.000' AND '"+getMonthLastDay()+" 23:59:59.000' ORDER BY LATE_WARING_DATE ";
		}else if("3".equals(type)){//查询全部预警记录
			 sql = "  SELECT SERVICE_STATION_ID as stationID, FIX_STATION_NAME as stationName,COUNT(*)  AS serverCount\r\n" + 
						"				FROM tbWaring_SN\r\n" + 
						"			    WHERE  LATE_WARING_DATE BETWEEN '"+LastMonday+" 00:00:00.000' AND '"+LastSunday+" 23:59:59.000'\r\n" + 
						"				GROUP BY SERVICE_STATION_ID,FIX_STATION_NAME \r\n" + 
						"				ORDER BY ServerCount DESC,FIX_STATION_NAME ASC";
				
		} else if("4".equals(type)){//查询全部预警记录
			sql = "   SELECT (CASE WHEN tableA.StationId IS NULL THEN  tableB.StationId ELSE tableA.StationId end) AS StationId,\r\n" + 
					"        (CASE WHEN tableA.StationName IS NULL THEN  tableB.StationName ELSE tableA.StationName end) AS StationName,\r\n" + 
					"        (CASE WHEN tableA.ServerCount IS NULL THEN 0 ELSE tableA.ServerCount end) AS LastMonth,\r\n" + 
					"         (CASE WHEN tableB.ServerCount IS NULL THEN 0 ELSE tableB.ServerCount end) AS LastBeforeMonth \r\n" + 
					"  FROM (\r\n" + 
					"	  SELECT StationId, StationName,ServerCount\r\n" + 
					"	  FROM tbWaring_MonthServerStation\r\n" + 
					"	  WHERE  Month='"+LastMonth+"' \r\n" + 
					"	  ) AS tableA\r\n" + 
					"  full JOIN\r\n" + 
					"  (\r\n" + 
					"	  SELECT StationId, StationName,ServerCount\r\n" + 
					"	  FROM tbWaring_MonthServerStation\r\n" + 
					"	  WHERE  Month ='"+LastBeforeMonth+"'\r\n" + 
					"     ) AS tableB\r\n" + 
					"  ON tableA.StationId=tableB.StationId\r\n" + 
					"  ORDER BY LastMonth desc";
				
			} 
		
		System.out.println(sql);
	
		List<String> listTime=new ArrayList<String>();
        listTime.add(LastMonday);
        listTime.add(LastSunday);
        listTime.add(LastMonth);
        listTime.add(LastBeforeMonth);
        System.out.println(listTime);
        
		List<Object> list = sqlServerHelper.query(sql);
		System.out.println(list);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("warningList", list);
		body.put("listTime", listTime);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 4、模糊搜索获取所有预警记录
	 * @param 
	 */
	@RequestMapping(value = "searchList")
	@ResponseBody
	public AjaxJson searchList(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		String sql = "  select * \r\n" + 
				"  FROM tbWaring_SN(NOLOCK) \r\n" + 
				"  WHERE PRODUCT_ID  LIKE '%"+searchName+"%'\r\n" + 
				"     or PRODUCT_TYPE_CODE LIKE '%"+searchName+"%'\r\n" + 
				"     or AREA_NAME like  '%"+searchName+"%'\r\n" + 
				"     or FIX_STATION_NAME like '%"+searchName+"%'\r\n" + 
				"     ORDER by PRODUCT_ID desc";
		
		List<Object> list = sqlServerHelper.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("warningList", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 5、获取预警信息统计数量
	 * @param memberId
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "findOwnCount")
	@ResponseBody
	public AjaxJson findOwnCount(HttpServletRequest request){
		AjaxJson ajax = new AjaxJson();			
		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		
		//查询个人的全部预警记录数量
		String sql0 = "";		
		Calendar cal=Calendar.getInstance();    
	    int y=cal.get(Calendar.YEAR);    
	    int m=cal.get(Calendar.MONTH)+1;    
	    int d=cal.get(Calendar.DATE);  	    
		String today=y+"-"+m+"-"+d;		
		sql0=" SELECT COUNT(*) as  count  \r\n" + 
				"  FROM dbo.tbWaring_SN\r\n" + 
				"  where LATE_WARING_DATE BETWEEN '"+today+" 00:00:00.000' AND '"+today+" 23:59:59.000' ";
		
		
		//查询个人的待处理的预警记录数量
		String sql1 = "";
		sql1=" SELECT COUNT(*) as  count \r\n" + 
				"  FROM dbo.tbWaring_SN\r\n" + 
				"  where LATE_WARING_DATE BETWEEN '"+getMonthFirstDay()+" 00:00:00.000' AND '"+getMonthLastDay()+" 23:59:59.000' ";
		
		
		List<Object> list0 = sqlServerHelper.query(sql0);
		List<Object> list1 = sqlServerHelper.query(sql1);
		//System.out.println(sql0);
		//System.out.println(sql1);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("count0", ((Map<String, Object>)list0.get(0)).get("count"));
		body.put("count1", ((Map<String, Object>)list1.get(0)).get("count"));

		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 6、周预警
	 * @param 
	 */
	@RequestMapping(value = "findWeekList")
	@ResponseBody
	public AjaxJson findWeekList(HttpServletRequest request){
		String LastMonday = request.getParameter("LastMonday");
		String LastSunday = request.getParameter("LastSunday");
		
		AjaxJson ajax = new AjaxJson();			
		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		String sql = "  SELECT SERVICE_STATION_ID as stationID, FIX_STATION_NAME as stationName,COUNT(*)  AS serverCount\r\n" + 
				"				FROM tbWaring_SN\r\n" + 
				"			    WHERE  LATE_WARING_DATE BETWEEN '"+LastMonday+" 00:00:00.000' AND '"+LastSunday+" 23:59:59.000'\r\n" + 
				"				GROUP BY SERVICE_STATION_ID,FIX_STATION_NAME \r\n" + 
				"				ORDER BY ServerCount DESC,FIX_STATION_NAME ASC";
		
		List<Object> list = sqlServerHelper.query(sql);
		System.out.println(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("WeekList", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 6、月对比预警
	 * @param 
	 */
	@RequestMapping(value = "findMonthList")
	@ResponseBody
	public AjaxJson findMonthList(HttpServletRequest request){
		String LastMonth = request.getParameter("LastMonth");
		String LastBeforeMonth = request.getParameter("LastBeforeMonth");
		
		AjaxJson ajax = new AjaxJson();			
		SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();
		String sql = "   SELECT (CASE WHEN tableA.StationId IS NULL THEN  tableB.StationId ELSE tableA.StationId end) AS StationId,\r\n" + 
				"        (CASE WHEN tableA.StationName IS NULL THEN  tableB.StationName ELSE tableA.StationName end) AS StationName,\r\n" + 
				"        (CASE WHEN tableA.ServerCount IS NULL THEN 0 ELSE tableA.ServerCount end) AS LastMonth,\r\n" + 
				"         (CASE WHEN tableB.ServerCount IS NULL THEN 0 ELSE tableB.ServerCount end) AS LastBeforeMonth \r\n" + 
				"  FROM (\r\n" + 
				"	  SELECT StationId, StationName,ServerCount\r\n" + 
				"	  FROM tbWaring_MonthServerStation\r\n" + 
				"	  WHERE  Month='"+LastMonth+"' \r\n" + 
				"	  ) AS tableA\r\n" + 
				"  full JOIN\r\n" + 
				"  (\r\n" + 
				"	  SELECT StationId, StationName,ServerCount\r\n" + 
				"	  FROM tbWaring_MonthServerStation\r\n" + 
				"	  WHERE  Month ='"+LastBeforeMonth+"'\r\n" + 
				"     ) AS tableB\r\n" + 
				"  ON tableA.StationId=tableB.StationId\r\n" + 
				"  ORDER BY LastMonth desc";
		
		List<Object> list = sqlServerHelper.query(sql);
		System.out.println(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("MonthList", list);
		ajax.setBody(body);
		return ajax;
	}
	
}
