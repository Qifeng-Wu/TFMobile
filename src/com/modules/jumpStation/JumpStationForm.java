package com.modules.jumpStation;


import java.util.HashMap;
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
import com.modules.entity.message.Textcard;
import com.modules.entity.message.TextcardMessage;
import com.modules.utils.AjaxJson;
import com.modules.utils.CommUtil;
import com.modules.utils.ConfigurationFileHelper;
import com.modules.utils.WxCommonAPI;

import net.sf.json.JSONObject;

/**
 * SFC跳站
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/jumpStation")
public class JumpStationForm {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getJumpStationSecret();
	private static String AgentId = ConfigurationFileHelper.getJumpStationAgentId();
	
	/**
	 * 0、通过主键获取记录信息
	 * @param exceptionId
	 */
	@RequestMapping(value = "findInfo")
	@Transactional
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		Integer jumpId = Integer.parseInt(request.getParameter("jumpId"));
			
		AjaxJson ajax = new AjaxJson();	
		if(jumpId==null||"".equals(request.getParameter("jumpId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_jump_station where jumpId = " + jumpId;
		List<Object> list = sqlhe.query(sql);
		Object jumpStation = null;
		if(list!=null && list.size()>0) {
			jumpStation = list.get(0);
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("jumpStation", jumpStation);
		ajax.setBody(body);
		return ajax;
	}
	
   /**
	 * 1、填写跳站信息发布
	 * @param 跳站信息
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "submit")
	@Transactional
	@ResponseBody
	public AjaxJson submit(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		Integer memberId = Integer.parseInt(request.getParameter("submitMan"));
		String submitManName = request.getParameter("submitManName");
		String ordermaking = request.getParameter("ordermaking");
		String sn = request.getParameter("sn");		
		String toStation = request.getParameter("toStation");
		String presentStation = request.getParameter("presentStation");
		String reason = request.getParameter("reason");

		AjaxJson ajax = new AjaxJson();	
		if(memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String touser = getManagerUserIds(department);//根据部门号找到对应经理领导发送消息
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String pendingPerson = getManagerMemberIds(department);//获取下一流程处理者
			
		String sql = "INSERT INTO tf_jump_station (state,engineeringSignFlag,qualitySignFlag,pendingPerson,ordermaking,sn,toStation,presentStation,reason,submitMan,submitTime)" + 
					 " VALUES"
					 +" (1,0,0,'"+pendingPerson+"','"+ordermaking+"','"+sn+"','"+toStation+"','"+presentStation+"','"+reason+"',"+memberId+",now())";
		String jumpId = String.valueOf(CommUtil.insertAndBackId(sql));
		if(jumpId!=null && !"".equals(jumpId)){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(reason.length()>38) {
						reason = reason.substring(0, 37)+"...";
					}
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("SFC跳站申请通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/jumpStation/jumpStationConfirmation.html?jumpId="+jumpId);
					textcard.setDescription("制令单号："+ordermaking+"        发文人："+submitManName+"\n当前站点："+presentStation+"           跳至站点："+toStation+"\nSN 编 码："+sn+"\n跳站原因："+reason+"\n待您审核 ，请及时处理，点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty("");
					textcardMessage.setMsgtype("textcard");
					textcardMessage.setAgentid(Integer.parseInt(AgentId));
					textcardMessage.setTextcard(textcard);
					JSONObject result = WxCommonAPI.sendMssage(access_token, textcardMessage);
					if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
						ajax.setSuccess(true);
						ajax.setMessage("成功！");
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
		}else{
			ajax.setSuccess(false);
			ajax.setMessage("数据保存失败！");
			ajax.setErrorCode("-2");
		}
		return ajax;
	}
	/**
	 * 2、编辑修改跳站信息发布
	 * @param 跳站信息
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "edit")
	@Transactional
	@ResponseBody
	public AjaxJson edit(HttpServletRequest request) throws WexinReqException{
		String jumpId = request.getParameter("jumpId");
		String department = request.getParameter("department");
		String submitManName = request.getParameter("submitManName");
		String ordermaking = request.getParameter("ordermaking");
		String sn = request.getParameter("sn");		
		String toStation = request.getParameter("toStation");
		String presentStation = request.getParameter("presentStation");
		String reason = request.getParameter("reason");
		
		AjaxJson ajax = new AjaxJson();	
		if(jumpId==null||jumpId.isEmpty()){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String touser = getManagerUserIds(department);//根据部门号找到对应经理领导发送消息
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		SQLHelper sqlhe = new SQLHelper();	
		String sql = "UPDATE tf_jump_station SET ordermaking='"+ordermaking+"',sn = '"+sn+"',presentStation = '"+presentStation+
				 "',toStation='"+toStation+"',reason='"+reason+"',submitTime = now() where jumpId = " + Integer.parseInt(jumpId);
		boolean isSuccess = sqlhe.update(sql);
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(reason.length()>38) {
						reason = reason.substring(0, 37)+"...";
					}
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("SFC跳站信息修改通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/jumpStation/jumpStationConfirmation.html?jumpId="+jumpId);
					textcard.setDescription("制令单号："+ordermaking+"           发文人："+submitManName+"\n当前站点："+presentStation+"           跳至站点："+toStation+"\nSN 编 码："+sn+"\n跳站原因："+reason+"\n发文已修改跳站信息，点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty("");
					textcardMessage.setMsgtype("textcard");
					textcardMessage.setAgentid(Integer.parseInt(AgentId));
					textcardMessage.setTextcard(textcard);
					JSONObject result = WxCommonAPI.sendMssage(access_token, textcardMessage);
					if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
						ajax.setSuccess(true);
						ajax.setMessage("成功！");
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
		}else{
			ajax.setSuccess(false);
			ajax.setMessage("数据保存失败！");
			ajax.setErrorCode("-2");
		}
		return ajax;
	}
	
	/**
	 * 2、发文部门（生产部）经理审核
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "submitConfirm")
	@Transactional
	@ResponseBody
	public AjaxJson submitConfirm(HttpServletRequest request) throws WexinReqException{
		String reason = request.getParameter("reason");
		String sn = request.getParameter("sn");
		String toStation = request.getParameter("toStation");
		String presentStation = request.getParameter("presentStation");
		String ordermaking = request.getParameter("ordermaking");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer jumpId = Integer.parseInt(request.getParameter("jumpId"));

		AjaxJson ajax = new AjaxJson();	
		if(jumpId==null||"".equals(request.getParameter("jumpId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//获取下一步操作人员memberId
		String pendingPerson = "";
		//1.获取工程部人员
		String  engineeringPerson = getEngineeringHandlerMemberId();
		//2.获取质控部人员
		String  qualityPerson = getQualityHandlerMemberId();
		
		if(engineeringPerson==null||engineeringPerson.isEmpty()){
			ajax.setSuccess(false);
			ajax.setMessage("未设置工程部相关人员，审核失败！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		if(qualityPerson==null||qualityPerson.isEmpty()){
			ajax.setSuccess(false);
			ajax.setMessage("未设置质控部相关人员，审核失败！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		pendingPerson = engineeringPerson+","+qualityPerson;
		
		String touser = "";	
		//获取相关人员的UserId
		if(pendingPerson!=null&&!pendingPerson.isEmpty()) {
			String pendingPersonArry[] = pendingPerson.split(",");
			for(String person:pendingPersonArry) {
				String userId = getUserIdByMemberId(Integer.parseInt(person));
				touser += userId +"|";
			}
		}
						
		SQLHelper sqlhe = new SQLHelper();				
		String sql = "UPDATE tf_jump_station SET state="+state+",pendingPerson='"+pendingPerson+"',submitLeader = "+ memberId+", submitLeaderTime = now() where jumpId = " + jumpId;
		boolean isSuccess = sqlhe.update(sql);

		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(reason.length()>38) {
						reason = reason.substring(0, 37)+"...";
					}
												
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					//1.发送消息给工程部和质控部相关人员会签处理
					textcard.setTitle("SFC跳站会签确认通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/jumpStation/jumpStationConfirmation.html?jumpId="+jumpId);
					textcard.setDescription("制令单号："+ordermaking+"\n当前站点："+presentStation+"           跳至站点："+toStation+"\nSN 编 码："+sn+"\n跳站原因："+reason+"\n待您会签处理 ，请及时处理，点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty("");
					textcardMessage.setMsgtype("textcard");
					textcardMessage.setAgentid(Integer.parseInt(AgentId));
					textcardMessage.setTextcard(textcard);
					JSONObject result = WxCommonAPI.sendMssage(access_token, textcardMessage);
					if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
						ajax.setSuccess(true);
						ajax.setMessage("成功！");
					}else {
						ajax.setSuccess(false);
						ajax.setMessage("消息发送失败！");
						ajax.setErrorCode("-1");
					}
				
				//2.发送消息通知所有人				
					textcard.setTitle("SFC跳站申请发布通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/jumpStation/jumpStationConfirmation.html?jumpId="+jumpId);
					textcard.setDescription("制令单号："+ordermaking+"\n当前站点："+presentStation+"           跳至站点："+toStation+"\nSN 编 码："+sn+"\n跳站原因："+reason+"\n点击查看详情！");
					textcardMessage.setTouser("@all");
					textcardMessage.setToparty("");
					textcardMessage.setMsgtype("textcard");
					textcardMessage.setAgentid(Integer.parseInt(AgentId));
					textcardMessage.setTextcard(textcard);
					JSONObject result1 = WxCommonAPI.sendMssage(access_token, textcardMessage);
					if (result1.has("errcode") && result1.getString("errcode").equals("0") && result1.getString("errmsg").equals("ok")) {
						ajax.setSuccess(true);
						ajax.setMessage("成功！");
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
		}else{
			ajax.setSuccess(false);
			ajax.setMessage("数据保存失败！");
			ajax.setErrorCode("-2");
		}
		return ajax;
	}
	/**
	 * 3、工程部会签处理
	 * @throws WexinReqException
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "engineeringHandle")
	@Transactional
	@ResponseBody
	public AjaxJson engineeringHandle(HttpServletRequest request) throws WexinReqException{
		String engineeringHandlerNote = request.getParameter("engineeringHandlerNote");
		String reason = request.getParameter("reason");
		String sn = request.getParameter("sn");
		String toStation = request.getParameter("toStation");
		String presentStation = request.getParameter("presentStation");
		String ordermaking = request.getParameter("ordermaking");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer jumpId = Integer.parseInt(request.getParameter("jumpId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(jumpId==null||"".equals(request.getParameter("jumpId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//获取下一步操作人员memberId
		String engineeringLeader = getEngineeringLeaderMemberId();//获取工程部审核人员memberId
		if(engineeringLeader==null || engineeringLeader.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("未设置部门审核人员，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		
		SQLHelper helper = new SQLHelper();
		//解析下一流程处理人
		String pendingPerson = "";
		String sql1 = "select * from tf_jump_station where jumpId = " + jumpId;
		List<Object> list = helper.query(sql1);
		Map<String, Object> map = new HashMap<String, Object>();
		if (list!=null && list.size()>0) {
			map = (Map<String, Object>) list.get(0);
			String pendingPersons[] = String.valueOf(map.get("pendingPerson")).split(",");
			String engineeringHandlerArray[] = getEngineeringHandlerMemberId().split(",");
			for(int i=0;i<pendingPersons.length;i++) {
				boolean pass = true;
				for(int j=0;j<engineeringHandlerArray.length;j++) {
					if(pendingPersons[i].equals(engineeringHandlerArray[j])) {
						pass= false;
						break;
					}
				} 
				if(pass) {
					pendingPerson = pendingPerson+pendingPersons[i]+",";
				}
			} 
			pendingPerson = pendingPerson + engineeringLeader;
			if(!"".equals(pendingPerson)&&pendingPerson.substring(pendingPerson.length()-1).equals(",")){
				pendingPerson = pendingPerson.substring(0,pendingPerson.length()-1);
			} 
			
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，该条信息不存在！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		
	    int engineeringSignFlag = 1;//工程已会签处理
		String sql = "UPDATE tf_jump_station SET state="+state+",engineeringSignFlag="+engineeringSignFlag+",pendingPerson='"+pendingPerson+"',engineeringHandlerNote='"+engineeringHandlerNote+"',engineeringHandler = "+ memberId+", engineeringHandlerTime = now() where jumpId = " + jumpId;
		boolean isSuccess = helper.update(sql);
			if(isSuccess){
				String access_token = "";
				String touser = getEngineeringLeaderMemberId();//获取工程部审核人员userId来推送消息
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						if(reason.length()>38) {
							reason = reason.substring(0, 37)+"...";
						}
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//发送消息通知所有人				
						textcard.setTitle("SFC跳站会签审核通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/jumpStation/jumpStationConfirmation.html?jumpId="+jumpId);
						textcard.setDescription("制令单号："+ordermaking+"\n当前站点："+presentStation+"           跳至站点："+toStation+"\nSN 编 码："+sn+"\n跳站原因："+reason+"\n待您审核，请及时处理！");
						textcardMessage.setTouser(touser);
						textcardMessage.setToparty("");
						textcardMessage.setMsgtype("textcard");
						textcardMessage.setAgentid(Integer.parseInt(AgentId));
						textcardMessage.setTextcard(textcard);
						JSONObject result1 = WxCommonAPI.sendMssage(access_token, textcardMessage);
						if (result1.has("errcode") && result1.getString("errcode").equals("0") && result1.getString("errmsg").equals("ok")) {
							ajax.setSuccess(true);
							ajax.setMessage("成功！");
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
			}else{
				ajax.setSuccess(false);
				ajax.setMessage("数据保存失败！");
				ajax.setErrorCode("-2");
			}	
				
		return ajax;
	}
	/**
	 * 4、工程部会签审核
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "engineeringConfirm")
	@Transactional
	@ResponseBody
	public AjaxJson engineeringConfirm(HttpServletRequest request) throws WexinReqException{
		String reason = request.getParameter("reason");
		String sn = request.getParameter("sn");
		String toStation = request.getParameter("toStation");
		String presentStation = request.getParameter("presentStation");
		String ordermaking = request.getParameter("ordermaking");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer jumpId = Integer.parseInt(request.getParameter("jumpId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(jumpId==null||"".equals(request.getParameter("jumpId"))||memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
			
		SQLHelper helper = new SQLHelper();
		String sql1 = "select * from tf_jump_station where jumpId = " + jumpId;
		List<Object> list = helper.query(sql1);
		Integer qualitySignFlag = 0;
		if (list!=null && list.size()>0) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) list.get(0);
			String flag = String.valueOf(map.get("qualitySignFlag"));//获取质控部门会签状态
			if(flag!=null && flag!="null" && !flag.isEmpty()) {
				qualitySignFlag = Integer.parseInt(flag);
			}
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，该条信息不存在！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		
		String pendingPerson = "";//待处理人
		int engineeringSignFlag = 2;//工程部门已会签审核	
		if(qualitySignFlag==2) {//1.质控已会签审核
			pendingPerson = "";//清空待处理人
			int state = 0;//代表完结
			String sql = "UPDATE tf_jump_station SET state="+state+",engineeringSignFlag="+engineeringSignFlag+",pendingPerson='"+pendingPerson+"',engineeringLeader = "+memberId+", engineeringLeaderTime = now() where jumpId = " + jumpId;
			boolean isSuccess = helper.update(sql);
			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						if(reason.length()>38) {
							reason = reason.substring(0, 37)+"...";
						}
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//发送消息通知所有人				
						textcard.setTitle("SFC跳站签核流程完结通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/jumpStation/jumpStationConfirmation.html?jumpId="+jumpId);
						textcard.setDescription("制令单号："+ordermaking+"\n当前站点："+presentStation+"           跳至站点："+toStation+"\nSN 编 码："+sn+"\n跳站原因："+reason+"\n点击查看详情！");
						textcardMessage.setTouser("@all");
						textcardMessage.setToparty("");
						textcardMessage.setMsgtype("textcard");
						textcardMessage.setAgentid(Integer.parseInt(AgentId));
						textcardMessage.setTextcard(textcard);
						JSONObject result1 = WxCommonAPI.sendMssage(access_token, textcardMessage);
						if (result1.has("errcode") && result1.getString("errcode").equals("0") && result1.getString("errmsg").equals("ok")) {
							ajax.setSuccess(true);
							ajax.setMessage("成功！");
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
			}else{
				ajax.setSuccess(false);
				ajax.setMessage("数据保存失败！");
				ajax.setErrorCode("-2");
			}	
			
		}else if(qualitySignFlag==1){//2.质控已会签处理但未审核		
			pendingPerson = getQualityLeaderMemberId();//待质控部领导审核
			String sql = "UPDATE tf_jump_station SET engineeringSignFlag="+engineeringSignFlag+",pendingPerson='"+pendingPerson+"',engineeringLeader = "+memberId+", engineeringLeaderTime = now() where jumpId = " + jumpId;
			boolean isSuccess = helper.update(sql);
			if(!isSuccess){
				ajax.setSuccess(false);
				ajax.setMessage("数据保存失败！");
				ajax.setErrorCode("-2");
			}
		}else {//3.质控未会签
			pendingPerson = getQualityHandlerMemberId();//待质控部人员处理
			String sql = "UPDATE tf_jump_station SET engineeringSignFlag="+engineeringSignFlag+",pendingPerson='"+pendingPerson+"',engineeringLeader = "+memberId+", engineeringLeaderTime = now() where jumpId = " + jumpId;
			boolean isSuccess = helper.update(sql);
			if(!isSuccess){
				ajax.setSuccess(false);
				ajax.setMessage("数据保存失败！");
				ajax.setErrorCode("-2");
			}
		}	
		return ajax;
	}
	/**
	 * 5、质量控制部会签处理
	 * @throws WexinReqException
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "qualityHandle")
	@Transactional
	@ResponseBody
	public AjaxJson qualityHandle(HttpServletRequest request) throws WexinReqException{
		String qualityHandlerNote = request.getParameter("qualityHandlerNote");
		String reason = request.getParameter("reason");
		String sn = request.getParameter("sn");
		String toStation = request.getParameter("toStation");
		String presentStation = request.getParameter("presentStation");
		String ordermaking = request.getParameter("ordermaking");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer jumpId = Integer.parseInt(request.getParameter("jumpId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(jumpId==null||"".equals(request.getParameter("jumpId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//获取下一步操作人员memberId
		String qualityLeader = getQualityLeaderMemberId();//获取质控部审核人员memberId
		if(qualityLeader==null || qualityLeader.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("未设置部门审核人员，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		
		SQLHelper helper = new SQLHelper();
		//解析下一流程处理人
		String pendingPerson = "";
		String sql1 = "select * from tf_jump_station where jumpId = " + jumpId;
		List<Object> list = helper.query(sql1);
		Map<String, Object> map = new HashMap<String, Object>();
		if (list!=null && list.size()>0) {
			map = (Map<String, Object>) list.get(0);
			String pendingPersons[] = String.valueOf(map.get("pendingPerson")).split(",");
			String qualityHandlerArray[] = getQualityHandlerMemberId().split(",");
			for(int i=0;i<pendingPersons.length;i++) {
				boolean pass = true;
				for(int j=0;j<qualityHandlerArray.length;j++) {
					if(pendingPersons[i].equals(qualityHandlerArray[j])) {
						pass= false;
						break;
					}
				} 
				if(pass) {
					pendingPerson = pendingPerson+pendingPersons[i]+",";
				}
			} 
			pendingPerson = pendingPerson + qualityLeader;
			if(!"".equals(pendingPerson)&&pendingPerson.substring(pendingPerson.length()-1).equals(",")){
				pendingPerson = pendingPerson.substring(0,pendingPerson.length()-1);
			} 
			
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，该条信息不存在！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		
		int qualitySignFlag = 1;//质控部已会签处理
		String sql = "UPDATE tf_jump_station SET state="+state+",qualitySignFlag="+qualitySignFlag+",pendingPerson='"+pendingPerson+"',qualityHandlerNote='"+qualityHandlerNote+"',qualityHandler = "+ memberId+", qualityHandlerTime = now() where jumpId = " + jumpId;
		boolean isSuccess = helper.update(sql);
		if(isSuccess){
			String access_token = "";
			String touser = getQualityLeaderMemberId();//获取质控部审核人员userId来推送消息
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(reason.length()>38) {
						reason = reason.substring(0, 37)+"...";
					}
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					//发送消息通知所有人				
					textcard.setTitle("SFC跳站会签审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/jumpStation/jumpStationConfirmation.html?jumpId="+jumpId);
					textcard.setDescription("制令单号："+ordermaking+"\n当前站点："+presentStation+"           跳至站点："+toStation+"\nSN 编 码："+sn+"\n跳站原因："+reason+"\n待您审核，请及时处理，点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty("");
					textcardMessage.setMsgtype("textcard");
					textcardMessage.setAgentid(Integer.parseInt(AgentId));
					textcardMessage.setTextcard(textcard);
					JSONObject result1 = WxCommonAPI.sendMssage(access_token, textcardMessage);
					if (result1.has("errcode") && result1.getString("errcode").equals("0") && result1.getString("errmsg").equals("ok")) {
						ajax.setSuccess(true);
						ajax.setMessage("成功！");
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
		}else{
			ajax.setSuccess(false);
			ajax.setMessage("数据保存失败！");
			ajax.setErrorCode("-2");
		}	
		
		return ajax;
	}
	/**
	 * 6、质量控制部会签审核
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "qualityConfirm")
	@Transactional
	@ResponseBody
	public AjaxJson qualityConfirm(HttpServletRequest request) throws WexinReqException{
		String reason = request.getParameter("reason");
		String sn = request.getParameter("sn");
		String toStation = request.getParameter("toStation");
		String presentStation = request.getParameter("presentStation");
		String ordermaking = request.getParameter("ordermaking");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer jumpId = Integer.parseInt(request.getParameter("jumpId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(jumpId==null||"".equals(request.getParameter("jumpId"))||memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		SQLHelper helper = new SQLHelper();
		String sql1 = "select * from tf_jump_station where jumpId = " + jumpId;
		List<Object> list = helper.query(sql1);
		Integer engineeringSignFlag = 0;
		if (list!=null && list.size()>0) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) list.get(0);
			String flag = String.valueOf(map.get("engineeringSignFlag"));//获取工程部门会签状态
			if(flag!=null && flag!="null" && !flag.isEmpty()) {
				engineeringSignFlag = Integer.parseInt(flag);
			}
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，该条信息不存在！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		
		String pendingPerson = "";//待处理人
		int qualitySignFlag = 2;//质控已会签审核	
		if(engineeringSignFlag==2) {//1.工程部门已会签审核
			pendingPerson = "";//清空待处理人
			int state = 0;//代表完结
			String sql = "UPDATE tf_jump_station SET state="+state+",qualitySignFlag="+qualitySignFlag+",pendingPerson='"+pendingPerson+"',qualityLeader = "+ memberId+", qualityLeaderTime = now() where jumpId = " + jumpId;
			boolean isSuccess = helper.update(sql);
			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						if(reason.length()>38) {
							reason = reason.substring(0, 37)+"...";
						}
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//发送消息通知所有人				
						textcard.setTitle("SFC跳站签核流程完结通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/jumpStation/jumpStationConfirmation.html?jumpId="+jumpId);
						textcard.setDescription("制令单号："+ordermaking+"\n当前站点："+presentStation+"           跳至站点："+toStation+"\nSN 编 码："+sn+"\n跳站原因："+reason+"\n点击查看详情！");
						textcardMessage.setTouser("@all");
						textcardMessage.setToparty("");
						textcardMessage.setMsgtype("textcard");
						textcardMessage.setAgentid(Integer.parseInt(AgentId));
						textcardMessage.setTextcard(textcard);
						JSONObject result1 = WxCommonAPI.sendMssage(access_token, textcardMessage);
						if (result1.has("errcode") && result1.getString("errcode").equals("0") && result1.getString("errmsg").equals("ok")) {
							ajax.setSuccess(true);
							ajax.setMessage("成功！");
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
			}else{
				ajax.setSuccess(false);
				ajax.setMessage("数据保存失败！");
				ajax.setErrorCode("-2");
			}	
			
		}else if(engineeringSignFlag==1){//2.工程部已处理但未审核
			pendingPerson = getEngineeringLeaderMemberId();//待工程部领导审核
			String sql = "UPDATE tf_jump_station SET qualitySignFlag="+qualitySignFlag+",pendingPerson='"+pendingPerson+"',qualityLeader = "+ memberId+", qualityLeaderTime = now() where jumpId = " + jumpId;
			boolean isSuccess = helper.update(sql);
			if(!isSuccess){
				ajax.setSuccess(false);
				ajax.setMessage("数据保存失败！");
				ajax.setErrorCode("-2");
			}
		}else {//3.工程未会签
			pendingPerson = getEngineeringHandlerMemberId();//待工程部人员处理
			String sql = "UPDATE tf_jump_station SET qualitySignFlag="+qualitySignFlag+",pendingPerson='"+pendingPerson+"',qualityLeader = "+ memberId+", qualityLeaderTime = now() where jumpId = " + jumpId;
			boolean isSuccess = helper.update(sql);
			if(!isSuccess){
				ajax.setSuccess(false);
				ajax.setMessage("数据保存失败！");
				ajax.setErrorCode("-2");
			}
		}		
		return ajax;
	}
	
	/**
	 * 从数据库获取对应userId的memberId
	 * @param userId
	 * @return memberId
	 */
	@SuppressWarnings("unchecked")
	public static String getMemberIdByUserId(String userId) {
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_member where userId = '" + userId + "'";
		List<Object> result = sqlhe.query(sql);
		if (!result.isEmpty() && result != null) {
			Map<String, Object> map = (Map<String, Object>) result.get(0);
			String memberId = String.valueOf(map.get("memberId"));
			return memberId;
		}
		return null;
	}
	/**
	 * 从数据库获取对应memberId的userId
	 * @param memberId
	 * @return userId
	 */
	@SuppressWarnings("unchecked")
	public static String getUserIdByMemberId(Integer memberId) {
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select userId from tf_member where memberId = "+memberId;
		List<Object> result = sqlhe.query(sql);
		if (!result.isEmpty() && result != null) {
			Map<String, Object> map = (Map<String, Object>) result.get(0);
			String userId = String.valueOf(map.get("userId"));
			return userId;
		}
		return null;
	}
	
	/**
	 * 根据部门编号获取该部门下所有成员的memberId字符串
	 * @param toparty
	 * @return memberIdStr
	 */
	@SuppressWarnings("unchecked")
	public static String getMemberIdStrByToparty(String toparty) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and department = '["+toparty+"]'";				
		List<Object> memberIdList = sqlhe.query(sql);//获取下一流程部门成员的memberId集合	
		String memberIdStr = "";
		if(memberIdList!=null && memberIdList.size()>0) {
			for(int i=0;i<memberIdList.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) memberIdList.get(i);
				String id = String.valueOf(map.get("memberId"));
				memberIdStr += id+",";					
			  }
			if(memberIdStr.substring(memberIdStr.length()-1).equals(",")){
				memberIdStr = memberIdStr.substring(0,memberIdStr.length()-1);
			}
		}
		return memberIdStr;
	}
	
	/**
	 * 根据部门编号获取该部门经理或审核人memberId
	 * @param department
	 * @return managerMemberId
	 */
	@SuppressWarnings("unchecked")
	public static String getManagerMemberIds(String department) {
		//根据部门号找到对应经理领导memberId发送消息
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and isleader = 1 and department = '"+department+"'";
		List<Object> memberIdList = sqlhe.query(sql);
		String memberIds = "";
		if (memberIdList!=null && memberIdList.size()>0) {
			for(int i=0;i<memberIdList.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) memberIdList.get(i);
				String memberId = String.valueOf(map.get("memberId"));
				memberIds += memberId+",";					
			  }
			if(memberIds.substring(memberIds.length()-1).equals(",")){
				memberIds = memberIds.substring(0,memberIds.length()-1);
			}
		}
		return memberIds;
	}
	
	/**
	 * 根据部门编号获取该部门经理userId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getManagerUserIds(String department) {
		//根据部门号找到对应经理领导发送消息
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select userId from tf_member where deleteFlag = 0 and isleader = 1 and department = '" + department + "'";
		List<Object> userIdList = sqlhe.query(sql);
		String userIds = "";
		if (userIdList!=null && userIdList.size()>0) {
			for(int i=0;i<userIdList.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) userIdList.get(i);
				String userId = String.valueOf(map.get("userId"));
				userIds += userId+"|";					
			  }
			if(userIds.substring(userIds.length()-1).equals("|")){
				userIds = userIds.substring(0,userIds.length()-1);
			}
		}
		return userIds;
	}
	/**
	 * 获取工程部工程师以上人员的memberId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getEngineeringHandlerMemberId() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and department = '[3]' and (isleader = 1 or position like '%主管' or position like '%工程师')";
		List<Object> list = sqlhe.query(sql);
		String memberIdStr = "";
		if(list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				String id = String.valueOf(map.get("memberId"));
				memberIdStr += id+",";					
			  }
			if(memberIdStr.substring(memberIdStr.length()-1).equals(",")){
				memberIdStr = memberIdStr.substring(0,memberIdStr.length()-1);
			}
		}
		return memberIdStr;
	}
	/**
	 * 获取工程部审核领导的memberId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getEngineeringLeaderMemberId() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and department = '[3]' and isleader = 1";
		List<Object> list = sqlhe.query(sql);
		String memberIdStr = "";
		if(list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				String id = String.valueOf(map.get("memberId"));
				memberIdStr += id+",";					
			}
			if(memberIdStr.substring(memberIdStr.length()-1).equals(",")){
				memberIdStr = memberIdStr.substring(0,memberIdStr.length()-1);
			}
		}
		return memberIdStr;
	}
	/**
	 * 获取工程部审核领导的userId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getEngineeringLeaderUserId() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select userId from tf_member where deleteFlag = 0 and department = '[3]' and isleader = 1";
		List<Object> list = sqlhe.query(sql);
		String userIdStr = "";
		if(list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) list.get(i);
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
	 * 获取质控部相关人员的memberId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getQualityHandlerMemberId() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and department = '[5]' and (isleader = 1 or position like '%PQC组组长' or position like '%工程师')";
		List<Object> list = sqlhe.query(sql);
		String memberIdStr = "";
		if(list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				String id = String.valueOf(map.get("memberId"));
				memberIdStr += id+",";					
			}
			if(memberIdStr.substring(memberIdStr.length()-1).equals(",")){
				memberIdStr = memberIdStr.substring(0,memberIdStr.length()-1);
			}
		}
		return memberIdStr;
	}
	/**
	 * 获取质量控制部审核领导的memberId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getQualityLeaderMemberId() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and department = '[5]' and isleader = 1";
		List<Object> list = sqlhe.query(sql);
		String memberIdStr = "";
		if(list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				String id = String.valueOf(map.get("memberId"));
				memberIdStr += id+",";					
			}
			if(memberIdStr.substring(memberIdStr.length()-1).equals(",")){
				memberIdStr = memberIdStr.substring(0,memberIdStr.length()-1);
			}
		}
		return memberIdStr;
	}
	/**
	 * 获取质量控制部审核领导的userId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getQualityLeaderUserId() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select userId from tf_member where deleteFlag = 0 and department = '[5]' and isleader = 1";
		List<Object> list = sqlhe.query(sql);
		String userIdStr = "";
		if(list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				String id = String.valueOf(map.get("userId"));
				userIdStr += id+"|";					
			}
			if(userIdStr.substring(userIdStr.length()-1).equals("|")){
				userIdStr = userIdStr.substring(0,userIdStr.length()-1);
			}
		}
		return userIdStr;
	}
}