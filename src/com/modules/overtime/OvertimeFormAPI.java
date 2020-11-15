package com.modules.overtime;
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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**
 * 加班申请及签核API
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/overtime")
public class OvertimeFormAPI {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getOvertimeApplicationSecret();
	private static String AgentId = ConfigurationFileHelper.getOvertimeApplicationAgentId();
   /**
	 * 1、加班申请
	 * @param wxIds,applicant,startTime,endTime
	 * @throws ParseException 
	 */
	@RequestMapping(value = "apply")
	@ResponseBody
	public AjaxJson apply(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String type = request.getParameter("type");
		String overtimeType = request.getParameter("overtimeType");
		String wxIds = request.getParameter("wxIds");
		Integer applicant = Integer.parseInt(request.getParameter("applicant"));
		String startTime = request.getParameter("startTime");
		String endTime = request.getParameter("endTime");
		String department = request.getParameter("department");
		String team = request.getParameter("team");
		String applicant_reason = request.getParameter("applicant_reason");		
		String departmentName = getDepartmentNameByCode(department);		
		
		SQLHelper sh = new SQLHelper();
		if("0".equals(overtimeType)) {//新增申请
			if("0".equals(type)) {//部门级别提交				
				String sqlm = "select a.* from tf_wuxi_member a left join tf_overtime_member b on a.wxId=b.wxId where b.wxId in ("+wxIds+") and DATE(b.start_time)=DATE('"+startTime+"')";
				List<Object> list = sh.query(sqlm);
				if(list!=null && list.size()>0) {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>) list.get(0);				
					ajax.setSuccess(false);
					ajax.setMessage(map.get("name")+" 在该日期已申请过加班啦！");
					return ajax;
				}	
				
				//检验是否已提交过该日期的加班申请
				String sq = "SELECT applicationId FROM tf_overtime_application WHERE TO_DAYS(date)=TO_DAYS('"+startTime+"') AND department = '"+departmentName+"'";
				List<Object> li = sh.query(sq);
				if(li!=null && li.size()>0) {
					ajax.setSuccess(false);
					ajax.setMessage("部门在该日期已提交过加班申请啦，请选择补充类型！");
					return ajax;
				}
				
				String sql;
				if("[4]".equals(department)) {//如果是生产部助理提交则状态为1
					sql = "INSERT INTO tf_overtime_application (state,date,department,applicant,applicant_reason,applicant_time) "+
							 "VALUES (1,DATE('"+startTime+"'),'"+departmentName+"',"+applicant+",'"+applicant_reason+"',now())";
				}else {//其他部门状态直接到2
					sql = "INSERT INTO tf_overtime_application (state,date,department,applicant,applicant_reason,applicant_time) "+
							 "VALUES (2,DATE('"+startTime+"'),'"+departmentName+"',"+applicant+",'"+applicant_reason+"',now())";
				}				
				String applicationId = String.valueOf(CommUtil.insertAndBackId(sql));
				if(applicationId!=null && !applicationId.isEmpty() && !"0".equals(applicationId)) {
					String wxIdsArray[] = wxIds.split(",");
					String sqls = "";
					boolean boo = false; 
					for(String wxId : wxIdsArray){
						sqls = "INSERT INTO tf_overtime_member (applicationId,wxId,start_time,end_time) "+
								 "VALUES ("+applicationId+","+wxId+",'"+startTime+"','"+endTime+"')";
						boo = sh.update(sqls);
					}
					if(boo) {
						if("[4]".equals(department)) {//如果是生产部助理提交则推送给调度确认并填写加班理由
							//推送消息给生产部调度填写加班理由
							String title = "加班申请单填写加班理由通知";
							String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
							String[] managerInfo = getDepartmentDispatcherInfo(department);//获取调度员信息
							String touser = managerInfo[1];
							String toparty = "";
							String description = departmentName+"有一份"+startTime.substring(0, 10).replace("-", "/")+"的加班申请单待您填写加班理由，请及时处理！";
							try {
								sendWXMessage(title, url, touser, toparty, description);
							} catch (WexinReqException e) {
								ajax.setSuccess(false);
								ajax.setMessage("申请加班成功，推送消息失败！");
								return ajax;
							}
						}else {
							//推送消息给部门经理审核
							String title = "加班申请单审核通知";
							String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
							String[] managerInfo = getManagerInfo(department);//获取部门经理信息
							String touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
							String toparty = "";
							String description = departmentName+"有一份"+startTime.substring(0, 10).replace("-", "/")+"的加班申请单待您审核，请及时处理！";
							try {
								sendWXMessage(title, url, touser, toparty, description);
							} catch (WexinReqException e) {
								ajax.setSuccess(false);
								ajax.setMessage("申请加班成功，推送消息失败！");
								return ajax;
							}
						}					
					}else {
						ajax.setSuccess(false);
						ajax.setMessage("系统出错啦，请联系管理员！");
						return ajax;
					}
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("系统出错啦，请联系管理员！");
					return ajax;
				}
			}else if("1".equals(type)) {//班组级别提交
				//1.检验该班组长是否已提交过该日期的加班申请
				String sq = "SELECT teamApplicationId FROM tf_overtime_team_application WHERE TO_DAYS(date)=TO_DAYS('"+startTime+"') AND applicant = "+applicant;
				List<Object> list = sh.query(sq);
				if(list!=null && list.size()>0) {
					ajax.setSuccess(false);
					ajax.setMessage("您已提交过该日期的加班申请啦！");
					return ajax;
				}
				
				//2.检验是否已提交过该日期的加班申请
				String se = "SELECT applicationId FROM tf_overtime_application WHERE TO_DAYS(date)=TO_DAYS('"+startTime+"') AND department = '"+departmentName+"'";
				List<Object> le = sh.query(se);
				if(le!=null && le.size()>0) {
					ajax.setSuccess(false);
					ajax.setMessage("部门在该日期已提交过加班申请啦，请联系部门助理进行加班人员补充！");
					return ajax;
				}
				
				//3.检验该班组长提交的加班申请的人员名单中人员在该日期下是否已被提交申请过
				String wxIdsArray[] = wxIds.split(",");
				String sqls = "";
				for(String wxId : wxIdsArray){
					sqls = "select *,(select name from tf_wuxi_member where wxId='"+wxId+"') as name from tf_overtime_team_application  where TO_DAYS(date)=TO_DAYS('"+startTime+"') and (select find_in_set('"+wxId+"',members))>0";
					List<Object> lists = sh.query(sqls);
					if(lists!=null && lists.size()>0) {
						@SuppressWarnings("unchecked")
						Map<String, Object> maps = (Map<String, Object>) lists.get(0);				
						ajax.setSuccess(false);
						ajax.setMessage(maps.get("name")+" 在该日期已申请过加班啦，请联系助理确认！");
						return ajax;
					}
				}				
				
				String sql = "INSERT INTO tf_overtime_team_application (state,date,start_time,end_time,applicant,members,team,department,apply_time) "+
							 "VALUES (0,DATE('"+startTime+"'),'"+startTime+"','"+endTime+"',"+applicant+",'"+wxIds+"','"+team+"','"+departmentName+"',now())";
				boolean boo = sh.update(sql);
				if(boo) {
					//推送消息给部门助理审核
					String title = "班组加班申请单确认通知";
					String url = "/TFMobile/webpage/overtime/teamApplicationConfirm.html?departmentNo="+department+"&date="+startTime.substring(0, 10)+"";
					String[] managerInfo = getDepartmentAssistantInfo(department);
					String touser = managerInfo[1];
					String toparty = "";
					String description = team+"组有一份"+startTime.substring(0, 10).replace("-", "/")+"的加班申请单待您确认，请及时处理！";
					try {
						sendWXMessage(title, url, touser, toparty, description);
					} catch (WexinReqException e) {
						ajax.setSuccess(false);
						ajax.setMessage("申请加班成功，推送消息失败！");
						return ajax;
					}
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("系统出错啦，请联系管理员！");
					return ajax;
				}
			}
		}else {//补充申请
			String sql = "select a.* from tf_wuxi_member a left join tf_overtime_member b on a.wxId=b.wxId where b.wxId in ("+wxIds+") and DATE(b.start_time)=DATE('"+startTime+"')";
			List<Object> list = sh.query(sql);
			if(list!=null && list.size()>0) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(0);				
				ajax.setSuccess(false);
				ajax.setMessage(map.get("name")+" 在该日期已申请过加班啦！");
				return ajax;
			}else {
				//判断当前部门有没有在该日期下申请过加班记录
				String s = "select applicationId from tf_overtime_application where date=DATE('"+startTime+"') and department='"+departmentName+"'";
				List<Object> applicationList = sh.query(s);
				if(applicationList==null || applicationList.size()<=0) {
					ajax.setSuccess(false);
					ajax.setMessage("部门在该日期下还未申请过加班，不能补充加班人员！");
					return ajax;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) applicationList.get(0);
				String applicationId = String.valueOf(map.get("applicationId"));
				String sq = "INSERT INTO tf_overtime_supplement (state,applicationId,date,start_time,end_time,department,applicant,apply_time,apply_reason,members) "+
						 "VALUES (0,"+applicationId+",DATE('"+startTime+"'),'"+startTime+"','"+endTime+"','"+departmentName+"',"+applicant+",now(),'"+applicant_reason+"','"+wxIds+"')";
				String supplementId = String.valueOf(CommUtil.insertAndBackId(sq));
				if(supplementId!=null && !supplementId.isEmpty() && !"0".equals(supplementId)) {
					//推送消息给部门经理审核
					String title = "补充加班人员申请审核通知";
					String url = "/TFMobile/webpage/overtime/supplementForm.html?supplementId="+supplementId;
					String[] managerInfo = getManagerInfo(department);//获取部门经理信息
					String touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
					String toparty = "";
					String description = departmentName+"有一份"+startTime.substring(0, 10).replace("-", "/")+"的补充加班人员申请待您审核，请及时处理！";
					try {
						sendWXMessage(title, url, touser, toparty, description);
					} catch (WexinReqException e) {
						ajax.setSuccess(false);
						ajax.setMessage("申请补充加班人员成功，推送消息失败！");
						return ajax;
					}
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("系统出错啦，请联系管理员！");
					return ajax;
				}
			}					
		}
		
		return ajax;
	}
	
	/**
	 * 2、根据部门获取部门所有班组加班人员
	 * @param  department
	 * @throws ParseException 
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "findListByDepartment")
	@ResponseBody
	public AjaxJson findListByDepartment(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String departmentName = request.getParameter("departmentName");
		String date = request.getParameter("date");
		SQLHelper sh = new SQLHelper();
		String sql = "SELECT teamApplicationId,members FROM tf_overtime_team_application WHERE state = 0 and department = '"+departmentName+"' and date='"+date+"'";
		List<Object> list = sh.query(sql);
		List<Object> teamList = new ArrayList<Object>();
		List<Object> memberList = new ArrayList<Object>();
		if(list!=null && list.size()>0) {
			Map<String, Object> map;
			String teamApplicationId,members, sqls;
			for(int i=0;i<list.size();i++) {				
				map = (Map<String, Object>) list.get(i);
				teamApplicationId = String.valueOf(map.get("teamApplicationId"));
				members = String.valueOf(map.get("members"));
				sqls = "select * from tf_wuxi_member a left join tf_overtime_team_application b on b.teamApplicationId = '"+teamApplicationId+"' where a.wxId in ("+members+")";
				memberList = sh.query(sqls);
				teamList.add(memberList);
			  }
		}		
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();	
		body.put("list", teamList);
		ajax.setBody(body);
		return ajax;
	}

	/**
	 * 3、助理确认班组加班申请
	 * @param members,teamApplicationIds,applicant,applicant_reason,department
	 * @throws ParseException 
	 */
	@RequestMapping(value = "applyConfirm")
	@Transactional
	@ResponseBody
	public AjaxJson applyConfirm(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String members = request.getParameter("members");
		String teamApplicationIds = request.getParameter("teamApplicationIds");
		String applicant = request.getParameter("applicant");
		String applicant_reason = request.getParameter("applicant_reason");
		String department = request.getParameter("department");
		String departmentName = getDepartmentNameByCode(department);			
		
		SQLHelper sh = new SQLHelper();	
		String sql;
		if("[4]".equals(department)) {//生产部门状态为1
			sql = "INSERT INTO tf_overtime_application (state,date,department,applicant,applicant_time) "+
					"VALUES (1,DATE(now()),'"+departmentName+"','"+applicant+"',now())";
		}else {//其他部门状态为2
			sql = "INSERT INTO tf_overtime_application (state,date,department,applicant,applicant_reason,applicant_time) "+
					"VALUES (2,DATE(now()),'"+departmentName+"','"+applicant+"','"+applicant_reason+"',now())";
		}	
		String applicationId = String.valueOf(CommUtil.insertAndBackId(sql));
		if(applicationId!=null && !applicationId.isEmpty()) {
			
			//再跟新tf_overtime_team_application表状态
			String sqls = "UPDATE tf_overtime_team_application SET state = 1 WHERE teamApplicationId IN ("+teamApplicationIds+")";
			boolean boo = sh.update(sqls);
			
			//解析并保存加班人员
			String insql;
			if(!members.isEmpty()&&!"null".equals(members)) {
				JSONObject jsonObject = JSONObject.fromObject(members);
				String faultsArray = jsonObject.getString("members"); 
				JSONArray faultsJsonArray = JSONArray.fromObject(faultsArray); 
				if(faultsJsonArray!=null && faultsJsonArray.size()>0) {
					for(int j=0;j<faultsJsonArray.size();j++) {
						String wxId = String.valueOf(JSONArray.fromObject(faultsJsonArray.get(j)).get(0));
						String stime = String.valueOf(JSONArray.fromObject(faultsJsonArray.get(j)).get(1));
						String etime = String.valueOf(JSONArray.fromObject(faultsJsonArray.get(j)).get(2));
						insql = "INSERT INTO tf_overtime_member (applicationId,wxId,start_time,end_time) "+
								 "VALUES ("+applicationId+","+wxId+",'"+stime+"','"+etime+"')";
						boo = sh.update(insql);
					}
				}
			}
			if(boo) {
				if("[4]".equals(department)) {
					//推送消息给生产部调度填写加班理由
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
					String title = "班组加班申请单填写加班理由通知";
					String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
					String[] managerInfo = getDepartmentDispatcherInfo(department);
					String touser = managerInfo[1];
					String toparty = "";
					String description = departmentName+"有一份"+sdf.format(new Date())+"的加班申请单待您填写加班理由，请及时处理！";
					try {
						sendWXMessage(title, url, touser, toparty, description);
					} catch (WexinReqException e) {
						ajax.setSuccess(false);
						ajax.setMessage("提交加班申请成功，推送消息失败！");
						return ajax;
					}
				}else {
					//推送消息给部门经理审核
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
					String title = "加班申请单审核通知";
					String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
					String[] managerInfo = getManagerInfo(department);//获取部门经理信息
					String touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
					String toparty = "";
					String description = departmentName+"有一份"+sdf.format(new Date())+"的加班申请单待您审核，请及时处理！";
					try {
						sendWXMessage(title, url, touser, toparty, description);
					} catch (WexinReqException e) {
						ajax.setSuccess(false);
						ajax.setMessage("提交加班申请成功，推送消息失败！");
						return ajax;
					}
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
	 * 4、生产调度填写加班理由
	 * @param applicationId dispatcher applicant_reason
	 * @throws ParseException 
	 */
	@RequestMapping(value = "dispatcherConfirm")
	@ResponseBody
	public AjaxJson dispatcherConfirm(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer applicationId = Integer.parseInt(request.getParameter("applicationId"));
		String dispatcher = request.getParameter("dispatcher");
		String applicant_reason = request.getParameter("applicant_reason");
		String department = request.getParameter("department");
		String departmentName = getDepartmentNameByCode(department);
		
		SQLHelper sh = new SQLHelper();		
		String sql = "UPDATE tf_overtime_application SET state = 2,dispatcher = "+dispatcher+",applicant_reason = '"+applicant_reason+"',dispatcher_time = now() WHERE applicationId = " + applicationId;
		boolean boo = sh.update(sql);
		if(boo) {
			//推送消息给部门经理审核
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			String title = "加班申请单审核通知";
			String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
			String[] managerInfo = getManagerInfo(department);//获取部门经理信息
			String touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
			String toparty = "";
			String description = departmentName+"有一份"+sdf.format(new Date())+"的加班申请单待您审核，请及时处理！";
			try {
				sendWXMessage(title, url, touser, toparty, description);
			} catch (WexinReqException e) {
				ajax.setSuccess(false);
				ajax.setMessage("确认申请加班成功，推送消息失败！");
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
	 * 5、部门经理确认
	 * @param applicationId department manager
	 * @throws ParseException 
	 */
	@RequestMapping(value = "managerConfirm")
	@ResponseBody
	public AjaxJson managerConfirm(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer applicationId = Integer.parseInt(request.getParameter("applicationId"));
		String department = request.getParameter("department");
		String manager = request.getParameter("manager");
		String departmentName = getDepartmentNameByCode(department);
		
		int state = 5;//其他部门直接转到4状态
		if("[4]".equals(department)) {//生产部需推送给工程确认
			state = 3;
		}
		SQLHelper sh = new SQLHelper();		
		String sql = "UPDATE tf_overtime_application SET state = "+state+",manager = "+manager+",manager_time = now() WHERE applicationId = " + applicationId;
		boolean boo = sh.update(sql);
		if(boo) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			if("[4]".equals(department)) {//生产部需推送给工程确认				
				String title = "生产部加班申请单工程确认通知";
				String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
				String[] managerInfo = getProcessSupervisorInfo();//获取工艺主管信息
				String touser = managerInfo[1];
				String toparty = "";
				String description = "生产部有一份"+sdf.format(new Date())+"的加班申请单待工程确认，请及时处理！";
				try {
					sendWXMessage(title, url, touser, toparty, description);
				} catch (WexinReqException e) {
					ajax.setSuccess(false);
					ajax.setMessage("推送消息失败！");
					return ajax;
				}
			}else {//推送给中心副总
				String title = departmentName+"-加班申请单审核通知";
				String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
				String[] managerInfo = getGeneralInfo(department);//获取部门所属中心副总信息
				String touser = managerInfo[1];
				String toparty = "";
				String description = departmentName+"有一份"+sdf.format(new Date())+"的加班申请单待您审核，请及时处理！";
				try {
					sendWXMessage(title, url, touser, toparty, description);
				} catch (WexinReqException e) {
					ajax.setSuccess(false);
					ajax.setMessage("确认成功，推送消息失败！");
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
	 * 6、生产加班申请单工程确认
	 * @param applicationId engineer
	 * @throws ParseException 
	 */
	@RequestMapping(value = "engineerConfirm")
	@ResponseBody
	public AjaxJson engineerConfirm(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer applicationId = Integer.parseInt(request.getParameter("applicationId"));
		String engineer = request.getParameter("engineer");
		
		SQLHelper sh = new SQLHelper();		
		String sql = "UPDATE tf_overtime_application SET state = 5,engineer = "+engineer+",engineer_time = now() WHERE applicationId = " + applicationId;
		boolean boo = sh.update(sql);
		if(boo) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			//推送给中心副总
			String title = "生产部-加班申请单审核通知";
			String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
			String[] managerInfo = getGeneralInfo("[4]");//获取部门所属中心副总信息
			String touser = managerInfo[1];
			String toparty = "";
			String description = "生产部有一份"+sdf.format(new Date())+"的加班申请单待您审核，请及时处理！";
			try {
				sendWXMessage(title, url, touser, toparty, description);
			} catch (WexinReqException e) {
				ajax.setSuccess(false);
				ajax.setMessage("确认成功，推送消息失败！");
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
	 * 7、部门所属中心副总审核确认
	 * @param applicationId general
	 * @throws ParseException 
	 */
	@RequestMapping(value = "generalConfirm")
	@ResponseBody
	public AjaxJson generalConfirm(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		Integer applicationId = Integer.parseInt(request.getParameter("applicationId"));
		String departmentName = request.getParameter("departmentName");
		String general = request.getParameter("general");
		
		SQLHelper sh = new SQLHelper();		
		String sql = "UPDATE tf_overtime_application SET state = 0,general = "+general+",general_time = now() WHERE applicationId = " + applicationId;
		boolean boo = sh.update(sql);
		if(boo) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			//推送给中心副总
			String title = "加班申请单审核通过通知";
			String url = "/TFMobile/webpage/overtime/applicationForm.html?applicationId="+applicationId;
			String department = "";
			if("生产部".equals(departmentName)) {
				department = "[4]";
			}else if("工程部".equals(departmentName)) {
				department = "[3]";
			}else if("工业技术部".equals(departmentName)) {
				department = "[2]";
			}else if("仓储部".equals(departmentName)) {
				department = "[11]";
			}else if("质量部".equals(departmentName)) {
				department = "[5]";
			}else if("研发部".equals(departmentName)) {
				department = "[7]";
			}
			String[] managerInfo = getDepartmentManagerAndAssistantInfo(department);//获取部门经理和助理或调度
			String touser = managerInfo[1];
			String toparty = "";
			String description = sdf.format(new Date())+"的加班申请单已审核完结并通过，查看详情！";
			try {
				sendWXMessage(title, url, touser, toparty, description);
			} catch (WexinReqException e) {
				ajax.setSuccess(false);
				ajax.setMessage("确认成功，推送消息失败！");
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
	 * 通过主键获取加班申请记录信息
	 * @param applicationId
	 */
	@RequestMapping(value = "findInfo")
	@Transactional
	@ResponseBody
	public AjaxJson findInfo(AjaxJson ajax,HttpServletRequest request){	
		if(request.getParameter("applicationId")==null||request.getParameter("applicationId").isEmpty()){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		Integer applicationId = Integer.parseInt(request.getParameter("applicationId"));		
		SQLHelper sh = new SQLHelper();
		String sql = "select * from tf_overtime_application where applicationId = " + applicationId;
		List<Object> list = sh.query(sql);
		Object application = null;
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		List<Object> memberList = new ArrayList<Object>();
		if(list!=null && list.size()>0) {
			application = list.get(0);
			
		    //获取加班人员
			String sql0 = "select * from tf_overtime_member a left join tf_wuxi_member b on a.wxId = b.wxId where a.applicationId = " + applicationId;
			memberList = sh.query(sql0);			
		}
		body.put("application", application);
		body.put("memberList", memberList);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 通过主键获取补充加班人员申请记录信息
	 * @param supplementId
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "findSupplementInfo")
	@ResponseBody
	public AjaxJson findSupplementInfo(AjaxJson ajax,HttpServletRequest request){	
		if(request.getParameter("supplementId")==null||request.getParameter("supplementId").isEmpty()){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		Integer supplementId = Integer.parseInt(request.getParameter("supplementId"));		
		SQLHelper sh = new SQLHelper();
		String sql = "select * from tf_overtime_supplement where supplementId = " + supplementId;
		List<Object> list = sh.query(sql);
		Object supplement = null;
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		List<Object> memberList = new ArrayList<Object>();
		if(list!=null && list.size()>0) {
			supplement = list.get(0);
			Map<String, Object> map = (Map<String, Object>) supplement;
			String members = String.valueOf(map.get("members"));
			//获取补充加班人员名单
			String sql0 = "select * from tf_wuxi_member where wxId in ("+members+")";
			memberList = sh.query(sql0);		
		}
		body.put("supplement", supplement);
		body.put("memberList", memberList);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 部门经理确认补充加班人员
	 * @param supplementId memberId
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "supplementConfirm")
	@ResponseBody
	public AjaxJson supplementConfirm(AjaxJson ajax,HttpServletRequest request){
		String memberId = request.getParameter("memberId");
		if(request.getParameter("supplementId")==null||request.getParameter("supplementId").isEmpty()){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		Integer supplementId = Integer.parseInt(request.getParameter("supplementId"));	
		
		SQLHelper sh = new SQLHelper();
		//跟新当前补充信息记录
		String updateSql = "UPDATE tf_overtime_supplement SET state=1,reviewer='"+memberId+"',review_time=now() WHERE supplementId="+supplementId;
		boolean boo = sh.update(updateSql);
		if(boo) {
			//获取当前补充人员记录
			String sq = "select * from tf_overtime_supplement where supplementId = " + supplementId;
			List<Object> list = sh.query(sq);
			if(list!=null && list.size()>0) {
				Map<String, Object> map = (Map<String, Object>) list.get(0);
				String applicationId = String.valueOf(map.get("applicationId"));
				String members = String.valueOf(map.get("members"));
				String startTime = String.valueOf(map.get("start_time"));
				String endTime = String.valueOf(map.get("end_time"));
				String wxIdsArray[] = members.split(",");
				String sqls = "";
				for(String wxId : wxIdsArray){
					sqls = "INSERT INTO tf_overtime_member (applicationId,wxId,start_time,end_time,remark) "+
							 "VALUES ("+applicationId+","+wxId+",'"+startTime+"','"+endTime+"','补充加班')";
					sh.update(sqls);
				}				

			}else {
				ajax.setSuccess(false);
				ajax.setMessage("系统出错啦，请联系管理员！");
				ajax.setErrorCode("-1");
				return ajax;
			}
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦，请联系管理员！");
			ajax.setErrorCode("-1");
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
			throw new WexinReqException("发送消息出错啦！");
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
	 * 根据部门编号获取中心副总信息
	 * @param department
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getGeneralInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();
		String sql = "";
		if("[2]".equals(department) || "[4]".equals(department) || "[11]".equals(department)) {
			sql = "select * from tf_member where deleteFlag = 0 and userId = 'ZangYun'";
		}else if("[3]".equals(department)) {
			sql = "select * from tf_member where deleteFlag = 0 and userId = 'XuFeng'";
		}else if("[5]".equals(department)) {
			sql = "select * from tf_member where deleteFlag = 0 and userId = 'ZhouMin'";
		}else if("[7]".equals(department)) {
			sql = "select * from tf_member where deleteFlag = 0 and userId = 'HuangZhongNan'";
		}
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
	 * 根据部门编号获取该部门经理或审核人信息
	 * @param department
	 * @return managerMemberId
	 */
	@SuppressWarnings("unchecked")
	public static String[] getManagerInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and isleader = 1 and department = '"+department+"'";
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
	 * 根据部门编号获取部门助理
	 * @param department
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getDepartmentAssistantInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and position like '%助理' and department = '" + department + "'";
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
	 * 根据部门生产调度员信息
	 * @param department
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getDepartmentDispatcherInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and (position like '%调度%' or position like '%产能%') and department = '" + department + "'";
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
	 * 获取工艺主管信息
	 * @param department
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getProcessSupervisorInfo() {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and position like '%工艺主管%' and department = '[3]'";
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
	 * 根据部门编号获取部门助理或生产调度或经理
	 * @param department
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getDepartmentManagerAndAssistantInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and (position like '%助理' or position like '%调度%' or position like '%产能%' or isleader=1) and department = '" + department + "'";
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
	
	public static void main(String[] args) throws ParseException {
		System.out.println("2018-04-05 11:22".substring(0, 10).replace("-", "/"));
	}
}
