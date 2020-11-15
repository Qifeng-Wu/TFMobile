package org.jeewx.api.core.req.model;

import org.jeewx.api.core.annotation.ReqType;


@ReqType("access_token_code")
public class AccessTokenCode extends WeixinReqParam{

//	'https://api.weixin.qq.com/sns/oauth2/access_token?appid='.$appid.'&secret='.$secret.'&code='.$code.'&grant_type=authorization_code';
 
	/**
	 * 获取access_token填写client_credential
	 */
	private String grant_type="authorization_code";
	
	/**
	 * 第三方用户唯一凭证
	 */
	private String appid;
	
	/**
	 * 第三方用户唯一凭证密钥，即appsecret
	 */
	private String secret;
	
	private String code ;
	
	

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getGrant_type() {
		return grant_type;
	}

	public void setGrant_type(String grant_type) {
		this.grant_type = grant_type;
	}

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
	 
}
