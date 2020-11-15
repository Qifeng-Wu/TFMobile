package com.modules.newException;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 * 异常通报控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/new-exception")
public class NewExceptionForm {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String savePicturePath = ConfigurationFileHelper.getSavePicturePath();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getNewExceptionSecret();
	private static String AgentId = ConfigurationFileHelper.getNewExceptionAgentId();
	
   /**
	 * 1、发布异常通报信息
	 * @param 问题描述
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "report")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson exceptionReport(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		String reporter = request.getParameter("reporter");
		String position = request.getParameter("position");
		String submitDepartment = request.getParameter("submitDepartment");
		String line = request.getParameter("line");
		String orderCategory = request.getParameter("orderCategory");
		String productCategory = request.getParameter("productCategory");
		String ordermaking = request.getParameter("ordermaking");
		String model = request.getParameter("model");
		Integer orderQuantity = Integer.parseInt(request.getParameter("orderQuantity"));
		Integer checkQuantity = Integer.parseInt(request.getParameter("checkQuantity"));
		Integer producedQuantity = Integer.parseInt(request.getParameter("producedQuantity"));
		Integer failuresQuantity = Integer.parseInt(request.getParameter("failuresQuantity"));
		Double defectiveRate = Double.parseDouble(request.getParameter("defectiveRate"));
		String findStation = request.getParameter("findStation");
		String exceptionDescription = request.getParameter("exceptionDescription");
		String exceptionPicture = request.getParameter("exceptionPicture");

		AjaxJson ajax = new AjaxJson();	
		if(memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] managerInfo = null;
		String touser = "";
		if("[5]".equals(department)) {
			if(position.contains("PQC") || position.contains("OQC")) {
				managerInfo = getQCManagerInfo(department);//获取质量控制部门经理信息
				touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
			}else {
				managerInfo = getQAManagerInfo(department);//获取品质保障部门经理信息
				touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
			}
		}else {
			managerInfo = getManagerInfo(department);//获取部门经理信息
			touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
		}

		if(touser==null || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}		
		
		String nextPerson = managerInfo[0];//获取下一流程处理者
		String serialNumber = "";//自动生成文件序列号
		//获取当前年份后两位
		String yearLast = new SimpleDateFormat("yy",Locale.CHINESE).format(Calendar.getInstance().getTime());
		//获取当前月份后两位
		String month = String.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1);
		if("10".equals(month)) {
			month = "A";
		}else if("11".equals(month)) {
			month = "B";
		}else if("12".equals(month)) {
			month = "C";
		}
		//判断发文部门
		String dep = "";
		if("生产部".equals(submitDepartment)) {
			dep = "PD";
		}else if("质量部".equals(submitDepartment)) {
			dep = "QC";
		}else if("工程部".equals(submitDepartment)) {
			dep = "ME";
		}
		//按规则生成文件序列号
		SQLHelper sqlhe = new SQLHelper();		
		String sql1 = "select serialNumber from tf_new_exception where (serialNumber like '"+dep+"%') and date_format(reporterTime,'%Y-%m')=date_format(now(),'%Y-%m') order by exceptionId DESC limit 1";
		List<Object> snList = sqlhe.query(sql1);

		if (snList!=null && snList.size()>0) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) snList.get(0);
			String sn = String.valueOf(map.get("serialNumber"));
			serialNumber = dep+yearLast+month+String.format("%03d", Integer.parseInt(sn.substring(sn.length()-3))+1);
		}else {
			serialNumber = dep+yearLast+month+"001";
		}
		
		//上传图片
		if(exceptionPicture!=null&&!exceptionPicture.isEmpty()) {	
			exceptionPicture = CommUtil.uploadPicture(savePicturePath,"exceptionPicture","exception_"+serialNumber,exceptionPicture);
		}
		
		String sql = "INSERT INTO tf_new_exception (state,nextPerson,reporterTime,reporter,serialNumber,submitDepartment,line,orderCategory,productCategory,ordermaking,model," + 
					 "orderQuantity,checkQuantity,producedQuantity,failuresQuantity,defectiveRate,findStation,exceptionDescription,exceptionPicture)" + 
					 " VALUES"
					 +" (0,'"+nextPerson+"',now(),"+memberId+",'"+serialNumber+"','"+submitDepartment+"','"+line+"','"+orderCategory+"','"+productCategory+"','"+ordermaking+"','"+model+
					 	 "',"+orderQuantity+","+checkQuantity+","+producedQuantity+","+failuresQuantity+","+defectiveRate+",'"+findStation+"','"+exceptionDescription+"','"+exceptionPicture+"')";
		String exceptionId = String.valueOf(CommUtil.insertAndBackId(sql));
		if(exceptionId!=null && !"".equals(exceptionId)){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(exceptionDescription.length()>38) {
						exceptionDescription = exceptionDescription.substring(0, 37)+"...";
					}
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"            发文人："+reporter+"\n制令单号："+ordermaking+"  机型："+model+"\n发文时间 ："+sdf.format(new Date())+"\n异常描述 ："+exceptionDescription+"\n待您审核 ，请及时处理，点击查看详情！");
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
	 * 2、异常通报信息修改
	 * @param 问题描述
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "exceptionEdit")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson exceptionEdit(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String line = request.getParameter("line");
		String orderCategory = request.getParameter("orderCategory");
		String productCategory = request.getParameter("productCategory");
		String ordermaking = request.getParameter("ordermaking");
		String model = request.getParameter("model");
		Integer orderQuantity = Integer.parseInt(request.getParameter("orderQuantity"));
		Integer checkQuantity = Integer.parseInt(request.getParameter("checkQuantity"));
		Integer producedQuantity = Integer.parseInt(request.getParameter("producedQuantity"));
		Integer failuresQuantity = Integer.parseInt(request.getParameter("failuresQuantity"));
		Double defectiveRate = Double.parseDouble(request.getParameter("defectiveRate"));
		String findStation = request.getParameter("findStation");
		String reporter = request.getParameter("reporter");
		String position = request.getParameter("position");
		String exceptionDescription = request.getParameter("exceptionDescription");
	
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] managerInfo = null;
		String touser = "";
		if("[5]".equals(department)) {
			if(position.contains("PQC") || position.contains("OQC")) {
				managerInfo = getQCManagerInfo(department);//获取质量控制部门经理信息
				touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
			}else {
				managerInfo = getQAManagerInfo(department);//获取质量控制部门经理信息
				touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
			}
		}else {
			managerInfo = getManagerInfo(department);//获取部门经理信息
			touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
		}
		
		if(touser==null || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String nextPerson = managerInfo[0];//获取下一流程处理者	
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "UPDATE tf_new_exception SET state=0,nextPerson='"+nextPerson+"',line = '"+line+"',orderCategory = '"+orderCategory+
					 "',productCategory = '"+productCategory+"',ordermaking = '"+ordermaking+"',model = '"+model+"',orderQuantity = "+orderQuantity+
					 ",checkQuantity="+checkQuantity+",producedQuantity="+producedQuantity+",failuresQuantity="+failuresQuantity+",defectiveRate="+defectiveRate+
					 ",findStation='"+findStation+"',exceptionDescription='"+exceptionDescription+"',reporter="+memberId+",reporterTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(exceptionDescription.length()>38) {
						exceptionDescription = exceptionDescription.substring(0, 37)+"...";
					}
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报修改审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("修改人："+reporter+"\n修改时间 ："+sdf.format(new Date())+"\n异常描述 ："+exceptionDescription+"\n待您审核 ，请及时处理，点击查看详情！");
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
	 * 3、问题描述部门经理审核
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "reportManager")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson reportManager(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] departmentEngineerMemberInfo = getDepartmentEngineerMemberInfo("[5]");//获取质量部工程师（不包含经理）成员信息
		String nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
		String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
		String toparty = "";//发送模板消息部门编号
		
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',reportManager = "+ memberId+", reportManagerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);

		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					//获去异常详情
					String sql1 = "select * from tf_new_exception a left join tf_member b on a.reporter = b.memberId where a.exceptionId = " + exceptionId;
					List<Object> list = sqlhe.query(sql1);
					if (list!=null && list.size()>0) {
						@SuppressWarnings("unchecked")
						Map<String, Object> map = (Map<String, Object>) list.get(0);
						String reporter = String.valueOf(map.get("name"));
						String reporterTime = String.valueOf(map.get("reporterTime"));
						String ordermaking = String.valueOf(map.get("ordermaking"));
						String model = String.valueOf(map.get("model"));
						String exceptionDescription = String.valueOf(map.get("exceptionDescription"));
						if(exceptionDescription.length()>38) {
							exceptionDescription = exceptionDescription.substring(0, 37)+"...";
						}
													
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//1.发送消息给质量控制部门做质量判定处理
						textcard.setTitle("异常通报质量判定处理通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号："+String.valueOf(map.get("serialNumber"))+"             发文人："+reporter+"\n制令单号："+ordermaking+"  机型："+model+"\n发文时间 ："+reporterTime+"\n异常描述 ："+exceptionDescription+"\n待您质量判定 ，请及时处理，点击查看详情！");
						textcardMessage.setTouser(touser);
						textcardMessage.setToparty(toparty);
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
	 * 4、问题描述部门经理驳回
	 * @param exceptionId,memberId,reporter
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "reportManagerRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson reportManagerRefuse(HttpServletRequest request) throws WexinReqException{
		String reporter = request.getParameter("reporter");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String reportManagerRefuse = request.getParameter("reportManagerRefuse");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||reporter==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		String nextPerson = getMemberIdByUserId(reporter);
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',reportManager = "+memberId+",reportManagerRefuse = '"+reportManagerRefuse+"',reportManagerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("您发布文件序号为 "+serialNumber+" 的异常通报已被审核经理驳回，点击查看详情！");
					textcardMessage.setTouser(reporter);
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
	 * 5、异常通报撤销
	 * @param exceptionId,memberId,reporter
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "exceptionDelete")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson exceptionDelete(HttpServletRequest request) throws WexinReqException{
		String reporter = request.getParameter("reporter");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String ordermaking = request.getParameter("ordermaking");
		String deleteReason = request.getParameter("deleteReason");
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||reporter==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//判断通报发给哪些部门
		String toparty = "";
		if(serialNumber.startsWith("PD")) {
			toparty = "4";//发给生产和质量
		}else if(serialNumber.startsWith("QC")) {
			toparty = "5";//发给质量
		}else if(serialNumber.startsWith("ME")) {
			toparty = "3";//发给工程和质量
		}
		
		SQLHelper sqlhe = new SQLHelper();			
		String sql = "UPDATE tf_new_exception SET deleteFlag = 1,deleteMan = "+memberId+",deleteReason = '"+deleteReason+"',deleteTime=now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报撤销通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"             发文人："+reporter+"\n制令单号："+ordermaking+"  机型："+model+"\n异常描述 ："+exceptionDescription+"\n点击查看详情！");
					textcardMessage.setTouser("");
					textcardMessage.setToparty(toparty);
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
	 * 6、质量判定处理信息保存
	 * @param exceptionId,memberId,state,problemAttribution,handlingOpinions
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "determineHandle")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson determineHandle(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String exceptionType = request.getParameter("exceptionType");
		String handlingDescription = request.getParameter("handlingDescription");
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String findStation = request.getParameter("findStation");
		String determinePicture = request.getParameter("determinePicture");
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] managerInfo = null;
		String touser = "";
		if("[5]".equals(department)) {
			if("PQC".equals(findStation) || "OQC".equals(findStation)) {
				managerInfo = getQCManagerInfo(department);//获取质量控制部门经理信息
				touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
			}else {
				managerInfo = getQAManagerInfo(department);//获取品质保障部门经理信息
				touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
			}
		}else {
			managerInfo = getManagerInfo(department);//获取部门经理信息
			touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
		}
		
		if(touser==null || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}		
		String nextPerson = managerInfo[0];//获取下一流程处理者		
		
		//上传图片
		if(determinePicture!=null&&!determinePicture.isEmpty()) {
			determinePicture = CommUtil.uploadPicture(savePicturePath,"exceptionPicture","determine_"+serialNumber,determinePicture);
		}
				
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',determineHandler = "+memberId+",exceptionType = '"+exceptionType+
							"',handlingDescription = '"+handlingDescription+"',determinePicture = '"+determinePicture+"',verifyManager=null, determineHandlerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报质量判定审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId+"&state="+state);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您审核 ，请及时处理，点击查看详情！");
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
	 * 7、质量判定部门经理审核
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "determineManager")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson determineManager(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionType = request.getParameter("exceptionType");
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		if("普通类".equals(exceptionType)) {
			
			String[] departmentEngineerMemberInfo = getDepartmentEngineerMemberInfo("[3]");//获取工程部工程师（不包含经理）成员信息
			String nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
			String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
			String toparty = "";//发送模板消息部门编号
			
			SQLHelper sqlhe = new SQLHelper();
			String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',determineManager ="+ memberId +", determineManagerTime = now() where exceptionId = " + exceptionId;
			boolean isSuccess = sqlhe.update(sql);

			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						textcard.setTitle("异常通报责任部门处理通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您处理 ，请及时处理，点击查看详情！");
						textcardMessage.setTouser(touser);
						textcardMessage.setToparty(toparty);
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
		}else if("CCC类".equals(exceptionType)) {//CCC类型通报(到体系专员维护验证结果)
			
			String[] systemCommissionerInfo = getSystemCommissionerInfo("[5]");//获取部门下体系专员信息
			String nextPerson = systemCommissionerInfo[0];//获取待处理人memberId字符串
			String touser = systemCommissionerInfo[1];//获取待处理人userId字符串,发送模板消息
			String toparty = "";//发送模板消息部门编号

			state = 10;//到质量验证
			SQLHelper sqlhe = new SQLHelper();
			List<String> sqlist = new ArrayList<>();		
			String upfanSql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',determineManager ="+ memberId +", determineManagerTime = now() where exceptionId = " + exceptionId;
			sqlist.add(upfanSql);
			boolean isSuccess = sqlhe.update(sqlist);

			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						//1.发送给体系专员
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						textcard.setTitle("CCC类异常通报验证维护通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您维护验证 ，请及时处理，点击查看详情！");
						textcardMessage.setTouser(touser);
						textcardMessage.setToparty(toparty);
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
						
						//2.发送给工程和生产
						TextcardMessage textcardMessage1 = new TextcardMessage();
						Textcard textcard1 = new Textcard();
						textcard1.setTitle("CCC类异常通报信息通知");
						textcard1.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard1.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n请知悉，点击查看详情！");
						textcardMessage1.setTouser("");
						textcardMessage1.setToparty("3|4");
						textcardMessage1.setMsgtype("textcard");
						textcardMessage1.setAgentid(Integer.parseInt(AgentId));
						textcardMessage1.setTextcard(textcard1);
						JSONObject result1 = WxCommonAPI.sendMssage(access_token, textcardMessage1);
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
		}else if("能效类".equals(exceptionType)) { //能效类型通报(到体系专员维护验证结果)
			
			String[] departmentEngineerMemberInfo = getSystemCommissionerInfo("[5]");//获取部门下体系专员信息
			String nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
			String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
			String toparty = "";//发送模板消息部门编号
		
			state = 10;//到质量验证
			
			SQLHelper sqlhe = new SQLHelper();
			String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',determineManager ="+ memberId +", determineManagerTime = now() where exceptionId = " + exceptionId;
			boolean isSuccess = sqlhe.update(sql);

			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						//1.发送给体系专员
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						textcard.setTitle("能效类异常通报验证维护通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您维护验证 ，请及时处理，点击查看详情！");
						textcardMessage.setTouser(touser);
						textcardMessage.setToparty(toparty);
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
						
						//2.发送给工程和生产
						TextcardMessage textcardMessage1 = new TextcardMessage();
						Textcard textcard1 = new Textcard();
						textcard1.setTitle("能效类异常通报信息通知");
						textcard1.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard1.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n请知悉，点击查看详情！");
						textcardMessage1.setTouser("");
						textcardMessage1.setToparty("3|4");
						textcardMessage1.setMsgtype("textcard");
						textcardMessage1.setAgentid(Integer.parseInt(AgentId));
						textcardMessage1.setTextcard(textcard1);
						JSONObject result1 = WxCommonAPI.sendMssage(access_token, textcardMessage1);
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
		}
		
		return ajax;
	}
	
	/**
	 * 8、质量判定驳回
	 * @param exceptionId,memberId,touser,toparty,state,submitdept
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "determineManagerRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson determineManagerRefuse(HttpServletRequest request) throws WexinReqException{
		String reporter = request.getParameter("reporter");
		String touser = request.getParameter("touser");
		String toparty = request.getParameter("toparty");
		String determineManagerRefuse = request.getParameter("determineManagerRefuse");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String nextPerson = getMemberIdByUserId(reporter);
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',determineManagerRefuse='"+determineManagerRefuse+"',determineManager ="+ memberId +", determineManagerTime = now() where exceptionId="+exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报质量判定驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("发布文件序号为"+serialNumber+"的异常通报已被质量判定审核经理驳回，驳回理由："+determineManagerRefuse+"。点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 9、责任部门处理信息保存
	 * @param exceptionId,memberId,state,exceptionReason,exceptionSolve,needSignDepartment
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "responsibility")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson responsibility(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String exceptionReason = request.getParameter("exceptionReason");
		String exceptionSolve = request.getParameter("exceptionSolve");
		String isSign = request.getParameter("isSign");
		String isRework = request.getParameter("isRework");
		String problemAttribution = request.getParameter("problemAttribution");
		String longExceptionSolve = request.getParameter("longExceptionSolve");
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}

		String[] managerInfo = getManagerInfo(department);//获取部门经理信息
		String touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
		if(touser==null || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String nextPerson = managerInfo[0];//获取下一流程处理者	
		
		SQLHelper sqlhe = new SQLHelper();		
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',responsibilityHandler ="+memberId+",exceptionReason = '"+exceptionReason+"',exceptionSolve = '"+exceptionSolve+
						  "',isRework = '"+isRework+"',problemAttribution = '"+problemAttribution+"',isSign = '"+isSign+"',longExceptionSolve = '"+longExceptionSolve+"', responsibilityHandlerTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报责任部门审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您审核，请及时处理，点击查看详情！");
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
	 * 10、责任部门部门经理审核
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "responsibilityManager")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson responsibilityManager(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String isSign = request.getParameter("isSign");
		String signDepartment = request.getParameter("signDepartment");//会签部门
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		String nextPerson= "";//下一流程处理着memberId
		//1.如果需要会签部门会签，则流到部门会签
		if("是".equals(isSign)){
			//解析需要会签的部门成员
			String[] departmentEngineerMemberInfo = getDepartmentEngineerMemberInfo(signDepartment);//获取会签部门工程师信息
			nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
			String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
			String toparty = "";//发送模板消息部门编号
			
			SQLHelper sqlhe = new SQLHelper();
			String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',responsibilityManager ="+ memberId +", responsibilityManagerTime = now() where exceptionId = " + exceptionId;
			boolean isSuccess = sqlhe.update(sql);

			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//根据会签部门发送给签部门经理确认	
						if(signDepartment!=null && !"".equals(signDepartment)) {									    												
							textcard.setTitle("异常通报部门会签处理通知");
							textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
							textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"..."+"\n待您会签处理，请及时处理，点击查看详情！");
							textcardMessage.setTouser(touser);
							textcardMessage.setToparty(toparty);
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
				
				
		}else {//2.如果不需要会签部门会签，则直接到质量验证部门处理
			
			state = 10;//改变异常通报状态
			
			String[] departmentEngineerMemberInfo = getDepartmentEngineerMemberInfo("[5]");//获取质量部工程师（不包含经理）成员信息
			nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
			String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
			String toparty = "";//发送模板消息部门编号
			
			
			SQLHelper sqlhe = new SQLHelper();
			List<String> sqlist = new ArrayList<>();		
			String upfanSql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',responsibilityManager ="+ memberId +", responsibilityManagerTime = now() where exceptionId = " + exceptionId;
			sqlist.add(upfanSql);
			boolean isSuccess = sqlhe.update(sqlist);

			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {	
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//推送质量验证通知
						textcard.setTitle("异常通报质量验证处理通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您质量验证处理，请及时处理，点击查看详情！");
						textcardMessage.setTouser(touser);
						textcardMessage.setToparty(toparty);
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
			
		}

		return ajax;
	}	
	
	/**
	 * 11、责任（工程）部门经理质量判定驳回
	 * @param exceptionId,memberId,touser,toparty,state,submitdept
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "responsibilityManagerRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson responsibilityManagerRefuse(HttpServletRequest request) throws WexinReqException{
		String responsibilityManagerRefuse = request.getParameter("responsibilityManagerRefuse");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] departmentEngineerMemberInfo = getDepartmentEngineerMemberInfo("[5]");//获取质量部工程师（不包含经理）成员信息
		String nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
		String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
		String toparty = "";//发送模板消息部门编号
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',responsibilityManagerRefuse='"+responsibilityManagerRefuse+"',responsibilityManager ="+ memberId +", responsibilityManagerTime = now() where exceptionId="+exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报工程驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号为 "+serialNumber+" 的异常通报质量判定已被工程部审核经理驳回，驳回理由："+responsibilityManagerRefuse+"。点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 12、会签部门处理信息保存
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "signHandle")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson signHandle(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		String longExceptionSolve = request.getParameter("longExceptionSolve");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] managerInfo = getManagerInfo(department);//获取部门经理信息
		String touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
		if(touser==null || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String nextPerson = managerInfo[0];//获取下一流程处理者	
		
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',longExceptionSolve = '"+longExceptionSolve+"',signHandler = "+memberId+",signHandlerTime = now() where exceptionId="+exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报转签审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您审核，请及时处理，点击查看详情！");
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
	 * 13、部门会签经理确认
	 * @param exceptionId,memberId,signManagerDescription
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "signManager")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson signManager(HttpServletRequest request) throws WexinReqException{
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] departmentEngineerMemberInfo = getDepartmentEngineerMemberInfo("[5]");//获取质量部工程师（不包含经理）成员信息
		String nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
		String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
		String toparty = "";//发送模板消息部门编号
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',signManager ="+ memberId +", signManagerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);

		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {	
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					//推送质量验证通知
					textcard.setTitle("异常通报质量验证处理通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您质量验证处理，请及时处理，点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 14、会签部门经理驳回
	 * @param exceptionId,memberId
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "signManagerRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson signManagerRefuse(HttpServletRequest request) throws WexinReqException{
		String signManagerRefuse = request.getParameter("signManagerRefuse");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] departmentEngineerMemberInfo = getDepartmentEngineerMemberInfo("[3]");//获取工程部工程师（不包含经理）成员信息
		String nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
		String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
		String toparty = "";//发送模板消息部门编号
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',signManagerRefuse='"+signManagerRefuse+"',signManager ="+ memberId +", signManagerTime = now() where exceptionId="+exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报转签驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号为 "+serialNumber+" 的异常通报已被转签部审核经理驳回，驳回理由："+signManagerRefuse+"。点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 15、质量验证信息保存
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "finalVerify")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson finalVerify(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String verifyConclusion = request.getParameter("verifyConclusion");
		String isLongSolveMeasure = request.getParameter("isLongSolveMeasure");
		String problemDepartment = request.getParameter("problemDepartment");
		String longSolveMeasureBrief = request.getParameter("longSolveMeasureBrief");
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}
		String verifyResult = request.getParameter("verifyResult");
		String verifyPicture = request.getParameter("verifyPicture");
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] managerInfo = getManagerInfo(department);//获取部门经理信息
		String touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
		if(touser==null || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		
		//上传质量验证图片		
		if(verifyPicture!=null&&!verifyPicture.isEmpty()) {	
			verifyPicture = CommUtil.uploadPicture(savePicturePath,"exceptionPicture","verify_"+serialNumber,verifyPicture);
		}
				
		String nextPerson = managerInfo[0];//获取下一流程处理者		
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',verifyHandler ="+memberId+",verifyConclusion = '"+verifyConclusion+"',verifyResult = '"+verifyResult+"',verifyPicture = '"+verifyPicture+
				"',longSolveMeasureBrief = '"+longSolveMeasureBrief+"',isLongSolveMeasure = '"+isLongSolveMeasure+"',problemDepartment = '"+problemDepartment+"', verifyHandlerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		//发送消息给经理通知审核				
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报质量验证审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您审核，请及时处理，点击查看详情！");		
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
	 * 16、质量验证经理审核
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "verifyManager")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson verifyManager(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		String isLongSolveMeasure = request.getParameter("isLongSolveMeasure");
		String problemDepartment = request.getParameter("problemDepartment");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
			
		String nextPerson = "";
		String toparty = "";
		String touser = "";
		if("是".equals(isLongSolveMeasure)) {//需要长期方案
			//判断部门编号
			if("工程部".equals(problemDepartment)) {
				String[] departmentMember = getDepartmentMemberInfo("[3]");		
				nextPerson = departmentMember[0];//获取部门下工程师以上的成员memberI的字符串集合
				touser = departmentMember[1];//获取工程师和质量验证经理userId的字符串
			}else if("生产部".equals(problemDepartment)) {
				String[] departmentMember = getDepartmentMemberInfo("[4]");		
				nextPerson = departmentMember[0];//获取部门下工程师以上的成员memberI的字符串集合
				touser = departmentMember[1];//获取工程师和质量验证经理userId的字符串
			}else if("质量部".equals(problemDepartment)) {
				String[] SQEMember = getSQEMemberInfo("[5]");					
				nextPerson = SQEMember[0];//获取SQE工程师和质量验证经理memberId的字符串;
				touser = SQEMember[1];//获取SQE工程师和质量验证经理userId的字符串
			}else if("研发部".equals(problemDepartment)) {
				String[] departmentMember = getDepartmentMemberInfo("[7]");		
				nextPerson = departmentMember[0];//获取部门下工程师以上的成员memberI的字符串集合
				touser = departmentMember[1];//获取工程师和质量验证经理userId的字符串
			}
		}else {
			state = 20;//状态值代表结案
			//判断通报发给哪些部门
			if(serialNumber.startsWith("PD")) {
				toparty = "4|5";//发给生产和质量
			}else if(serialNumber.startsWith("QC")) {
				toparty = "5";//发给质量
			}else if(serialNumber.startsWith("ME")) {
				toparty = "3|5";//发给工程和质量
			}
		}
		
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',verifyManager = "+memberId +", verifyManagerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(exceptionDescription.length()>38) {
						exceptionDescription = exceptionDescription.substring(0, 37)+"...";
					}
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					if("是".equals(isLongSolveMeasure)) {//需要长期方案	
						textcard.setTitle("异常通报责任归属部门长期方案处理通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您处理 ，请及时处理，点击查看详情！");
						
					}else {
						textcard.setTitle("异常通报流程结案通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n异常问题通报流程已结案，点击查看详情！");		
											
					}
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 17、质量验证经理驳回
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "verifyManagerRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson verifyManagerRefuse(HttpServletRequest request) throws WexinReqException{
		String dep = request.getParameter("dep");
		String verifyManagerRefuse = request.getParameter("verifyManagerRefuse");
		String serialNumber = request.getParameter("serialNumber");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		String[] departmentMemberInfo = getDepartmentMemberInfo(dep);//获取部门工程师（含经理）成员信息
		String nextPerson = departmentMemberInfo[0];//获取待处理人memberId字符串
		String touser = departmentMemberInfo[1];//获取待处理人userId字符串,发送模板消息
		String toparty = "";//发送模板消息部门编号
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson = '"+nextPerson +"',verifyManagerRefuse = '"+verifyManagerRefuse +"',verifyManager = "+memberId +", verifyManagerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报质量验证驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId+"&mark"+state);
					textcard.setDescription("文件序号为 "+serialNumber+" 的异常通报质量验证已被驳回，点击查看详情！");
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 18、责任部门长期方案处理保存
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "longSolveMeasure")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson longSolveMeasure(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String department = request.getParameter("department");
		String serialNumber = request.getParameter("serialNumber");
		String measure = request.getParameter("measure");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String touser = "";
		String toparty = "";
		if(department.equals("[3]")) {
			toparty = "3|5";//发给工程和质量
		}else if(department.equals("[4]")) {
			toparty = "4|5";//发给生产和质量
		}else if(department.equals("[5]")) {
			toparty = "5";//发给质量
		}else if(department.equals("[7]")) {
			toparty = "5|7";//发给质量和研发
		}
		
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String sql1 = "UPDATE tf_new_exception SET state="+state+" where exceptionId = " + exceptionId;
		String sql2 = "INSERT INTO tf_new_exception_measure (exceptionId,measure,submitter,createTime) VALUES ("+exceptionId+",'"+measure+"',"+memberId+",now())";
		sqlist.add(sql1);
		sqlist.add(sql2);
		boolean isSuccess = sqlhe.update(sqlist);

		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(exceptionDescription.length()>38) {
						exceptionDescription = exceptionDescription.substring(0, 37)+"...";
					}
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报长期方案记录更新通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n长期方案记录已更新，点击查看详情！");		
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 19、长期方案结束跟踪
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "endLongSolveMeasure")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson endLongSolveMeasure(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] departmentEngineerMemberInfo = getDepartmentEngineerMemberInfo("[5]");//获取部门工程师（不含经理）成员信息
		String nextPerson = departmentEngineerMemberInfo[0];//获取待处理人memberId字符串
		String touser = departmentEngineerMemberInfo[1];//获取待处理人userId字符串,发送模板消息
		String toparty = "";//发送模板消息部门编号
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',longSolveMeasureEnder ="+memberId+",longSolveMeasureEnderTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报长期方案跟踪验证通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您验证处理，请及时处理，点击查看详情！");		
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 20、质量部门长期方案验证保存
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "longSolveMeasureVerify")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson longSolveMeasureVerify(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String department = request.getParameter("department");
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		String longSolveMeasureVerifyDescription = request.getParameter("longSolveMeasureVerifyDescription");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String[] managerInfo = getManagerInfo(department);//获取部门经理信息
		String touser = managerInfo[1];//根据部门号找到对应经理领导发送消息
		if(touser==null || touser.isEmpty()) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String nextPerson = managerInfo[0];//获取下一流程处理者		
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',longSolveMeasureVerifyDescription='"+longSolveMeasureVerifyDescription+"',longSolveMeasureVerifier ="+memberId+",longSolveMeasureVerifierTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报长期方案验证审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您长期方案验证审核，请及时处理，点击查看详情！");		
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
	 * 21、长期方案验证经理审核
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "longSolveMeasureReviewer")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson longSolveMeasureReviewer(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
			
		String nextPerson = "";
		String toparty = "";
		String touser = "";
		state = 20;//状态值代表结案
		
		//判断通报发给哪些部门
		if(serialNumber.startsWith("PD")) {
			toparty = "4|5";//发给生产和质量
		}else if(serialNumber.startsWith("QC")) {
			toparty = "5";//发给质量
		}else if(serialNumber.startsWith("ME")) {
			toparty = "3|5";//发给工程和质量
		}
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',longSolveMeasureReviewer = "+memberId +", longSolveMeasureReviewerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					if(exceptionDescription.length()>38) {
						exceptionDescription = exceptionDescription.substring(0, 37)+"...";
					}
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报流程结案通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n异常问题通报流程已结案，点击查看详情！");													
					textcardMessage.setTouser(touser);
					textcardMessage.setToparty(toparty);
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
	 * 22、长期方案验证经理驳回长期方案
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "longSolveMeasureRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson longSolveMeasureRefuse(HttpServletRequest request) throws WexinReqException{
	String longSolveMeasureRefuse = request.getParameter("longSolveMeasureRefuse");
	String problemDepartment = request.getParameter("problemDepartment");
	Integer memberId = Integer.parseInt(request.getParameter("memberId"));
	Integer state = Integer.parseInt(request.getParameter("state"));
	Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
	String serialNumber = request.getParameter("serialNumber");
	
	AjaxJson ajax = new AjaxJson();	
	if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
		ajax.setSuccess(false);
		ajax.setMessage("传入参数有误！");
		ajax.setErrorCode("-1");
		return ajax;
	}
	
	//判断长期责任部门
	String problemDepartmentNo = "";
	if("生产部".equals(problemDepartment)) {
		problemDepartmentNo = "[4]";
	}else if("工程部".equals(problemDepartment)) {
		problemDepartmentNo = "[3]";
	}else if("质量部".equals(problemDepartment)) {
		problemDepartmentNo = "[5]";
	}else if("研发部".equals(problemDepartment)) {
		problemDepartmentNo = "[7]";
	}
	
	String[] departmentMemberInfo = getDepartmentMemberInfo(problemDepartmentNo);//获取责任部门工程师（含经理）成员信息
	String nextPerson = departmentMemberInfo[0];//获取待处理人memberId字符串
	String touser = departmentMemberInfo[1];//获取待处理人userId字符串,发送模板消息
	String toparty = "";//发送模板消息部门编号
	
	SQLHelper sqlhe = new SQLHelper();
	List<String> sqlist = new ArrayList<>();		
	String sql1 = "UPDATE tf_new_exception SET state="+state+",nextPerson='"+nextPerson+"',longSolveMeasureRefuse='"+longSolveMeasureRefuse+"',longSolveMeasureReviewer ="+ memberId +", longSolveMeasureReviewerTime = now() where exceptionId="+exceptionId;
	String sql2 = "UPDATE tf_new_exception_measure SET refuseFlag = 1 where exceptionId="+exceptionId;
	sqlist.add(sql1);
	sqlist.add(sql2);
	boolean isSuccess = sqlhe.update(sqlist);
	
	if(isSuccess){
		String access_token = "";
		try {
			access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
			if(access_token!=null && !"".equals(access_token)) {
				
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();
				textcard.setTitle("长期方案被驳回通知");
				textcard.setUrl(serviceUrl+"/TFMobile/webpage/newException/exceptionForm.html?exceptionId="+exceptionId);
				textcard.setDescription("文件序号为"+serialNumber+"异常通报的长期方案已被质量验证经理驳回\n驳回理由："+longSolveMeasureRefuse+"\n点击查看详情！");
				textcardMessage.setTouser(touser);
				textcardMessage.setToparty(toparty);
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
	 * 0、通过主键获取异常记录信息
	 * @param exceptionId
	 */
	@RequestMapping(value = "findInfo")
	@Transactional
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_new_exception where exceptionId = " + exceptionId;
		List<Object> list = sqlhe.query(sql);
		Object exception = null;
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		if(list!=null && list.size()>0) {
			exception = list.get(0);
		}
		body.put("exception", exception);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 0、通过主键获取异常通报的长期方案记录信息
	 * @param exceptionId
	 */
	@RequestMapping(value = "findLongSolveMeasure")
	@Transactional
	@ResponseBody
	public AjaxJson findLongSolveMeasure(HttpServletRequest request){
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_new_exception_measure a left join tf_member b on a.submitter=b.memberId where a.exceptionId = " + exceptionId;
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("measure", list);
		ajax.setBody(body);
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
	 * 获取质量控制部经理或审核人信息
	 * @param department
	 * @return managerMemberId
	 */
	@SuppressWarnings("unchecked")
	public static String[] getQCManagerInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and isleader = 1 and (position like '质量控制部%' or position like '%主管') and department = '"+department+"'";
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
	 * 获取品质保障部经理或审核人信息
	 * @param department
	 * @return managerMemberId
	 */
	@SuppressWarnings("unchecked")
	public static String[] getQAManagerInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and isleader = 1 and (position like '品质保障部%' or position like '%主管') and department = '"+department+"'";
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
	 * 根据部门编号获取该部门下工程师以上（不包含经理）成员信息
	 * @param department
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getDepartmentEngineerMemberInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and isleader = 0 and (position like '%主管' or position like '%工程师') and department = '" + department + "'";
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
	 * 根据部门编号获取该部门下工程师以上（包含经理）成员信息
	 * @param department
	 * @return infoArray
	 */
	@SuppressWarnings("unchecked")
	public static String[] getDepartmentMemberInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and (position like '%经理' or position like '%主管' or position like '%工程师') and department = '" + department + "'";
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
	 * 获取质量部体系专员信息
	 * @param toparty
	 * @return userIds
	 */
	@SuppressWarnings("unchecked")
	public static String[] getSystemCommissionerInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and (position like '%体系%专员' or position like '%文控%专员') and department = '" + department + "'";
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
	 * 获取质量部品质工程师（SQE）信息
	 * @param toparty
	 * @return userIds
	 */
	@SuppressWarnings("unchecked")
	public static String[] getSQEMemberInfo(String department) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where deleteFlag = 0 and position = '品质工程师' and department = '" + department + "'";
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
	public static void main(String[] args) {
		System.out.println(getDepartmentMemberInfo("3")[0]);
	}
}