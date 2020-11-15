package com.modules.newException;

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
 * 导出exceptionWord文档
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/new-exception/word")
public class NewExceptionExportWord {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	
	@RequestMapping("export")
	@Transactional
	@ResponseBody
    public AjaxJson exportWordData(HttpServletRequest request,HttpServletResponse response){
		AjaxJson ajax = new AjaxJson();	
		String exceptionId = request.getParameter("exceptionId");
		//获取异常问题信息
		Map<String, Object> exceptionMap = getExceptionInfoById(Integer.parseInt(exceptionId));
		
        MSWordTool changer = new MSWordTool();  
        changer.setTemplate("/data/webapps/TFMobile/static/file/newExceptionTemplateFile.docx");//模板路径  
        //changer.setTemplate("C:/Users/pc/Desktop/exceptionTemplateFile.docx");//模板路径  
        Map<String,String> content = new HashMap<String,String>();  
        content.put("文件序号", String.valueOf(exceptionMap.get("serialNumber")));
        if("生产部".equals(exceptionMap.get("submitDepartment"))) {
            content.put("发文部门之生产部", "√");  
            content.put("发文部门之质量部", "□");  
            content.put("发文部门之工程部", "□");
        }else if("质量控制部".equals(exceptionMap.get("submitDepartment"))) {
        	content.put("发文部门之生产部", "□");  
            content.put("发文部门之质量部", "√");  
            content.put("发文部门之工程部", "□");
        }else if("品质保障部".equals(exceptionMap.get("submitDepartment"))) {
        	content.put("发文部门之生产部", "□");  
            content.put("发文部门之质量部", "□");  
            content.put("发文部门之工程部", "√");
        } 
        content.put("线别",String.valueOf(exceptionMap.get("line"))); 
        if("特单/大单".equals(exceptionMap.get("orderCategory"))) {
            content.put("订单类别之大单", "√");  
            content.put("订单类别之小批量", "□");  
            content.put("订单类别之常规", "□");
        }else if("小批量".equals(exceptionMap.get("orderCategory"))) {
        	content.put("订单类别之大单", "□");  
            content.put("订单类别之小批量", "√");  
            content.put("订单类别之常规", "□");
        }else if("常规".equals(exceptionMap.get("orderCategory"))) {
        	content.put("订单类别之大单", "□");  
            content.put("订单类别之小批量", "□");  
            content.put("订单类别之常规", "√");
        }
        if("台式".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "√");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "□");
        	content.put("产品类别之显示器", "□");
        	content.put("产品类别之工作台", "□");
        }else if("一体机".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "√");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "□");
        	content.put("产品类别之显示器", "□");
        	content.put("产品类别之工作台", "□");
        }else if("服务器".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "√");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "□");
        	content.put("产品类别之显示器", "□");
        	content.put("产品类别之工作台", "□");
        }else if("笔记本".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "√");
        	content.put("产品类别之云终端", "□");
        	content.put("产品类别之显示器", "□");
        	content.put("产品类别之工作台", "□");
        }else if("云终端".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "√");
        	content.put("产品类别之显示器", "□");
        	content.put("产品类别之工作台", "□");
        }else if("显示器".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "□");
        	content.put("产品类别之显示器", "√");
        	content.put("产品类别之工作台", "□");
        }else if("工作台".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "□");
        	content.put("产品类别之显示器", "□");
        	content.put("产品类别之工作台", "√");
        }
        //发现组别
        if("备料".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "√");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "□");
        }else if("装配".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "√");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "□");
        }else if("高温".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "√");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "□");
        }else if("终检".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "√");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "□");
        }else if("包装".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "√");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "□");
        }else if("资料".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "√");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "□");
        }else if("PQC".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "√");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "□");
        }else if("OQC".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "√");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "□");
        }else if("ORT".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "√");
        	content.put("发现组别之其他", "□");
        }else if("其他".equals(exceptionMap.get("findStation"))) {
        	content.put("发现组别之备料", "□");  
        	content.put("发现组别之装配", "□");  
        	content.put("发现组别之高温", "□");
        	content.put("发现组别之终检", "□");
        	content.put("发现组别之包装", "□");
        	content.put("发现组别之资料", "□");
        	content.put("发现组别之PQC", "□");
        	content.put("发现组别之OQC", "□");
        	content.put("发现组别之ORT", "□");
        	content.put("发现组别之其他", "√");
        }
        
        /*content.put("制令单号",String.valueOf(exceptionMap.get("ordermaking"))); 
        content.put("机型",String.valueOf(exceptionMap.get("model")));
        content.put("订单数量",String.valueOf(exceptionMap.get("orderQuantity")));  
        content.put("检验数量",String.valueOf(exceptionMap.get("checkQuantity")));  
        content.put("已生产", String.valueOf(exceptionMap.get("producedQuantity")));  
        content.put("故障数量",String.valueOf(exceptionMap.get("failuresQuantity")));  
        content.put("不良率",String.valueOf(exceptionMap.get("defectiveRate"))+"%");*/  
        content.put("异常描述",String.valueOf(exceptionMap.get("exceptionDescription")));
        content.put("报告人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("reporter")))).get("name")));
        if(!String.valueOf(exceptionMap.get("reportManager")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("reportManager")))) {
        	content.put("问题描述审核经理",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("reportManager")))).get("name")));
        	content.put("问题描述审核时间",String.valueOf(exceptionMap.get("reportManagerTime")).substring(0,String.valueOf(exceptionMap.get("reportManagerTime")).length()-2));
        }
        //质量判定
        //通报类型
        String exceptionType = String.valueOf(exceptionMap.get("exceptionType"));
        if("普通类".equals(exceptionType)) {
        	content.put("通报类型之普通类", "√");  
        	content.put("通报类型之CCC类", "□");  
        	content.put("通报类型之能效类", "□");  
        }else if("CCC类".equals(exceptionType)) {
        	content.put("通报类型之普通类", "□");  
        	content.put("通报类型之CCC类", "√");  
        	content.put("通报类型之能效类", "□");   
        }else if("能效类".equals(exceptionType)) {
        	content.put("通报类型之普通类", "□");  
        	content.put("通报类型之CCC类", "□");  
        	content.put("通报类型之能效类", "√");  
        }else {
        	content.put("通报类型之普通类", "□");  
        	content.put("通报类型之CCC类", "□");  
        	content.put("通报类型之能效类", "□");
        }
        
        if(!String.valueOf(exceptionMap.get("determineHandler")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("determineHandler")))) {
        	content.put("质量判定之处理意见",  String.valueOf(exceptionMap.get("handlingDescription")));
            content.put("质量判定之处理人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("determineHandler")))).get("name")));
        }
        if(!String.valueOf(exceptionMap.get("determineManager")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("determineManager")))) {
        	content.put("质量判定之审核人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("determineManager")))).get("name")));
        	content.put("质量判定之审核时间",String.valueOf(exceptionMap.get("determineManagerTime")).substring(0,String.valueOf(exceptionMap.get("determineManagerTime")).length()-2));
        }
        
        
        //责任部门
        if(!String.valueOf(exceptionMap.get("responsibilityHandler")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("responsibilityHandler")))) {
        	content.put("责任部门之造成原因",  String.valueOf(exceptionMap.get("exceptionReason")));
            content.put("责任部门之临时方案",  String.valueOf(exceptionMap.get("exceptionSolve")));
            
            String longExceptionSolve = String.valueOf(exceptionMap.get("longExceptionSolve"));
            if(longExceptionSolve==null || longExceptionSolve.isEmpty() || longExceptionSolve.equals("null")){
            	longExceptionSolve = "无";
            }
            content.put("责任部门之长期方案",  longExceptionSolve);
            content.put("责任部门之是否返工",  String.valueOf(exceptionMap.get("isRework")));
            content.put("责任部门之问题归属",  String.valueOf(exceptionMap.get("problemAttribution")));
            
            String isSign = String.valueOf(exceptionMap.get("isSign"));
            String problemAttribution = String.valueOf(exceptionMap.get("problemAttribution"));
            String signDepartment = "无";
            if("是".equals(isSign)){
            	if(problemAttribution.equals("操作")){
            		signDepartment = "生产部";
   				}else if(problemAttribution.equals("材料")){
   					signDepartment = "质量部";
   				}else if(problemAttribution.equals("设计")){
   					signDepartment = "研发部";
   				}
            }else {
            	isSign = "否";
            }
            content.put("责任部门之是否转签",  isSign);
            content.put("责任部门之转签部门",  signDepartment);
            
            content.put("责任部门之处理人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("responsibilityHandler")))).get("name")));
            content.put("责任部门之处理时间",String.valueOf(exceptionMap.get("responsibilityHandlerTime")).substring(0,String.valueOf(exceptionMap.get("responsibilityHandlerTime")).length()-2));
        }
        if(!String.valueOf(exceptionMap.get("responsibilityManager")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("responsibilityManager")))) {
        	content.put("责任部门之审核人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("responsibilityManager")))).get("name")));
            content.put("责任部门之审核时间",String.valueOf(exceptionMap.get("responsibilityManagerTime")).substring(0,String.valueOf(exceptionMap.get("responsibilityManagerTime")).length()-2));
        }
                      
        //会签部门
        String isSign = String.valueOf(exceptionMap.get("isSign"));
        if("是".equals(isSign)) {
        	  if(!String.valueOf(exceptionMap.get("signHandler")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("signHandler")))) {
        		  String signDepartment = "无";
        		  String problemAttribution = String.valueOf(exceptionMap.get("problemAttribution"));
        		  if(problemAttribution.equals("操作")){
              		signDepartment = "生产部";
     				}else if(problemAttribution.equals("材料")){
     					signDepartment = "质量部";
     				}else if(problemAttribution.equals("设计")){
     					signDepartment = "研发部";
     				}
              	  content.put("会签部门之问题转签部门",  signDepartment);
                  content.put("会签部门之长期方案意见",  String.valueOf(exceptionMap.get("longExceptionSolve")));
                 
                  content.put("会签部门之处理人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("signHandler")))).get("name")));
              }
              if(!String.valueOf(exceptionMap.get("signManager")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("signManager")))) {
              	content.put("会签部门之审核人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("signManager")))).get("name")));
                  content.put("会签部门之审核时间",String.valueOf(exceptionMap.get("signManagerTime")).substring(0,String.valueOf(exceptionMap.get("signManagerTime")).length()-2));
              }
        }
     
        //质量验证
        if(!String.valueOf(exceptionMap.get("verifyHandler")).isEmpty()&&!"null".equals(String.valueOf(exceptionMap.get("verifyHandler")))) {
            content.put("质量验证之质量验证结论",  String.valueOf(exceptionMap.get("verifyConclusion")));
            String isLongSolveMeasure = String.valueOf(exceptionMap.get("isLongSolveMeasure"));
            String longSolveMeasureBrief = "无";
            String problemDepartment = "无";
            if("是".equals(isLongSolveMeasure)){
            	longSolveMeasureBrief = String.valueOf(exceptionMap.get("longSolveMeasureBrief"));
            	problemDepartment = String.valueOf(exceptionMap.get("problemDepartment"));
            }else {
            	isLongSolveMeasure = "否";
            }
            content.put("质量验证之需要长期方案",  isLongSolveMeasure);
            content.put("质量验证之责任归属部门",  problemDepartment);
            content.put("质量验证之长期方案简述",  longSolveMeasureBrief);
        	content.put("质量验证之处理人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("verifyHandler")))).get("name")));   
        	content.put("质量验证之处理时间",String.valueOf(exceptionMap.get("verifyHandlerTime")).substring(0,String.valueOf(exceptionMap.get("verifyHandlerTime")).length()-2));
        }
        if(!String.valueOf(exceptionMap.get("verifyManager")).isEmpty()&&!"null".equals(String.valueOf(exceptionMap.get("verifyManager")))) {
        	content.put("质量验证之审核人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("verifyManager")))).get("name")));
            content.put("质量验证之审核时间",String.valueOf(exceptionMap.get("verifyManagerTime")).substring(0,String.valueOf(exceptionMap.get("verifyManagerTime")).length()-2));
        }
            
        changer.replaceBookMark(content,"");  
        
        Map<String,String> content2 = new HashMap<String,String>();  
        content2.put("制令单号",String.valueOf(exceptionMap.get("ordermaking"))); 
        content2.put("机型",String.valueOf(exceptionMap.get("model")));
        content2.put("订单数量",String.valueOf(exceptionMap.get("orderQuantity")));  
        content2.put("检验数量",String.valueOf(exceptionMap.get("checkQuantity")));  
        content2.put("已生产", String.valueOf(exceptionMap.get("producedQuantity")));  
        content2.put("故障数量",String.valueOf(exceptionMap.get("failuresQuantity")));  
        content2.put("不良率",(Double.parseDouble(String.valueOf(exceptionMap.get("defectiveRate")))*100)+"%");
        changer.replaceBookMark(content2,"1"); 
        //保存替换后的WORD 
        String savePath = "/data/webapps/word/exception_"+String.valueOf(exceptionMap.get("serialNumber")+".docx"); 

        File file = new File("/data/webapps/word");      
        //如果文件夹不存在则创建      
        if(!file.exists() && !file.isDirectory()) {          
            file .mkdir();     
        }
        
        changer.saveAs(savePath); 
        ajax.setMessage(serviceUrl+"/word/exception_"+String.valueOf(exceptionMap.get("serialNumber")+".docx"));
        return ajax;
    }
	  
	/**
	 * 根据异常主键获取异常信息
	 * @param exceptionId
	 * @return exception
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getExceptionInfoById(Integer exceptionId) {
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select * from tf_new_exception where exceptionId = " + exceptionId;
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
