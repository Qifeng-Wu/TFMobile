package com.modules.common;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.apache.poi.ss.usermodel.Row;
import com.modules.db.SQLHelper;
import com.modules.export.ImportExcel;
import com.modules.utils.AjaxJson;

/**
 * 无锡工厂成员控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/wx-member")
public class WxMemberAPI {
   /**
	 * 1、新增员工信息
	 * @param name,no,position,organization,department,team,hiredate
	 * @throws ParseException 
	 */
	@RequestMapping(value = "add")
	@ResponseBody
	public AjaxJson add(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String name = request.getParameter("name");
		String no = request.getParameter("no");
		String position = request.getParameter("position");
		String organization = request.getParameter("organization");
		String department = request.getParameter("department");
		String team = request.getParameter("team");
		String hiredate = request.getParameter("hiredate");
		SQLHelper sh = new SQLHelper();
		String sq = "SELECT wxId FROM tf_wuxi_member WHERE no = '"+no+"'";
		List<Object> list = sh.query(sq);
		if(list!=null && list.size()>0) {
			ajax.setSuccess(false);
			ajax.setMessage("员工工号已存在！");
			return ajax;
		}

		String sql = "INSERT INTO tf_wuxi_member (name,no,position,organization,department,team,hiredate,create_time) "+
					 "VALUES ('"+name+"','"+no+"','"+position+"','"+organization+"','"+department+"','"+team+"','"+hiredate+"',now())";
		sh.update(sql);			
		return ajax;
	}
	/**
	 * 2、根据主键编辑人员信息
	 * @param wxId
	 * @throws ParseException 
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "edit")
	@ResponseBody
	public AjaxJson edit(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String wxId = request.getParameter("wxId");
		String name = request.getParameter("name");
		String no = request.getParameter("no");
		String position = request.getParameter("position");
		String organization = request.getParameter("organization");
		String department = request.getParameter("department");
		String team = request.getParameter("team");
		String hiredate = request.getParameter("hiredate");
		SQLHelper sh = new SQLHelper();
		String sq = "SELECT wxId FROM tf_wuxi_member WHERE no = '"+no+"'";
		List<Object> list = sh.query(sq);
		if(list!=null && list.size()>0) {
			if(!wxId.equals(String.valueOf(((Map<String, Object>)list.get(0)).get("wxId")))) {
				ajax.setSuccess(false);
				ajax.setMessage("员工工号已存在！");
				return ajax;	
			}
		}		
		String sql = "UPDATE tf_wuxi_member SET name='"+name+"',no='"+no+"',position='"+position+"',organization='"+organization+
					 "',department='"+department+"',team='"+team+"',hiredate='"+hiredate+"',create_time=now() WHERE wxId = '"+wxId+"'";
		sh.update(sql);
		return ajax;
	}
	/**
	 * 3、根据主键获取人员信息
	 * @param wxId
	 * @throws ParseException 
	 */
	@RequestMapping(value = "info")
	@ResponseBody
	public AjaxJson info(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String wxId = request.getParameter("wxId");
		SQLHelper sh = new SQLHelper();
		String sql = "SELECT * FROM tf_wuxi_member WHERE delete_flag = 0 AND wxId = '"+wxId+"'";
		List<Object> list = sh.query(sql);
		Object member = null;
		if(list!=null && list.size()>0) {
			member = list.get(0);
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("无此人员信息");
			return ajax;
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();	
		body.put("member", member);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 4、删除人员信息
	 * @param wxIds
	 * @throws ParseException 
	 */
	@RequestMapping(value = "delete")
	@ResponseBody
	public AjaxJson delete(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String wxIds = request.getParameter("wxIds");
		String wxIdsArray[] = wxIds.split(",");
		SQLHelper sh = new SQLHelper();
		for(String wxId : wxIdsArray){
			String sql = "update tf_wuxi_member set delete_flag = 1,leavedate = DATE(now()) where wxId = '"+wxId+"'";
			sh.update(sql);
		}			
		return ajax;
	}
	
   /**
	 *5、根据条件获取人员信息
	 * @param  department,team
	 * @throws ParseException 
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findList(AjaxJson ajax,HttpServletRequest request) throws ParseException{
		String department = request.getParameter("department");
		String team = request.getParameter("team");
		SQLHelper sh = new SQLHelper();
		List<Object> list = new ArrayList<Object>();
		String sql = "";
		if(department!=null && !department.isEmpty() && team!=null && !team.isEmpty()) {
			sql = "SELECT * FROM tf_wuxi_member WHERE delete_flag = 0 AND department = '"+department+"' AND team=  '"+team+"' ORDER BY team";
			list = sh.query(sql);
		}else if(department!=null && !department.isEmpty()){
			if("质量部".equals(department)) {
				sql = "select team from tf_wuxi_member where delete_flag = 0 and organization = '质量中心' group by team desc";
			}else {
				sql = "select team from tf_wuxi_member where delete_flag = 0 and department = '"+department+"' group by team";
			}
			List<Object> teamGrouplist = sh.query(sql);
			if(teamGrouplist!=null && teamGrouplist.size()>0) {
				List<Object> memberList = new ArrayList<Object>();
				Map<String, Object> maps;
				String teamName, sqls;
				for(int i=0;i<teamGrouplist.size();i++) {				
					maps = (Map<String, Object>) teamGrouplist.get(i);
					teamName = String.valueOf(maps.get("team"));
					sqls = "select * from tf_wuxi_member where delete_flag = 0 and team = '"+teamName+"'";
					memberList = sh.query(sqls);
					list.add(memberList);
				  }
			}
		}else {
			ajax.setSuccess(false);
			ajax.setMessage("传参错误，请联系管理员");
			return ajax;
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();	
		body.put("list", list);
		ajax.setBody(body);
		return ajax;
	}

	/**
	 * 6、人员信息excel导入
	 * @param  department,team
	 * @throws ParseException 
	 */
	@RequestMapping(value = "importExcel")	
	public String importExcel(HttpServletRequest request) {
		MultipartResolver resolver = new CommonsMultipartResolver(request.getSession().getServletContext());
		MultipartHttpServletRequest multipartRequest = resolver.resolveMultipart(request);
		MultipartFile file = multipartRequest.getFile("file");
		int successNum = 0;
		try {
			SQLHelper sh = new SQLHelper();
			String name,no,position,team,department,organization,hiredate;			
			ImportExcel ei = new ImportExcel(file, 1, 0);
			String sql ="";
			for (int i = ei.getDataRowNum(); i < ei.getLastDataRowNum(); i++) {
				Row row = ei.getRow(i);
				name = String.valueOf(ei.getCellValue(row, 0));
				no = String.valueOf(ei.getCellValue(row, 1));
				position = String.valueOf(ei.getCellValue(row, 2));
				team = String.valueOf(ei.getCellValue(row, 3));
				department = String.valueOf(ei.getCellValue(row, 4));
				organization = String.valueOf(ei.getCellValue(row, 5));
				hiredate = String.valueOf(ei.getCellValue(row, 6));
				
				String sq = "SELECT wxId FROM tf_wuxi_member WHERE delete_flag = 0 and no = '"+no+"'";
				List<Object> list = sh.query(sq);
				if(list!=null && list.size()>0) {
					continue;
				}
				
				if(name.isEmpty()||no.isEmpty()||position.isEmpty()||department.isEmpty()||organization.isEmpty()||hiredate.isEmpty()) {
					//return "EXCEL Information Error Please Contact Administrator";
					continue;
				}
				
				sql = "INSERT INTO tf_wuxi_member (name,no,position,organization,department,team,hiredate,delete_flag,create_time) "+
						 "VALUES ('"+name+"','"+no+"','"+position+"','"+organization+"','"+department+"','"+team+"','"+hiredate+"',0,now())";
				boolean b = sh.update(sql);
				if(b) {
					successNum++;
				}				
			}		
		} catch (Exception e) {
			return "EXCEL Information Error Please Contact Administrator";
		}
		return "A total of "+successNum+" pieces of data were successfully imported";
	}
	

	/**
	 * 导入测试
	 */
	public static void main(String[] args) throws Throwable {
		
		int successNum = 0;
		try {
			ImportExcel ei = new ImportExcel("C:\\Users\\pc\\Desktop\\全部人员信息表.xlsx", 1 ,0);
			SQLHelper sh = new SQLHelper();
			String name,no,position,team,department,organization,hiredate;			
			String sql ="";
			for (int i = ei.getDataRowNum(); i < ei.getLastDataRowNum(); i++) {
				Row row = ei.getRow(i);
				name = String.valueOf(ei.getCellValue(row, 0));
				no = String.valueOf(ei.getCellValue(row, 1));
				position = String.valueOf(ei.getCellValue(row, 2));
				team = String.valueOf(ei.getCellValue(row, 3));
				department = String.valueOf(ei.getCellValue(row, 4));
				organization = String.valueOf(ei.getCellValue(row, 5));
				hiredate = String.valueOf(ei.getCellValue(row, 6));
				
				sql = "INSERT INTO tf_wuxi_member (name,no,position,organization,department,team,hiredate,delete_flag,create_time) "+
						 "VALUES ('"+name+"','"+no+"','"+position+"','"+organization+"','"+department+"','"+team+"','"+hiredate+"',0,now())";
				boolean b = sh.update(sql);
				System.out.println(i+"==="+b);
				successNum++;
				System.out.println("==="+successNum);
			}		
		} catch (Exception e) {
			
		}
	}
}
