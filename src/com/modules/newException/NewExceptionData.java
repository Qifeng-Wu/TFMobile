package com.modules.newException;


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
@RequestMapping(value = "tf-api/new-exception/data")
public class NewExceptionData {
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
		if("0".equals(type)){//查询已结案异常记录
			sql = "select * from tf_new_exception where deleteFlag = 0 and state = 20 order by exceptionId desc limit "+page+",10";
		}else if("1".equals(type)) {//查询待完成异常
			sql = "select a.*,b.* from tf_new_exception a,(select count(*) as exceptionCount from tf_new_exception where deleteFlag = 0 and ((state < 13 and state <> 10) or (state = 10 and exceptionType = '普通类'))) b "
					+ "where a.deleteFlag = 0 and ((a.state < 13 and a.state <> 10) or (a.state = 10 and a.exceptionType = '普通类')) order by exceptionId desc limit "+page+",10";
		}else if("2".equals(type)){//查询已完成异常
			sql = "select a.*,b.* from tf_new_exception a,(select count(*) as exceptionCount from tf_new_exception where deleteFlag = 0 and ((state > 12 and state < 20) or (state = 10 and (exceptionType = 'CCC类' or exceptionType = '能效类')))) b "
					+ "where a.deleteFlag = 0 and ((a.state > 12 and a.state < 20) or (a.state = 10 and (a.exceptionType = 'CCC类' or a.exceptionType = '能效类'))) order by exceptionId desc limit "+page+",10";
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
		String sql = "select * from tf_new_exception where deleteFlag = 0 and (serialNumber like concat('%','"+searchName+"','%') "
					+ "or ordermaking like concat('%','"+searchName+"','%') "
					+ "or model like concat('%','"+searchName+"','%') "
					+ "or isLongSolveMeasure = '"+searchName+"' "
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
			sql = "select * from tf_new_exception  where deleteFlag = 0 and ((select find_in_set("+memberId+",nextPerson)>0) or reporter= "+memberId+" or reportManager ="+memberId+" or determineHandler= "+memberId+
					" or determineManager= "+memberId+" or responsibilityHandler= "+memberId+" or responsibilityManager= "+memberId+" or signHandler= "+memberId+
					" or signManager= "+memberId+" or verifyHandler= "+memberId+" or verifyManager= "+memberId+" or longSolveMeasureEnder= "+memberId+
					" or longSolveMeasureVerifier= "+memberId+" or longSolveMeasureReviewer= "+memberId+") order by exceptionId desc limit "+page+",10";
		}else if("1".equals(type)) {//查询待办异常
			sql = "select *,(select count(*) from tf_new_exception  where deleteFlag = 0 and state <> 20 and (select find_in_set("+memberId+",nextPerson)>0)) as exceptionCount from tf_new_exception  where deleteFlag = 0 and state <> 20 and (select find_in_set("+memberId+",nextPerson)>0) order by exceptionId desc limit "+page+",10";
		}else if("2".equals(type)){//查询已办异常
			sql = "select * from tf_new_exception  where deleteFlag = 0 and (reporter= "+memberId+" or reportManager = "+memberId+" or determineHandler= "+memberId+
					" or determineManager= "+memberId+" or responsibilityHandler= "+memberId+" or responsibilityManager= "+memberId+" or signHandler= "+memberId+
					" or signManager= "+memberId+" or verifyHandler= "+memberId+" or verifyManager= "+memberId+" or longSolveMeasureEnder= "+memberId+
					" or longSolveMeasureVerifier= "+memberId+" or longSolveMeasureReviewer= "+memberId+") order by exceptionId desc limit "+page+",10";
		}
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("exceptionList", list);
		ajax.setBody(body);
		return ajax;
	}
	
	/**
	 * 4、判断当前制令单号有没有未完成的通报
	 * @param ordermaking
	 * @return boolean
	 */
	@RequestMapping(value = "isUnclosedByOrdermaking")
	@ResponseBody
	public boolean isUnclosedByOrdermaking(HttpServletRequest request){
		String ordermaking = request.getParameter("ordermaking");
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_new_exception where state < 13 and ordermaking = '"+ordermaking+"'";
		List<Object> list = sqlhe.query(sql);
		if(list!=null && list.size()>0) {
			return true;
		}
		return false;
	}
}
