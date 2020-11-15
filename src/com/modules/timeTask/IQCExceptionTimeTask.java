package com.modules.timeTask;  
  
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.jeewx.api.core.exception.WexinReqException;
import org.springframework.scheduling.annotation.Scheduled;  
import org.springframework.stereotype.Component;
import com.modules.db.SQLHelper;
import com.modules.entity.message.Textcard;
import com.modules.entity.message.TextcardMessage;
import com.modules.utils.ConfigurationFileHelper;
import com.modules.utils.WxCommonAPI;

import net.sf.json.JSONObject; 
  
@Component  
public class IQCExceptionTimeTask {  
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getIQCExceptionSecret();
	private static String AgentId = ConfigurationFileHelper.getIQCExceptionAgentId();
	/**
	 *每天凌00:07改变物料异常通报为未会签完状态
	 * @throws WexinReqException 
	 * @throws ParseException 
	 */
	//@Scheduled(cron = "0 0 7 * * ?")  
	//@Scheduled(cron = "0 0 0/1 * * ?")  //每隔两小时执行一次0 0 0/2 * * ? 
	@Scheduled(cron = "0 30 16 * * ?")  //每天下午4:30统计未完成的通报，推送到各部门经理及所有人。
    public void updateException(){  
    	SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_iqc_exception where deleteFlag = 0 and (state = 2 or state = 3) and HOUR(timediff(now(),reportTime))>24";//待会签状态
		//String updatesql = "update tf_iqc_exception set state = 4, nextPerson = '64' where deleteFlag = 0 and (state = 2 or state = 3) and HOUR(timediff(now(),handleTime))>24";//待会签状态
		List<Object> list = sqlhe.query(sql);
		if(list!=null && list.size()>0) {
			String serialNumbers = "";
			for(int i=0;i<list.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				serialNumbers += String.valueOf(map.get("serialNumber"))+",";
			}
			if(serialNumbers.substring(serialNumbers.length()-1).equals(",")){
				serialNumbers = serialNumbers.substring(0,serialNumbers.length()-1);
			}
			
			//boolean isSuccess = sqlhe.update(updatesql);//跟新
			//if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//推送IQC处理结果处理
						textcard.setTitle("物料异常通报IQC处理通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionList.html");
						//textcard.setDescription("文件序号 ："+serialNumbers+" 会签已超时\n待您IQC验证处理 ，请及时处理，点击查看详情！");
						textcard.setDescription("文件序号 ："+serialNumbers+" 会签已超时\n ，请您及时跟进会签处理，点击查看详情！");
						textcardMessage.setTouser("NanJuXianSheng|XieJianMei");//"NanJuXianSheng|WuQiFeng"
						textcardMessage.setToparty("");
						textcardMessage.setMsgtype("textcard");
						textcardMessage.setAgentid(Integer.parseInt(AgentId));
						textcardMessage.setTextcard(textcard);
						JSONObject result = WxCommonAPI.sendMssage(access_token, textcardMessage);
						if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
							
						}else {
							
						}			
					}
					
				} catch (Exception e) {
	
				}
			//}
		}  
	}	
}
