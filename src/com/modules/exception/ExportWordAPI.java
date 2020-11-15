package com.modules.exception;

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

import net.sf.json.JSONArray;

/**
 * 导出exceptionWord文档
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/exception")
public class ExportWordAPI {
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	
	@RequestMapping("exportWord")
	@Transactional
	@ResponseBody
    public AjaxJson exportWordData(HttpServletRequest request,HttpServletResponse response){
		AjaxJson ajax = new AjaxJson();	
		String exceptionId = request.getParameter("exceptionId");
		//获取异常问题信息
		Map<String, Object> exceptionMap = getExceptionInfoById(Integer.parseInt(exceptionId));
		
        MSWordTool changer = new MSWordTool();  
        changer.setTemplate("/data/webapps/TFMobile/static/file/exceptionTemplateFile.docx");//模板路径  
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
        }else if("一体机".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "√");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "□");
        }else if("服务器".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "√");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "□");
        }else if("笔记本".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "√");
        	content.put("产品类别之云终端", "□");
        }else if("云终端".equals(exceptionMap.get("productCategory"))) {
        	content.put("产品类别之台式", "□");  
        	content.put("产品类别之一体机", "□");  
        	content.put("产品类别之服务器", "□");
        	content.put("产品类别之笔记本", "□");
        	content.put("产品类别之云终端", "√");
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
        //问题归属
        String problemAttribution = String.valueOf(exceptionMap.get("problemAttribution"));
        if(problemAttribution!=null && !problemAttribution.isEmpty() && !"null".equals(problemAttribution)) {
        	String problemAttributionArray[] = problemAttribution.split(",");
        	boolean shengchan = false;
        	boolean gongcheng = false;
        	boolean zhiliang = false;
        	boolean yanfa = false;
        	boolean shebei = false;
        	boolean sfc = false;
        	boolean chanping = false;
        	boolean yunying = false;
        	for(int i=0;i<problemAttributionArray.length;i++) {
        		if("生产".equals(problemAttributionArray[i])) {
        			content.put("问题归属之生产", "√"); shengchan = true;
        		}else if("工程".equals(problemAttributionArray[i])) {
        			content.put("问题归属之工程", "√"); gongcheng = true;
        		}else if("质量".equals(problemAttributionArray[i])) {
        			content.put("问题归属之质量", "√"); zhiliang = true;
        		}else if("研发".equals(problemAttributionArray[i])) {
        			content.put("问题归属之研发", "√"); yanfa = true;
        		}else if("设备".equals(problemAttributionArray[i])) {
        			content.put("问题归属之设备", "√"); shebei = true;
        		}else if("SFC".equals(problemAttributionArray[i])) {
        			content.put("问题归属之SFC", "√"); sfc = true;
        		}else if("运营".equals(problemAttributionArray[i])) {
        			content.put("问题归属之运营", "√"); yunying = true;
        		}else if("产品".equals(problemAttributionArray[i])) {
        			content.put("问题归属之产品", "√"); chanping = true;
        		}
        	}
        	
        	if(!shengchan) {
        		content.put("问题归属之生产", "□");
        	}
        	if(!gongcheng) {
        		content.put("问题归属之工程", "□");
        	}
        	if(!zhiliang) {
        		content.put("问题归属之质量", "□");
        	}
        	if(!yanfa) {
        		content.put("问题归属之研发", "□");
        	}
        	if(!shebei) {
        		content.put("问题归属之设备", "□");
        	}
        	if(!sfc) {
        		content.put("问题归属之SFC", "□");
        	}
        	if(!yunying) {
        		content.put("问题归属之运营", "□");
        	}
        	if(!chanping) {
        		content.put("问题归属之产品", "□");
        	}
        }else {
    		content.put("问题归属之生产", "□");
    		content.put("问题归属之工程", "□");
    		content.put("问题归属之质量", "□");
    		content.put("问题归属之研发", "□");
    		content.put("问题归属之设备", "□");
    		content.put("问题归属之SFC", "□");
    		content.put("问题归属之运营", "□");
    		content.put("问题归属之产品", "□");
        }
        //初步处理意见
        if("暂停".equals(exceptionMap.get("handlingOpinions"))) {
        	content.put("初步处理意见之暂停", "√");  
        	content.put("初步处理意见之返工", "□");  
        	content.put("初步处理意见之修复", "□");  
        	content.put("初步处理意见之送修", "□");  
        	content.put("初步处理意见之其他", "□");  
        }else if("返工".equals(exceptionMap.get("handlingOpinions"))) {
           	content.put("初步处理意见之暂停", "□");  
        	content.put("初步处理意见之返工", "√");  
        	content.put("初步处理意见之修复", "□");  
        	content.put("初步处理意见之送修", "□");  
        	content.put("初步处理意见之其他", "□"); 
        }else if("后续工序修复".equals(exceptionMap.get("handlingOpinions"))) {
        	content.put("初步处理意见之暂停", "□");  
        	content.put("初步处理意见之返工", "□");  
        	content.put("初步处理意见之修复", "√");  
        	content.put("初步处理意见之送修", "□");  
        	content.put("初步处理意见之其他", "□"); 
        }else if("不良送修".equals(exceptionMap.get("handlingOpinions"))) {
        	content.put("初步处理意见之暂停", "□");  
        	content.put("初步处理意见之返工", "□");  
        	content.put("初步处理意见之修复", "□");  
        	content.put("初步处理意见之送修", "√");  
        	content.put("初步处理意见之其他", "□"); 
        }else if("其他".equals(exceptionMap.get("handlingOpinions"))) {
        	content.put("初步处理意见之暂停", "□");  
        	content.put("初步处理意见之返工", "□");  
        	content.put("初步处理意见之修复", "□");  
        	content.put("初步处理意见之送修", "□");  
        	content.put("初步处理意见之其他", "√"); 
        }else {
        	content.put("初步处理意见之暂停", "□");  
        	content.put("初步处理意见之返工", "□");  
        	content.put("初步处理意见之修复", "□");  
        	content.put("初步处理意见之送修", "□");  
        	content.put("初步处理意见之其他", "□"); 
        }
        
        if(!String.valueOf(exceptionMap.get("determineHandler")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("determineHandler")))) {
        	content.put("判定描述",  String.valueOf(exceptionMap.get("handlingDescription")));
            content.put("质量判定处理人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("determineHandler")))).get("name")));
        }
        if(!String.valueOf(exceptionMap.get("determineManager")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("determineManager")))) {
        	content.put("质量判定审核经理",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("determineManager")))).get("name")));
        	content.put("质量判定审核时间",String.valueOf(exceptionMap.get("determineManagerTime")).substring(0,String.valueOf(exceptionMap.get("determineManagerTime")).length()-2));
        }
        
        
        //责任部门
        if(!String.valueOf(exceptionMap.get("responsibilityHandler")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("responsibilityHandler")))) {
        	content.put("原因分析",  String.valueOf(exceptionMap.get("exceptionReason")));
            content.put("解决方法",  String.valueOf(exceptionMap.get("exceptionSolve")));
            content.put("责任部门处理人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("responsibilityHandler")))).get("name")));
        }
        if(!String.valueOf(exceptionMap.get("responsibilityManager")).isEmpty() && !"null".equals(String.valueOf(exceptionMap.get("responsibilityManager")))) {
        	content.put("责任部门审核经理",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("responsibilityManager")))).get("name")));
            content.put("责任部门审核时间",String.valueOf(exceptionMap.get("responsibilityManagerTime")).substring(0,String.valueOf(exceptionMap.get("responsibilityManagerTime")).length()-2));
        }
                      
        //会签部门
        String needSignDepartment = String.valueOf(exceptionMap.get("needSignDepartment"));
        if(needSignDepartment!=null && !needSignDepartment.isEmpty() && !"null".equals(needSignDepartment)&&!"无".equals(needSignDepartment)) {
        	String needSignDepartmentArray[] = needSignDepartment.split(",");
        	boolean shengchan1 = false;
        	boolean gongcheng1 = false;
        	boolean zhiliang1 = false;
        	boolean yanfa1 = false;
        	boolean gongyejishu1 = false;
        	boolean chanping1 = false;
        	boolean yunying1 = false;
        	for(int i=0;i<needSignDepartmentArray.length;i++) {
        		if("生产".equals(needSignDepartmentArray[i])) {
        			content.put("需要会签部门之生产", "√"); shengchan1 = true;
        			content.put("抄送之生产部", "√");
        		}else if("工程".equals(needSignDepartmentArray[i])) {
        			content.put("需要会签部门之工程", "√"); gongcheng1 = true;
        			content.put("抄送之工程部", "√");
        		}else if("质量".equals(needSignDepartmentArray[i])) {
        			content.put("需要会签部门之质量", "√"); zhiliang1 = true;
        			content.put("抄送之质量部", "√");
        		}else if("研发".equals(needSignDepartmentArray[i])) {
        			content.put("需要会签部门之研发", "√"); yanfa1 = true;
        			content.put("抄送之研发部", "√");
        		}else if("工业技术".equals(needSignDepartmentArray[i])) {
        			content.put("需要会签部门之工业技术", "√"); gongyejishu1 = true;
        			content.put("抄送之其他部门", "√");content.put("抄送之其他部门名称", "工业技术部");
        		}else if("运营".equals(needSignDepartmentArray[i])) {
        			content.put("需要会签部门之运营", "√"); yunying1 = true;
        			content.put("抄送之其他部门", "√");content.put("抄送之其他部门名称", "运营部");
        		}else if("产品".equals(needSignDepartmentArray[i])) {
        			content.put("需要会签部门之产品", "√"); chanping1 = true;
        			content.put("抄送之其他部门", "√");content.put("抄送之其他部门名称", "产品部");
        		}
        	}
        	if(!shengchan1) {
        		content.put("需要会签部门之生产", "□");
        		content.put("抄送之生产部", "□");
        	}
        	if(!gongcheng1) {
        		content.put("需要会签部门之工程", "□");
        		content.put("抄送之工程部", "□");
        	}
        	if(!zhiliang1) {
        		content.put("需要会签部门之质量", "□");
        		content.put("抄送之质量部", "□");
        	}
        	if(!yanfa1) {
        		content.put("需要会签部门之研发", "□");
        		content.put("抄送之研发部", "□");
        	}
        	if(!gongyejishu1) {
        		content.put("需要会签部门之工业技术", "□");
        	}
        	if(!yunying1) {
        		content.put("需要会签部门之运营", "□");
        	}
        	if(!chanping1) {
        		content.put("需要会签部门之产品", "□");
        	}
        	content.put("需要会签部门之无", "□");
        	
        	if(!gongyejishu1 && !yunying1 &&!chanping1) {
        		content.put("抄送之其他部门", "□");
        	}
        	
        }else {
    		content.put("需要会签部门之无", "√");
    		content.put("需要会签部门之生产", "□");
    		content.put("需要会签部门之工程", "□");
    		content.put("需要会签部门之质量", "□");
    		content.put("需要会签部门之研发", "□");
    		content.put("需要会签部门之工业技术", "□");
    		content.put("需要会签部门之运营", "□");
    		content.put("需要会签部门之产品", "□");
    		
    		content.put("抄送之生产部", "□");
    		content.put("抄送之工程部", "□");
    		content.put("抄送之质量部", "□");
    		content.put("抄送之研发部", "□");
    		content.put("抄送之其他部门", "□");
        	
        }
        //解析会签部门信息
        if(needSignDepartment!=null&&!needSignDepartment.isEmpty()&&!"null".equals(needSignDepartment)&&!"无".equals(needSignDepartment)) {
        	String signHandlerInfo = String.valueOf(exceptionMap.get("signHandlerInfo"));//会签各部门处理人信息
        	String signManagerInfo = String.valueOf(exceptionMap.get("signManagerInfo"));//会签各部门审核经理信息
        	if(signHandlerInfo!=null && !signHandlerInfo.isEmpty() && !"null".equals(signHandlerInfo)) {
        		JSONArray jsonArray = JSONArray.fromObject(signHandlerInfo);
				String description = "";//会签描述(总的)
				String description2 = "";//工业技术部的会签描述
				String description3 = "";//工程部的会签描述
				String description4 = "";//生产部的会签描述
				String description5 = "";//质量部的会签描述
				String description7 = "";//研发部的会签描述
				String description8 = "";//运营部的会签描述
				String description9 = "";//产品部的会签描述
        		for(int i=0;i<jsonArray.size();i++) {
        			String memberId = String.valueOf(JSONArray.fromObject(jsonArray.get(i)).get(0));
        			String handlerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//处理人姓名
        			String handlerDscription = String.valueOf(JSONArray.fromObject(jsonArray.get(i)).get(3));//处理人描述
        			String depNo = String.valueOf(JSONArray.fromObject(jsonArray.get(i)).get(1)); //部门编号
        			if("2".equals(depNo)) {//工业技术部
        				description2 = "工业技术： 处理描述："+handlerDscription+"，处理人："+handlerName;
        				if(signManagerInfo!=null && !signManagerInfo.isEmpty() && !"null".equals(signManagerInfo)) {
        					JSONArray jsonArray1 = JSONArray.fromObject(signManagerInfo);
        					for(int j=0;j<jsonArray1.size();j++) {
        						String depNo1 = String.valueOf(JSONArray.fromObject(jsonArray1.get(j)).get(1)); //部门编号
        						if(depNo1.equals(depNo)) {
        							String managerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//审核经理姓名
        		        			String managerTime = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(2));//审核经理处理时间
        		        			String managerDscription = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(3));//审核经理描述
        		        			description2 += "； 审核描述："+managerDscription+"，审核人："+managerName+"，审核时间："+managerTime;
        						}
        					}
        				}
       				
        			}else if("3".equals(depNo)) {//工程部
        				description3 = "工程： 处理描述："+handlerDscription+"，处理人："+handlerName;
        				if(signManagerInfo!=null && !signManagerInfo.isEmpty() && !"null".equals(signManagerInfo)) {
        					JSONArray jsonArray1 = JSONArray.fromObject(signManagerInfo);
        					for(int j=0;j<jsonArray1.size();j++) {
        						String depNo1 = String.valueOf(JSONArray.fromObject(jsonArray1.get(j)).get(1)); //部门编号
        						if(depNo1.equals(depNo)) {
        							String managerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//审核经理姓名
        		        			String managerTime = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(2));//审核经理处理时间
        		        			String managerDscription = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(3));//审核经理描述
        		        			description3 += "； 审核描述："+managerDscription+"，审核人："+managerName+"，审核时间："+managerTime;
        						}
        					}
        				}
        				
        			}else if("4".equals(depNo)) {//生产部
        				description4 = "生产： 处理描述："+handlerDscription+"，处理人："+handlerName;
        				if(signManagerInfo!=null && !signManagerInfo.isEmpty() && !"null".equals(signManagerInfo)) {
        					JSONArray jsonArray1 = JSONArray.fromObject(signManagerInfo);
        					for(int j=0;j<jsonArray1.size();j++) {
        						String depNo1 = String.valueOf(JSONArray.fromObject(jsonArray1.get(j)).get(1)); //部门编号
        						if(depNo1.equals(depNo)) {
        							String managerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//审核经理姓名
        		        			String managerTime = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(2));//审核经理处理时间
        		        			String managerDscription = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(3));//审核经理描述
        		        			description4 += "； 审核描述："+managerDscription+"，审核人："+managerName+"，审核时间："+managerTime;
        						}
        					}
        				}
        				
        			}else if("5".equals(depNo)) {//质量部部
        				description5 = "质量： 处理描述："+handlerDscription+"，处理人："+handlerName;
        				if(signManagerInfo!=null && !signManagerInfo.isEmpty() && !"null".equals(signManagerInfo)) {
        					JSONArray jsonArray1 = JSONArray.fromObject(signManagerInfo);
        					for(int j=0;j<jsonArray1.size();j++) {
        						String depNo1 = String.valueOf(JSONArray.fromObject(jsonArray1.get(j)).get(1)); //部门编号
        						if(depNo1.equals(depNo)) {
        							String managerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//审核经理姓名
        		        			String managerTime = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(2));//审核经理处理时间
        		        			String managerDscription = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(3));//审核经理描述
        		        			description5 += "； 审核描述："+managerDscription+"，审核人："+managerName+"，审核时间："+managerTime;
        						}
        					}
        				}
        				
        			}else if("7".equals(depNo)) {//研发部
        				description7 = "研发： 处理描述："+handlerDscription+"，处理人："+handlerName;
        				if(signManagerInfo!=null && !signManagerInfo.isEmpty() && !"null".equals(signManagerInfo)) {
        					JSONArray jsonArray1 = JSONArray.fromObject(signManagerInfo);
        					for(int j=0;j<jsonArray1.size();j++) {
        						String depNo1 = String.valueOf(JSONArray.fromObject(jsonArray1.get(j)).get(1)); //部门编号
        						if(depNo1.equals(depNo)) {
        							String managerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//审核经理姓名
        		        			String managerTime = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(2));//审核经理处理时间
        		        			String managerDscription = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(3));//审核经理描述
        		        			description7 += "； 审核描述："+managerDscription+"，审核人："+managerName+"，审核时间："+managerTime;
        						}
        					}
        				}
        				

        			}else if("8".equals(depNo)) {//运营部
        				description8 = "运营： 处理描述："+handlerDscription+"，处理人："+handlerName;
        				if(signManagerInfo!=null && !signManagerInfo.isEmpty() && !"null".equals(signManagerInfo)) {
        					JSONArray jsonArray1 = JSONArray.fromObject(signManagerInfo);
        					for(int j=0;j<jsonArray1.size();j++) {
        						String depNo1 = String.valueOf(JSONArray.fromObject(jsonArray1.get(j)).get(1)); //部门编号
        						if(depNo1.equals(depNo)) {
        							String managerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//审核经理姓名
        		        			String managerTime = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(2));//审核经理处理时间
        		        			String managerDscription = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(3));//审核经理描述
        		        			description8 += "； 审核描述："+managerDscription+"，审核人："+managerName+"，审核时间："+managerTime;
        						}
        					}
        				}

        			}else if("9".equals(depNo)) {//产品部
        				description9 = "产品： 处理描述："+handlerDscription+"，处理人："+handlerName;
        				if(signManagerInfo!=null && !signManagerInfo.isEmpty() && !"null".equals(signManagerInfo)) {
        					JSONArray jsonArray1 = JSONArray.fromObject(signManagerInfo);
        					for(int j=0;j<jsonArray1.size();j++) {
        						String depNo1 = String.valueOf(JSONArray.fromObject(jsonArray1.get(j)).get(1)); //部门编号
        						if(depNo1.equals(depNo)) {
        							String managerName = String.valueOf(getMemberInfoById(Integer.parseInt(memberId)).get("name"));//审核经理姓名
        		        			String managerTime = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(2));//审核经理处理时间
        		        			String managerDscription = String.valueOf(JSONArray.fromObject(jsonArray1.get(i)).get(3));//审核经理描述
        		        			description9 += "； 审核描述："+managerDscription+"，审核人："+managerName+"，审核时间："+managerTime;
        						}
        					}
        				}
        				
        			}
        			    			
        		}
        		if(!"".equals(description2)) {
        			description += description2 + "\n";
        		}
        		if(!"".equals(description3)) {
        			description += description3 + "\n";
        		}
        		if(!"".equals(description4)) {
        			description += description4 + "\n";
        		}
        		if(!"".equals(description5)) {
        			description += description5 + "\n";
        		}
        		if(!"".equals(description7)) {
        			description += description7 + "\n";
        		}
        		if(!"".equals(description8)) {
        			description += description8 + "\n";
        		}
        		if(!"".equals(description9)) {
        			description += description9 + "\n";
        		}
        		content.put("会签信息", description);  
        		System.out.println("胡倩信息"+description);
        	}
        }
        
        //质量验证
        if("暂停".equals(exceptionMap.get("finalHandleMethod"))) {
        	content.put("最终处理方式之暂停", "√");  
        	content.put("最终处理方式之返工", "□");  
        	content.put("最终处理方式之后续工序修复", "□");  
        	content.put("最终处理方式之不良送修", "□");  
        	content.put("最终处理方式之其他", "□");  
        }else if("返工".equals(exceptionMap.get("finalHandleMethod"))) {
           	content.put("最终处理方式之暂停", "□");  
        	content.put("最终处理方式之返工", "√");  
        	content.put("最终处理方式之后续工序修复", "□");  
        	content.put("最终处理方式之不良送修", "□");   
        	content.put("最终处理方式之其他", "□"); 
        }else if("后续工序修复".equals(exceptionMap.get("finalHandleMethod"))) {
        	content.put("最终处理方式之暂停", "□");  
        	content.put("最终处理方式之返工", "□");  
        	content.put("最终处理方式之后续工序修复", "√");  
        	content.put("最终处理方式之不良送修", "□");  
        	content.put("最终处理方式之其他", "□"); 
        }else if("不良送修".equals(exceptionMap.get("finalHandleMethod"))) {
        	content.put("最终处理方式之暂停", "□");  
        	content.put("最终处理方式之返工", "□");  
        	content.put("最终处理方式之后续工序修复", "□");    
        	content.put("最终处理方式之不良送修", "√");  
        	content.put("最终处理方式之其他", "□"); 
        }else if("其他".equals(exceptionMap.get("finalHandleMethod"))) {
        	content.put("最终处理方式之暂停", "□");  
        	content.put("最终处理方式之返工", "□");  
        	content.put("最终处理方式之后续工序修复", "□");  
        	content.put("最终处理方式之不良送修", "□");  
        	content.put("最终处理方式之其他", "√"); 
        }else {
        	content.put("最终处理方式之暂停", "□");  
        	content.put("最终处理方式之返工", "□");  
        	content.put("最终处理方式之后续工序修复", "□");  
        	content.put("最终处理方式之不良送修", "□");  
        	content.put("最终处理方式之其他", "□");
        }

        if(!String.valueOf(exceptionMap.get("verifyHandler")).isEmpty()&&!"null".equals(String.valueOf(exceptionMap.get("verifyHandler")))) {
            content.put("验证结论",  String.valueOf(exceptionMap.get("verifyConclusion")));
            content.put("是否返工",  String.valueOf(exceptionMap.get("isRework")));
        	content.put("质量验证处理人",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("verifyHandler")))).get("name")));           
        }
        if(!String.valueOf(exceptionMap.get("verifyManager")).isEmpty()&&!"null".equals(String.valueOf(exceptionMap.get("verifyManager")))) {
        	content.put("质量验证审核经理",String.valueOf(getMemberInfoById(Integer.parseInt(String.valueOf(exceptionMap.get("verifyManager")))).get("name")));
            content.put("质量验证审核时间",String.valueOf(exceptionMap.get("verifyManagerTime")).substring(0,String.valueOf(exceptionMap.get("verifyManagerTime")).length()-2));
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
		String sql = "select * from tf_exception where exceptionId = " + exceptionId;
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
