package com.modules.improvement;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping(value = "tf-api/improvement/word")
public class ImprovementExportWord {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	
	@RequestMapping("export")
	@Transactional
	@ResponseBody
    public AjaxJson exportWordData(HttpServletRequest request,HttpServletResponse response){
		AjaxJson ajax = new AjaxJson();	
		String improvementId = request.getParameter("improvementId");
		//获取异常问题信息
		Map<String, Object> map = getImprovementInfoById(Integer.parseInt(improvementId));
		
        MSWordTool changer = new MSWordTool();  
        changer.setTemplate("/data/webapps/TFMobile/static/file/improvementTemplateFile.docx");//模板路径  
        Map<String,String> content = new HashMap<String,String>();  
        content.put("提案名称", String.valueOf(map.get("title")));
        content.put("目前做法", String.valueOf(map.get("current_measure")));
        content.put("改善办法", String.valueOf(map.get("improve_measure")));
        content.put("预期效果", String.valueOf(map.get("anticipate_effect")));
        content.put("提案人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(map.get("proposer")))).get("name")));
        content.put("提出日期",String.valueOf(map.get("proposer_time")).substring(0,String.valueOf(map.get("proposer_time")).length()-2));
        if("是".equals(String.valueOf(map.get("isAdopt")))) {
        	 content.put("评审结果", "采纳");
        }else if("否".equals(String.valueOf(map.get("isAdopt")))) {
        	 content.put("评审结果", "不采纳");
        }
        content.put("希望完成时间", String.valueOf(map.get("expect_finished_date")));
        content.put("实施人", String.valueOf(map.get("implement_person")));
            
        changer.replaceBookMark(content,"");  
  
        //保存替换后的WORD 
        String savePath = "/data/webapps/word/improvement.docx"; 

        File file = new File("/data/webapps/word");      
        //如果文件夹不存在则创建      
        if(!file.exists() && !file.isDirectory()) {          
            file .mkdir();     
        }
        
        changer.saveAs(savePath); 
        ajax.setMessage(serviceUrl+"/word/improvement.docx");
        return ajax;
    }
	  
	/**
	 * 根据异常主键获取异常信息
	 * @param exceptionId
	 * @return exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getImprovementInfoById(Integer improvementId) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_improvement where improvementId = " + improvementId;
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
		String sql = "select * from tf_another_member where memberId = " + memberId;
		List<Object> list = sqlhe.query(sql);
		Map<String, Object> map = null;
		if (list!=null && list.size()>0) {
			map = (Map<String, Object>) list.get(0);
		}
		return map;
	}

}
