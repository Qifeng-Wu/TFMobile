package com.modules.warning;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

import com.modules.db.ProductionSqlServerHelper;
import com.modules.entity.message.Textcard;
import com.modules.entity.message.TextcardMessage;
import com.modules.utils.AjaxJson;
import com.modules.utils.CommUtil;
import com.modules.utils.ConfigurationFileHelper;
import com.modules.utils.WxCommonAPI;

import net.sf.json.JSONObject;

/**
 * 获取订单信息
 * @author 
 */
@RestController
@RequestMapping(value = "tf-api/GetOrderInfo")
public class OrderInfoAPI {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getWarningSecret();
	private static String AgentId = ConfigurationFileHelper.getWarningAgentId();
	
   /**
	 * 1、推送文本卡片消息(发送预警提醒)
	 * @param ordernum
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "OrderInfo")
	@Transactional
	@ResponseBody
	public AjaxJson OrderInfo(HttpServletRequest request) throws WexinReqException{
		String ordernum = request.getParameter("ordernum");//
		AjaxJson ajax = new AjaxJson();	
		if(ordernum==null||"".equals(ordernum)){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}

		ProductionSqlServerHelper sqlhe = new ProductionSqlServerHelper();		
		String sql = "select * from tborderex where OrderNumber='"+ordernum+"'";
		List<Object> List = sqlhe.query(sql);

		if (List!=null && List.size()>0) {
			LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
			body.put("orderList", List);
			ajax.setBody(body);
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("无此订单信息！");
			ajax.setErrorCode("-1");
		}

		return ajax;
	}
	
	
}
