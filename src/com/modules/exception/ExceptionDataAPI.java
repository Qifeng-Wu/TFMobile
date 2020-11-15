package com.modules.exception;


import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SQLHelper;
import com.modules.utils.AjaxJson;

/**
 * 异常数据统计控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/exception/data")
public class ExceptionDataAPI {
	/**
	 * 1、获取所有异常记录信息
	 * @param 
	 */
	@RequestMapping(value = "findList")
	@ResponseBody
	public AjaxJson findInfo(HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String type = request.getParameter("type");
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sqlhe = new SQLHelper();
		String sql = "";
		if("0".equals(type)){//查询全部异常记录
			sql = "select * from tf_exception where deleteFlag = 0 order by exceptionId desc limit "+page+",10";
		}else if("1".equals(type)) {//查询待办异常
			sql = "select a.*,b.* from tf_exception a,(select count(*) as exceptionCount from tf_exception where deleteFlag = 0 and state <> 12) b "
					+ "where a.deleteFlag = 0 and a.state <> 12 order by exceptionId desc limit "+page+",10";
		}else if("2".equals(type)){//查询已办异常
			sql = "select * from tf_exception where deleteFlag = 0 and state = 12 order by exceptionId desc limit "+page+",10";
		}
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("exceptionList", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 2、模糊搜索获取所有异常记录
	 * @param 
	 */
	@RequestMapping(value = "searchList")
	@ResponseBody
	public AjaxJson searchList(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_exception where deleteFlag = 0 and (serialNumber like concat('%','"+searchName+"','%') "
				+ "or ordermaking like concat('%','"+searchName+"','%') "
				+ "or model like concat('%','"+searchName+"','%') "
				+ "or exceptionDescription like concat('%','"+searchName+"','%')) order by exceptionId desc";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("exceptionList", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 3、获取当前个人的异常信息
	 * @param memberId,type,page
	 */
	@RequestMapping(value = "findOwnList")
	@ResponseBody
	public AjaxJson findOwnList(HttpServletRequest request){
		Integer page = 	Integer.parseInt(request.getParameter("page"))*10;
		String type = request.getParameter("type");
		Integer memberId = Integer.parseInt(request.getParameter("memberId"));
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sqlhe = new SQLHelper();
		String sql = "";
		if("0".equals(type)){//查询个人全部异常
			sql = "select * from tf_exception  where deleteFlag = 0 and ((select find_in_set("+memberId+",pendingPerson)>0) or reporter= "+memberId+" or reportManager ="+memberId+" or determineHandler= "+memberId+
					" or determineManager= "+memberId+" or responsibilityHandler= "+memberId+" or responsibilityManager= "+memberId+
					" or verifyHandler= "+memberId+" or verifyManager= "+memberId+") order by exceptionId desc limit "+page+",10";
		}else if("1".equals(type)) {//查询待办异常
			sql = "select * from tf_exception  where deleteFlag = 0 and state <> 12 and (select find_in_set("+memberId+",pendingPerson)>0) order by exceptionId desc limit "+page+",10";
		}else if("2".equals(type)){//查询已办异常
			sql = "select * from tf_exception  where deleteFlag = 0 and (reporter= "+memberId+" or reportManager = "+memberId+" or determineHandler= "+memberId+
					" or determineManager= "+memberId+" or responsibilityHandler= "+memberId+" or responsibilityManager= "+memberId+
					" or verifyHandler= "+memberId+" or verifyManager= "+memberId+") order by exceptionId desc limit "+page+",10";
		}
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("exceptionList", list);
		ajax.setBody(body);
		return ajax;
	}
}
