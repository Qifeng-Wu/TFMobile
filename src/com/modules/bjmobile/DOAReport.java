package com.modules.bjmobile;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.QualitySqlServerHelper;
import com.modules.utils.AjaxJson;


/**
 * DOA退换货
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/bj-mobile/DOAReport")
public class DOAReport {

	/**
	 * 1、获取所有记录信息
	 * @param 
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		QualitySqlServerHelper sqlhelper = new QualitySqlServerHelper();
		String sql = "SELECT ProductCategory,ArriveCategory,count(*) as number FROM TB_Receive WHERE ProductCategory like ('%"+searchName+"%') OR ArriveCategory like ('%"+searchName+"%') group by ArriveCategory,ProductCategory ORDER BY ProductCategory DESC";		
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
		String ProductCategory = request.getParameter("ProductCategory");
		String ArriveCategory = request.getParameter("ArriveCategory");
		AjaxJson ajax = new AjaxJson();			
		QualitySqlServerHelper sqlhelper = new QualitySqlServerHelper();
		String sql = "SELECT Model,count(*) as number FROM  TB_Receive where ProductCategory='"+ProductCategory+"' and ArriveCategory='"+ArriveCategory+"' group by Model";		
		List<Object> list = sqlhelper.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}

}
