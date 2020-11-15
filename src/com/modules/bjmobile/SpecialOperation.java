package com.modules.bjmobile;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.ProductionSqlServerHelper;
import com.modules.utils.AjaxJson;


/**
 * 特殊操作
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/bj-mobile/specialOperation")
public class SpecialOperation {

	/**
	 * 1、获取所有特殊操作记录信息
	 * @param 
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sql = "select top 10 * from tbEngineeringSpecialOp where specialOperationId not in (select top "+page+" specialOperationId from tbEngineeringSpecialOp order by specialOperationId desc) order by specialOperationId desc";		
		List<Object> list = sqlhelper.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 2、模糊搜索获取所有特殊操作记录
	 * @param 
	 */
	@RequestMapping(value = "searchList")
	@ResponseBody
	public AjaxJson searchList(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sql = "select * from tbEngineeringSpecialOp where OrderNumber like ('%"+searchName+"%') or Rate like ('%"+searchName+"%') order by specialOperationId desc";
		List<Object> list = sqlhelper.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
}
