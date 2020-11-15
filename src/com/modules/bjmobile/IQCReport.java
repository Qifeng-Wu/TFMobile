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
 * IQC检验
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/bj-mobile/IQCReport")
public class IQCReport {

	/**
	 * 1、获取所有记录信息(包括搜索)
	 * @param 
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		String date = request.getParameter("date");
		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sql = "select *,[供 应 商] as 供应商 from IQC_Report where CONVERT(varchar(100),检验日期,23)='"+date+"' and 部件料号 like ('%"+searchName+"%') order by 检验日期 desc";		
		List<Object> list = sqlhelper.query(sql);
		System.out.println("========="+list);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	
}
