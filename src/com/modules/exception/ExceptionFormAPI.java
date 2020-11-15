package com.modules.exception;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * 异常通报控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/exception")
public class ExceptionFormAPI {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String savePicturePath = ConfigurationFileHelper.getSavePicturePath();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getExceptionSecret();
	private static String AgentId = ConfigurationFileHelper.getExceptionAgentId();
	
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

		AjaxJson ajax = new AjaxJson();	
		if(memberId==null||"".equals(request.getParameter("memberId"))){
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
		String pendingPerson = getManagerMemberId(department.replace("[", "").replace("]", ""));//获取下一流程处理者
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
		String sql1 = "select serialNumber from tf_exception where (serialNumber like '"+dep+"%') and date_format(reporterTime,'%Y-%m')=date_format(now(),'%Y-%m') order by exceptionId DESC limit 1";
		List<Object> snList = sqlhe.query(sql1);

		if (snList!=null && snList.size()>0) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) snList.get(0);
			String sn = String.valueOf(map.get("serialNumber"));
			serialNumber = dep+yearLast+month+String.format("%03d", Integer.parseInt(sn.substring(sn.length()-3))+1);
		}else {
			serialNumber = dep+yearLast+month+"001";
		}
		
		
		String sql = "INSERT INTO tf_exception (state,pendingPerson,reporterTime,reporter,serialNumber,submitDepartment,line,orderCategory,productCategory,ordermaking,model," + 
					 "orderQuantity,checkQuantity,producedQuantity,failuresQuantity,defectiveRate,findStation,exceptionDescription)" + 
					 " VALUES"
					 +" (0,'"+pendingPerson+"',now(),"+memberId+",'"+serialNumber+"','"+submitDepartment+"','"+line+"','"+orderCategory+"','"+productCategory+"','"+ordermaking+"','"+model+"',"+orderQuantity+","+checkQuantity+","+producedQuantity+","+failuresQuantity+","+defectiveRate+",'"+findStation+"','"+exceptionDescription+"')";
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
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
		String exceptionDescription = request.getParameter("exceptionDescription");
	
		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))){
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
		String pendingPerson = getManagerMemberId(department.replace("[", "").replace("]", ""));//获取下一流程处理者	
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "UPDATE tf_exception SET state=0,pendingPerson='"+pendingPerson+"',line = '"+line+"',orderCategory = '"+orderCategory+
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
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
		String touser = request.getParameter("touser");
		String toparty = request.getParameter("toparty");
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
		
		String pendingPerson = getMemberIdStrByToparty(toparty);//获取部门下的工程师以上成员memberId的字符串集合
		
		SQLHelper sqlhe = new SQLHelper();		
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',reportManager = "+ memberId+", reportManagerTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);

		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					//获去异常详情
					String sql = "select * from tf_exception a left join tf_member b on a.reporter = b.memberId where a.exceptionId = " + exceptionId;
					List<Object> list = sqlhe.query(sql);
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
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
					
					//2.发送消息通知所有人有异常问题通报表信息
					
						/*textcard.setTitle("异常通报发布通知");
						textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
						textcard.setDescription("文件序号："+String.valueOf(map.get("serialNumber"))+"             发文人："+reporter+"\n制令单号："+ordermaking+"  机型："+model+"\n发文时间 ："+reporterTime+"\n异常描述 ："+exceptionDescription+"\n点击查看详情！");
						textcardMessage.setTouser("");
						textcardMessage.setToparty("2|3|4|6|7|8|9|10|11|12|13");
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
						}*/
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
		String pendingPerson = getMemberIdByUserId(reporter);
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',reportManager = "+memberId+",reportManagerRefuse = '"+reportManagerRefuse+"',reportManagerTime = now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
		String sql = "UPDATE tf_exception SET deleteFlag = 1,deleteMan = "+memberId+",deleteReason = '"+deleteReason+"',deleteTime=now() where exceptionId = " + exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报撤销通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
		String problemAttribution = request.getParameter("problemAttribution");
		String handlingOpinions = request.getParameter("handlingOpinions");
		String handlingDescription = request.getParameter("handlingDescription");
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
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
		String touser = getManagerUserId(department);//根据部门号找到对应经理领导发送消息
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}		
		//上传图片
		if(determinePicture!=null&&!determinePicture.isEmpty()) {
			determinePicture = CommUtil.uploadPicture(savePicturePath,"exceptionPicture","determine_"+serialNumber,determinePicture);
		}
		
		String pendingPerson = getManagerMemberId(department.replace("[", "").replace("]", ""));//获取下一流程处理者
		
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',determineHandler = "+memberId+",problemAttribution = '"+problemAttribution+"',handlingOpinions = '"+handlingOpinions+
							"',handlingDescription = '"+handlingDescription+"',determinePicture = '"+determinePicture+"',verifyManager=null, determineHandlerTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报质量判定审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId+"&state="+state);
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
		String touser = request.getParameter("touser");
		String toparty = request.getParameter("toparty");
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

		//判断是多个部门还是单个部门
		boolean bo = toparty.contains("|");
		String pendingPersons = "";
		String pendingPerson = "";
		if(bo) {//多个部门
			String partyArray[] =toparty.split("|");
			for(String party : partyArray){
				pendingPerson = getMemberIdStrByToparty(party);//获取部门下的成员memberI的字符串集合
				if(pendingPerson==null || "".equals(pendingPerson)) {
					continue;
				}
				pendingPersons += pendingPerson +",";
			}
			if(!"".equals(pendingPersons)&&pendingPersons.substring(pendingPersons.length()-1).equals(",")){
				pendingPersons = pendingPersons.substring(0,pendingPersons.length()-1);
			}
		}else {//单个部门
			pendingPersons = getMemberIdStrByToparty(toparty);//获取部门下工程师以上的成员memberI的字符串集合
		}

		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPersons+"',determineManager ="+ memberId +", determineManagerTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);

		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报责任部门处理通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
		
		String pendingPerson = getMemberIdByUserId(reporter);
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',determineManagerRefuse='"+determineManagerRefuse+"',determineManager ="+ memberId +", determineManagerTime = now() where exceptionId="+exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报质量判定驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
					textcard.setDescription("发布文件序号为"+serialNumber+"的异常通报已被质量控制部审核经理驳回，驳回理由："+determineManagerRefuse+"。点击查看详情！");
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
		String needSignDepartment = request.getParameter("needSignDepartment");
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
		String touser = getManagerUserId(department);//根据部门号找到对应经理领导发送消息
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String pendingPerson = getManagerMemberId(department.replace("[", "").replace("]", ""));//获取下一流程处理者
		String signDepartment = "";//将已会签部门字段清空，（防止质量验证驳回时bug）
		SQLHelper sqlhe = new SQLHelper();		
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',responsibilityHandler ="+memberId+",exceptionReason = '"+exceptionReason+"',exceptionSolve = '"+exceptionSolve+
						  "',needSignDepartment = '"+needSignDepartment+"',signDepartment = '"+signDepartment+"', responsibilityHandlerTime = now() where exceptionId = " + exceptionId;
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
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
		String signDepartment = request.getParameter("signDepartment");//会签部门
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
		String pendingPerson= "";//下一流程处理着memberId
		//1.如果不需要会签部门会签，则直接到质量验证部门处理
		if(signDepartment==null || "".equals(signDepartment) || "null".equals(signDepartment) || "无".equals(signDepartment)){
				state = 10;//改变异常通报状态
				String toparty = "5";
				pendingPerson = getMemberIdStrByToparty("5");
				
				
				SQLHelper sqlhe = new SQLHelper();
				List<String> sqlist = new ArrayList<>();		
				String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',responsibilityManager ="+ memberId +",signHandlerInfo='',signManagerInfo='', responsibilityManagerTime = now() where exceptionId = " + exceptionId;
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
							textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
							textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n待您质量验证处理，请及时处理，点击查看详情！");
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
				
				
		}else {//2.如果需要会签部门会签，则流到部门会签
			//解析需要会签的部门成员
			String partyArray[] =signDepartment.split(",");
			String singnMemberId = "";//获取会签部门成员的memberId
			String singnMemberIds = "";
			for(String party : partyArray){
				singnMemberId = getMemberIdStrByToparty(party);
				if(singnMemberId==null || "".equals(singnMemberId)) {
					continue;
				}
				singnMemberIds += singnMemberId +",";			
			}
			if(!"".equals(singnMemberIds) && singnMemberIds.substring(singnMemberIds.length()-1).equals(",")){				
				singnMemberIds = singnMemberIds.substring(0,singnMemberIds.length()-1);
			}
			
			//解析需要会签的部门号
			String signdepartments = "";
			if(signDepartment!=null&&!"".equals(signDepartment)) {
				signdepartments = signDepartment.replaceAll(",", "|");
			}
			pendingPerson = singnMemberIds;
			
			SQLHelper sqlhe = new SQLHelper();
			List<String> sqlist = new ArrayList<>();		
			String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',responsibilityManager ="+ memberId +",signHandlerInfo='',signManagerInfo='', responsibilityManagerTime = now() where exceptionId = " + exceptionId;
			sqlist.add(upfanSql);
			boolean isSuccess = sqlhe.update(sqlist);

			if(isSuccess){
				String access_token = "";
				try {
					access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
					if(access_token!=null && !"".equals(access_token)) {
						TextcardMessage textcardMessage = new TextcardMessage();
						Textcard textcard = new Textcard();
						//根据不同的会签部门发送给不同的会签部门经理确认	
						if(signdepartments!=null && !"".equals(signdepartments)) {									    												
							textcard.setTitle("异常通报部门会签处理通知");
							textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
							textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"..."+"\n待您会签处理，请及时处理，点击查看详情！");
							textcardMessage.setTouser("");
							textcardMessage.setToparty(signdepartments);
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
	 * 11、责任（工程）部门经理质量判定驳回
	 * @param exceptionId,memberId,touser,toparty,state,submitdept
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "responsibilityManagerRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson responsibilityManagerRefuse(HttpServletRequest request) throws WexinReqException{
		String touser = request.getParameter("touser");
		String toparty = request.getParameter("toparty");
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
		
		String pendingPerson = getMemberIdStrByToparty(toparty);//获取部门下的成员memberI的字符串集合
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',responsibilityManagerRefuse='"+responsibilityManagerRefuse+"',responsibilityManager ="+ memberId +",signHandlerInfo='',signManagerInfo='', responsibilityManagerTime = now() where exceptionId="+exceptionId;
		boolean isSuccess = sqlhe.update(sql);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报工程驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exception.html");
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
		String touser = getManagerUserId(department);//根据部门号找到对应经理领导发送消息
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String pendingPerson = "";//获取下一流程处理者
		
		//解析存储会签处理人的处理信息
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();	
		String sql = "select * from tf_exception where exceptionId="+exceptionId;
		List<Object> list1 = sqlhe.query(sql);	
		Map<String, Object> map = new HashMap<String, Object>();
		String signHandlerInfo ="";
		String signManagerInfo = "";
		if(list1!=null && list1.size()>0 && !"".equals(department)){
			map = (Map<String, Object>) list1.get(0);
			String depart = department.replace("[", "").replace("]", "");
			//解析下一流程处理人
			String pendingPersons[] = String.valueOf(map.get("pendingPerson")).split(",");
			String departmentPersonsArray[] = getMemberIdStrByToparty(depart).split(",");
			for(int i=0;i<pendingPersons.length;i++) {
				boolean pass = true;
				for(int j=0;j<departmentPersonsArray.length;j++) {
					if(pendingPersons[i].equals(departmentPersonsArray[j])) {
						pass= false;
						break;
					}
				} 
				if(pass) {
					pendingPerson = pendingPerson+pendingPersons[i]+",";
				}
			} 
			pendingPerson = pendingPerson + getManagerMemberId(department.replace("[", "").replace("]", ""));
			if(!"".equals(pendingPerson)&&pendingPerson.substring(pendingPerson.length()-1).equals(",")){
				pendingPerson = pendingPerson.substring(0,pendingPerson.length()-1);
			}						
			
			if(map.get("signHandlerInfo")!=null && !"".equals(map.get("signHandlerInfo"))) {
				JSONArray jsonArray = JSONArray.fromObject(map.get("signHandlerInfo"));  				
				JSONArray jsonArray1 = new JSONArray();  				
				if(jsonArray!=null && jsonArray.size()>0) {
					for(int i=0;i<jsonArray.size();i++) {
						String dep = String.valueOf(JSONArray.fromObject(jsonArray.get(i)).get(1));
						if(depart.equals(dep)) {
							continue;
						}
						jsonArray1.add(jsonArray.get(i));
					}
					jsonArray1.add(JSONArray.fromObject("[\""+memberId+"\",\""+depart+"\",\""+df.format(new Date())+"\",\""+signHandleDescription+"\"]"));
					signHandlerInfo = String.valueOf(jsonArray1);
				}
			}else {
				signHandlerInfo = "[[\""+memberId+"\",\""+depart+"\",\""+df.format(new Date())+"\",\""+signHandleDescription+"\"]]";
			}
			
			//然后把该部门经理审核信息去除（会签修改时用到）
			if(map.get("signManagerInfo")!=null && !"".equals(map.get("signManagerInfo"))) {
				JSONArray jsonArray = JSONArray.fromObject(map.get("signManagerInfo")); 
				JSONArray jsonArray2 = new JSONArray(); 
				if(jsonArray!=null && jsonArray.size()>0) {
					for(int i=0;i<jsonArray.size();i++) {
						String dept = String.valueOf(JSONArray.fromObject(jsonArray.get(i)).get(1));
						if(depart.equals(dept)) {
							continue;
						}
						jsonArray2.add(jsonArray.get(i));
					}
					signManagerInfo = String.valueOf(jsonArray2);
				}
			}else {
				signManagerInfo = "";
			}

		}
			
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',signHandlerInfo = '"+signHandlerInfo+"',signManagerInfo = '"+signManagerInfo+"' where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报会签审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "signManager")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson signManager(HttpServletRequest request) throws WexinReqException{
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		Integer exceptionId = Integer.parseInt(request.getParameter("exceptionId"));
		String department = request.getParameter("department");
		String departmentText = request.getParameter("departmentText");
		String signManagerDescription = request.getParameter("signManagerDescription");
		String toparty = request.getParameter("toparty");
		String touser = request.getParameter("touser");
		String serialNumber = request.getParameter("serialNumber");
		String model = request.getParameter("model");
		String exceptionDescription = request.getParameter("exceptionDescription");
		if(exceptionDescription.length()>38) {
			exceptionDescription = exceptionDescription.substring(0, 37)+"...";
		}
		Integer state = 9;

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//解析会签审核人信息存储和下一流程处理人
		String signDepartment ="";
		String pendingPerson = "";
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();	
		String sql = "select * from tf_exception where exceptionId="+exceptionId;
		List<Object> list = sqlhe.query(sql);	
		Map<String, Object> map = new HashMap<String, Object>();
		String signManagerInfo ="";
		if(list!=null && list.size()>0 && !department.isEmpty() && !departmentText.isEmpty()){
			map = (Map<String, Object>) list.get(0);
			
			//1.解析存储会签审核人的处理信息
			String depart = department.replace("[", "").replace("]", "");
			if(map.get("signManagerInfo")!=null && !"".equals(map.get("signManagerInfo"))) {
				JSONArray jsonArray = JSONArray.fromObject(map.get("signManagerInfo"));  
				if(jsonArray!=null && jsonArray.size()>0) {
					for(int i=0;i<jsonArray.size();i++) {
						String dep = String.valueOf(JSONArray.fromObject(jsonArray.get(i)).get(1));
						if(depart.equals(dep)) {
							ajax.setSuccess(false);
							ajax.setMessage("该部门会签已确认！");
							ajax.setErrorCode("-3");
							return ajax;
						}
					}
					jsonArray.add(JSONArray.fromObject("[\""+memberId+"\",\""+depart+"\",\""+df.format(new Date())+"\",\""+signManagerDescription+"\"]"));
					signManagerInfo = String.valueOf(jsonArray);
				}
			}else {
				signManagerInfo = "[[\""+memberId+"\",\""+depart+"\",\""+df.format(new Date())+"\",\""+signManagerDescription+"\"]]";
			}
			
			//2.解析下一流程处理人
			/*String pendingPersons[] = String.valueOf(map.get("pendingPerson")).split(",");
			for(int i=0;i<pendingPersons.length;i++) {
				if(!String.valueOf(memberId).equals(pendingPersons[i])) {
					pendingPerson = pendingPerson+pendingPersons[i]+",";
				}
			} */
			String pendingPersons[] = String.valueOf(map.get("pendingPerson")).split(",");//原待处理人memberId集合
			String managerMemberIds[] = getManagerMemberId(department.replace("[", "").replace("]", "")).split(",");//当前处理部门审核人memberId集合
			//从待处理人memberId集合去除当前处理部门审核人的memberId集合
			for(int i=0;i<pendingPersons.length;i++) {
				boolean pass = true;
				for(int j=0;j<managerMemberIds.length;j++) {
					if(pendingPersons[i].equals(managerMemberIds[j])) {
						pass= false;
						break;
					}
				} 
				if(pass) {
					pendingPerson = pendingPerson+pendingPersons[i]+",";
				}
			} 
						
			if(!"".equals(pendingPerson)&&pendingPerson.substring(pendingPerson.length()-1).equals(",")){
				pendingPerson = pendingPerson.substring(0,pendingPerson.length()-1);
			}
			
			if(map.get("signDepartment")!=null && !"null".equals(map.get("signDepartment")) && !"".equals(map.get("signDepartment"))) {
				String needSignDepartment = String.valueOf(map.get("needSignDepartment"));
				signDepartment = String.valueOf(map.get("signDepartment"))+","+departmentText;
				System.out.println("stephen1====="+departmentText);
				System.out.println("stephen====="+signDepartment.length()+"h"+needSignDepartment.length());
				if(signDepartment.length()==needSignDepartment.length()){//判断如果是最后一个会签的话发送推送消息
					pendingPerson = getMemberIdStrByToparty(toparty);//获取质量验证处理人的memberId
					state = 10;
					String access_token = "";
					try {
						access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
						if(access_token!=null && !"".equals(access_token)) {
							TextcardMessage textcardMessage = new TextcardMessage();
							Textcard textcard = new Textcard();
							//推送质量验证通知
							textcard.setTitle("异常通报质量验证处理通知");
							textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
					
				}
			}else {
				String needSignDepartment = String.valueOf(map.get("needSignDepartment"));
				if(needSignDepartment.contains(",")) {
					signDepartment = departmentText;
				}else {
					pendingPerson = getMemberIdStrByToparty(toparty);//获取质量验证处理人的memberId
					signDepartment = departmentText;
					state = 10;
					String access_token = "";
					try {
						access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
						if(access_token!=null && !"".equals(access_token)) {
							TextcardMessage textcardMessage = new TextcardMessage();
							Textcard textcard = new Textcard();
							//推送质量验证通知
							textcard.setTitle("异常通报质量验证处理通知");
							textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
				}
			}
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("系统出错啦！");
			ajax.setErrorCode("-3");
		}
	
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',signDepartment='"+signDepartment+"',signManagerInfo = '"+signManagerInfo+"' where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);

		if(isSuccess){

		}else{
			ajax.setSuccess(false);
			ajax.setMessage("数据保存失败！");
			ajax.setErrorCode("-2");
		}
		return ajax;
	}
	
	/**
	 * 14、质量验证信息保存
	 * @param exceptionId,memberId,state,signDepartment
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
		String finalHandleMethod = request.getParameter("finalHandleMethod");
		String isRework = request.getParameter("isRework");
		String verifyConclusion = request.getParameter("verifyConclusion");
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
		
		String touser = getManagerUserId(department);//根据部门号找到对应经理领导发送消息
		if(touser==null || "".equals(touser)) {
			ajax.setSuccess(false);
			ajax.setMessage("尚未设置部门经理，提交失败！");
			ajax.setErrorCode("-2");
			return ajax;
		}
		String pendingPerson = getManagerMemberId(department.replace("[", "").replace("]", ""));//获取下一流程处理者
		
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='"+pendingPerson+"',verifyHandler ="+memberId+",verifyConclusion = '"+verifyConclusion+
						  "',finalHandleMethod = '"+finalHandleMethod+"',isRework = '"+isRework+"', verifyHandlerTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报质量验证审核通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
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
	 * 15、质量验证经理审核
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

		AjaxJson ajax = new AjaxJson();	
		if(exceptionId==null||"".equals(request.getParameter("exceptionId"))||memberId==null||"".equals(request.getParameter("memberId"))||state==null){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		
		//判断通报发给哪些部门
		String toparty = "";
		if(serialNumber.startsWith("PD")) {
			toparty = "4|5";//发给生产和质量
		}else if(serialNumber.startsWith("QC")) {
			toparty = "5";//发给质量
		}else if(serialNumber.startsWith("ME")) {
			toparty = "3|5";//发给工程和质量
		}
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson='',verifyManager = "+memberId +", verifyManagerTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
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
					textcard.setTitle("异常通报流程完结通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId);
					textcard.setDescription("文件序号："+serialNumber+"   机型："+model+"\n异常描述 ："+exceptionDescription+"\n异常问题通报流程已完结，点击查看详情！");		
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
	 * 16、质量验证经理驳回
	 * @param exceptionId,memberId,state
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "verifyManagerRefuse")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson verifyManagerRefuse(HttpServletRequest request) throws WexinReqException{
		String touser = request.getParameter("touser");
		String toparty = request.getParameter("toparty");
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
		String pendingPerson = getMemberIdStrByToparty(dep);
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();		
		String upfanSql = "UPDATE tf_exception SET state="+state+",pendingPerson = '"+pendingPerson +"',verifyManagerRefuse = '"+verifyManagerRefuse +"',verifyManager = "+memberId +", verifyManagerTime = now() where exceptionId = " + exceptionId;
		sqlist.add(upfanSql);
		boolean isSuccess = sqlhe.update(sqlist);
		
		if(isSuccess){
			String access_token = "";
			try {
				access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
				if(access_token!=null && !"".equals(access_token)) {
					TextcardMessage textcardMessage = new TextcardMessage();
					Textcard textcard = new Textcard();
					textcard.setTitle("异常通报质量验证驳回通知");
					textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionConfirmation.html?exceptionId="+exceptionId+"&mark"+state);
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
		String sql = "select * from tf_exception where exceptionId = " + exceptionId;
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("exception", list.get(0));
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
	 * 根据部门编号获取该部门下工程师以上成员的memberId字符串
	 * @param toparty
	 * @return memberIdStr
	 */
	@SuppressWarnings("unchecked")
	public static String getMemberIdStrByToparty(String toparty) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select memberId from tf_member where deleteFlag = 0 and (position like '%主管' or position like '%工程师') and department = '["+toparty+"]'";				
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