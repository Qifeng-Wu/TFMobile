package org.jeewx.api.core.handler.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jeewx.api.core.annotation.ReqType;
import org.jeewx.api.core.exception.WexinReqException;
import org.jeewx.api.core.handler.WeiXinReqHandler;
import org.jeewx.api.core.req.model.WeixinReqConfig;
import org.jeewx.api.core.req.model.WeixinReqParam;
import org.jeewx.api.core.req.model.message.IndustryTemplateMessageSend;
import org.jeewx.api.core.req.model.message.TemplateDataKey;
import org.jeewx.api.core.req.model.message.TemplateMessage;
import org.jeewx.api.core.util.HttpRequestProxy;
import org.jeewx.api.core.util.WeiXinReqUtil;

import com.google.gson.Gson;

/**
 * 模板消息发送
 * @author sfli.sir
 *
 */
public class WeixinReqTemplateMessageHandler implements WeiXinReqHandler {

	private static Logger logger = Logger.getLogger(WeixinReqTemplateMessageHandler.class);
	
	@SuppressWarnings("rawtypes")
	public String doRequest(WeixinReqParam weixinReqParam) throws WexinReqException{
		// TODO Auto-generated method stub
		String strReturnInfo = "";
		if(weixinReqParam.getClass().isAnnotationPresent(ReqType.class)){
			ReqType reqType = weixinReqParam.getClass().getAnnotation(ReqType.class);
			WeixinReqConfig objConfig = WeiXinReqUtil.getWeixinReqConfig(reqType.value());
			if(objConfig != null){
				String reqUrl = objConfig.getUrl();
				IndustryTemplateMessageSend mc = (IndustryTemplateMessageSend) weixinReqParam;
				Map<String, String> parameters = new HashMap<String, String>();
				parameters.put("access_token", mc.getAccess_token());
				String jsonData = getMsgJson(mc) ;
				logger.info("处理模板消息"+jsonData);
				strReturnInfo = HttpRequestProxy.doJsonPost(reqUrl, parameters, jsonData);
			}
		}else{
			logger.info("没有找到对应的配置信息");
		}
		return strReturnInfo;
	}

	/**
	 * 单独处理 json信息
	 * @param name
	 * @param b
	 * @return
	 */
	private  String getMsgJson(IndustryTemplateMessageSend mc){
		
		StringBuffer json = new StringBuffer();
		Gson gson = new Gson();
		TemplateMessage tm = mc.getData();
		TemplateDataKey td=mc.getDataKey();
		String template_type=mc.getTemplate_type();
		
		mc.setData(null);
		mc.setTemplate_type(null);
		mc.setDataKey(null);
		
		String objJson = gson.toJson(mc);
		mc.setData(tm);
		json.append(objJson);
		json.setLength(json.length()-1);
		json.append(",");
		json.append("\"data\":{");
		if(template_type.equals("2")){//模板消息方式 2（通用）
			
			if(! td.getFirstDataKey().equals("") && ! td.getFirstDataKey().isEmpty() && td.getFirstDataKey()!=null){
				json.append("\""+td.getFirstDataKey()+"\":");
				json.append(gson.toJson(tm.getFirst()));
				json.append(",");
			}
			if( ! td.getKeyword1DataKey().equals("") && ! td.getKeyword1DataKey().isEmpty() && td.getKeyword1DataKey()!=null){
				json.append("\""+td.getKeyword1DataKey()+"\":");
				json.append(gson.toJson(tm.getkeynote1()));
				json.append(",");
			}
			if( ! td.getKeyword2DataKey().equals("") && ! td.getKeyword2DataKey().isEmpty() && td.getKeyword2DataKey()!=null){
				json.append("\""+td.getKeyword2DataKey()+"\":");
				json.append(gson.toJson(tm.getkeynote2()));
				json.append(",");
			}
			if( ! td.getKeyword3DataKey().equals("") && ! td.getKeyword3DataKey().isEmpty() && td.getKeyword3DataKey()!=null){
				json.append("\""+td.getKeyword3DataKey()+"\":");
				json.append(gson.toJson(tm.getkeynote3()));
				json.append(",");
			}
			if( ! td.getKeyword4DataKey().equals("") && ! td.getKeyword4DataKey().isEmpty() && td.getKeyword4DataKey()!=null){
				json.append("\""+td.getKeyword4DataKey()+"\":");
				json.append(gson.toJson(tm.getkeynote4()));
				json.append(",");
			}
			if( ! td.getKeyword5DataKey().equals("") && ! td.getKeyword5DataKey().isEmpty() && td.getKeyword5DataKey()!=null){
				json.append("\""+td.getKeyword5DataKey()+"\":");
				json.append(gson.toJson(tm.getkeynote5()));
				json.append(",");
			}
			if( ! td.getKeyword6DataKey().equals("") && ! td.getKeyword6DataKey().isEmpty() && td.getKeyword6DataKey()!=null){
				json.append("\""+td.getKeyword6DataKey()+"\":");
				json.append(gson.toJson(tm.getkeynote6()));
				json.append(",");
			}
			if( ! td.getRemarkDataKey().equals("") && ! td.getRemarkDataKey().isEmpty() && td.getRemarkDataKey()!=null){
				json.append("\""+td.getRemarkDataKey()+"\":");
				json.append(gson.toJson(tm.getRemark()));
			}
			
		}else if(template_type.equals("1")){//模板消息方式 1(每新增一个模板，需手动添加switch-cath方法)
			
			switch (mc.getTemplate_no()) {//模板编号
			
			case "OPENTM202521708"://汽车服务预约成功通知
				
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote3());
				json.append(" \"keyword3\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote4());
				json.append(" \"keyword4\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);
				
				break;
			case "OPENTM400905497"://预约提交成功通知
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote3());
				json.append(" \"keyword3\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);
				
				break;
				
			case "OPENTM405760897"://服务完成提醒
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote3());
				json.append(" \"keyword3\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);
				
				break;
			
			case "OPENTM405877971"://预约变更通知
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote3());
				json.append(" \"keyword3\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote4());
				json.append(" \"keyword4\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);
				break;
				
			case "OPENTM408260257"://预约失败通知
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote3());
				json.append(" \"keyword3\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);
				break;	
				
			case "OPENTM207850257"://维修保养提醒
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote3());
				json.append(" \"keyword3\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote4());
				json.append(" \"keyword4\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote5());
				json.append(" \"keyword5\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);			
				break;
				
			case "OPENTM410119156"://充值成功提醒
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);			
				break;
				
			case "OPENTM407744726"://待付款提醒
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote3());
				json.append(" \"keyword3\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);			
				break;
				
			case "OPENTM402074550"://支付成功通知
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote3());
				json.append(" \"keyword3\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote4());
				json.append(" \"keyword4\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);			
				break;
			case "OPENTM411720456"://审核失败
				objJson = gson.toJson(tm.getFirst());
				json.append(" \"first\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote1());
				json.append(" \"keyword1\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getkeynote2());
				json.append(" \"keyword2\":");
				json.append(objJson);
				json.append(",");
				objJson = gson.toJson(tm.getRemark());
				json.append(" \"remark\":");
				json.append(objJson);			
				break;
			
			default:
				break;
			}
			
		}
		
		
		
		json.append("}}");
		return json.toString();
	}
	
}
