package com.modules.timeTask;  
  
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.jeewx.api.core.exception.WexinReqException;
import org.springframework.scheduling.annotation.Scheduled;  
import org.springframework.stereotype.Component;

import com.modules.db.SaleSqlServerHelper;
import com.modules.entity.message.Textcard;
import com.modules.entity.message.TextcardMessage;
import com.modules.utils.ConfigurationFileHelper;
import com.modules.utils.WxCommonAPI;

import net.sf.json.JSONObject;  
  
@Component  
public class MonthServStationTimeTask {  
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getSaleQualitySecret();
	private static String AgentId = ConfigurationFileHelper.getSaleQualityAgentId();
	
	
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
	public static  String getLastMonthFirstDay(){
		 //获取前一个月第一天
        Calendar calendar1 = Calendar.getInstance();
        calendar1.add(Calendar.MONTH, -1);
        calendar1.set(Calendar.DAY_OF_MONTH,1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String firstDay = sdf.format(calendar1.getTime());
        return firstDay ;  
	}
	public static  String getLastMonthLastDay(){
		 //获取前一个月的最后一天
		Calendar calendar2 = Calendar.getInstance();
        calendar2.set(Calendar.DAY_OF_MONTH, 0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String lastDay = sdf.format(calendar2.getTime());
        return lastDay ;  
	}
	/**
	 *每月，统计售后SN多次维修的服务站次数排名，推送到各部门经理及所有人。
	 * @throws WexinReqException 
	 * @throws ParseException 
	 */
    @SuppressWarnings({ "unchecked", "unused" })
	//@Scheduled(cron = "0 0 8 ? * 1#1")//每月第一个星期的星期一早上9:00
    @Scheduled(cron = "0 0 9 1 * ?")
    public void sendUnfinishedException() throws WexinReqException, ParseException{  
    	SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();	
    	
    	String lastMonth=getLastMonth();
    	String lastBeforeMonth=getLastBeforeMonth();
    	String lastMonthFirstDay=getLastMonthFirstDay();
    	String lastMonthLastDay=getLastMonthLastDay();
    	
    	System.out.println(lastMonth);
    	System.out.println(lastBeforeMonth);
    	
    	//判断上个月的服务站多次维修统计数据是否导入tbWaring_MonthServerStation表中
    	String sqltemp=" SELECT * FROM tbWaring_MonthServerStation WHERE Month='"+lastMonth+"'";
    	List<Object> listtemp = sqlServerHelper.query(sqltemp);
    	if(listtemp.size()==0) {
    		String sql=" insert into tbWaring_MonthServerStation \r\n" + 
        			"  SELECT '"+lastMonth+"',SERVICE_STATION_ID AS StationId,FIX_STATION_NAME AS StationName,COUNT(*) AS ServerCount\r\n" + 
        			"  FROM dbo.tbWaring_SN\r\n" + 
        			"  WHERE LATE_WARING_DATE BETWEEN '"+lastMonthFirstDay+" 00:00:00.000' AND '"+lastMonthLastDay+" 23:59:59.000'\r\n" + 
        			"  GROUP BY SERVICE_STATION_ID,FIX_STATION_NAME\r\n" + 
        			"  ORDER BY ServerCount DESC,StationName ASC ";
        	System.out.println(sql);
    		List<Object> list = sqlServerHelper.query(sql);	
    	}
		String sql1 = "SELECT top 5 (CASE WHEN tableA.StationName IS NULL THEN  tableB.StationName ELSE tableA.StationName end) AS StationName,\r\n" + 
				"         (CASE WHEN tableB.ServerCount IS NULL THEN 0 ELSE tableB.ServerCount end) AS LastMonth,\r\n" + 
				"         (CASE WHEN tableA.ServerCount IS NULL THEN 0 ELSE tableA.ServerCount end) AS LastBeforeMonth\r\n" + 
				"  FROM (\r\n" + 
				"	  SELECT StationId, StationName,ServerCount\r\n" + 
				"	  FROM tbWaring_MonthServerStation\r\n" + 
				"	  WHERE  Month='"+lastBeforeMonth+"' \r\n" + 
				"	  ) AS tableA\r\n" + 
				"  full JOIN\r\n" + 
				"  (\r\n" + 
				"	  SELECT StationId, StationName,ServerCount\r\n" + 
				"	  FROM tbWaring_MonthServerStation\r\n" + 
				"	  WHERE  Month ='"+lastMonth+"'\r\n" + 
				"     ) AS tableB\r\n" + 
				"  ON tableA.StationId=tableB.StationId\r\n" + 
				"  ORDER BY LastMonth desc";
		System.out.println(sql1);
		List<Object> list1 = sqlServerHelper.query(sql1);

		System.out.println(list1);
		
		//发送消息给送有人
		String title2 = "服务站多次维修预警月报表"; 
		String stationName = "";
		String LastMonthserverCount = "";
		String LastJBeforeMonthserverCount = "";
		String description = "         服务站名称                   "+lastMonth+"   "+lastBeforeMonth;
		Map<String, Object> map = null;
		for(int i=0;i<list1.size();i++) {
			map = (Map<String, Object>) list1.get(i);
			stationName = String.valueOf(map.get("StationName")).trim();
			if(stationName.trim().length()>12) {
				stationName = stationName.substring(0, 11)+"...";
			}
			else {
				int length=stationName.trim().length();
				String temp="";
				for(int j=0;j<11-length;j++) {
					temp +="     ";
				}
				System.out.println(stationName.length());
				stationName=stationName.trim().toString()+temp;
				System.out.println(stationName.length());
			}
			LastMonthserverCount = String.valueOf(map.get("LastMonth"));	
			LastJBeforeMonthserverCount = String.valueOf(map.get("LastBeforeMonth"));
			description+="\n"+stationName+"        "+LastMonthserverCount+"              "+LastJBeforeMonthserverCount ;
		}	
		description+="\n \n点击查看更多" ;
		//System.out.println(description);
		String access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
		sendMessageToManager(access_token,title2,"WuJingJing|WuQiFeng",description,lastMonth,lastBeforeMonth);
    }  
    
	/**
	 * 根据不同变量推送消息
	 * @param access_token
	 * @return 
	 * @throws WexinReqException 
	 */
	public static void sendMessageToManager(String access_token,String title,String touser,String description,String LastMonth,String LastBeforeMonth) throws WexinReqException {
		try {
			if(access_token!=null && !"".equals(access_token)) {											
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();
				//推送质量验证通知
				textcard.setTitle(title);
				textcard.setUrl(serviceUrl+"/TFMobile/webpage/customerService/MonthServStationList.html?LastMonth="+LastMonth+"&LastBeforeMonth="+LastBeforeMonth);
				textcard.setDescription(description);
				textcardMessage.setTouser(touser);
				textcardMessage.setToparty("5|13");
				textcardMessage.setMsgtype("textcard");
				textcardMessage.setAgentid(Integer.parseInt(AgentId));
				textcardMessage.setTextcard(textcard);
				JSONObject result = WxCommonAPI.sendMssage(access_token, textcardMessage);
				if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
				}else {			
					throw new WexinReqException("发送消息！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
				}			
			}
			
		} catch (Exception e) {
			throw new WexinReqException("发送消息出错啦！");
		}
	}
	
	


}
