package com.modules.improvement;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jeewx.api.core.exception.WexinReqException;
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
 * 改善提案处理流程API
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/improvement")
public class FormAPI {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String savePicturePath = ConfigurationFileHelper.getSavePicturePath();
	private static String CorpID = ConfigurationFileHelper.getAnotherCorpID();
	private static String Secret = ConfigurationFileHelper.getImprovementSecret();
	private static String AgentId = ConfigurationFileHelper.getImprovementAgentId();
   /**
	 * 1、提交改善提案
	 * @param title,current_measure,improve_measure,anticipate_effect,picture,name,position,department,team,proposer
	 * @throws ParseException 
	 */
	@RequestMapping(value = "apply")
	@ResponseBody
	public AjaxJson apply(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String title = request.getParameter("title");
		String current_measure = request.getParameter("current_measure");
		String improve_measure = request.getParameter("improve_measure");
		String anticipate_effect = request.getParameter("anticipate_effect");
		String picture = request.getParameter("picture");
		String name = request.getParameter("name");
		String position = request.getParameter("position");
		String department = request.getParameter("department");
		String team = request.getParameter("team");
		Integer proposer = Integer.parseInt(request.getParameter("proposer"));
		
		String departmentName = getDepartmentNameByCode(department); 
		if(!"[4]".equals(department)) {
			ajax.setSuccess(false);
			ajax.setMessage("暂不支持该部门提交改善提案！");
			return ajax;
		}
		
		String[] leaderInfo;
		String touser;
		if(position.indexOf("主管")!=-1 || position.indexOf("工程师")!=-1) {
			//获取部门经理信息
			leaderInfo = getManagerInfo(department);
			touser = leaderInfo[1];
			if(leaderInfo==null || touser==null || "null".equals(touser) || touser.isEmpty()) {
				ajax.setSuccess(false);
				ajax.setMessage("未设置部门经理，提交失败！");
				return ajax;
			}
		}else {
			//获取对应主管
			leaderInfo = getSupervisorInfo(department,team);
			touser = leaderInfo[1];
			if(leaderInfo==null || touser==null || "null".equals(touser) || touser.isEmpty()) {
				ajax.setSuccess(false);
				ajax.setMessage("未设置部门主管，提交失败！");
				return ajax;
			}
		}
		
		//上传图片
		if(picture!=null&&!picture.isEmpty()) {	
			picture = CommUtil.uploadPicture(savePicturePath,"improvementPicture","improvement_"+System.currentTimeMillis(),picture);
		}
				
		String sql;
		if(position.indexOf("主管")!=-1 || position.indexOf("工程师")!=-1) {//如果是主管或工程师提交则直接到经理
			sql = "INSERT INTO tf_improvement (type,state,delete_flag,title,current_measure,improve_measure,anticipate_effect,picture,department,team,proposer,proposer_time) "+
					 "VALUES (1,1,0,'"+title+"','"+current_measure+"','"+improve_measure+"','"+anticipate_effect+"','"+picture+"','"+departmentName+"','"+team+"',"+proposer+",now())";
		}else {//其他普通员工提交
			sql = "INSERT INTO tf_improvement (type,state,delete_flag,title,current_measure,improve_measure,anticipate_effect,picture,department,team,proposer,proposer_time) "+
					 "VALUES (0,1,0,'"+title+"','"+current_measure+"','"+improve_measure+"','"+anticipate_effect+"','"+picture+"','"+departmentName+"','"+team+"',"+proposer+",now())";
		}
		String improvementId = String.valueOf(CommUtil.insertAndBackId(sql));
		if(improvementId!=null && !improvementId.isEmpty() && !"0".equals(improvementId) && !"null".equals(improvementId)) {
			if(position.indexOf("主管")!=-1 || position.indexOf("工程师")!=-1) {
				//推送消息给部门经理
				String messageTitle = "改善提案审核通知";
				String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
				String toparty = "";
				String description = name+" 提交了一份改善提案待您审核，请及时处理！";
				try {
					sendWXMessage(messageTitle, url, touser, toparty, description);
				} catch (WexinReqException e) {
					ajax.setSuccess(false);
					ajax.setMessage("提交信息成功，推送消息失败！");
					return ajax;
				}
			}else {
				//推送消息给对应主管
				String messageTitle = "改善提案审核通知";
				String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
				String toparty = "";
				String description = name+" 提交了一份改善提案待您审核，请及时处理！";
				try {
					sendWXMessage(messageTitle, url, touser, toparty, description);
				} catch (WexinReqException e) {
					ajax.setSuccess(false);
					ajax.setMessage("提交信息成功，推送消息失败！");
					return ajax;
				}	
			}
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			return ajax;
		}
		
		return ajax;
	}
	
