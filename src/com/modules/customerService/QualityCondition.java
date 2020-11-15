package com.modules.customerService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SaleSqlServerHelper;
import com.modules.utils.AjaxJson;

/**
 * 原载入大区，现不需要
 * 
 * @author Caroline
 */
@RestController
@RequestMapping(value = "/tf-api/customerService/QualityCondition/loadData")
public class QualityCondition {
	/**
	 * 1、获取大区选项
	 * 
	 * @param
	 */
	@RequestMapping(value = "getAreas")
	@ResponseBody
	public AjaxJson Areas(HttpServletRequest request) {
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();
		List<String> areaList = new ArrayList<String>();
		SaleSqlServerHelper sqlServerHelper = new SaleSqlServerHelper();
		String sql = " SELECT DISTINCT AREA_NAME FROM tb_CSS_Data(nolock)";
		List<Object> list1 = sqlServerHelper.query(sql);
		if (list1.size() < 1) {
			// 弹出提示信息
			ajax.setSuccess(false);
			ajax.setMessage("大区信息读取错误！");
			return ajax;
		}
		else {
			for (int i = 0; i < list1.size(); i++) {
				Map<String, Object> map = (Map<String, Object>) list1.get(i);
				String area = String.valueOf(map.get("AREA_NAME")).trim();
				areaList.add(area);
			}
		}
		System.out.println(areaList);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("areaList", areaList);
		ajax.setBody(body);
		return ajax;
	}
	

