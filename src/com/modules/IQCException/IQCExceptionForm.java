package com.modules.IQCException;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * IQC物料异常通报接口控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/iqc-exception")
public class IQCExceptionForm {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getIQCExceptionSecret();
	private static String AgentId = ConfigurationFileHelper.getIQCExceptionAgentId();
	
   /**
	 * 1、发布异常通报信息
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "submit")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson exceptionReport(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		String reporter = request.getParameter("reporter");
		String inspectionNumber = request.getParameter("inspectionNumber");
		String materialCode = request.getParameter("materialCode");
		String arrivalQuantity = request.getParameter("arrivalQuantity");
		String inspectionQuantity = request.getParameter("inspectionQuantity");
		String failuresQuantity = request.getParameter("failuresQuantity");
		String arrivalDate = request.getParameter("arrivalDate");
		String inspectionDate = request.getParameter("inspectionDate");
		String supplier = request.getParameter("supplier");
		String factory = request.getParameter("factory");
		String inspector = request.getParameter("inspector");
		String materialDescription = request.getParameter("materialDescription");
		String description = request.getParameter("description");
		String touser = request.getParameter("touser");

		AjaxJson ajax = new AjaxJson();	
		if(memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//判断是否有审核人
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置发文审核人员，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String nextPerson = getMemberIdByUserId(touser);//获取下一流程处理者
		//创建生成文件序列号
		SimpleDateFormat sdf = new SimpleDateFormat("yyMM");	//获取当前年份和月份
		String serialNumber = "SQMIQC"+sdf.format(new Date());
		//获取系统内当前年份月份下的最后一条数据的serialNumber
		SQLHelper sqlhe = new SQLHelper();		
		String snSql = "select * FROM tf_iqc_exception where date_format(reportTime,'%Y-%m')=date_format(now(),'%Y-%m') order by reportTime DESC limit 1";
		List<Object> list = sqlhe.query(snSql);
		if (list!=null && list.size()>0) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) list.get(0);
			String lastSerialNumber = String.valueOf(map.get("serialNumber"));
			serialNumber += String.format("%03d", Integer.parseInt(lastSerialNumber.substring(lastSerialNumber.length()-3))+1);
		}else {
			serialNumber += "001";
		}
		
		
		String sql = "INSERT INTO tf_iqc_exception (state,nextPerson,serialNumber,inspectionNumber,materialCode,arrivalQuantity,inspectionQuantity,failuresQuantity,arrivalDate," + 
					 "inspectionDate,supplier,factory,inspector,materialDescription,description,reporter,reportTime)" + 
					 " VALUES"
					 +" (0,'"+nextPerson+"','"+serialNumber+"','"+inspectionNumber+"','"+materialCode+"','"+arrivalQuantity+"','"+inspectionQuantity+"','"+failuresQuantity+"','"+arrivalDate+"','"+inspectionDate+"','"+supplier+"','"+factory+"','"+inspector+"','"+materialDescription+"','"+description+"',"+memberId+",now())";
		String exceptionId = String.valueOf(CommUtil.insertAndBackId(sql));
		if(exceptionId!=null && !exceptionId.isEmpty() && !"null".equals(exceptionId)){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !access_token.isEmpty() && !"null".equals(access_token)) {
					if(description.length()>38) {
						description = description.substring(0, 37)+"...";
					}
					SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("物料异常通报审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号 ："+serialNumber+"   发文人 ："+reporter+"\n检验批号 ："+inspectionNumber+"\n物料编号 ："+materialCode+"\n发文时间 ："+sd.format(new Date())+"\n故障明细 ："+description+"\n待您审核 ，请及时处理，点击查看详情！");
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
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		String reporter = request.getParameter("reporter");
		String serialNumber = request.getParameter("serialNumber");
		String inspectionNumber = request.getParameter("inspectionNumber");
		String materialCode = request.getParameter("materialCode");
		String arrivalQuantity = request.getParameter("arrivalQuantity");
		String inspectionQuantity = request.getParameter("inspectionQuantity");
		String failuresQuantity = request.getParameter("failuresQuantity");
		String arrivalDate = request.getParameter("arrivalDate");
		String inspectionDate = request.getParameter("inspectionDate");
		String supplier = request.getParameter("supplier");
		String factory = request.getParameter("factory");
		String inspector = request.getParameter("inspector");
		String materialDescription = request.getParameter("materialDescription");
		String description = request.getParameter("description");
		String touser = request.getParameter("touser");
	
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未发文审核人员，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String nextPerson = getMemberIdByUserId(touser);//获取下一流程处理者	
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "UPDATE tf_iqc_exception SET state=0,nextPerson='"+nextPerson+"',inspectionNumber = '"+inspectionNumber+"',materialCode = '"+materialCode+
					 "',arrivalQuantity = '"+arrivalQuantity+"',inspectionQuantity = '"+inspectionQuantity+"',failuresQuantity = '"+failuresQuantity+
					 "',arrivalDate = '"+arrivalDate+"',inspectionDate='"+inspectionDate+"',supplier='"+supplier+"',factory='"+factory+"',inspector='"+inspector+
					 "',materialDescription='"+materialDescription+"',description='"+description+"',reporter="+memberId+",reportTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !access_token.isEmpty() && !"null".equals(access_token)) {
					if(description.length()>38) {
						description = description.substring(0, 37)+"...";
					}
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("物料异常通报修改审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("修改人 ："+reporter+"\n文件序号 ："+serialNumber+"\n修改时间 ："+sdf.format(new Date())+"\n故障明细 ："+description+"\n待您审核 ，请及时处理，点击查看详情！");
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
	 * 3、问题描述审核
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "reportReview")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson reportManager(HttpServletRequest request) throws WexinReqException{
		String handler = request.getParameter("handler");
		String serialNumber = request.getParameter("serialNumber");
		String inspectionNumber = request.getParameter("inspectionNumber");
		String description = request.getParameter("description");
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
		
		String nextPerson = handler;//获取下以流程处理人memberId
		//通过handler解析成userId发送消息
		String touser = "";
		if(!handler.isEmpty()) {
			String handlerUserIds = "";
			if(handler.contains(",")) {
				String handlerArray[] =handler.split(",");
				String handlerUserId = "";//获取成员的userId
				for(String handlerMemberId : handlerArray){
					handlerUserId = getUserIdByMemberId(handlerMemberId);
					if(handlerUserId==null || handlerUserId.isEmpty()) {
						continue;
					}
					handlerUserIds += handlerUserId +"|";			
				}
			}else {
				handlerUserIds = getUserIdByMemberId(handler);
			}
			touser = handlerUserIds;
		}		
		
		SQLHelper sqlhe = new SQLHelper();		
		List<String> sqlist = new ArrayList<>();		
		String sql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='"+nextPerson+"',reviewer = "+ memberId+", reviewTime = now() where exceptionId = " + exceptionId;
		sqlist.add(sql);
		boolean isSuccess = sqlhe.update(sqlist);

		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !access_token.isEmpty() && !"null".equals(access_token)) {
					if(description.length()>38) {
						description = description.substring(0, 37)+"...";
					}
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("物料异常处理通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号 ："+serialNumber+"\n检验批号 ："+inspectionNumber+"\n故障明细 ："+description+"\n待您处理 ，请及时处理，点击查看详情！");
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
	 * 4、供应品质部意见处理
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "handle")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson responsibilityManager(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String needSignDepartment = request.getParameter("needSignDepartment");//需要会签部门
		String handleDescription = request.getParameter("handleDescription");
		String serialNumber = request.getParameter("serialNumber");
		String inspectionNumber = request.getParameter("inspectionNumber");
		String description = request.getParameter("description");
		if(description.length()>38) {
			description = description.substring(0, 37)+"...";
		}

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		String nextPerson= "";//下一流程处理着memberId
		//1.如果不需要会签部门会签，则直接到IQC处理结果
		if(needSignDepartment==null || needSignDepartment.isEmpty() || "null".equals(needSignDepartment) || "无".equals(needSignDepartment)){
				state = 4;//改变异常通报状态
				String touser = "NanJuXianSheng|XieJianMei";
				nextPerson = getMemberIdByUserId(touser);
				
				SQLHelper sqlhe = new SQLHelper();
				List<String> sqlist = new ArrayList<>();		
				String upfanSql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='"+nextPerson+"',needSignDepartment='"+needSignDepartment+"',handleDescription='"+handleDescription+"',handler ="+ memberId +", handleTime = now() where exceptionId = " + exceptionId;
				sqlist.add(upfanSql);
				boolean isSuccess = sqlhe.update(sqlist);

				if(isSuccess){
					String access_token = "";
					try {
						access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
						if(access_token!=null && !"".equals(access_token)) {	
							TextcardMessage textcardMessage = new TextcardMessage();
							Textcard textcard = new Textcard();
							//推送IQC处理结果处理
							textcard.setTitle("物料异常通报IQC处理通知");
							textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
							textcard.setDescription("文件序号 ："+serialNumber+"\n检验批号 ："+inspectionNumber+"\n故障明细 ："+description+"\n待您处理 ，请及时处理，点击查看详情！");
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
				
				
		}else {//2.如果需要会签部门会签，则流到部门会签
			//解析需要会签的部门成员
			String needSignDepartmentIds = needSignDepartment.replaceAll("常规物料采购部", "12").replaceAll("研发部", "7")
								.replaceAll("工程部", "3").replaceAll("生产部", "4").replaceAll("质量部", "5")
								.replaceAll("储运部", "11");
			String partyArray[] = needSignDepartmentIds.split(",");
			String signMemberId = "";//获取会签部门成员(工程师以上)的memberId
			String signMemberIds = "";
			for(String party : partyArray){
				signMemberId = getMemberIdStrByToparty(party);
				if(signMemberId==null || "".equals(signMemberId)) {
					continue;
				}
				signMemberIds += signMemberId +",";			
			}
			if(!"".equals(signMemberIds) && signMemberIds.substring(signMemberIds.length()-1).equals(",")){				
				signMemberIds = signMemberIds.substring(0,signMemberIds.length()-1);
			}
			
			nextPerson = signMemberIds;
			//解析需要会签的部门成员发送消息
			String topartyArray[] = signMemberIds.split(",");
			String userId ="";
			String userIds="";
			for(String touser : topartyArray){
				userId = getUserIdByMemberId(touser);
				if(userId==null || "".equals(userId)) {
					continue;
				}
				userIds += userId +"|";			
			}
			SQLHelper sqlhe = new SQLHelper();
			List<String> sqlist = new ArrayList<>();		
			String upfanSql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='"+nextPerson+"',needSignDepartment='"+needSignDepartment+"',handleDescription='"+handleDescription+"',handler ="+ memberId +", HandleTime = now() where exceptionId = " + exceptionId;
			sqlist.add(upfanSql);
			boolean isSuccess = sqlhe.update(sqlist);

			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//根据不同的会签部门发送给不同的会签部门
						if(userIds!=null && !"".equals(userIds)) {									    												
							textcard.setTitle("物料异常通报部门会签处理通知");
							textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
							textcard.setDescription("文件序号 ："+serialNumber+"\n检验批号 ："+inspectionNumber+"\n故障明细 ："+description+"\n待您处理 ，请及时处理，点击查看详情！");
							textcardMessage.setTouser(userIds);
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
	 * 5、会签部门处理信息保存
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "signHandle")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson signHandle(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		String signHandleDescription = request.getParameter("signHandleDescription");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String inspectionNumber = request.getParameter("inspectionNumber");
		String description = request.getParameter("description");
		if(description.length()>38) {
			description = description.substring(0, 37)+"...";
		}
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		String touser = getManagerUserId(department);//根据部门号找到对应经理领导发送消息
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String nextPerson = "";//获取下一流程处理者
		
		//解析存储会签处理人的处理信息
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();	
		String sql = "select * from tf_iqc_exception where exceptionId="+exceptionId;
		List<Object> list1 = sqlhe.query(sql);	
		Map<String, Object> map = new HashMap<String, Object>();
		String signHandleDepartment = "";
		String signHandlerInfo = "";
		if(list1!=null && list1.size()>0 && !"".equals(department)){
			map = (Map<String, Object>) list1.get(0);
			String depart = department.replace("[", "").replace("]", "");
			//解析下一流程处理人
			String nextPersons[] = String.valueOf(map.get("nextPerson")).split(",");
			String departmentPersonsArray[] = getMemberIdStrByToparty(depart).split(",");
			for(int i=0;i<nextPersons.length;i++) {
				boolean pass = true;
				for(int j=0;j<departmentPersonsArray.length;j++) {
					if(nextPersons[i].equals(departmentPersonsArray[j])) {
						pass= false;
						break;
					}
				} 
				if(pass) {
					nextPerson = nextPerson+nextPersons[i]+",";
				}
			} 
			nextPerson = nextPerson + getManagerMemberId(department.replace("[", "").replace("]", ""));
			if(!"".equals(nextPerson)&&nextPerson.substring(nextPerson.length()-1).equals(",")){
				nextPerson = nextPerson.substring(0,nextPerson.length()-1);
			}
			
			String purchaseSignInfo = "";//常规物料采购部
			String rdSignInfo = "";//研发部
			String engineeringSignInfo = "";//工程部
			String productionSignInfo = "";//生产部
			String qualitySignInfo = "";//质量部
			String stSignInfo = "";//储运部
			//判断是哪个部门的
			if("12".equals(depart)) {
				signHandleDepartment = "purchaseSignInfo";
				purchaseSignInfo  = "[[\""+memberId+"\",\""+df.format(new Date())+"\",\""+signHandleDescription+"\"]]";	
				signHandlerInfo = purchaseSignInfo;
			}else if("7".equals(depart)) {
				signHandleDepartment = "rdSignInfo";
				rdSignInfo  = "[[\""+memberId+"\",\""+df.format(new Date())+"\",\""+signHandleDescription+"\"]]";
				signHandlerInfo = rdSignInfo;
			}else if("3".equals(depart)) {
				signHandleDepartment = "engineeringSignInfo";
				engineeringSignInfo  = "[[\""+memberId+"\",\""+df.format(new Date())+"\",\""+signHandleDescription+"\"]]";
				signHandlerInfo = engineeringSignInfo;
			}else if("4".equals(depart)) {
				signHandleDepartment = "productionSignInfo";
				productionSignInfo  = "[[\""+memberId+"\",\""+df.format(new Date())+"\",\""+signHandleDescription+"\"]]";
				signHandlerInfo = productionSignInfo;
			}else if("5".equals(depart)) {
				signHandleDepartment = "qualitySignInfo";
				qualitySignInfo  = "[[\""+memberId+"\",\""+df.format(new Date())+"\",\""+signHandleDescription+"\"]]";
				signHandlerInfo = qualitySignInfo;
			}else if("11".equals(depart)) {
				signHandleDepartment = "stSignInfo";
				stSignInfo  = "[[\""+memberId+"\",\""+df.format(new Date())+"\",\""+signHandleDescription+"\"]]";
				signHandlerInfo = stSignInfo;
			}

		}
			
		String updateSql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='"+nextPerson+"',"+signHandleDepartment+" = '"+signHandlerInfo+"' where exceptionId = " + exceptionId;
		sqlist.add(updateSql);
		boolean isSuccess = sqlhe.update(sqlist);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("物料异常通报会签审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号 ："+serialNumber+"\n检验批号 ："+inspectionNumber+"\n故障明细 ："+description+"\n待您审核 ，请及时处理，点击查看详情！");
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
	public static String [] splitStr(String str){
		return str.split(",");
	}
	/**
	 * 6、会签部门审核确认
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "signConfirm")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson signConfirm(HttpServletRequest request) throws WexinReqException{
		String department = request.getParameter("department");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String inspectionNumber = request.getParameter("inspectionNumber");
		String description = request.getParameter("description");
		if(description.length()>38) {
			description = description.substring(0, 37)+"...";
		}
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String nextPerson = "";//获取下一流程处理者		
		//解析存储会签处理人的处理信息
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();	
		String sql = "select * from tf_iqc_exception where exceptionId="+exceptionId;
		List<Object> list1 = sqlhe.query(sql);	
		Map<String, Object> map = new HashMap<String, Object>();
	    String signDepartment = "";
		String signHandleDepartment = "";
		String signHandlerInfo = "";
		if(list1!=null && list1.size()>0 && !"".equals(department)){
			map = (Map<String, Object>) list1.get(0);
			signDepartment = String.valueOf(map.get("signDepartment"))==""?"":String.valueOf(map.get("signDepartment"));
			//System.out.println(String.valueOf(map.get("signDepartment")));
			//System.out.println(signDepartment);
			String needSignDepartment = String.valueOf(map.get("needSignDepartment"));
			String depart = department.replace("[", "").replace("]", "");
			//解析下一流程处理人
			String nextPersons[] = String.valueOf(map.get("nextPerson")).split(",");
			String departmentPersonsArray[] = getMemberIdStrByToparty(depart).split(",");
			for(int i=0;i<nextPersons.length;i++) {
				boolean pass = true;
				for(int j=0;j<departmentPersonsArray.length;j++) {
					if(nextPersons[i].equals(departmentPersonsArray[j])) {
						pass= false;
						break;
					}
				} 
				if(pass) {
					nextPerson = nextPerson+nextPersons[i]+",";
				}
			} 
			if(!"".equals(nextPerson)&&nextPerson.substring(nextPerson.length()-1).equals(",")){
				nextPerson = nextPerson.substring(0,nextPerson.length()-1);
			}
			
			String purchaseSignInfo = String.valueOf(map.get("purchaseSignInfo"));//常规物料采购部
			String rdSignInfo = String.valueOf(map.get("rdSignInfo"));//研发部
			String engineeringSignInfo = String.valueOf(map.get("engineeringSignInfo"));//工程部
			String productionSignInfo = String.valueOf(map.get("productionSignInfo"));//生产部
			String qualitySignInfo = String.valueOf(map.get("qualitySignInfo"));//质量部
			String stSignInfo = String.valueOf(map.get("stSignInfo"));//储运部
			//判断是哪个部门的
			if("12".equals(depart)) {
				JSONArray jsonArray = JSONArray.fromObject(purchaseSignInfo); 
				if(jsonArray.size()==1) {
					jsonArray.add(JSONArray.fromObject("[\""+memberId+"\",\""+df.format(new Date())+"\",\"\"]"));
					purchaseSignInfo = String.valueOf(jsonArray);
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("该部门会签已确认！");
					ajax.setErrorCode("-3");
					return ajax;
				}
				signHandleDepartment = "purchaseSignInfo";
				signHandlerInfo = purchaseSignInfo;
				signDepartment = signDepartment+"常规物料采购部,";

			}else if("7".equals(depart)) {
				JSONArray jsonArray = JSONArray.fromObject(rdSignInfo); 
				if(jsonArray.size()==1) {
					jsonArray.add(JSONArray.fromObject("[\""+memberId+"\",\""+df.format(new Date())+"\",\"\"]"));
					rdSignInfo = String.valueOf(jsonArray);
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("该部门会签已确认！");
					ajax.setErrorCode("-3");
					return ajax;
				}
				signHandleDepartment = "rdSignInfo";
				signHandlerInfo = rdSignInfo;
				signDepartment = signDepartment+"研发部,";

			}else if("3".equals(depart)) {
				JSONArray jsonArray = JSONArray.fromObject(engineeringSignInfo); 
				if(jsonArray.size()==1) {
					jsonArray.add(JSONArray.fromObject("[\""+memberId+"\",\""+df.format(new Date())+"\",\"\"]"));
					engineeringSignInfo = String.valueOf(jsonArray);
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("该部门会签已确认！");
					ajax.setErrorCode("-3");
					return ajax;
				}
				signHandleDepartment = "engineeringSignInfo";
				signHandlerInfo = engineeringSignInfo;
				signDepartment = signDepartment+"工程部,";

			}else if("4".equals(depart)) {
				JSONArray jsonArray = JSONArray.fromObject(productionSignInfo); 
				if(jsonArray.size()==1) {
					jsonArray.add(JSONArray.fromObject("[\""+memberId+"\",\""+df.format(new Date())+"\",\"\"]"));
					productionSignInfo = String.valueOf(jsonArray);
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("该部门会签已确认！");
					ajax.setErrorCode("-3");
					return ajax;
				}
				signHandleDepartment = "productionSignInfo";
				signHandlerInfo = productionSignInfo;
				signDepartment = signDepartment+"生产部,";

			}else if("5".equals(depart)) {
				JSONArray jsonArray = JSONArray.fromObject(qualitySignInfo); 
				if(jsonArray.size()==1) {
					jsonArray.add(JSONArray.fromObject("[\""+memberId+"\",\""+df.format(new Date())+"\",\"\"]"));
					qualitySignInfo = String.valueOf(jsonArray);
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("该部门会签已确认！");
					ajax.setErrorCode("-3");
					return ajax;
				}
				signHandleDepartment = "qualitySignInfo";
				signHandlerInfo = qualitySignInfo;
				signDepartment = signDepartment+"质量部,";

			}else if("11".equals(depart)) {
				JSONArray jsonArray = JSONArray.fromObject(stSignInfo); 
				if(jsonArray.size()==1) {
					jsonArray.add(JSONArray.fromObject("[\""+memberId+"\",\""+df.format(new Date())+"\",\"\"]"));
					stSignInfo = String.valueOf(jsonArray);
				}else {
					ajax.setSuccess(false);
					ajax.setMessage("该部门会签已确认！");
					ajax.setErrorCode("-3");
					return ajax;
				}
				signHandleDepartment = "stSignInfo";
				signHandlerInfo = stSignInfo;
				signDepartment = signDepartment+"储运部,";
			}
			
			
			if(splitStr(needSignDepartment).length==splitStr(signDepartment).length)
			{//判断如果是最后一个会签的话发送推送消息
				String touser = "NanJuXianSheng|XieJianMei";
				nextPerson = getMemberIdByUserId(touser);//获取下一流程处理者
				state = 4;
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//推送IQC处理结果处理
						textcard.setTitle("物料异常通报IQC处理通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号 ："+serialNumber+"\n检验批号 ："+inspectionNumber+"\n故障明细 ："+description+"\n待您处理 ，请及时处理，点击查看详情！");
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
				
			}			
		}
	
		//String updateSql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='"+nextPerson+"',"+signHandleDepartment+" = '"+signHandlerInfo+"',"+signDepartment+" = '"+signDepartment+"' where exceptionId = " + exceptionId;			
		String updateSql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='"+nextPerson+"',"+signHandleDepartment+" = '"+signHandlerInfo+"',signDepartment = '"+signDepartment+"' where exceptionId = " + exceptionId;
		sqlist.add(updateSql);
		sqlhe.update(sqlist);		
		return ajax;
	}
	
		
	/**
	 * 7、IQC处理结果信息保存
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "iqcHandle")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson iqcHandle(HttpServletRequest request) throws WexinReqException{
		//String department = request.getParameter("department");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String inspectionNumber = request.getParameter("inspectionNumber");
		String description = request.getParameter("description");
		String iqcHandleDescription = request.getParameter("iqcHandleDescription");
		if(description.length()>38) {
			description = description.substring(0, 37)+"...";
		}
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		String touser = "ZhouMin";//getManagerUserId(department);//根据部门号找到对应经理领导发送消息
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String nextPerson = "28";//getManagerMemberId(department.replace("[", "").replace("]", ""));//获取下一流程处理者
		
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='"+nextPerson+"',iqcHandler ="+memberId+",iqcHandleDescription = '"+iqcHandleDescription+
						  "',iqcHandleTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("物料异常IQC处理结果审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号 ："+serialNumber+"\n检验批号 ："+inspectionNumber+"\n故障明细 ："+description+"\n待您处理 ，请及时处理，点击查看详情！");		
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
	 * 8、IQC处理驳回
	 * @param exceptionId,memberId,state...
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "iqcHandleRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson iqcHandleRefuse(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String inspectionNumber = request.getParameter("inspectionNumber");
		Integer handlerMemberId = Integer.parseInt(request.getParameter("handlerMemberId"));
		String handlerUserId = request.getParameter("handlerUserId");
		String iqcHandleRefusedReason = request.getParameter("iqcHandleRefusedReason");
		
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String updateSql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='"+handlerMemberId+"',iqcHandler ="+memberId+",iqcHandleRefusedReason = '"+iqcHandleRefusedReason+
						  "',iqcHandleTime = now() where exceptionId = " + exceptionId;
		sqlist.add(updateSql);
		boolean isSuccess = sqlhe.update(sqlist);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("IQC处理驳回至供应品质部处理通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号 ："+serialNumber+"\n检验批号 ："+inspectionNumber+"\n驳回原因 ："+iqcHandleRefusedReason+"\n待您处理 ，请及时处理，点击查看详情！");		
					textcardMessage.setTouser(handlerUserId);
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
	 * 9、IQC处理结果经理审核
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "iqcHandleConfirm")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson iqcHandleConfirm(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer state = Integer.parseInt(request.getParameter("state"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String serialNumber = request.getParameter("serialNumber");
		String inspectionNumber = request.getParameter("inspectionNumber");
		String description = request.getParameter("description");
		String needSignDepartment = request.getParameter("needSignDepartment");
		if(description.length()>38) {
			description = description.substring(0, 37)+"...";
		}

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//判断通报发给哪些部门
		String toparty = "5";
		if(!needSignDepartment.isEmpty() && !"null".equals(needSignDepartment) && !"无".equals(needSignDepartment)) {
			if(needSignDepartment.contains(",")) {
				String needSignDepartmentArray[] = needSignDepartment.split(",");
				for(int i=0;i<needSignDepartmentArray.length;i++) {
					if(needSignDepartmentArray[i].equals("常规物料采购部")) {
						toparty += "|12";
					}
					if(needSignDepartmentArray[i].equals("研发部")) {
						toparty += "|7";
					}
					if(needSignDepartmentArray[i].equals("工程部")) {
						toparty += "|2";
					}
					if(needSignDepartmentArray[i].equals("生产部")) {
						toparty += "|3";
					}
					if(needSignDepartmentArray[i].equals("储运部")) {
						toparty += "|11";
					}
				}
			}else {
				if(needSignDepartment.equals("常规物料采购部")) {
					toparty += "|12";
				}
				if(needSignDepartment.equals("研发部")) {
					toparty += "|7";
				}
				if(needSignDepartment.equals("工程部")) {
					toparty += "|2";
				}
				if(needSignDepartment.equals("生产部")) {
					toparty += "|3";
				}
				if(needSignDepartment.equals("储运部")) {
					toparty += "|11";
				}
			}
			
		}

		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_iqc_exception SET state="+state+",nextPerson='',iqcReviewer = "+memberId +", iqcReviewTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);

		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
				
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("物料异常通报流程完结通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/IQCException/exceptionForm.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号 ："+serialNumber+"\n检验批号 ："+inspectionNumber+"\n故障明细 ："+description+"\n待您处理 ，请及时处理，点击查看详情！");		
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
		String sql = "select * from tf_iqc_exception where exceptionId = " + exceptionId;
		System.out.print(sql);
		List<Object> list = sqlhe.query(sql);
		System.out.print(list);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("exception", list.get(0));
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 1、获取质量工程师信息
	 * @param exceptionId
	 */
	@RequestMapping(value = "findQualityEngineer")
	@Transactional
	@ResponseBody
	public AjaxJson findQualityEngineer(HttpServletRequest request){		
		AjaxJson ajax = new AjaxJson();		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_member where deleteFlag = 0 and (position like '%主管' or position like '%工程师' or position like '%助理') and department = '[5]' order by memberId desc";				
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
		
	/**
	 * 从数据库获取对应userId的memberId
	 * @param userIds
	 * @return memberIds
	 */
	@SuppressWarnings("unchecked")
	public static String getMemberIdByUserId(String userIds) {
        String userIdArray[] = userIds.split("\\|");
        String sql = "select * from tf_member where userId = ";
        for (int i=0; i<userIdArray.length; i++) {
        	if(i==0) {
        		sql = sql + "'" + userIdArray[i] + "'";
        	}else {
        		sql = sql + " or userId = '" + userIdArray[i] + "'"; 
        	}
        }
		
		SQLHelper sqlhe = new SQLHelper();
		List<Object> result = sqlhe.query(sql);
		String memberIds = "";
		if (!result.isEmpty() && result != null && result.size()>0) {
			for(int i=0;i<result.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) result.get(i);
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
	 * 从数据库获取对应memberId的userId
	 * @param memberId
	 * @return userId
	 */
	@SuppressWarnings("unchecked")
	public static String getUserIdByMemberId(String memberId) {
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select userId from tf_member where memberId = '" + memberId + "'";
		List<Object> result = sqlhe.query(sql);
		if (!result.isEmpty() && result != null) {
			Map<String, Object> map = (Map<String, Object>) result.get(0);
			String userId = String.valueOf(map.get("userId"));
			return userId;
		}
		return null;
	}
	
	/**
	 * 根据部门编号获取该部门下工程师以上成员的memberId字符串
	 * @param toparty
	 * @return memberIdStr
	 */
	@SuppressWarnings("unchecked")
	public static String getMemberIdStrByToparty(String toparty) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and (position like '%主管' or position like '%工程师' or position like '物料%') and department = '["+toparty+"]'";				
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
	public static String getManagerMemberId(String department) {
		//根据部门号找到对应经理领导memberId发送消息
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and isleader = 1 and department = '["+department+"]'";
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
	 * 根据部门编号获取该部门经理或审核人userId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getManagerUserId(String department) {
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
}