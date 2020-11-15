package com.modules.member;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.jeewx.api.core.exception.WexinReqException;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.SQLHelper;
import com.modules.utils.AjaxJson;

/**
 * 企业成员控制层
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/member")
public class MemberAPI {
   /**
	 * 1、根据主键memberId获取企业成员信息
	 * @param memberId
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "getInfo")
	@ResponseBody
	public AjaxJson getMemberInfo(HttpServletRequest request){
		String memberId = request.getParameter("memberId");
		AjaxJson ajax = new AjaxJson();	
		if(memberId==null||"".equals(request.getParameter("memberId"))){
			ajax.setSuccess(false);
			ajax.setMessage("传入参数有误！");
			ajax.setErrorCode("-1");
			return ajax;
		}
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_member where memberId="+memberId;
		List<Object> list = sqlhe.query(sql);		
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();	
		if(list!=null && list.size()>0) {
			body.put("member", list.get(0));
		}		
		ajax.setBody(body);
		return ajax;
	}
	/**
	 * 2、根据主键memberId获取企业成员信息
	 * @param memberIds
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "getInfoList")
	@ResponseBody
	public AjaxJson getMemberInfoList(HttpServletRequest request){
		String memberIds = request.getParameter("memberIds");
		String memberIdArray[] = memberIds.split(",");
		AjaxJson ajax = new AjaxJson();	
		SQLHelper sqlhe = new SQLHelper();
		List<Object> list = new ArrayList<Object>();
		for(String memberId : memberIdArray){
			String sql = "select * from tf_member where memberId="+memberId;
			List<Object> li = sqlhe.query(sql);
			if(list!=null && list.size()>0) {
				list.add(li.get(0));
			}else {
				list.add(null);
			}
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();		
		body.put("memberList", list);
		ajax.setBody(body);
		return ajax;
	}
	
}
