package com.modules.overtime;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SQLHelper;
import com.modules.export.MSWordTool;
import com.modules.utils.AjaxJson;
import com.modules.utils.ConfigurationFileHelper;


/**
 * 导出Word文档
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/overtime/word")
public class OvertimeExportWord {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	
	@SuppressWarnings("unchecked")
	@RequestMapping("export")
	@ResponseBody
    public AjaxJson exportWordData(HttpServletRequest request,HttpServletResponse response){
		AjaxJson ajax = new AjaxJson();	
		String applicationId = request.getParameter("applicationId");
		//获取信息
		Map<String, Object> map = getApplicationInfoById(Integer.parseInt(applicationId));
		
		String date = String.valueOf(map.get("date")).substring(5);
		String sql = "select * from tf_overtime_member a left join tf_wuxi_member b on a.wxId = b.wxId where applicationId='"+applicationId+"'";
		SQLHelper sh = new SQLHelper();		
		List<Object> memberList = sh.query(sql);
		if(memberList!=null && memberList.size()>0) {
			MSWordTool changer = new MSWordTool();  
	        changer.setTemplate("/data/webapps/TFMobile/static/file/overtimeTemplateFile.docx");//模板路径  
	        Map<String,String> content = new HashMap<String,String>(); 
	        
	        String dateStr = "";
	        String teamStr = "";
	        String nameStr = "";
	        String overtimeStr = "";
	        Map<String, Object> member;
	        int a = 0;
	        for(int i=0;i<memberList.size();i++) {
	        	member = (Map<String, Object>) memberList.get(i);	
	        	a = (i/3)+1;
	        	if(i<27) {
	        		if(i%3==0) {
	        			dateStr = "日期10"+a;
	        			teamStr = "班组10"+a;
	        			nameStr = "姓名10"+a;
	        			overtimeStr = "加班时间10"+a;
	        		}else if(i%3==1) {
	        			dateStr = "日期20"+a;
	        			teamStr = "班组20"+a;
	        			nameStr = "姓名20"+a;
	        			overtimeStr = "加班时间20"+a;
	        		}else {
	        			dateStr = "日期30"+a;
	        			teamStr = "班组30"+a;
	        			nameStr = "姓名30"+a;
	        			overtimeStr = "加班时间30"+a;
	        		}        		
	        	}else {
	        		if(i%3==0) {
	        			dateStr = "日期1"+a;
	        			teamStr = "班组1"+a;
	        			nameStr = "姓名1"+a;
	        			overtimeStr = "加班时间1"+a;
	        		}else if(i%3==1) {
	        			dateStr = "日期2"+a;
	        			teamStr = "班组2"+a;
	        			nameStr = "姓名2"+a;
	        			overtimeStr = "加班时间2"+a;
	        		}else {
	        			dateStr = "日期3"+a;
	        			teamStr = "班组3"+a;
	        			nameStr = "姓名3"+a;
	        			overtimeStr = "加班时间3"+a;
	        		}
	        	}	        	        
	        	
	        	String startTime = String.valueOf(member.get("start_time")).substring(11,16);
				String endTime = String.valueOf(member.get("end_time")).substring(11,16);
	        	content.put(dateStr, date);
	        	content.put(teamStr, String.valueOf(member.get("team")));
	        	content.put(nameStr, String.valueOf(member.get("name")));
	        	content.put(overtimeStr, startTime+"~"+endTime);
	        }
	        content.put("加班理由", String.valueOf(map.get("applicant_reason")));
	        if(String.valueOf(map.get("manager"))!=null && !String.valueOf(map.get("manager")).isEmpty() && !"null".equals(String.valueOf(map.get("manager")))) {
	        	content.put("部门经理",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(map.get("manager")))).get("name")));
	        	content.put("经理意见", "同意");		  
	        }
	        if(String.valueOf(map.get("engineer"))!=null && !String.valueOf(map.get("engineer")).isEmpty() && !"null".equals(String.valueOf(map.get("engineer")))) {
		        content.put("工艺主管",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(map.get("engineer")))).get("name")));
		        content.put("工程意见", "同意");		  
	        }
	        if(String.valueOf(map.get("general"))!=null && !String.valueOf(map.get("general")).isEmpty() && !"null".equals(String.valueOf(map.get("general")))) {
	        	content.put("事业部意见", "同意");		        
	        	content.put("副总经理",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(map.get("general")))).get("name")));
	        }

	        changer.replaceBookMark(content,"");  
	  
	        //保存替换后的WORD 
	        String savePath = "/data/webapps/word/overtime.docx"; 

	        File file = new File("/data/webapps/word");      
	        //如果文件夹不存在则创建      
	        if(!file.exists() && !file.isDirectory()) {          
	            file .mkdir();     
	        }
	        
	        changer.saveAs(savePath); 
	        ajax.setMessage(serviceUrl+"/word/overtime.docx");
		}else {
			ajax.setErrorCode("-1");
			ajax.setSuccess(false);
			ajax.setMessage("未查询到该日期的加班申请单或该日期加班申请单未完结！");
		}
       
        return ajax;
    }
	  
	/**
	 * 根据异常主键获取异常信息
	 * @param exceptionId
	 * @return exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getApplicationInfoById(Integer applicationId) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_overtime_application where applicationId = " + applicationId;
		List<Object> list = sqlhe.query(sql);
		Map<String, Object> map = null;
		if (list!=null && list.size()>0) {
			map = (Map<String, Object>) list.get(0);
		}
		return map;
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
