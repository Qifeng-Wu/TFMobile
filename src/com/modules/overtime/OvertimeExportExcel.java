package com.modules.overtime;

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
@RequestMapping(value = "tf-api/overtime/excel")
public class OvertimeExportExcel {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	@SuppressWarnings("unchecked")
	@RequestMapping("export")
	@ResponseBody
    public AjaxJson exportExcelData(HttpServletRequest request,HttpServletResponse response,AjaxJson ajax) throws FileNotFoundException, IOException, ParseException{
		String dateStr = request.getParameter("date");
		String department = request.getParameter("department");
		String dateSql ="select * from tf_overtime_application where state=0 and department='"+department+"' and date='"+dateStr+"'";
		
		//获取异常问题信息
		SQLHelper sqlhe = new SQLHelper();		
		List<Object> list = sqlhe.query(dateSql);
		if(list!=null && list.size()>0) {
			Map<String, Object> map = (Map<String, Object>) list.get(0);
			
			List<String> headerList = Lists.newArrayList();
			headerList.add("序号");
			headerList.add("日期");
			headerList.add("班组");
			headerList.add("姓名");
			headerList.add("加班时间");
			headerList.add("保安确认");
			
			//获取加班人员名单
			String applicationId = String.valueOf(map.get("applicationId"));
			String date = String.valueOf(map.get("date"));
			String sql = "select * from tf_overtime_member a left join tf_wuxi_member b on a.wxId = b.wxId where applicationId='"+applicationId+"'";
			List<Object> memberList = sqlhe.query(sql);
			Map<String, Object> member;
			if(memberList!=null && memberList.size()>0) {
				List<List<String>> dataList = Lists.newArrayList();
				for(int i=0;i<memberList.size();i++) {	
					List<String> dataRowList = Lists.newArrayList();
					member = (Map<String, Object>) memberList.get(i);					
					String name = String.valueOf(member.get("name"));
					String team = String.valueOf(member.get("team"));
					String startTime = String.valueOf(member.get("start_time")).substring(11,16);
					String endTime = String.valueOf(member.get("end_time")).substring(11,16);
					dataRowList.add(i+1+"");
					dataRowList.add(date);
					dataRowList.add(team);
					dataRowList.add(name);
					dataRowList.add(startTime+"~"+endTime);
					dataRowList.add("");
					dataList.add(dataRowList);	
				}
				ExportExcel ee = new ExportExcel("加班单（"+date+"）", headerList);
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
			    String excelPath = "/data/webapps/excel/overtime_excel.xlsx";
				ee.writeFile(excelPath);

				ee.dispose();
				
				LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
				body.put("excelPath", serviceUrl+"/excel/overtime_excel.xlsx");
				ajax.setBody(body);
			}else {
				ajax.setErrorCode("-1");
				ajax.setSuccess(false);
				ajax.setMessage("未查询到该日期的加班申请单或该日期加班申请单未完结！");
			}
			
		}else {
			ajax.setErrorCode("-1");
			ajax.setSuccess(false);
			ajax.setMessage("未查询到该日期的加班申请单或该日期加班申请单未完结！");
		}		    
        return ajax;
    }
}