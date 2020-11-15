package com.modules.newException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.modules.db.SQLHelper;
import com.modules.export.ExportExcel;
import com.modules.utils.AjaxJson;
import com.modules.utils.ConfigurationFileHelper;

/**
 * 导出exceptionExcel文档
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/new-exception/excel")
public class NewExceptionExportExcel {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	@RequestMapping("export")
	@Transactional
	@ResponseBody
    public AjaxJson exportExcelData(HttpServletRequest request,HttpServletResponse response) throws FileNotFoundException, IOException, ParseException{
		String serialNumber = request.getParameter("serialNumber");
		String startTime = request.getParameter("startTime");
		String endTime = request.getParameter("endTime");
		String snSql ="";
		String dateSql ="";
		if(!startTime.isEmpty()||!endTime.isEmpty()) {
			dateSql = "and (date(reporterTime) between '"+startTime+"' and '"+endTime+"')";
		}else if(!serialNumber.isEmpty()) {
			snSql = "and serialNumber like concat('%','"+serialNumber+"','%')";
		}
		AjaxJson ajax = new AjaxJson();	
		//获取异常问题信息
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_new_exception where deleteFlag = 0 "+snSql+dateSql+" order by exceptionId desc";
		List<Object> list = sqlhe.query(sql);
		
		if(list!=null && list.size()>0) {
			List<String> headerList = Lists.newArrayList();
			headerList.add("文件序号");
			headerList.add("部门");
			headerList.add("线别");
			headerList.add("发文日期");
			headerList.add("制令单号");
			headerList.add("机型");
			headerList.add("订单数量");
			headerList.add("检验数量");
			headerList.add("故障数量");
			headerList.add("不良比例");
			headerList.add("问题描述");
			headerList.add("质量判定");
			headerList.add("质量判定耗时");
			headerList.add("原因分析");
			headerList.add("解决方案");
			headerList.add("工程分析耗时");
			headerList.add("问题归属");
			headerList.add("是否会签");
			headerList.add("会签内容");
			headerList.add("会签耗时");
			headerList.add("质量验证");
			headerList.add("质量验证耗时");
			headerList.add("长期方案责任部门耗时");
			headerList.add("长期方案质量验证耗时");
			headerList.add("是否返工");
			headerList.add("是否放行");
			headerList.add("处理人");
			headerList.add("工程超时(2hour)");
			headerList.add("通报类型");
			headerList.add("通报状态");
			headerList.add("流程状态");
			
			List<List<String>> dataList = Lists.newArrayList();
			for(int i=0;i<list.size();i++) {
				List<String> dataRowList = Lists.newArrayList();
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				dataRowList.add(String.valueOf(map.get("serialNumber")));
				dataRowList.add(String.valueOf(map.get("submitDepartment")));
				dataRowList.add(String.valueOf(map.get("line")));
				dataRowList.add(String.valueOf(map.get("reporterTime")));
				dataRowList.add(String.valueOf(map.get("ordermaking")));
				dataRowList.add(String.valueOf(map.get("model")));
				dataRowList.add(String.valueOf(map.get("orderQuantity")));
				dataRowList.add(String.valueOf(map.get("checkQuantity")));
				dataRowList.add(String.valueOf(map.get("failuresQuantity")));
				dataRowList.add(String.format("%.2f",(Double.parseDouble(String.valueOf(map.get("defectiveRate"))))*100)+"%");
				dataRowList.add(String.valueOf(map.get("exceptionDescription")));
				dataRowList.add(String.valueOf(map.get("handlingDescription")));
				//解析质量判定耗时
				String reportManagerTime = String.valueOf(map.get("reportManagerTime"));
				String determineManagerTime = String.valueOf(map.get("determineManagerTime"));
				if(reportManagerTime!=null && !reportManagerTime.isEmpty() && !"null".equals(reportManagerTime)
						&&determineManagerTime!=null && !determineManagerTime.isEmpty() && !"null".equals(determineManagerTime)) {
					dataRowList.add(getDistanceHourOfTwoDate(reportManagerTime,determineManagerTime,1).toString());
				}else {
					dataRowList.add("");
				}				
				dataRowList.add(String.valueOf(map.get("exceptionReason")));
				dataRowList.add(String.valueOf(map.get("exceptionSolve")));
				//解析工程分析耗时
				String responsibilityManagerTime = String.valueOf(map.get("responsibilityManagerTime"));
				if(responsibilityManagerTime!=null && !responsibilityManagerTime.isEmpty() && !"null".equals(responsibilityManagerTime)
						&&determineManagerTime!=null && !determineManagerTime.isEmpty() && !"null".equals(determineManagerTime)) {
					dataRowList.add(getDistanceHourOfTwoDate(determineManagerTime,responsibilityManagerTime,1).toString());
				}else {
					dataRowList.add("");
				}
				dataRowList.add(String.valueOf(map.get("problemAttribution")));
				dataRowList.add(String.valueOf(map.get("isSign")));				
				dataRowList.add(String.valueOf(map.get("longExceptionSolve")));
				//解析会签耗时
				String signManagerTime = String.valueOf(map.get("signManagerTime"));
				if(responsibilityManagerTime!=null && !responsibilityManagerTime.isEmpty() && !"null".equals(responsibilityManagerTime)
						&&signManagerTime!=null && !signManagerTime.isEmpty() && !"null".equals(signManagerTime)) {
					dataRowList.add(getDistanceHourOfTwoDate(responsibilityManagerTime,signManagerTime,1).toString());
				}else {
					dataRowList.add("");
				}				
				dataRowList.add(String.valueOf(map.get("verifyConclusion")));
				//解析质量验证耗时
				if("普通类".equals(String.valueOf(map.get("exceptionType")))) {
					if("是".equals(String.valueOf(map.get("isSign")))) {
						String verifyManagerTime = String.valueOf(map.get("verifyManagerTime"));
						if(verifyManagerTime!=null && !verifyManagerTime.isEmpty() && !"null".equals(verifyManagerTime)
								&&signManagerTime!=null && !signManagerTime.isEmpty() && !"null".equals(signManagerTime)) {
							dataRowList.add(getDistanceHourOfTwoDate(signManagerTime,verifyManagerTime,1).toString());
						}else {
							dataRowList.add("");
						}
					}else {
						String verifyManagerTime = String.valueOf(map.get("verifyManagerTime"));
						if(verifyManagerTime!=null && !verifyManagerTime.isEmpty() && !"null".equals(verifyManagerTime)
								&&signManagerTime!=null && !signManagerTime.isEmpty() && !"null".equals(signManagerTime)) {
							dataRowList.add(getDistanceHourOfTwoDate(signManagerTime,verifyManagerTime,1).toString());
						}else {
							dataRowList.add("");
						}
					}
				}else {
					String verifyManagerTime = String.valueOf(map.get("verifyManagerTime"));
					if(verifyManagerTime!=null && !verifyManagerTime.isEmpty() && !"null".equals(verifyManagerTime)
							&&determineManagerTime!=null && !determineManagerTime.isEmpty() && !"null".equals(determineManagerTime)) {
						dataRowList.add(getDistanceHourOfTwoDate(determineManagerTime,verifyManagerTime,1).toString());
					}else {
						dataRowList.add("");
					}
				}
				//解析长期方案责任部门耗时	
				if("是".equals(String.valueOf(map.get("isLongSolveMeasure")))) {
					String verifyManagerTime = String.valueOf(map.get("verifyManagerTime"));
					String longSolveMeasureEnderTime = String.valueOf(map.get("longSolveMeasureEnderTime"));
					if(longSolveMeasureEnderTime!=null && !longSolveMeasureEnderTime.isEmpty() && !"null".equals(longSolveMeasureEnderTime)
							&&verifyManagerTime!=null && !verifyManagerTime.isEmpty() && !"null".equals(verifyManagerTime)) {
						dataRowList.add(getDistanceHourOfTwoDate(verifyManagerTime,longSolveMeasureEnderTime,1).toString());
					}else {
						dataRowList.add("");
					}
					String longSolveMeasureReviewerTime = String.valueOf(map.get("longSolveMeasureReviewerTime"));
					if(longSolveMeasureEnderTime!=null && !longSolveMeasureEnderTime.isEmpty() && !"null".equals(longSolveMeasureEnderTime)
							&&longSolveMeasureReviewerTime!=null && !longSolveMeasureReviewerTime.isEmpty() && !"null".equals(longSolveMeasureReviewerTime)) {
						dataRowList.add(getDistanceHourOfTwoDate(longSolveMeasureEnderTime,longSolveMeasureReviewerTime,1).toString());
					}else {
						dataRowList.add("");
					}
				}else {
					dataRowList.add("");
					dataRowList.add("");
				}
	
				dataRowList.add(String.valueOf(map.get("isRework")));
				dataRowList.add(String.valueOf(map.get("verifyResult")));
				String memberId = String.valueOf(map.get("responsibilityHandler"));
				if(memberId!=null && !memberId.isEmpty() && !"null".equals(memberId)) {
					dataRowList.add((String)getMemberInfoById(Integer.parseInt(memberId)).get("name"));
				}else {
					dataRowList.add("");
				}
				
				//解析工程是否超时（质量判定审核到工程经理审核几点大于两小时就是超时）
				String isOverTime = "否";
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				determineManagerTime = String.valueOf(map.get("determineManagerTime"));
				responsibilityManagerTime = String.valueOf(map.get("responsibilityManagerTime"));
				if(determineManagerTime!=null && !determineManagerTime.isEmpty() && !"null".equals(determineManagerTime)
						&&responsibilityManagerTime!=null && !responsibilityManagerTime.isEmpty() && !"null".equals(responsibilityManagerTime)) {
					long determineManagerTimeDate = sdf.parse(determineManagerTime).getTime()+(2*60*60*1000);//加两小时（处理时间为两小时内，超出则为超时）
					if(sdf.parse(responsibilityManagerTime).getTime()>determineManagerTimeDate) {
						isOverTime = "是";
					}
				}
				dataRowList.add(isOverTime);
				
				dataRowList.add(String.valueOf(map.get("exceptionType")));
				String exception_state = "";
				if ("20".equals(String.valueOf(map.get("state")))) {
					exception_state = "已结案";
				}else if ((Integer.parseInt(String.valueOf(map.get("state")))<13 
						&& Integer.parseInt(String.valueOf(map.get("state")))!=10) 
						|| (Integer.parseInt(String.valueOf(map.get("state")))==10)
						&& "普通类".equals(String.valueOf(map.get("exceptionType")))) {
					//(a.state < 13 and a.state <> 10) or (a.state = 10 and a.exceptionType = '普通类')
					exception_state = "待完成";
				}else {
					exception_state = "待结案";
				}
				dataRowList.add(exception_state);
				
				//通报流程状态
				String status = String.valueOf(map.get("state"));
				String status_description = "";
				if("0".equals(status)){
					status_description = "待发文部门（"+String.valueOf(map.get("submitDepartment"))+"）经理审核";
				}else if("1".equals(status)){
					status_description = "通报已被发文部门经理驳回，待发文人员（"+(String)getMemberInfoById(Integer.parseInt(String.valueOf(map.get("reporter")))).get("name")+"）处理";
				}else if("2".equals(status)){
					status_description = "待质量判定处理";
				}else if("3".equals(status)){
					status_description = "待质量判定审核";
				}else if("4".equals(status)){
					status_description = "待工程部门处理";
				}else if("5".equals(status)){
					status_description = "通报已被质量判定驳回，待发文人员（"+(String)getMemberInfoById(Integer.parseInt(String.valueOf(map.get("reporter")))).get("name")+"）处理";
				}else if("6".equals(status)){
					status_description = "待工程部门经理审核";
				}else if("7".equals(status)){
					String departmentName = "";
					if("操作".equals(String.valueOf(map.get("problemAttribution")))){
						departmentName = "生产部";				
					}else if("材料".equals(String.valueOf(map.get("problemAttribution")))){
						departmentName = "质量部";			
					}else if("设计".equals(String.valueOf(map.get("problemAttribution")))){
						departmentName = "研发部";			
					}
					status_description = "待转签部门（"+departmentName+"）会签处理";
				}else if("8".equals(status)){
					status_description = "通报已被责任部门驳回，待质量判定处理";
				}else if("9".equals(status)){
					String departmentName = "";
					if("操作".equals(String.valueOf(map.get("problemAttribution")))){
						departmentName = "生产部";				
					}else if("材料".equals(String.valueOf(map.get("problemAttribution")))){
						departmentName = "质量部";			
					}else if("设计".equals(String.valueOf(map.get("problemAttribution")))){
						departmentName = "研发部";			
					}
					status_description = "待转签部门（"+departmentName+"）会签审核";
				}else if("10".equals(status)){
					if("CCC类".equals(String.valueOf(map.get("exceptionType")))||"能效类".equals(String.valueOf(map.get("exceptionType")))){
						status_description = "待体系专员维护验证结果";
					}else{
						status_description = "待质量验证处理";
					}			
				}else if("11".equals(status)){
					status_description = "通报已被转签部门驳回，待工程部门处理";
				}else if("12".equals(status)){
					status_description = "待质量验证审核";
				}else if("13".equals(status)){
					status_description = "待责任部门（"+String.valueOf(map.get("problemDepartment"))+"）长期方案处理";
				}else if("14".equals(status)){
					status_description = "待责任部门（"+String.valueOf(map.get("problemDepartment"))+"）长期方案跟踪处理";
				}else if("15".equals(status)){
					status_description = "待质量部门长期方案验证处理";
				}else if("16".equals(status)){
					status_description = "通报已被转签部门驳回，待工程部门处理";
				}else if("17".equals(status)){
					status_description = "待质量部门长期方案验证审核";
				}else if("20".equals(status)){
					status_description = "已结案";
				}
				dataRowList.add(status_description);
								
				dataList.add(dataRowList);
			}
			ExportExcel ee = new ExportExcel("异常通报信息报表", headerList);
			for (int i = 0; i < dataList.size(); i++) {
				Row row = ee.addRow();
				for (int j = 0; j < dataList.get(i).size(); j++) {
					ee.addCell(row, j, dataList.get(i).get(j));
				}
			}
			
			 File file = new File("/data/webapps/excel");      
		        //如果文件夹不存在则创建      
		        if(!file.exists() && !file.isDirectory()) {          
		            file .mkdir();     
		        }
		    String excelPath = "/data/webapps/excel/new_exception_excel.xlsx";
			ee.writeFile(excelPath);

			ee.dispose();
			
			LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
			body.put("excelPath", serviceUrl+"/excel/new_exception_excel.xlsx");
			ajax.setBody(body);
			
		}else {
			ajax.setErrorCode("-1");
			ajax.setSuccess(false);
			ajax.setMessage("未查询到任何结果！");
		}
		    
        return ajax;
    }
	  
	/**
	 * 根据企业成员主键获取异常信息
	 * @param exceptionId
	 * @return exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getMemberInfoById(Integer memberId) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_member where memberId = " + memberId;
		List<Object> list = sqlhe.query(sql);
		Map<String, Object> map = null;
		if (list!=null && list.size()>0) {
			map = (Map<String, Object>) list.get(0);
		}
		return map;
	}
	
	/**
	 * 获取两个日期字符串之间的小时差(字符串日期格式：yyyy-MM-dd hh:mm:ss)
	 * @param beforeDate 开始时间
	 * @param afterDate 结束时间
	 * @param digit 保留小数有效位数
	 * @return
	 * @throws ParseException 
	 */
	public static BigDecimal getDistanceHourOfTwoDate(String beforeDate, String afterDate, int digit) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date date1 = sdf.parse(beforeDate);
		Date date2 = sdf.parse(afterDate);
		Double do1 = Double.valueOf(date1.getTime());
		Double do2 = Double.valueOf(date2.getTime());
		BigDecimal bd = new BigDecimal((do2-do1)/(60*60*1000));  
		BigDecimal distance = bd.setScale(digit,BigDecimal.ROUND_HALF_UP);
		return distance;
	}
}