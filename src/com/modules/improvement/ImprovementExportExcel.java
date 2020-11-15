package com.modules.improvement;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Row;
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
@RequestMapping(value = "tf-api/improvement/excel")
public class ImprovementExportExcel {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	@RequestMapping("export")
	@ResponseBody
    public AjaxJson exportExcelData(HttpServletRequest request,HttpServletResponse response) throws FileNotFoundException, IOException, ParseException{
		String serialNumber = request.getParameter("serialNumber");
		String startTime = request.getParameter("startTime");
		String endTime = request.getParameter("endTime");
		String snSql ="";
		String dateSql ="";
		if(!startTime.isEmpty()||!endTime.isEmpty()) {
			dateSql = "and (date(proposer_time) between '"+startTime+"' and '"+endTime+"')";
		}else if(!serialNumber.isEmpty()) {
			snSql = "and title like concat('%','"+serialNumber+"','%')";
		}
		AjaxJson ajax = new AjaxJson();	
		//获取列表
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_improvement where delete_flag = 0 "+snSql+dateSql+" order by proposer_time desc";
		List<Object> list = sqlhe.query(sql);
		
		if(list!=null && list.size()>0) {
			List<String> headerList = Lists.newArrayList();
			headerList.add("提案名称");
			headerList.add("目前做法");
			headerList.add("改善办法");
			headerList.add("预期效果");
			headerList.add("提案部门");
			headerList.add("提案班组");
			headerList.add("提案人");
			headerList.add("提案时间");
			headerList.add("是否采纳");
			headerList.add("IE处理信息");
			headerList.add("实施人");
			headerList.add("希望完成日期");
			headerList.add("提案类型");
			headerList.add("流程状态");
			
			List<List<String>> dataList = Lists.newArrayList();
			for(int i=0;i<list.size();i++) {
				List<String> dataRowList = Lists.newArrayList();
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				dataRowList.add(String.valueOf(map.get("title")));
				dataRowList.add(String.valueOf(map.get("current_measure")));
				dataRowList.add(String.valueOf(map.get("improve_measure")));
				dataRowList.add(String.valueOf(map.get("anticipate_effect")));
				dataRowList.add(String.valueOf(map.get("department")));
				dataRowList.add(String.valueOf(map.get("team")));
				String proposerMemberId = String.valueOf(map.get("proposer"));
				dataRowList.add((String)getMemberInfoById(Integer.parseInt(proposerMemberId)).get("name"));
				dataRowList.add(String.valueOf(map.get("proposer_time")));
				
				dataRowList.add(String.valueOf(map.get("isAdopt")));
				dataRowList.add(String.valueOf(map.get("handle_info")));
				dataRowList.add(String.valueOf(map.get("implement_person")));
				dataRowList.add(String.valueOf(map.get("expect_finished_date")));
				//提案类型
				String type = String.valueOf(map.get("type"));
				if("0".equals(type)) {
					dataRowList.add("普通员工");
				}else {
					dataRowList.add("主管或工程师");	
				}			
				
				//流程状态
				String status = String.valueOf(map.get("state"));
				String status_description = "";
				if("0".equals(type)) {
					if("0".equals(status)){
						status_description = "流程完结";
					}else if("1".equals(status)){
						status_description = "待主管确认";
					}else if("2".equals(status)){
						status_description = "提案已被主管驳回，待提案人处理";
					}else if("3".equals(status)){
						status_description = "待部门经理审核";
					}else if("4".equals(status)){
						status_description = "提案已被部门经理驳回，待主管处理";
					}else if("5".equals(status)){
						status_description = "待工业主管处理";
					}
				}else {
					if("0".equals(status)){
						status_description = "流程完结";
					}else if("1".equals(status)){
						status_description = "待部门经理审核";
					}else if("2".equals(status)){
						status_description = "提案已被部门经理驳回，待提案人处理";
					}else if("3".equals(status)){
						status_description = "待工业主管处理";
					}
				}			
				dataRowList.add(status_description);
								
				dataList.add(dataRowList);
			}
			ExportExcel ee = new ExportExcel("改善提案表", headerList);
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
		    String excelPath = "/data/webapps/excel/improvement_excel.xlsx";
			ee.writeFile(excelPath);

			ee.dispose();
			
			LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
			body.put("excelPath", serviceUrl+"/excel/improvement_excel.xlsx");
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
		String sql = "select * from tf_another_member where memberId = " + memberId;
		List<Object> list = sqlhe.query(sql);
		Map<String, Object> map = null;
		if (list!=null && list.size()>0) {
			map = (Map<String, Object>) list.get(0);
		}
		return map;
	}
}