	/**
	 * 2、通过主键获取改善提案记录信息
	 * @param improvementId
	 */
	@RequestMapping(value = "findInfo")
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		Integer improvementId = Integer.parseInt(request.getParameter("improvementId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(improvementId==null||"".equals(request.getParameter("improvementId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_improvement where improvementId = " + improvementId;
		List<Object> list = sqlhe.query(sql);
		Object improvement = null;
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		if(list!=null && list.size()>0) {
			improvement = list.get(0);
		}
		body.put("improvement", improvement);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 3、主管驳回
	 * @param improvementId supervisor supervisor_refused_reason
	 * @throws ParseException 
	 */
	@RequestMapping(value = "supervisorRefuse")
	@ResponseBody
	public AjaxJson supervisorRefuse(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer improvementId = Integer.parseInt(request.getParameter("improvementId"));
		String supervisor_refused_reason = request.getParameter("supervisor_refused_reason");
		String supervisor = request.getParameter("supervisor");
		String proposer = request.getParameter("proposer");
		
		SQLHelper sh = new SQLHelper();		
		String sql = "UPDATE tf_improvement SET state = 2,supervisor_refused_reason = '"+supervisor_refused_reason+"',supervisor = '"+supervisor+"',supervisor_time = now() WHERE improvementId = " + improvementId;
		boolean boo = sh.update(sql);
		if(boo) {
			String title = "改善提案被驳回通知";
			String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
			String touser = getUserIdByMemberId(proposer);
			String toparty = "";
			String description = "您提交的一份改善提案被主管驳回\n驳回原因："+supervisor_refused_reason+"\n请及时处理！";
			try {
				sendWXMessage(title, url, touser, toparty, description);
			} catch (WexinReqException e) {
				ajax.setSuccess(false);
				ajax.setMessage("驳回成功，推送消息失败！");
				return ajax;
			}													
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			return ajax;
		}
		return ajax;
	}
	
	/**
	 * 4、撤销改善提案
	 * @param improvementId
	 * @throws ParseException 
	 */
	@RequestMapping(value = "delete")
	@ResponseBody
	public AjaxJson delete(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer improvementId = Integer.parseInt(request.getParameter("improvementId"));
		
		SQLHelper sh = new SQLHelper();		
		String sql = "UPDATE tf_improvement SET delete_flag = 1 WHERE improvementId = " + improvementId;
		boolean boo = sh.update(sql);
		if(!boo){
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			return ajax;
		}		
		return ajax;
	}
	/**
	 * 5、编辑改善提案
	 * @param improvementId,title,current_measure,improve_measure,anticipate_effect,picture
	 * @throws ParseException 
	 */
	@RequestMapping(value = "edit")
	@ResponseBody
	public AjaxJson edit(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer improvementId = Integer.parseInt(request.getParameter("improvementId"));
		String title = request.getParameter("title");
		String current_measure = request.getParameter("current_measure");
		String improve_measure = request.getParameter("improve_measure");
		String anticipate_effect = request.getParameter("anticipate_effect");
		String picture = request.getParameter("picture");
		String name = request.getParameter("name");
		String position = request.getParameter("position");
		String department = request.getParameter("department");
		String team = request.getParameter("team");
		
		if(!"[4]".equals(department)) {
			ajax.setSuccess(false);
			ajax.setMessage("暂不支持该部门提交改善提案！");
			return ajax;
		}
		
		String[] leaderInfo;
		String touser;
		if(position.indexOf("主管")!=-1 || position.indexOf("工程师")!=-1) {
			//获取部门经理信息
			leaderInfo = getManagerInfo(department);
			touser = leaderInfo[1];
			if(leaderInfo==null || touser==null || "null".equals(touser) || touser.isEmpty()) {
				ajax.setSuccess(false);
				ajax.setMessage("未设置部门经理，提交失败！");
				return ajax;
			}
		}else {
			//获取对应主管
			leaderInfo = getSupervisorInfo(department,team);
			touser = leaderInfo[1];
			if(leaderInfo==null || touser==null || "null".equals(touser) || touser.isEmpty()) {
				ajax.setSuccess(false);
				ajax.setMessage("未设置部门主管，提交失败！");
				return ajax;
			}
		}
		
		//上传图片
		if(picture!=null&&!picture.isEmpty()) {	
			picture = CommUtil.uploadPicture(savePicturePath,"improvementPicture","improvement_"+System.currentTimeMillis(),picture);
		}
		SQLHelper sh = new SQLHelper();		
		String sql = "UPDATE tf_improvement SET state = 1,title='"+title+"',current_measure='"+current_measure+"',improve_measure='"+improve_measure+
				"',anticipate_effect='"+anticipate_effect+"',picture='"+picture+"',proposer_time=now() WHERE improvementId = " + improvementId;
		boolean boo = sh.update(sql);
		if(boo) {
			if(position.indexOf("主管")!=-1 || position.indexOf("工程师")!=-1) {
				//推送消息给部门经理
				String messageTitle = "改善提案修改审核通知";
				String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
				String toparty = "";
				String description = name+" 修改了一份改善提案待您审核，请及时处理！";
				try {
					sendWXMessage(messageTitle, url, touser, toparty, description);
				} catch (WexinReqException e) {
					ajax.setSuccess(false);
					ajax.setMessage("提交信息成功，推送消息失败！");
					return ajax;
				}
			}else {
				//推送消息给对应主管
				String messageTitle = "改善提案修改审核通知";
				String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
				String toparty = "";
				String description = name+" 修改了一份改善提案待您审核，请及时处理！";
				try {
					sendWXMessage(messageTitle, url, touser, toparty, description);
				} catch (WexinReqException e) {
					ajax.setSuccess(false);
					ajax.setMessage("提交信息成功，推送消息失败！");
					return ajax;
				}	
			}
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			return ajax;
		}
		
		return ajax;
	}
	
	/**
	 * 6、主管确认通过
	 * @param improvementId supervisor supervisor_refused_reason
	 * @throws ParseException 
	 */
	@RequestMapping(value = "supervisorConfirm")
	@ResponseBody
	public AjaxJson supervisorConfirm(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer improvementId = Integer.parseInt(request.getParameter("improvementId"));
		String supervisor = request.getParameter("supervisor");
		String department = request.getParameter("department");
		
		String[] leaderInfo;
		String touser;
		//获取部门经理信息
		leaderInfo = getManagerInfo(department);
		touser = leaderInfo[1];
		if(leaderInfo==null || touser==null || "null".equals(touser) || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("未设置部门经理，提交失败！");
			return ajax;
		}
		
		SQLHelper sh = new SQLHelper();		
		String sql = "UPDATE tf_improvement SET state = 3,supervisor = '"+supervisor+"',supervisor_time = now() WHERE improvementId = " + improvementId;
		boolean boo = sh.update(sql);
		if(boo) {
			String title = "改善提案审核通知";
			String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
			String toparty = "";
			String description = "有一份改善提案待您审核\n请及时处理！";
			try {
				sendWXMessage(title, url, touser, toparty, description);
			} catch (WexinReqException e) {
				ajax.setSuccess(false);
				ajax.setMessage("提交信息成功，推送消息失败！");
				return ajax;
			}													
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			return ajax;
		}
		return ajax;
	}
	
	/**
	 * 7、经理驳回
	 * @param improvementId supervisor manager_refused_reason
	 * @throws ParseException 
	 */
	@RequestMapping(value = "managerRefuse")
	@ResponseBody
	public AjaxJson managerRefuse(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer improvementId = Integer.parseInt(request.getParameter("improvementId"));
		String type = request.getParameter("type");
		String manager_refused_reason = request.getParameter("manager_refused_reason");
		String supervisor = request.getParameter("supervisor");
		String manager = request.getParameter("manager");
		
		SQLHelper sh = new SQLHelper();	
		String sql="",description="";
		if("0".equals(type)) {//普通员工提案
			sql = "UPDATE tf_improvement SET state = 4,manager_refused_reason = '"+manager_refused_reason+"',manager = '"+manager+"',manager_time = now() WHERE improvementId = " + improvementId;
			description = "您确认的一份改善提案被经理驳回\n驳回原因："+manager_refused_reason+"\n请及时处理！";
		}else if("1".equals(type)) {//主管或工程师提案
			sql = "UPDATE tf_improvement SET state = 2,manager_refused_reason = '"+manager_refused_reason+"',manager = '"+manager+"',manager_time = now() WHERE improvementId = " + improvementId;
			description = "您提交的一份改善提案被经理驳回\n驳回原因："+manager_refused_reason+"\n请及时处理！";
		}
		boolean boo = sh.update(sql);
		if(boo) {
			String title = "改善提案被驳回通知";
			String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
			String touser = getUserIdByMemberId(supervisor);
			try {
				sendWXMessage(title, url, touser, "", description);
			} catch (WexinReqException e) {
				ajax.setSuccess(false);
				ajax.setMessage("驳回成功，推送消息失败！");
				return ajax;
			}													
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			return ajax;
		}
		return ajax;
	}
	
	/**
	 * 8、经理审核通过
	 * @param improvementId supervisor
	 * @throws ParseException 
	 */
	@RequestMapping(value = "managerConfirm")
	@ResponseBody
	public AjaxJson managerConfirm(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer improvementId = Integer.parseInt(request.getParameter("improvementId"));
		String type = request.getParameter("type");
		String manager = request.getParameter("manager");
		String submitDepartmentName = request.getParameter("submitDepartmentName");
		
		String touser;
		//获取部门经理信息
		String[] IESupervisorInfo = getIESupervisorInfo();
		touser = IESupervisorInfo[1];
		if(IESupervisorInfo==null || touser==null || "null".equals(touser) || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("未设置工业主管信息，提交失败！");
			return ajax;
		}
		
		SQLHelper sh = new SQLHelper();	
		String sql="";
		if("0".equals(type)) {//普通员工提案
			sql = "UPDATE tf_improvement SET state = 5,manager = '"+manager+"',manager_time = now() WHERE improvementId = " + improvementId;
		}else if("1".equals(type)) {//主管或工程师提案
			sql = "UPDATE tf_improvement SET state = 3,manager = '"+manager+"',manager_time = now() WHERE improvementId = " + improvementId;			
		}
		boolean boo = sh.update(sql);
		if(boo) {
			String title = "改善提案通知-"+submitDepartmentName;
			String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
			String description = submitDepartmentName+"提交了一份改善提案待您处理\n请及时处理！";
			try {
				sendWXMessage(title, url, touser, "", description);
			} catch (WexinReqException e) {
				ajax.setSuccess(false);
				ajax.setMessage("审核成功，推送消息失败！");
				return ajax;
			}													
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			return ajax;
		}
		return ajax;
	}
	/**
	 * 9、工业主管确认处理
	 * @param improvementId 
	 * @throws ParseException 
	 */
	@RequestMapping(value = "IESupervisorConfirm")
	@ResponseBody
	public AjaxJson IESupervisorConfirm(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer improvementId = Integer.parseInt(request.getParameter("improvementId"));
		String ie_supervisor = request.getParameter("ie_supervisor");
		String isAdopt = request.getParameter("isAdopt");
		String handle_info = request.getParameter("handle_info");
		String implement_person = request.getParameter("implement_person");
		String expect_finished_date = request.getParameter("expect_finished_date");
		String proposer = request.getParameter("proposer");
		String supervisor = request.getParameter("supervisor");
		String manager = request.getParameter("manager");
	
		SQLHelper sh = new SQLHelper();	
		String sql = "UPDATE tf_improvement SET state=0,isAdopt = '"+isAdopt+"',handle_info = '"+handle_info+"',implement_person = '"+implement_person+
				  "',expect_finished_date = '"+expect_finished_date+"',ie_supervisor = '"+ie_supervisor+"',ie_supervisor_time = now() WHERE improvementId = " + improvementId;	
		boolean boo = sh.update(sql);
		if(boo) {
			String isAdoptDesc,touser;
			if("是".equals(isAdopt)) {
				isAdoptDesc = "被采纳";
			}else {
				isAdoptDesc = "未被采纳";
			}
			//获取发送消息人员信息
			String proposerUserId = getUserIdByMemberId(proposer);
			String supervisorUserId = getUserIdByMemberId(supervisor);
			String managerUserId = getUserIdByMemberId(manager);
			touser = proposerUserId + "|" + managerUserId;
			if(supervisorUserId!=null && supervisorUserId!="null"&& !supervisorUserId.isEmpty()) {
				touser +=  "|" + supervisorUserId;	
			}
			String title = "改善提案"+isAdoptDesc+"通知";
			String url = "/TFMobile/webpage/improvement/improvementForm.html?improvementId="+improvementId;
			String description = "您提交或审核确认的一份改善提案"+isAdoptDesc+"\n点击查看详情！";
			try {
				sendWXMessage(title, url, touser, "", description);
			} catch (WexinReqException e) {
				ajax.setSuccess(false);
				ajax.setMessage("审核成功，推送消息失败！");
				return ajax;
			}													
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			return ajax;
		}
		return ajax;
	}
	/**
	 * 根据不同变量推送消息
	 * @param access_token
	 * @return 
	 * @throws WexinReqException 
	 */
	public static void sendWXMessage(String title,String url,String touser,String toparty,String description) throws WexinReqException {
		try {
			String access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
			if(access_token!=null && !"".equals(access_token)) {											
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();
				textcard.setTitle(title);
				textcard.setUrl(serviceUrl+url);
				textcard.setDescription(description);
				textcardMessage.setTouser(touser);
				textcardMessage.setToparty(toparty);
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
			throw new WexinReqException("发送消息出 错啦！");
		}
	}

	/**
	 * 部门编号解析
	 * @param department
	 * @return departmentName
	 */
	public static String getDepartmentNameByCode(String department) {
		String departmentName = "";
		if("[2]".equals(department)) {
			departmentName = "工业技术部";
		}else if("[3]".equals(department)) {
			departmentName = "工程部";
		}else if("[4]".equals(department)) {
			departmentName = "生产部";
		}else if("[5]".equals(department)) {
			departmentName = "质量部";
		}else if("[7]".equals(department)) {
			departmentName = "研发部";
		}else if("[11]".equals(department)) {
			departmentName = "仓储部";
		}
		return departmentName;
	}
		
	/**
	 * 根据部门编号获取该部门经理或审核人信息
	 * @param department
	 * @return managerMemberId
	 */
	@SuppressWarnings("unchecked")
	public static String[] getManagerInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_another_member where deleteFlag = 0 and isleader = 1 and department = '"+department+"'";
		List<Object> list = sqlhe.query(sql);
		String[] infoArray = new String[2];
		String memberIds = "";
		String userIds = "";
		Map<String, Object> map = null;
		String memberId = "";
		String userId = "";
		if (list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				map = (Map<String, Object>) list.get(i);
				memberId = String.valueOf(map.get("memberId"));
				userId = String.valueOf(map.get("userId"));
				memberIds += memberId+",";					
				userIds += userId+"|";		
			}
			if(memberIds.substring(memberIds.length()-1).equals(",")){
				memberIds = memberIds.substring(0,memberIds.length()-1);
			}
			if(userIds.substring(userIds.length()-1).equals("|")){
				userIds = userIds.substring(0,userIds.length()-1);
			}
		}
		infoArray[0] = memberIds;
		infoArray[1] = userIds;
		return infoArray;
	}

	/**
	 * 获取员工对应部门主管信息
	 * @param department team
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getSupervisorInfo(String department,String team) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_another_member where deleteFlag = 0 and position like '%"+team+"%主管%' and department = '" + department + "'";
		List<Object> list = sqlhe.query(sql);
		String[] infoArray = new String[2];
		String memberIds = "";
		String userIds = "";
		Map<String, Object> map = null;
		String memberId = "";
		String userId = "";
		if (list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				map = (Map<String, Object>) list.get(i);
				memberId = String.valueOf(map.get("memberId"));
				userId = String.valueOf(map.get("userId"));
				memberIds += memberId+",";					
				userIds += userId+"|";		
			}
			if(memberIds.substring(memberIds.length()-1).equals(",")){
				memberIds = memberIds.substring(0,memberIds.length()-1);
			}
			if(userIds.substring(userIds.length()-1).equals("|")){
				userIds = userIds.substring(0,userIds.length()-1);
			}
		}
		infoArray[0] = memberIds;
		infoArray[1] = userIds;
		return infoArray;
	}
	/**
	 * 获取工业技术部IE主管userId
	 * @param department team
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getIESupervisorInfo() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_another_member where deleteFlag = 0 and position like '%工业主管%' and department = '[2]'";
		List<Object> list = sqlhe.query(sql);
		String[] infoArray = new String[2];
		String memberIds = "";
		String userIds = "";
		Map<String, Object> map = null;
		String memberId = "";
		String userId = "";
		if (list!=null && list.size()>0) {
			for(int i=0;i<list.size();i++) {				
				map = (Map<String, Object>) list.get(i);
				memberId = String.valueOf(map.get("memberId"));
				userId = String.valueOf(map.get("userId"));
				memberIds += memberId+",";					
				userIds += userId+"|";		
			}
			if(memberIds.substring(memberIds.length()-1).equals(",")){
				memberIds = memberIds.substring(0,memberIds.length()-1);
			}
			if(userIds.substring(userIds.length()-1).equals("|")){
				userIds = userIds.substring(0,userIds.length()-1);
			}
		}
		infoArray[0] = memberIds;
		infoArray[1] = userIds;
		return infoArray;
	}

	/**
	 * 根据memberId获取对应的userId
	 * @param memberId
	 * @return userId
	 */
	@SuppressWarnings("unchecked")
	public static String getUserIdByMemberId(String memberId) {
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select userId from tf_another_member where memberId = '" + memberId + "'";
		List<Object> result = sqlhe.query(sql);
		if (!result.isEmpty() && result != null) {
			Map<String, Object> map = (Map<String, Object>) result.get(0);
			String userId = String.valueOf(map.get("userId"));
			return userId;
		}
		return null;
	}
	
   /**
	 * 根据主键memberId获取企业成员信息
	 * @param memberId
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "member/getInfo")
	@ResponseBody
	public AjaxJson getMemberInfo(HttpServletRequest request){
		String memberId = request.getParameter("memberId");
		AjaxJson ajax = new AjaxJson();	
		if(memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_another_member where memberId="+memberId;
		List<Object> list = sqlhe.query(sql);		
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();	
		if(list!=null && list.size()>0) {
			body.put("member", list.get(0));
		}		
		ajax.setBody(body);
		return ajax;
	}
}
