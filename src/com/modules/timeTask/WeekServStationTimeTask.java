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
public class WeekServStationTimeTask {  
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getSaleQualitySecret();
	private static String AgentId = ConfigurationFileHelper.getSaleQualityAgentId();
	
	
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
	 *每周一早上九点，统计售后SN多次维修的服务站次数排名，推送到各部门经理及所有人。
	 * @throws WexinReqException 
	 * @throws ParseException 
	 */
    @SuppressWarnings({ "unchecked", "unused" })
    @Scheduled(cron = "0 0 9 ? * 1")
    public void sendUnfinishedException() throws WexinReqException, ParseException{  
    	SaleSqlServerHelper sqlServerHelper=new SaleSqlServerHelper();	
    	
    	String LastMonday=getLastMonday();
    	String LastSunday=getLastSunday();
    	
		String sql1 = "   SELECT top 5 SERVICE_STATION_ID as stationID, FIX_STATION_NAME as stationName,COUNT(*)  AS serverCount\r\n" + 
				"	FROM tbWaring_SN\r\n" + 
				"	WHERE  LATE_WARING_DATE BETWEEN '"+LastMonday+" 00:00:00.000' AND '"+LastSunday+" 23:59:59.000'\r\n" + 
				"	GROUP BY SERVICE_STATION_ID,FIX_STATION_NAME\r\n" + 
				"	 ORDER BY ServerCount DESC,FIX_STATION_NAME ASC";
		System.out.println(sql1);
		List<Object> list1 = sqlServerHelper.query(sql1);

		//发送消息给送有人
		String title2 = "服务站多次维修预警周统计"; 
		String stationName = "";
		String serverCount = "";
		String description = "上周次数                        服务站名称";
		Map<String, Object> map = null;
		for(int i=0;i<list1.size();i++) {
			map = (Map<String, Object>) list1.get(i);
			//String stationID = String.valueOf(map.get("stationID"));
			stationName = String.valueOf(map.get("stationName"));
			if(stationName.trim().length()>15) {
				stationName = stationName.substring(0, 14)+"...";
			}
			serverCount = String.valueOf(map.get("serverCount"));			
			description+="\n       "+serverCount+"           "+stationName ;
		}	
		description+="\n \n点击查看更多" ;
		//System.out.println(description);
		String access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
		sendMessageToManager(access_token,title2,"WuJingJing|WuQiFeng",description,LastMonday,LastSunday);
    }  
    
	/**
	 * 根据不同变量推送消息
	 * @param access_token
	 * @return 
	 * @throws WexinReqException 
	 */
	public static void sendMessageToManager(String access_token,String title,String touser,String description,String LastMonday,String LastSunday) throws WexinReqException {
		try {
			if(access_token!=null && !"".equals(access_token)) {											
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();
				//推送质量验证通知
				textcard.setTitle(title);
				textcard.setUrl(serviceUrl+"/TFMobile/webpage/customerService/WeekServStationList.html?LastSunday="+LastSunday+"&LastMonday="+LastMonday);
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
