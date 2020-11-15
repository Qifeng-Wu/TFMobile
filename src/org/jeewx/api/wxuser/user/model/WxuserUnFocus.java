package org.jeewx.api.wxuser.user.model;

import java.util.List;

/**   
 * @Title: Entity
 * @Description: 微信服务号未关注用户的信息
 * @author onlineGenerator
 * @date 2015-01-25 19:28:24
 * @version V1.0   
 *
 *
 *	{
 *		"openid":"osSvowWSArBB8ZWYn5G-4w_d7K40",
 *		"nickname":"YeJR",
 *		"sex":1,
 *		"language":"zh_CN",
 *		"city":"Suzhou",
 *		"province":"Jiangsu",
 *		"country":"CN",
 *		"headimgurl":"http:\/\/wx.qlogo.cn\/mmopen\/BMokdrjYnER8QibJhCia0LDAaDQmHI7FzrFcibldBCB9pSypzCaIdBOIFwRxcNzstibAohgEqht8Y9XPHR3rpFyJHsuXM7VoRtKic\/0",
 *		"privilege":["chinaunicom"]
 *	}
 */
public class WxuserUnFocus {
	
	/**用户的标识*/
	private java.lang.String openid;
	/**用户的昵称*/
	private java.lang.String nickname;
	/**性别*/
	private java.lang.String sex;
	/**用户的语言zh_CN*/
	private java.lang.String language;
	/**用户所在城市*/
	private java.lang.String city;
	/**用户所在国家*/
	private java.lang.String country;
	/**用户所在省份*/
	private java.lang.String province;
	/**用户头像*/
	private java.lang.String headimgurl;
	/**公众号*/
	private java.lang.String unionid;
	/**权限*/
	private List<String> privilege;
	
	
	
	public java.lang.String getLanguage() {
		return language;
	}
	public void setLanguage(java.lang.String language) {
		this.language = language;
	}
	public List<String> getPrivilege() {
		return privilege;
	}
	public void setPrivilege(List<String> privilege) {
		this.privilege = privilege;
	}
	public java.lang.String getOpenid() {
		return openid;
	}
	public void setOpenid(java.lang.String openid) {
		this.openid = openid;
	}
	public java.lang.String getNickname() {
		return nickname;
	}
	public void setNickname(java.lang.String nickname) {
		this.nickname = nickname;
	}
	public java.lang.String getSex() {
		return sex;
	}
	public void setSex(java.lang.String sex) {
		this.sex = sex;
	}
	public java.lang.String getCity() {
		return city;
	}
	public void setCity(java.lang.String city) {
		this.city = city;
	}
	public java.lang.String getCountry() {
		return country;
	}
	public void setCountry(java.lang.String country) {
		this.country = country;
	}
	public java.lang.String getProvince() {
		return province;
	}
	public void setProvince(java.lang.String province) {
		this.province = province;
	}
	public java.lang.String getHeadimgurl() {
		return headimgurl;
	}
	public void setHeadimgurl(java.lang.String headimgurl) {
		this.headimgurl = headimgurl;
	}
	
	public java.lang.String getUnionid() {
		return unionid;
	}
	public void setUnionid(java.lang.String unionid) {
		this.unionid = unionid;
	}
	
}
