package com.modules.utils;

import net.sf.json.JSONObject;
import org.jeewx.api.core.common.WxstoreUtils;
import org.jeewx.api.core.exception.WexinReqException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 微信--公共信息API
 * @author stephen
 * @version 2018-4-12
 * 
 */
public class WxCommonAPI {
	private static Logger logger = LoggerFactory.getLogger(WxCommonAPI.class);
	//获取企业应用access_token
	private static String get_access_token_url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=ID&corpsecret=SECRECT";
	//发送消息
	private static String send_message_url = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=ACCESS_TOKEN";  
	//根据code获取成员信息
	private static String get_user_info_url = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?access_token=ACCESS_TOKEN&code=CODE";  
	//使用userid获取成员详情
	private static String get_user_detail_url = "https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=ACCESS_TOKEN&userid=USERID";  

    /**
	 * 1、获取企业应用access_token
	 * @param corpid
	 * @param corpsecret
	 * @return kY9Y9rfdcr8AEtYZ9gPaRUjIAuJBvXO5ZOnbv2PYFxox__uSUQcqOnaGYN1xc4N1rI7NDCaPm_0ysFYjRVnPwCJHE7v7uF_l1hI6qi6QBsA
	 * @throws WexinReqException
	 */
	public static String getAccessToken(String corpid,String corpsecret) throws WexinReqException{
		String access_token = "";
		String requestUrl = get_access_token_url.replace("ID", corpid).replace("SECRECT",corpsecret);
		JSONObject result = WxstoreUtils.httpRequest(requestUrl, "GET", null);
		if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
			access_token = result.getString("access_token");
		} else {
			logger.error("获取企业应用access_token！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
			throw new WexinReqException("获取企业应用access_token！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));

		}
		return access_token;
	}
	/**
	 * 2、发送消息
	 * @param access_token
	 * @param entity
	 * @throws WexinReqException
	 */
	public static JSONObject sendMssage(String access_token,Object ob) throws WexinReqException{
		String requestUrl = send_message_url.replace("ACCESS_TOKEN", access_token);
		JSONObject obj = JSONObject.fromObject(ob);
		JSONObject result = WxstoreUtils.httpRequest(requestUrl, "POST", obj.toString());		
		if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
			
		} else {
			logger.error("发送消息！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
			throw new WexinReqException("发送消息！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
		}
		return result;
	}
	/**
	 * 3、根据code获取成员信息
	 * @param access_token
	 * @param code
	 * @throws WexinReqException
	 */
	public static JSONObject getUserInfo(String access_token,String code) throws WexinReqException{
		String requestUrl = get_user_info_url.replace("ACCESS_TOKEN", access_token).replace("CODE",code);
		JSONObject result = WxstoreUtils.httpRequest(requestUrl, "GET", null);		
		if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
			
		} else {
			logger.error("获取成员信息！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
			throw new WexinReqException("获取成员信息！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
		}
		return result;
	}
	/**
	 * 4、使用userId获取成员详情
	 * @param access_token
	 * @param userId
	 * @throws WexinReqException
	 */
	public static JSONObject getUserDetail(String access_token,String userId) throws WexinReqException{
		String requestUrl = get_user_detail_url.replace("ACCESS_TOKEN", access_token).replace("USERID", userId);
		JSONObject result = WxstoreUtils.httpRequest(requestUrl, "GET",null);		
		if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
			
		} else {
			logger.error("获取成员详情！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
			throw new WexinReqException("获取成员详情！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
		}
		return result;
	}

	public static void main(String[] args) throws WexinReqException {
		
	}
}
