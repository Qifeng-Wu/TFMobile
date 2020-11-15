package org.jeewx.api.wxsendmsg;

import java.util.SortedMap;
import java.util.TreeMap;

import org.jeewx.api.core.exception.WexinReqException;
import org.jeewx.api.core.req.WeiXinReqService;
import org.jeewx.api.core.req.model.message.IndustryTemplateAdd;
import org.jeewx.api.core.req.model.message.IndustryTemplateMessageSend;
import org.jeewx.api.core.req.model.message.IndustryTemplateSet;
import org.jeewx.api.core.util.WeiXinConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.modules.utils.CommUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 模板消息接口
 * 
 * @author lizr
 * 
 */
public class JwTemplateMessageAPI {

	private static Logger logger = LoggerFactory
			.getLogger(JwTemplateMessageAPI.class);

	/**
	 * 设置行业信息
	 * @param accessToken
	 * @param industry_id1
	 * @param industry_id2
	 * @return
	 * @throws WexinReqException
	 */
	public static String setIndustry(String accessToken,String industry_id1,String industry_id2) throws WexinReqException{
		IndustryTemplateSet s = new IndustryTemplateSet();
		s.setAccess_token(accessToken);
		s.setIndustry_id1(industry_id1);
		s.setIndustry_id2(industry_id2);
		JSONObject result = WeiXinReqService.getInstance().doWeinxinReqJson(s);
		String msg = result.getString(WeiXinConstant.RETURN_ERROR_INFO_MSG);
		return msg;
	}
	
	/**
	 * 添加模板信息
	 * @param accessToken
	 * @param template_id_short
	 * @return
	 * @throws WexinReqException
	 */
	public static String addTemplate(String accessToken,String template_id_short) throws WexinReqException{
		IndustryTemplateAdd t = new IndustryTemplateAdd();
		t.setAccess_token(accessToken);
		t.setTemplate_id_short(template_id_short);
		JSONObject result = WeiXinReqService.getInstance().doWeinxinReqJson(t);
		String msg = result.getString(WeiXinConstant.RETURN_ERROR_INFO_MSG);
		if("ok".equalsIgnoreCase(msg)){
			msg = result.getString("template_id");
		}
		
		return msg;
	}
	
	/**
	 * 发送客服模板消息
	 * @param industryTemplateMessageSend
	 * @return
	 * @throws WexinReqException
	 */
	public static String sendTemplateMsg(IndustryTemplateMessageSend industryTemplateMessageSend) throws WexinReqException{
		JSONObject result = WeiXinReqService.getInstance().doWeinxinReqJson(industryTemplateMessageSend);
		String msg = result.getString(WeiXinConstant.RETURN_ERROR_INFO_MSG);
		return msg;
	}
	
