package com.modules.bjmobile;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.ProductionSqlServerHelper;
import com.modules.utils.AjaxJson;


/**
 * 订单进度
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/bj-mobile/orderProgress")
public class OrderProgress {

	/**
	 * 1、获取所有订单进度记录信息
	 * @param 
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*5;
		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sq = "select top 5 a.OrderNumber from tbOrder a where a.StartDate = convert(VARCHAR(10),getdate()-1,20) and a.OrderNumber not in (select top "+page+" b.OrderNumber from tbOrder b where b.StartDate = convert(VARCHAR(10),getdate()-1,20) order by b.OrderNumber desc) order by a.OrderNumber desc";
		List<Object> list = sqlhelper.query(sq);
		List<Object> resultList = new ArrayList<Object>();
		if(list!=null && list.size()>0) {
			String OrderNumber = "";
			for(int i=0;i<list.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				OrderNumber = String.valueOf(map.get("OrderNumber"));
				//获取订单的投产中的数量
				String sql1 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9) where SUBSTRING(p.SN,6,9)='"+OrderNumber+"'";
				//获取订单的投产中的已装配数量
				String sql2 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=005";
				//获取订单的投产中的已终检数量
				String sql3 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=010";
				//获取订单的投产中的已包装数量
				String sql4 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=013";
				//获取订单的投产中的已入库数量
				String sql5 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=018";
				//该条记录总sql
				String sql = "select a.OrderNumber,a.Quantity,a.MNumber,a.StartDate,a.MModel,("+sql1+") as orderCount,("+sql2+") as assembly,("+sql3+") as finalInspection,("+sql4+") as packaging,("+sql5+") as putStorage from tbOrder a where a.OrderNumber='"+OrderNumber+"'";
				List<Object> list1 = sqlhelper.query(sql);
				resultList.add(list1.get(0));
			}
			
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", resultList);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 2、模糊搜索获取所有订单进度记录
	 * @param 
	 */
	@RequestMapping(value = "searchList")
	@ResponseBody
	public AjaxJson searchList(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sq = "select OrderNumber from tbOrder where OrderNumber = '"+searchName+"'";				
		List<Object> list = sqlhelper.query(sq);
		List<Object> resultList = new ArrayList<Object>();
		if(list!=null && list.size()>0) {
			String OrderNumber = "";
			for(int i=0;i<list.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				OrderNumber = String.valueOf(map.get("OrderNumber"));
				//获取订单的投产中的数量
				String sql1 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9) where SUBSTRING(p.SN,6,9)='"+OrderNumber+"'";
				//获取订单的投产中的已装配数量
				String sql2 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=005";
				//获取订单的投产中的已终检数量
				String sql3 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=010";
				//获取订单的投产中的已包装数量
				String sql4 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=013";
				//获取订单的投产中的已入库数量
				String sql5 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=018";
				//该条记录总sql
				String sql = "select a.OrderNumber,a.Quantity,a.StartDate,a.MModel,("+sql1+") as orderCount,("+sql2+") as assembly,("+sql3+") as finalInspection,("+sql4+") as packaging,("+sql5+") as putStorage from tbOrder a where a.OrderNumber='"+OrderNumber+"'";
				List<Object> list1 = sqlhelper.query(sql);
				resultList.add(list1.get(0));
			}
			
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", resultList);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 3、根据日期搜索订单进度
	 * @param startTime , endTime
	 * @throws ParseException 
	 */
	@RequestMapping(value = "dateSearch")
	@ResponseBody
	public AjaxJson dateSearch(HttpServletRequest request) throws ParseException{
		String startTime = request.getParameter("startTime");
		String endTime = request.getParameter("endTime");

		AjaxJson ajax = new AjaxJson();			
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sq = "select OrderNumber from tbOrder where StartDate between '"+startTime+"' and '"+endTime+"' order by StartDate desc";				
		List<Object> list = sqlhelper.query(sq);
		List<Object> resultList = new ArrayList<Object>();
		if(list!=null && list.size()>0) {
			String OrderNumber = "";
			for(int i=0;i<list.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				OrderNumber = String.valueOf(map.get("OrderNumber"));
				//获取订单的投产中的数量
				String sql1 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9) where SUBSTRING(p.SN,6,9)='"+OrderNumber+"'";
				//获取订单的投产中的已装配数量
				String sql2 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=005";
				//获取订单的投产中的已终检数量
				String sql3 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=010";
				//获取订单的投产中的已包装数量
				String sql4 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=013";
				//获取订单的投产中的已入库数量
				String sql5 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=018";
				//该条记录总sql
				String sql = "select a.OrderNumber,a.Quantity,a.StartDate,a.MModel,("+sql1+") as orderCount,("+sql2+") as assembly,("+sql3+") as finalInspection,("+sql4+") as packaging,("+sql5+") as putStorage from tbOrder a where a.OrderNumber='"+OrderNumber+"'";
				List<Object> list1 = sqlhelper.query(sql);
				resultList.add(list1.get(0));
			}
			
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", resultList);
		ajax.setBody(body);
		return ajax;
	}
	
	public static void main(String[] args) {
		ProductionSqlServerHelper sqlhelper = new ProductionSqlServerHelper();
		String sq = "select top 10 OrderNumber from tbOrder ";
		List<Object> list = sqlhelper.query(sq);
		List<Object> resultList = new ArrayList<Object>();
		if(list!=null && list.size()>0) {
			String OrderNumber = "";
			for(int i=0;i<list.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list.get(i);
				OrderNumber = String.valueOf(map.get("OrderNumber"));
				//获取订单的投产中的数量
				String sql1 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9) where SUBSTRING(p.SN,6,9)='"+OrderNumber+"'";
				//获取订单的投产中的已装配数量
				String sql2 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=005";
				//获取订单的投产中的已终检数量
				String sql3 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=010";
				//获取订单的投产中的已包装数量
				String sql4 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=013";
				//获取订单的投产中的已入库数量
				String sql5 = "select count(*) from tbProduct p left join tbOrder o on o.OrderNumber=SUBSTRING(p.SN,6,9)  left join tbStation s on p.CurrentStation=s.Station where SUBSTRING(p.SN,6,9)='"+OrderNumber+"' and s.StationID>=018";
				//该条记录总sql
				//String sql = "select top 10 * from tbEngineeringSpecialOp where specialOperationId not in (select top "+page+" specialOperationId from tbEngineeringSpecialOp order by specialOperationId desc) order by specialOperationId desc";		
				String sql = "select *,("+sql1+") as orderCount,("+sql2+") as assembly,("+sql3+") as finalInspection,("+sql4+") as packaging,("+sql5+") as putStorage from tbOrder";
				List<Object> list1 = sqlhelper.query(sql);
				resultList.add(list1.get(0));
			}
			
		}
		System.out.println(resultList);
}
}