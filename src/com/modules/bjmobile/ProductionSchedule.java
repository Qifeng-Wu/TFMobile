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
 * 排产信息
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/bj-mobile/productionSchedule")
public class ProductionSchedule {

	/**
	 * 1、获取排产信息记录信息
	 * @param 
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		String date = request.getParameter("date");
		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sql = "select o.OrderNumber,count(p.SN) as 'number',o.StartDate,o.MNumber,o.Type,e.Rate,p.Line from tbProduct p "
					+ "left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9) "
					+ "left join tbEngineeringSpecialOp e on e.OrderNumber=o.OrderNumber "
					+ "left join tbTime t on p.SN = t.SN where StationID='005' and CONVERT(varchar(100),t.DTTime,23)='"+date+"'  "
					+ "group by o.OrderNumber,o.StartDate,o.MNumber,o.Type,e.Rate,p.Line"; 				
		List<Object> list = sqlhelper.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	
}
