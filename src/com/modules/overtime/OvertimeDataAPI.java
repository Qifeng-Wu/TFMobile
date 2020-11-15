package com.modules.overtime;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SQLHelper;
import com.modules.utils.AjaxJson;

/**
 * 加班申请数据统计控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/overtime/data")
public class OvertimeDataAPI {
	/**
	 * 1、获取当前个人的待处理加班申请记录信息
	 * @param memberId,type,page
	 */
	@RequestMapping(value = "findOwnList")
	@ResponseBody
	public AjaxJson findOwnList(AjaxJson ajax, HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String type = request.getParameter("type");
		String memberId = request.getParameter("memberId");
		String department = request.getParameter("department");
		String position = request.getParameter("position");
		String isleader = request.getParameter("isleader");
		String userId = request.getParameter("userId");		
		String departmentName = getDepartmentNameByCode(department);
		SQLHelper sqlhe = new SQLHelper();
		List<Object> supplementList = new ArrayList<Object>();
		String sql = "";
		if("1".equals(type)){//查询个人待处理记录
			//待中心副总处理
			if("ZangYun".equals(userId)) {
				sql = "select * from tf_overtime_application where state = 5 and department in('生产部','仓储部','工业技术部')";
			}else if("XuFeng".equals(userId)) {
				sql = "select * from tf_overtime_application where state = 5 and department ='工程部'";
			}else if("ZhouMin".equals(userId)) {
				sql = "select * from tf_overtime_application where state = 5 and department ='质量部'";
			}else if("HuangZhongNan".equals(userId)) {
				sql = "select * from tf_overtime_application where state = 5 and department ='研发部'";
			}else{			
				//待部门经理处理
				if("1".equals(isleader)) {//待部门经理处理					
					sql = "select * from tf_overtime_application where state = 2 and department = '"+departmentName+"'";
				}
				//待生产调度处理		
				if("[4]".equals(department) && position.indexOf("调度")!=-1) {
					sql = "select * from tf_overtime_application where state = 1 and department = '生产部'";
				}
				//待工程工艺主管处理
				if("[3]".equals(department)) {//如果是工程部，生产发的加班申请也得确认
					if("1".equals(isleader) && position.indexOf("工艺主管")!=-1) {
						sql = "select * from tf_overtime_application where (state = 3 and department='生产部') or (state = 2 and department = '"+departmentName+"')";
					}else if(position.indexOf("工艺主管")!=-1) {
						sql = "select * from tf_overtime_application where state = 3 and department='生产部'";
					}else if("1".equals(isleader)){
						sql = "select * from tf_overtime_application where state = 2 and department = '"+departmentName+"'";
					}					
				}
			}
			//获取补充加班人员信息数据待处理
			if("1".equals(isleader)) {//待部门经理处理
				String sqls = "select * from tf_overtime_supplement where state = 0 and department = '"+departmentName+"'";
				supplementList = sqlhe.query(sqls);
			}
		}else if("2".equals(type)){//已处理
			sql = "select * from tf_overtime_application where applicant = '"+memberId+"' or dispatcher = '"+memberId+"' or manager = '"+memberId+"' or engineer = '"+memberId+"' or general = '"+memberId+"'";
		}
		sql += " order by date desc limit "+page+",10";
		List<Object> list = sqlhe.query(sql);	
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		body.put("supplementList", supplementList);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 2、获取我的申请加班记录
	 * @param memberId,type,page
	 */
	@RequestMapping(value = "findMyList")
	@ResponseBody
	public AjaxJson findMyList(AjaxJson ajax, HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String memberId = request.getParameter("memberId");
		String type = request.getParameter("type");
		String overtimeType = request.getParameter("overtimeType");
		SQLHelper sqlhe = new SQLHelper();
		String sql = "";
		if("0".equals(overtimeType)) {//新增类型			
			if("0".equals(type)){//部门
				sql = "select * from tf_overtime_application where applicant = '"+memberId+"'";	
			}else if("1".equals(type)){//班组
				sql = "select * from tf_overtime_team_application where applicant = '"+memberId+"'";
			}		
		}else if("1".equals(overtimeType)){//补充类型
			sql = "select * from tf_overtime_supplement where applicant = '"+memberId+"'";				
		}		
		sql += " order by date desc limit "+page+",10";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 3、获取我的申请加班记录(日期查询)
	 * @param memberId,type,page,date
	 */
	@RequestMapping(value = "findMyListByDate")
	@ResponseBody
	public AjaxJson findMyListByDate(AjaxJson ajax, HttpServletRequest request){
		String memberId = request.getParameter("memberId");
		String date = request.getParameter("date");
		String type = request.getParameter("type");
		String overtimeType = request.getParameter("overtimeType");
		SQLHelper sqlhe = new SQLHelper();
		String sql = "";
		if("0".equals(overtimeType)) {
			if("0".equals(type)){//部门
				sql = "select * from tf_overtime_application where applicant = '"+memberId+"' and date= '"+date+"'";			
			}else if("1".equals(type)){//班组
				sql = "select * from tf_overtime_team_application where applicant = '"+memberId+"' and date= '"+date+"'";		
			}
		}else if("1".equals(overtimeType)) {
			sql = "select * from tf_overtime_supplement where applicant = '"+memberId+"' and date= '"+date+"'";			
		}
		
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 4、获取部门申请加班记录
	 * @param department,page
	 */
	@RequestMapping(value = "findDepartmentList")
	@ResponseBody
	public AjaxJson findDepartmentList(AjaxJson ajax, HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String department = request.getParameter("department");
		String departmentName = getDepartmentNameByCode(department);
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_overtime_application where department = '"+departmentName+"' order by date desc limit "+page+",10";		
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 5、获取部门申请加班记录(日期查询)
	 * @param department,date
	 */
	@RequestMapping(value = "findDepartmentListByDate")
	@ResponseBody
	public AjaxJson findDepartmentListByDate(AjaxJson ajax, HttpServletRequest request){
		String date = request.getParameter("date");
		String department = request.getParameter("department");
		String departmentName = getDepartmentNameByCode(department);
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_overtime_application where department = '"+departmentName+"' and date= '"+date+"'";		
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