	public static void main(String[] args){
		String template_id = "8_cYM1-9gNyvSIrc0k5z0uYTDs8rfJriJx5qW3z6Cn0";
		String url = "";
		
		String firstDataKey = "first";
		String keyword1DataKey = "keyword1";
		String keyword2DataKey = "keyword2";
		String keyword3DataKey = "keyword3";
		String keyword4DataKey = "keyword4";
		String keyword5DataKey = "keyword5";
		String remarkDataKey = "remark";
		
		
		String firstContent = "firstContent";
		String keynoteContent1 = "keynoteContent1";
		String keynoteContent2 = "keynoteContent2";
		String keynoteContent3 = "keynoteContent3";
		String keynoteContent4 = "keynoteContent4";
		String keynoteContent5 = "keynoteContent5";
		String remarkContent = "请及时接单！";
		
		SortedMap<Object, Object> parameters = new TreeMap<Object, Object>();
		parameters.put("template_type", 2);
		parameters.put("template_id", template_id);
		parameters.put("fansId", 39);
		parameters.put("unicalId", 2);
		parameters.put("url", url);
		
		parameters.put("firstContent", firstContent);
		parameters.put("keynoteContent1", keynoteContent1);
		parameters.put("keynoteContent2", keynoteContent2);
		parameters.put("keynoteContent3", keynoteContent3);
		parameters.put("keynoteContent4", keynoteContent4);
		parameters.put("keynoteContent5", keynoteContent5);
		parameters.put("remarkContent", remarkContent);

		parameters.put("firstDataKey", firstDataKey);
		parameters.put("keyword1DataKey", keyword1DataKey);
		parameters.put("keyword2DataKey", keyword2DataKey);
		parameters.put("keyword3DataKey", keyword3DataKey);
		parameters.put("keyword4DataKey", keyword4DataKey);
		parameters.put("keyword5DataKey", keyword5DataKey);
		parameters.put("remarkDataKey", remarkDataKey);
		
		String jsonStr = JSONArray.fromObject(parameters).toString();
		// 去掉头尾“[]”
		jsonStr = jsonStr.substring(1, jsonStr.length() - 1);
		System.out.println(jsonStr);
		// 获取发送后接口返回数据
		String sendMsgUrl = "http://www.wx-vicmob.cn/VicmobMobile/setTemplateMessageUtil/setTemplateMessage.do";
		CommUtil.doPost(sendMsgUrl, jsonStr);
		
		
//		try {
//			String s = "ECuvdJegdSVSDuDR6a6kAh-2QR5dOCJbldBMAfbKe207MhDiGDhbTvznfagMVz07Mn5t8Bgr4CtEVHXrUgsx2PZ3KBzYO9NUd4EOcRWPgvGBGgohv5rpMn9Iz0hxOSLKPQEaADAHIW";
//			//JwTokenAPI.getAccessToken("wx00737224cb9dbc7d","b9479ebdb58d1c6b6efd4171ebe718b5");
//			System.out.println(s);
//			//"qQo8f2B0D0ZnlTP-8TKOMWoDcGiCoAhICn09S_QKxMgpSVp0VG8rgg_8PAJhy893z4lU-kY89DsZAsC3M54zxQBxuwTehg2nC_dO75VEGqw";
//			//JwTokenAPI.getAccessToken("wx00737224cb9dbc7d","b9479ebdb58d1c6b6efd4171ebe718b5");
//			IndustryTemplateMessageSend industryTemplateMessageSend = new IndustryTemplateMessageSend();
//			industryTemplateMessageSend.setAccess_token(s);
//			industryTemplateMessageSend.setTemplate_id("DGZNiinbgkmr6o7vm4kFe2lw7iaDflvfN5TUo1lVylQ");
//			industryTemplateMessageSend.setTouser("oOCrjv2e1KgFp0Yf-OLB84U889tI");
//			industryTemplateMessageSend.setUrl("www.baidu.com");
//			industryTemplateMessageSend.setTopcolor("#ffAADD");
//			TemplateMessage data = new TemplateMessage();
//			TemplateData first = new TemplateData();
//			first.setColor("#173177");
//			first.setValue("恭喜你购买成2323功！");
//			
//			
//			TemplateData keynote1= new TemplateData();
//			keynote1.setColor("#173177");
//			keynote1.setValue("巧克22力");
//			
//			TemplateData keynote2= new TemplateData();
//			keynote2.setColor("39.8元");
//			keynote2.setValue("恭喜你购买成功！");
//			
//			TemplateData keynote3= new TemplateData();
//			keynote3.setColor("#173177");
//			keynote3.setValue("2014年9月16日");
//			
//			TemplateData remark= new TemplateData();
//			remark.setColor("#173177");
//			remark.setValue("欢迎再次购买！");
//			data.setFirst(first);
//			data.setkeynote1(keynote1);
//			data.setkeynote2(keynote2);
//			data.setkeynote3(keynote3);
//			data.setRemark(remark);
//			industryTemplateMessageSend.setData(data);
//			
//			s  = JwTemplateMessageAPI.sendTemplateMsg(industryTemplateMessageSend);
//			
//			System.out.println(s);
			// 4m3vrpiSA-CPyL9YqHw2jKDlZSX6Sz65SoMKvA9BV1s
			
			
//		} catch (WexinReqException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
}
