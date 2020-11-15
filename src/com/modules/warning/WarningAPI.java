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

import com.modules.db.SQLHelper;
import com.modules.entity.message.Textcard;
import com.modules.entity.message.TextcardMessage;
import com.modules.utils.AjaxJson;
import com.modules.utils.CommUtil;
import com.modules.utils.ConfigurationFileHelper;
import com.modules.utils.WxCommonAPI;

import net.sf.json.JSONObject;

/**
 * 发送预警通知消息
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/warning")
public class WarningAPI {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getWarningSecret();
	private static String AgentId = ConfigurationFileHelper.getWarningAgentId();
	
   /**
	 * 1、推送文本卡片消息(发送预警提醒)
	 * @param ordermaking,warningTime,number,description
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "alarmReminder")
	@Transactional
	@ResponseBody
	public AjaxJson alarmReminder(HttpServletRequest request) throws WexinReqException{
		String description = request.getParameter("description");//不良描述
		String ordermaking = request.getParameter("ordermaking");//制令单号
		String number = request.getParameter("number");//不良出现次数
		String model = request.getParameter("type");//机型
		String snlist = request.getParameter("snlist");//SN集合
		AjaxJson ajax = new AjaxJson();	
		if(ordermaking==null||"".equals(ordermaking)){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//按规则生成预警编号
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");//设置日期格式
		String serialNumber = "";
		SQLHelper sqlhe = new SQLHelper();		
		String sql1 = "select count(warningId) from tf_warning where date_format(warningTime,'%Y-%m-%d')=date_format(now(),'%Y-%m-%d')";
		List<Object> snList = sqlhe.query(sql1);

		if (snList!=null && snList.size()>0) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) snList.get(0);
			String count = String.valueOf(map.get("count(warningId)"));
			serialNumber = "YJ"+sdf.format(new Date())+String.format("%03d", Integer.parseInt(count)+1);
		}else {
			serialNumber = "";
		}
		
		//存储预警信息
		String sql = "INSERT INTO tf_warning (state,warningTime,serialNumber,ordermaking,description,number,model,snlist)" + 
				 " VALUES (0,now(),'"+serialNumber+"','"+ordermaking+"','"+description+"','"+number+"','"+model+"','"+snlist+"')";
		String warningId = String.valueOf(CommUtil.insertAndBackId(sql));
		
	    //获取要发送消息对象
		String touser = getsendMessageUserIds();
		//发送预警消息
		String access_token = "";
		try {
			access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
			if(access_token!=null && !"".equals(access_token)) {
								
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();
				textcard.setTitle("预警通知   ( "+serialNumber+" )");
				textcard.setUrl(serviceUrl+"/TFMobile/webpage/warning/warningForm.html?warningId="+warningId);
				textcard.setDescription(df.format(new Date())+" 制令单号 "+ordermaking+" 中：不良现象 \""+description+"\" 已经出现 "+number+" 次！请相关人员注意！！！");
				textcardMessage.setTouser(touser);
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
	 * 2、通过主键获取预警记录信息
	 * @param warningId
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "findInfo")
	@Transactional
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request) throws WexinReqException{
		Integer warningId = Integer.parseInt(request.getParameter("warningId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(warningId==null||"".equals(request.getParameter("warningId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_warning a left join tf_member b on a.handler = b.memberId where a.warningId = " + warningId;
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("warning", list.get(0));
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 3、通过主键跟新预警记录信息（保存预警处理措施）
	 * @param warningId,handler,handleMethod
	 * @throws WexinReqException
	 * @throws ParseException 
	 */
	@RequestMapping(value = "saveHandleMethod")
	@Transactional
	@ResponseBody
	public AjaxJson saveHandleMethod(HttpServletRequest request) throws WexinReqException, ParseException{
		Integer warningId = Integer.parseInt(request.getParameter("warningId"));		
		Integer handler = Integer.parseInt(request.getParameter("handler"));		
		String handleMethod = request.getParameter("handleMethod");
		String description = request.getParameter("description");
		String ordermaking = request.getParameter("ordermaking");
		String number = request.getParameter("number");
		String handleTime = request.getParameter("handleTime");
		String handlerName = request.getParameter("handlerName");
		String serialNumber = request.getParameter("serialNumber");
		String touser = request.getParameter("touser");
		String toparty = request.getParameter("toparty");

		AjaxJson ajax = new AjaxJson();	
		if(warningId==null||"".equals(request.getParameter("warningId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
	
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String sql = "UPDATE tf_warning SET handler="+handler+",handleTime='"+handleTime+"',handleMethod='"+handleMethod+"',state=1,reviewState=0, submitTime = now() where warningId = " + warningId;
		sqlist.add(sql);	
		boolean isSuccess = sqlhe.update(sqlist);
		if(isSuccess) {
		//发送预警处理消息
		String access_token = "";
		try {
			access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
			if(access_token!=null && !"".equals(access_token)) {
				
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();		
				textcard.setTitle("预警处理通知 ( "+serialNumber+" )");
				textcard.setUrl(serviceUrl+"/TFMobile/webpage/warning/warningForm.html?warningId="+warningId);
				textcard.setDescription("制令单号："+ordermaking+"，不良现象 ："+description+"， 出现次数："+number+"；该预警在 "+handleTime+" 已被 "+handlerName+" 处理，点击查看详情！");
				textcardMessage.setTouser(touser);
				textcardMessage.setToparty(toparty);
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
		}else {
			ajax.setErrorCode("-2");
			ajax.setSuccess(false);
			ajax.setMessage("保存失败！");
		}
		return ajax;
	}
	
	/**
	 * 4、通过主键跟新预警记录信息（保存预警处理措施）
	 * @param warningId,memberId,type,refusedReason
	 * @throws WexinReqException
	 * @throws ParseException 
	 */
	@RequestMapping(value = "review")
	@Transactional
	@ResponseBody
	public AjaxJson review(HttpServletRequest request) throws WexinReqException, ParseException{
		Integer warningId = Integer.parseInt(request.getParameter("warningId"));		
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));		
		String type = request.getParameter("type");
		String refusedReason = request.getParameter("refusedReason");
		String description = request.getParameter("description");
		String ordermaking = request.getParameter("ordermaking");
		String serialNumber = request.getParameter("serialNumber");
		String number = request.getParameter("number");
		String touser = request.getParameter("touser");
		String toparty = request.getParameter("toparty");

		AjaxJson ajax = new AjaxJson();	
		if(warningId==null||"".equals(request.getParameter("warningId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
	
		SQLHelper sqlhe = new SQLHelper();
		String sql = "";
		if("0".equals(type)) {
			sql = "UPDATE tf_warning SET state=0,reviewState=1,reviewer="+memberId+",refusedReason='"+refusedReason+"',reviewTime = now() where warningId = " + warningId;
		}else if("1".equals(type)) {
			sql = "UPDATE tf_warning SET reviewState=1,reviewer="+memberId+",reviewTime = now() where warningId = " + warningId;
		}
		boolean isSuccess = sqlhe.update(sql);
		if(isSuccess) {
		//发送预警处理消息
		String access_token = "";
		try {
			access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
			if(access_token!=null && !"".equals(access_token)) {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();	
				String textcardTitle = "";
				String textcardDescription = "";
				if("0".equals(type)) {
					textcardTitle = "预警处理被驳回通知( "+serialNumber+" )";
					textcardDescription = "制令单号"+ordermaking+"中出现"+number+description+"不良现象的预警处理措施在"+df.format(new Date())+"已被驳回，点击查看详情！";
				}else if("1".equals(type)) {
					textcardTitle = "预警处理确认通过通知( "+serialNumber+" )";
					textcardDescription = "制令单号"+ordermaking+"中出现"+number+description+"不良现象的预警处理措施在"+df.format(new Date())+"已被确认通过，点击查看详情！";
				}
				textcard.setTitle(textcardTitle);
				textcard.setUrl(serviceUrl+"/TFMobile/webpage/warning/warningForm.html?warningId="+warningId);
				textcard.setDescription(textcardDescription);
				textcardMessage.setTouser(touser);
				textcardMessage.setToparty(toparty);
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
		}else {
			ajax.setErrorCode("-2");
			ajax.setSuccess(false);
			ajax.setMessage("保存失败！");
		}
		return ajax;
	}
	/**
	 * 5、获取预警信息列表
	 * @param type,page
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findList(HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String type = request.getParameter("type");
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sqlhe = new SQLHelper();
		String sql = "";
		if("0".equals(type)){//查询全部预警记录
			sql = "select * from tf_warning order by warningId desc limit "+page+",10";
		}else if("1".equals(type)) {//查询待办预警
			sql = "select * from tf_warning where state <>1 or reviewState <>1 order by warningId desc limit "+page+",10";
		}else if("2".equals(type)){//查询已办预警
			sql = "select * from tf_warning where state = 1 and reviewState = 1 order by warningId desc limit "+page+",10";
		}
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("warningList", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 6、模糊搜索获取所有预警记录
	 * @param 
	 */
	@RequestMapping(value = "searchList")
	@ResponseBody
	public AjaxJson searchList(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_warning where serialNumber like concat('%','"+searchName+"','%') or ordermaking like concat('%','"+searchName+"','%') or description like concat('%','"+searchName+"','%') order by warningId desc";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("warningList", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 7、获取当前个人的预警信息
	 * @param memberId,type,page
	 */
	@RequestMapping(value = "findOwnList")
	@ResponseBody
	public AjaxJson findOwnList(HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String type = request.getParameter("type");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		String department = request.getParameter("department");
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sqlhe = new SQLHelper();
		String sql = "";
		if("0".equals(type)){//查询个人的全部预警记录
			if(department.equals("[3]")) {
				sql = "select * from tf_warning where state = 0 or (state =1 and handler="+memberId+") order by warningId desc limit "+page+",10";
			}else if(department.equals("[5]")) {
				sql = "select * from tf_warning where (state =1 and reviewState =0) or (reviewState=1 and reviewer="+memberId+") order by warningId desc limit "+page+",10";
			}else {
				sql = "select * from tf_warning where handler=0";
			}
		}else if("1".equals(type)) {//查询个人的待处理的预警
			if(department.equals("[3]")) {
				sql = "select * from tf_warning where state = 0 order by warningId desc limit "+page+",10";
			}else if(department.equals("[5]")) {
				sql = "select * from tf_warning where state =1 and reviewState =0 order by warningId desc limit "+page+",10";;
			}else {
				sql = "select * from tf_warning where handler=0";
			}
		}else if("2".equals(type)){//查询个人的已处理预警
			if(department.equals("[3]")) {
				sql = "select * from tf_warning where state = state =1 and handler="+memberId+" order by warningId desc limit "+page+",10";
			}else if(department.equals("[5]")) {
				sql = "select * from tf_warning where reviewState=1 and reviewer="+memberId+" order by warningId desc limit "+page+",10";
			}else {
				sql = "select * from tf_warning where handler=0";
			}
		}
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("warningList", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 8、获取当前个人的预警信息统计数量
	 * @param memberId
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "findOwnCount")
	@ResponseBody
	public AjaxJson findOwnCount(HttpServletRequest request){
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		String department = request.getParameter("department");
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sqlhe = new SQLHelper();		
		//查询个人的全部预警记录数量
		String sql0 = "";
		if(department.equals("[3]")) {
			sql0 = "select count(warningId) from tf_warning where state = 0 or (state =1 and handler="+memberId+")";
		}else if(department.equals("[5]")) {
			sql0 = "select count(warningId) from tf_warning where (state =1 and reviewState =0) or (reviewState=1 and reviewer="+memberId+")";
		}else {
			sql0 = "select count(warningId) from tf_warning where handler=0";
		}
		
		//查询个人的待处理的预警记录数量
		String sql1 = "";
		if(department.equals("[3]")) {
			sql1 = "select count(warningId) from tf_warning where state = 0";
		}else if(department.equals("[5]")) {
			sql1 = "select count(warningId) from tf_warning where state =1 and reviewState =0";
		}else {
			sql1 = "select count(warningId) from tf_warning where handler=0";
		}
		
		//查询个人的已处理预警记录数量
		String sql2 = "";
		if(department.equals("[3]")) {
			sql2 = "select count(warningId) from tf_warning where state = state =1 and handler="+memberId;
		}else if(department.equals("[5]")) {
			sql2 = "select count(warningId) from tf_warning where reviewState=1 and reviewer="+memberId;
		}else {
			sql2 = "select count(warningId) from tf_warning where handler=0";
		}
	
		List<Object> list0 = sqlhe.query(sql0);
		List<Object> list1 = sqlhe.query(sql1);
		List<Object> list2 = sqlhe.query(sql2);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("count0", ((Map<String, Object>)list0.get(0)).get("count(warningId)"));
		body.put("count1", ((Map<String, Object>)list1.get(0)).get("count(warningId)"));
		body.put("count2", ((Map<String, Object>)list2.get(0)).get("count(warningId)"));
		ajax.setBody(body);
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
	
	
	
	
public static void main(String[] args) {
	System.out.println(getsendMessageUserIds());
}
}