	/**
	 * 2、获取待查询条件，并输出按省份统计的表
	 * 
	 * @param
	 */
	@RequestMapping(value = "countByProvince")
	@ResponseBody
	public AjaxJson getCountByProvince(HttpServletRequest request) {
		//1.读取界面传过来的参数
		//String area=request.getParameter("area"); //"201805";
		String mydate=request.getParameter("mydate");//"台式机";
		//用于存储返回结果
		List<String> ConditionList=new ArrayList<String>();
		List<Integer> CountList=new ArrayList<Integer>();	
		//用于存储返回结果
//		List<String> Top5ConditionList=new ArrayList<String>();
//		List<Integer> Top5CountList=new ArrayList<Integer>();
		
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();
		SaleSqlServerHelper sqlServerHelper = new SaleSqlServerHelper();
//		String sql = " SELECT Province,TotalCount FROM tbCountByProvinceMonth(nolock) where Area='"+area+"' AND SelectedMonth ='"+mydate+"' ORDER BY TotalCount DESC";
//		List<Object> list1 = sqlServerHelper.query(sql);
//		if (list1.size() < 1) {
//			//没有记录，执行过存储过程
//			String strSql =	"EXEC [dbo].[CountByProvince] @area ='"+ area +"' , @mydate ='"+mydate+"'" ;
//			sqlServerHelper.query(strSql);
//			list1 = sqlServerHelper.query(sql);
//		}
//		if (list1.size() > 0) {
//			for(int i=0;i<list1.size();i++) {
//				@SuppressWarnings("unchecked")
//				Map<String, Object> map = (Map<String, Object>) list1.get(i);
//				String provinces = String.valueOf(map.get("Province"));
//				String counts = String.valueOf(map.get("TotalCount"));				
//				ConditionList.add(provinces);
//				CountList.add(Integer.parseInt(counts));
//				System.out.println(provinces+":"+counts);
//			}
//		}

//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
//        String time=dateFormat.format(date);
//        if(mydate==time) {
//        	String sqlDel = " DELETE FROM tbCountByProvinceMonth WHERE SelectedMonth ='"+mydate+"'";
//        	sqlServerHelper.update(sqlDel); 
//        	System.out.println("已删除"+mydate);
//        }       

		String sqlTop5 = " SELECT TOP 5 Province,TotalCount FROM tbCountByProvinceMonth(nolock) where SelectedMonth ='"+mydate+"' ORDER BY TotalCount DESC";
		List<Object> list2 = sqlServerHelper.query(sqlTop5);
		
		if (list2.size() < 1 ) {
			//没有记录，执行过存储过程
			String strSql =	"EXEC [dbo].[CountByProvince]  @mydate ='"+mydate+"'" ;
			sqlServerHelper.query(strSql);
			list2 = sqlServerHelper.query(sqlTop5);
		}
		if (list2.size() > 1) 
		{	
			for(int i=0;i<list2.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list2.get(i);
				String provinces = String.valueOf(map.get("Province"));
				String counts = String.valueOf(map.get("TotalCount"));				
				ConditionList.add(provinces);
				CountList.add(Integer.parseInt(counts));
				System.out.println(provinces+":"+counts);
			}
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("ConditionList", ConditionList);
		body.put("CountList", CountList);
//		body.put("Top5ConditionList", Top5ConditionList);
//		body.put("Top5CountList", Top5CountList);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 3、获取待查询条件，并输出台式机故障件统计的表
	 * 
	 * @param
	 */
	@RequestMapping(value = "DTcountByPart")
	@ResponseBody
	public AjaxJson getCountByDTPart(HttpServletRequest request) {
		//1.读取界面传过来的参数
//		String area=request.getParameter("area"); //"201805";
		String mydate=request.getParameter("mydate");//"2018-08";	

        
		//用于存储返回结果
		List<String> ConditionList=new ArrayList<String>();
		List<Integer> CountList=new ArrayList<Integer>();
		
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();
		SaleSqlServerHelper sqlServerHelper = new SaleSqlServerHelper();
		
//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
//        String time=dateFormat.format(date);
//        if(mydate==time) {
//        	String sqlDel = " DELETE FROM tbCountByProvinceMonth WHERE SelectedMonth ='"+mydate+"'";
//        	sqlServerHelper.update(sqlDel); 
//        	System.out.println("已删除"+mydate);
//        }     
		
		String sql = " SELECT PartType,TotalCount FROM tbCountByPart(nolock) where SelectedMonth ='"+mydate+"' AND MType ='台式机' ORDER BY TotalCount DESC";
		List<Object> list1 = sqlServerHelper.query(sql);
		System.out.println(list1.size()+"");
		if (list1.size() < 1) {
			//没有记录，执行过存储过程
			String strSql =	"EXEC [dbo].[CountByPart]  @mydate ='"+mydate+"'" ;
			sqlServerHelper.query(strSql);
			list1 = sqlServerHelper.query(sql);
		}

		if (list1.size() > 0) {
			for(int i=0;i<list1.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list1.get(i);
				String conditions = String.valueOf(map.get("PartType"));
				String counts = String.valueOf(map.get("TotalCount"));				
				ConditionList.add(conditions);
				CountList.add(Integer.parseInt(counts));
				System.out.println(conditions+":"+counts);
			}
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("ConditionList", ConditionList);
		body.put("CountList", CountList);
		ajax.setBody(body);
		return ajax;
	}

	/**
	 * 4、获取待查询条件，并输出故障件统计的表 一体机 AIO
	 * 
	 * @param
	 */
	@RequestMapping(value = "AIOcountByPart")
	@ResponseBody
	public AjaxJson getCountByAIOPart(HttpServletRequest request) {
		//1.读取界面传过来的参数
//		String area=request.getParameter("area"); //"201805";

		String mydate=request.getParameter("mydate");//"2018-08";	
		System.out.println(mydate);	
		//用于存储返回结果
		List<String> ConditionList=new ArrayList<String>();
		List<Integer> CountList=new ArrayList<Integer>();
		
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();
		SaleSqlServerHelper sqlServerHelper = new SaleSqlServerHelper();
		String sql = " SELECT PartType,TotalCount FROM tbCountByPart(nolock) where SelectedMonth ='"+mydate+"' AND MType='一体机' ORDER BY TotalCount DESC";
		List<Object> list1 = sqlServerHelper.query(sql);
		System.out.println(list1.size()+"");

//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
//        String time=dateFormat.format(date);
//        if(mydate==time) {
//        	String sqlDel = " DELETE FROM tbCountByProvinceMonth WHERE SelectedMonth ='"+mydate+"'";
//        	sqlServerHelper.update(sqlDel); 
//        	System.out.println("已删除"+mydate);
//        }     
        
		if (list1.size() < 1) {
			//没有记录，执行过存储过程
			String strSql =	"EXEC [dbo].[CountByPart]  @mydate ='"+mydate+"'" ;
			sqlServerHelper.query(strSql);
			list1 = sqlServerHelper.query(sql);
		}

		if (list1.size() > 0) {
			for(int i=0;i<list1.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list1.get(i);
				String conditions = String.valueOf(map.get("PartType"));
				String counts = String.valueOf(map.get("TotalCount"));				
				ConditionList.add(conditions);
				CountList.add(Integer.parseInt(counts));
				System.out.println(conditions+":"+counts);
			}
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("ConditionList", ConditionList);
		body.put("CountList", CountList);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 5、获取待查询条件，并输出按主板统计的表    取消未用
	 * 
	 * @param
	 */
	@RequestMapping(value = "countByMainBoardCom")
	@ResponseBody
	public AjaxJson getCountByMainBoardCom(HttpServletRequest request) {
		//1.读取界面传过来的参数
//		String area=request.getParameter("area"); //"201805";
		String mydate=request.getParameter("mydate");//"台式机";		
		//用于存储返回结果
		List<String> ConditionList=new ArrayList<String>();
		List<Integer> CountList=new ArrayList<Integer>();
		
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();
		SaleSqlServerHelper sqlServerHelper = new SaleSqlServerHelper();
//		String sql = " SELECT Company,TotalCount FROM tbCountByMBCompany(nolock) where Area='"+area+"' AND SelectedMonth ='"+mydate+"' ORDER BY TotalCount DESC";
		String sql = " SELECT Company,TotalCount FROM tbCountByMBCompany(nolock) where SelectedMonth ='"+mydate+"' ORDER BY TotalCount DESC";
		List<Object> list1 = sqlServerHelper.query(sql);
		
//	    Date date = new Date();
//	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
//	    String time=dateFormat.format(date);
//	    if(mydate==time) {
//	    	String sqlDel = " DELETE FROM tbCountByProvinceMonth WHERE SelectedMonth ='"+mydate+"'";
//	    	sqlServerHelper.update(sqlDel); 
//	    	System.out.println("已删除"+mydate);
//	    }       

		if (list1.size() < 1) {
			//没有记录，执行过存储过程
//			String strSql =	"EXEC [dbo].[CountByMainBoard] @area ='"+ area +"' , @mydate ='"+mydate+"'" ;
			String strSql =	"EXEC [dbo].[CountByMainBoard] @mydate ='"+mydate+"'" ;
			sqlServerHelper.query(strSql);
			list1 = sqlServerHelper.query(sql);
		}

		if (list1.size() > 0) {
			for(int i=0;i<list1.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list1.get(i);
				String conditions = String.valueOf(map.get("Company"));
				String counts = String.valueOf(map.get("TotalCount"));				
				ConditionList.add(conditions);
				CountList.add(Integer.parseInt(counts));
				System.out.println(conditions+":"+counts);
			}
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("ConditionList", ConditionList);
		body.put("CountList", CountList);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 6、前五的服务站数量统计
	 * 
	 * @param
	 */
	@RequestMapping(value = "top5ServiceStation")
	@ResponseBody
	public AjaxJson getStation(HttpServletRequest request) {
		//1.读取界面传过来的参数
		String mydate=request.getParameter("mydate");//"2018-08";		
		//用于存储返回结果
		List<String> ConditionList=new ArrayList<String>();
		List<Integer> CountList=new ArrayList<Integer>();	
		List<String> StationNameList=new ArrayList<String>();	
		
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();
		SaleSqlServerHelper sqlServerHelper = new SaleSqlServerHelper();

//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
//        String time=dateFormat.format(date);
//        if(mydate==time) {
//        	String sqlDel = " DELETE FROM tbCountByProvinceMonth WHERE SelectedMonth ='"+mydate+"'";
//        	sqlServerHelper.update(sqlDel); 
//        	System.out.println("已删除"+mydate);
//        }       


		String sqlTop5 = " SELECT TOP 5 SERVICE_STATION_ID,FIX_STATION_NAME,TotalCount FROM tbTop10ServiceStation(nolock) where SelectedMonth ='"+mydate+"' ORDER BY TotalCount DESC";
		List<Object> list1 = sqlServerHelper.query(sqlTop5);
		if (list1.size() < 1) {
			//没有记录，执行过存储过程
			String strSql =	"EXEC [dbo].[Top10ServiceStation] @mydate ='"+mydate+"'" ;
			sqlServerHelper.query(strSql);
			list1 = sqlServerHelper.query(sqlTop5);
		}
		if (list1.size() > 1) 
		{	
			for(int i=0;i<list1.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list1.get(i);
				String stationId = String.valueOf(map.get("SERVICE_STATION_ID"));
				String counts = String.valueOf(map.get("TotalCount"));		
				String stationName = String.valueOf(map.get("FIX_STATION_NAME"));		
				ConditionList.add(stationId);
				CountList.add(Integer.parseInt(counts));	
				StationNameList.add(stationName.trim());
				System.out.println(stationId+','+stationName+":"+counts);
			}
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("ConditionList", ConditionList);
		body.put("CountList", CountList);
		body.put("StationNameList", StationNameList);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 7、台式机一体机服务器，两月数据对比
	 * 
	 * @param
	 */
	@RequestMapping(value = "monthsCompare")
	@ResponseBody
	public AjaxJson getCompareData(HttpServletRequest request) {
		//1.读取界面传过来的参数
		String mydate=request.getParameter("mydate");//"2018-08";		
		//用于存储返回结果
		List<String> ConditionList=new ArrayList<String>();
		List<Integer> CountList=new ArrayList<Integer>();	
		List<String> LastMonthConditionList=new ArrayList<String>();	
		List<Integer> LastMonthCountList=new ArrayList<Integer>();	
		
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();
		SaleSqlServerHelper sqlServerHelper = new SaleSqlServerHelper();

//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
//        String time=dateFormat.format(date);
//        if(mydate==time) {
//        	String sqlDel = " DELETE FROM tbCountByType WHERE SelectedMonth ='"+mydate+"'";
//        	sqlServerHelper.update(sqlDel); 
//        	System.out.println("已删除"+mydate);
//        }       

		String sql = " SELECT MType,TotalCount FROM tbCountByType(nolock) where SelectedMonth ='"+mydate+"' ORDER BY TotalCount DESC";
		List<Object> list1 = sqlServerHelper.query(sql);
		if (list1.size() < 1) {
			//没有记录，执行过存储过程
			String strSql =	"EXEC [dbo].[TwoMonthsCompare] @mydate ='"+mydate+"'" ;
			sqlServerHelper.query(strSql);
			list1 = sqlServerHelper.query(sql);
		}
		if (list1.size() > 1) 
		{	
			for(int i=0;i<list1.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list1.get(i);
				String type = String.valueOf(map.get("MType"));
				String counts = String.valueOf(map.get("TotalCount"));			
				ConditionList.add(type);
				CountList.add(Integer.parseInt(counts));
				System.out.println(type+":"+counts);
			}
		}
		
		String lastMonth="";
		String month=mydate.substring(5);
		if(month=="01")
		{
			int lastYear=Integer.parseInt(mydate.substring(0, 4))-1;
			lastMonth=lastYear+"-12";
		}
		else
		{
			int i=Integer.parseInt(month)-1;
			if(i<10)
				lastMonth=mydate.substring(0, 5)+"0"+i;
			else
				lastMonth=mydate.substring(0, 5)+i;
		}

		System.out.println(lastMonth);

		SaleSqlServerHelper sqlServerHelper2 = new SaleSqlServerHelper();
		String sqlLastMonth = " SELECT MType,TotalCount FROM tbCountByType(nolock) where SelectedMonth ='"+lastMonth+"' ORDER BY TotalCount DESC";
		List<Object> list2 = sqlServerHelper2.query(sqlLastMonth);
		if (list2.size() < 1) {
			//没有记录，执行过存储过程
			String strSql =	"EXEC [dbo].[TwoMonthsCompare] @mydate ='"+lastMonth+"'" ;
			sqlServerHelper2.query(strSql);
			list2 = sqlServerHelper2.query(sqlLastMonth);
		}
		if (list2.size() > 1) 
		{	
			for(int i=0;i<list2.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list2.get(i);
				String type = String.valueOf(map.get("MType"));
				String counts = String.valueOf(map.get("TotalCount"));		
				LastMonthConditionList.add(type);
				LastMonthCountList.add(Integer.parseInt(counts));
				System.out.println(type+":"+counts);
			}
		}
		
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("ConditionList", ConditionList);
		body.put("CountList", CountList);
		body.put("LastMonthConditionList", LastMonthConditionList);
		body.put("LastMonthCountList", LastMonthCountList);
		body.put("LastMonth", lastMonth);
		
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 8、使用时间
	 * 
	 * @param
	 */
	@RequestMapping(value = "pieUsingTime")
	@ResponseBody
	public AjaxJson getUsingTime(HttpServletRequest request) {
		//1.读取界面传过来的参数
		String mydate=request.getParameter("mydate");//"2018-08";		
		//用于存储返回结果
		List<String> ConditionList=new ArrayList<String>();
		List<Integer> CountList=new ArrayList<Integer>();	
		
		// TODO Auto-generated method stub
		AjaxJson ajax = new AjaxJson();
		SaleSqlServerHelper sqlServerHelper = new SaleSqlServerHelper();

//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
//        String time=dateFormat.format(date);
//        if(mydate==time) {
//        	String sqlDel = " DELETE * FROM tbCountByUsingTime WHERE SelectedMonth ='"+mydate+"'";
//        	sqlServerHelper.query(sqlDel); 
//        }
        
		String sql = " SELECT UsingTime,TotalCount FROM tbCountByUsingTime(nolock) where SelectedMonth ='"+mydate+"' ORDER BY TotalCount DESC";
		List<Object> list1 = sqlServerHelper.query(sql);
		if (list1.size() < 1) {
			//没有记录，执行过存储过程
			String strSql =	"EXEC [dbo].[CountByUsingTime] @mydate ='"+mydate+"'" ;
			sqlServerHelper.query(strSql);
			list1 = sqlServerHelper.query(sql);
		}
		if (list1.size() > 1) 
		{	
			for(int i=0;i<list1.size();i++) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) list1.get(i);
				String usingTime = String.valueOf(map.get("UsingTime"));
				String counts = String.valueOf(map.get("TotalCount"));			
				ConditionList.add(usingTime);
				CountList.add(Integer.parseInt(counts));	
				System.out.println(usingTime+":"+counts);
			}
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("ConditionList", ConditionList);
		body.put("CountList", CountList);
		ajax.setBody(body);
		return ajax;
	}

	
}
