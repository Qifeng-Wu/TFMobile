package com.modules.improvement;


import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SQLHelper;
import com.modules.utils.AjaxJson;

/**
 * 改善提案据统计控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/improvement/data")
public class DataAPI {
	/**
	 * 1、获取我的改善提案记录
	 * @param memberId page
	 */
	@RequestMapping(value = "findMyList")
	@ResponseBody
	public AjaxJson findMyList(AjaxJson ajax, HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String memberId = request.getParameter("memberId");
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_improvement where delete_flag = 0 and proposer = '"+memberId+"' order by proposer_time desc limit "+page+",10";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 2、查询我的改善提案记录
	 * @param memberId searchName
	 */
	@RequestMapping(value = "searchMyList")
	@ResponseBody
	public AjaxJson searchMyList(AjaxJson ajax, HttpServletRequest request){
		String memberId = request.getParameter("memberId");
		String searchName = request.getParameter("searchName");
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_improvement where delete_flag = 0 and proposer = '"+memberId+"' and title like '%"+searchName+"%' order by proposer_time desc";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 3、获取待我处理改善提案记录
	 * @param memberId page
	 */
	@RequestMapping(value = "findOwnList")
	@ResponseBody
	public AjaxJson findOwnList(AjaxJson ajax, HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String department = request.getParameter("department");
		String memberId = request.getParameter("memberId");
		String position = request.getParameter("position");
		String isleader = request.getParameter("isleader");
		String departmentName = getDepartmentNameByCode(department);
		
		SQLHelper sqlhe = new SQLHelper();
		String sql;
		if("[2]".equals(department) && position.indexOf("工业主管")!=-1) {//工业主管
			sql = "select * from tf_improvement where delete_flag=0 and type=0 and state=5 union "+
				  "select * from tf_improvement where delete_flag=0 and type=1 and state=3";
		}else if("1".equals(isleader)) {//部门经理
			sql = "select * from tf_improvement where delete_flag=0 and type=0 and state=3 and department= '"+departmentName+"' union "+
					  "select * from tf_improvement where delete_flag=0 and type=1 and state=1 and department= '"+departmentName+"'";
		}else if(position.indexOf("主管")!=-1){
			sql = "select * from tf_improvement where delete_flag=0 and type=0 and (state=1 or state=4) and team ='"+position.replace("主管", "")+"' union "+
					  "select * from tf_improvement where delete_flag=0 and type=1 and state=2 and proposer='"+memberId+"'";
		}else {
			sql = "select * from tf_improvement where delete_flag=0 and type=0 and state=2 and proposer='"+memberId+"'";
		}
		sql += " order by proposer_time desc limit "+page+",10";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 3、查询待我处理改善提案记录
	 * @param memberId page
	 */
	@RequestMapping(value = "searchOwnList")
	@ResponseBody
	public AjaxJson searchOwnList(AjaxJson ajax, HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		String department = request.getParameter("department");
		String memberId = request.getParameter("memberId");
		String position = request.getParameter("position");
		String isleader = request.getParameter("isleader");
		String departmentName = getDepartmentNameByCode(department);
		
		SQLHelper sqlhe = new SQLHelper();
		String sql;
		if("[2]".equals(department) && position.indexOf("工业主管")!=-1) {//工业主管
			sql = "select * from tf_improvement where delete_flag=0 and type=0 and state=5 and title like '%"+searchName+"%' union "+
					"select * from tf_improvement where delete_flag=0 and type=1 and state=3 and title like '%"+searchName+"%'";
		}else if("1".equals(isleader)) {//部门经理
			sql = "select * from tf_improvement where delete_flag=0 and type=0 and state=3 and department= '"+departmentName+"' and title like '%"+searchName+"%' union "+
					"select * from tf_improvement where delete_flag=0 and type=1 and state=1 and department= '"+departmentName+"' and title like '%"+searchName+"%'";
		}else if(position.indexOf("主管")!=-1){
			sql = "select * from tf_improvement where delete_flag=0 and type=0 and (state=1 or state=4) and team ='"+position.replace("主管", "")+"' and title like '%"+searchName+"%' union "+
					"select * from tf_improvement where delete_flag=0 and type=1 and state=2 and proposer='"+memberId+"' and title like '%"+searchName+"%'";
		}else {
			sql = "select * from tf_improvement where delete_flag=0 and type=0 and state=2 and proposer='"+memberId+"' and title like '%"+searchName+"%'";
		}
		sql += " order by proposer_time desc";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 5、获取改善提案记录
	 * @param type page
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findList(AjaxJson ajax, HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String type = request.getParameter("type");
		
		SQLHelper sqlhe = new SQLHelper();
		String sql;
		if("1".equals(type)) {//未完结
			sql = "select * from tf_improvement where delete_flag=0 and state<>0";
		}else {//已完结
			sql = "select * from tf_improvement where delete_flag=0 and state=0";
		}
		sql += " order by proposer_time desc limit "+page+",10";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 6、查询改善提案记录
	 * @param type page
	 */
	@RequestMapping(value = "searchList")
	@ResponseBody
	public AjaxJson searchList(AjaxJson ajax, HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_improvement where delete_flag=0 and title like concat('%','"+searchName+"','%')";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 部门编号解析
	 * @param department
	 * @return departmentName
	 */
	public static String getDepartmentNameByCode(String department) {
		String departmentName = "";
		if("[2]".equals(department)) {
			departmentName = "工业技术部";
		}else if("[3]".equals(department)) {
			departmentName = "工程部";
		}else if("[4]".equals(department)) {
			departmentName = "生产部";
		}else if("[5]".equals(department)) {
			departmentName = "质量部";
		}else if("[7]".equals(department)) {
			departmentName = "研发部";
		}else if("[11]".equals(department)) {
			departmentName = "仓储部";
		}
		return departmentName;
	}
}
