package com.modules.exception;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

import net.sf.json.JSONArray; 


/**
 * 导出exceptionExcel文档
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/exception/excel")
public class ExportExcelAPI {
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
		String sql = "select * from tf_exception where state = 12 "+snSql+dateSql;
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
			headerList.add("原因分析");
			headerList.add("解决方法");
			headerList.add("会签部门");
			headerList.add("会签部门");
			headerList.add("质量验证");
			headerList.add("是否返工");
			headerList.add("处理人");
			headerList.add("工程超时(2hour)");
			headerList.add("问题类型");
			headerList.add("是否封闭");
			
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
				dataRowList.add(String.valueOf(map.get("exceptionReason")));
				dataRowList.add(String.valueOf(map.get("exceptionSolve")));
				
				 //解析会签部门信息
				String productionHandlerName = "";//生产部的会签处理人
				String description2 = "";//工业技术部的会签描述
				String description3 = "";//工程部的会签描述
				String description4 = "";//生产部的会签描述
				String description5 = "";//质量部的会签描述
				String description7 = "";//研发部的会签描述
				String needSignDepartment = String.valueOf(map.get("needSignDepartment"));
		        if(needSignDepartment!=null&&!"".equals(needSignDepartment)&&!"null".equals(needSignDepartment)&&!"无".equals(needSignDepartment)) {
		        	String signHandlerInfo = String.valueOf(map.get("signHandlerInfo"));//会签各部门处理人信息
		        	if(signHandlerInfo!=null && !"null".equals(signHandlerInfo) && !"".equals(signHandlerInfo)) {
		        		JSONArray jsonArray = JSONArray.fromObject(signHandlerInfo);
		        		for(int k=0;k<jsonArray.size();k++) {
		        			String memberId = String.valueOf(JSONArray.fromObject(jsonArray.get(k)).get(0));
		        			String handlerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//处理人姓名
		        			String handlerDscription = String.valueOf(JSONArray.fromObject(jsonArray.get(k)).get(3));//处理人描述
		        			String depNo = String.valueOf(JSONArray.fromObject(jsonArray.get(k)).get(1)); //部门编号
		        			if("2".equals(depNo)) {//工业技术部
		        				description2 = handlerDscription;		      			       				
		        			}else if("3".equals(depNo)) {//工程部
		        				description3 = handlerDscription;	        				
		        			}else if("4".equals(depNo)) {//生产部
		        				description4 = handlerDscription;
		        				productionHandlerName = handlerName;
		        			}else if("5".equals(depNo)) {//质量部
		        				description5 = handlerDscription;		        				
		        			}else if("7".equals(depNo)) {//研发部
		        				description7 = handlerDscription;		        			
		        			}
		        			    			
		        		}
		        	}	
		        }
		        
		        dataRowList.add(description4);//生产部会签内容
			    String description =  ""; 
		        if(!description2.isEmpty()) {
		        	description = description2;//工业技术部会签内容	
	        	}else if(!description7.isEmpty()){
	        		description = description7;//研发部会签内容
	        	}else if(!description5.isEmpty()){
	        		description = description5;//质控部会签内容
	        	}else if(!description3.isEmpty()){
	        		description = description3;//工程部会签内容
	        	}
				dataRowList.add(description);//其他会签部门的一个
				dataRowList.add(String.valueOf(map.get("verifyConclusion")));
				dataRowList.add(String.valueOf(map.get("isRework")));
				dataRowList.add(productionHandlerName);
				
				//解析工程是否超时（质量判定审核到工程经理审核几点大于两小时就是超时）
				String isOverTime = "否";
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String determineManagerTime = String.valueOf(map.get("determineManagerTime"));
				String responsibilityManagerTime = String.valueOf(map.get("responsibilityManagerTime"));
				long determineManagerTimeDate = sdf.parse(determineManagerTime).getTime()+(2*60*60*1000);//加两小时（处理时间为两小时内，超出则为超时）
				if(sdf.parse(responsibilityManagerTime).getTime()>determineManagerTimeDate) {
					isOverTime = "是";
				}
				dataRowList.add(isOverTime);
				dataRowList.add("");
				dataRowList.add("");
				
				dataList.add(dataRowList);
			}
			ExportExcel ee = new ExportExcel("异常通报信息报表", headerList);
			for (int i = 0; i < dataList.size(); i++) {
				Row row = ee.addRow();
				for (int j = 0; j < dataList.get(i).size(); j++) {
					ee.addCell(row, j, dataList.get(i).get(j));
				}
			}
			
			 File file = new File("/data/webapps/exceptionExcel");      
		        //如果文件夹不存在则创建      
		        if(!file.exists() && !file.isDirectory()) {          
		            file .mkdir();     
		        }
		     String excelPath = "/data/webapps/exceptionExcel/exception_excel.xlsx";
			ee.writeFile(excelPath);

			ee.dispose();
			
			LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
			body.put("excelPath", serviceUrl+"/exceptionExcel/exception_excel.xlsx");
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
}