package com.modules.jumpStation;


import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SQLHelper;
import com.modules.utils.AjaxJson;

/**
 * 跳站数据统计控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/jumpStation/data")
public class JumpStationData {
	/**
	 * 1、获取所有跳站记录信息
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
		if("0".equals(type)){//查询全部跳站记录
			sql = "select * from tf_jump_station order by jumpId desc limit "+page+",10";
		}else if("1".equals(type)) {//查询待办跳站
			sql = "select * from tf_jump_station where state <> 0 order by jumpId desc limit "+page+",10";
		}else if("2".equals(type)){//查询已办跳站
			sql = "select * from tf_jump_station where state = 0 order by jumpId desc limit "+page+",10";
		}
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("jumpStationList", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 2、模糊搜索获取所有跳站记录
	 * @param 
	 */
	@RequestMapping(value = "searchList")
	@ResponseBody
	public AjaxJson searchList(HttpServletRequest request){
		String searchName = request.getParameter("searchName");
		AjaxJson ajax = new AjaxJson();			
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_jump_station where ordermaking like concat('%','"+searchName+"','%') or sn like concat('%','"+searchName+"','%') order by jumpId desc";
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("jumpStationList", list);
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 3、获取当前个人的跳站信息
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
		if("0".equals(type)){//查询个人全部跳站
			sql = "select * from tf_jump_station  where (select find_in_set("+memberId+",pendingPerson)>0) or submitMan= "+memberId+" or submitLeader ="+memberId+" or engineeringHandler= "+memberId+
					" or engineeringLeader= "+memberId+" or qualityHandler= "+memberId+" or qualityLeader= "+memberId+" order by jumpId desc limit "+page+",10";
		}else if("1".equals(type)) {//查询待办跳站
			sql = "select * from tf_jump_station  where state <> 0 and (select find_in_set("+memberId+",pendingPerson)>0) order by jumpId desc limit "+page+",10";
		}else if("2".equals(type)){//查询已办跳站
			sql = "select * from tf_jump_station  where submitMan= "+memberId+" or submitLeader = "+memberId+" or engineeringHandler= "+memberId+
					" or engineeringLeader= "+memberId+" or qualityHandler= "+memberId+" or qualityLeader= "+memberId+" order by jumpId desc limit "+page+",10";
		}
		List<Object> list = sqlhe.query(sql);
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("jumpStationList", list);
		ajax.setBody(body);
		return ajax;
	}
}
