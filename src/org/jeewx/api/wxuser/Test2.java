package org.jeewx.api.wxuser;

import org.jeewx.api.core.exception.WexinReqException;

public class Test2 {

	public static void main(String[] args) throws WexinReqException {
		
//		String accessToken="x2zvXX6EBou4S86lgTe0AJqM7V7JIz3OQmbjIrulw1yWbapP5Z6gttLwQrf-9nZyyOcEaKmVsDMADOz91r13SibrLJQ2QHJD148NHRAY4o8a1p0DJ4MOmihFFBgDWo2uXUXeAIAOLO";
//		String template_id_short="TM00050";
//		
//		String s  = JwTemplateMessageAPI.addTemplate(accessToken, template_id_short);
//		
//		System.out.println(s);{"errmsg":"template num exceeds limit hint: [586bBA0070vr18]","errcode":45026}
		
		String mfansid="1";
		String unicalid="1";
		
		String orderNo="OrderNo";
		String deliveryEndTime="DeliveryEndTime";
		
		String realPrice="RealPrice";
		String actualDeliveryPrice="ActualDeliveryPrice";
		String title="title";
		
		String appid="wxeb63aa2a25fd449b";
		String app_secret="29a90f3382b52f21c5fbf2c4c1bfbb24";
		String template_id="DGZNiinbgkmr6o7vm4kFe2lw7iaDflvfN5TUo1lVylQ";
		String url="";
		String firstContent="您已确认收货\n";
		String keynoteContent1=title;
		String keynoteContent2=deliveryEndTime;
		String keynoteContent3="订单编号："+orderNo;
		String keynoteContent4=realPrice+"\n";
		String remarkContent="订单已完成！";
		//给配送员
		String fansid="17";
		String firstContent1="用户已确认收货\n";
		String keynoteContent41=actualDeliveryPrice+"\n";
		String remarkContent1="配送已完成！";
		/*try {
			WxTemplateMessageUtils.setTemplateMessage(appid, app_secret, template_id,mfansid,unicalid, url, firstContent, keynoteContent1, 
					keynoteContent2, keynoteContent3, keynoteContent4, remarkContent);
			WxTemplateMessageUtils.setTemplateMessage(appid, app_secret, template_id,fansid,unicalid, url, firstContent1, keynoteContent1, 
					keynoteContent2, keynoteContent3, keynoteContent41, remarkContent1);
			
		} catch (WexinReqException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
			
	}
	
}
