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
 * 研发PVT阶段测试进度
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/bj-mobile/PVTReport")
public class PVTReport {

	/**
	 * 1、获取所有记录信息
	 * @param 
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findList(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sql = "SELECT type,count(*) as number FROM PVT_data WHERE type like ('%"+searchName+"%') group by type";		
		List<Object> list = sqlhelper.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 2、获取所详情信息
	 * @param 
	 */
	@RequestMapping(value = "detail")
	@ResponseBody
	public AjaxJson detail(HttpServletRequest request){
		String type = request.getParameter("type");
		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sql = "SELECT * FROM PVT_data where type='"+type+"'";		
		List<Object> list = sqlhelper.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}

}
