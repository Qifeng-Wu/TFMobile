package org.jeewx.api.wxuser;

import org.jeewx.api.core.exception.WexinReqException;
import org.jeewx.api.wxbase.wxtoken.JwTokenAPI;
import org.jeewx.api.wxuser.group.JwGroupAPI;
import org.jeewx.api.wxuser.user.JwUserAPI;
import org.jeewx.api.wxuser.user.model.Wxuser;
import org.jeewx.api.wxuser.user.model.WxuserUnFocus;

public class Test {

	public static void main(String[] args) {
		try {
			//osSvowWSArBB8ZWYn5G-4w_d7K40
			
//			String s = JwTokenAPI.getAccessToken("wxeb530a06275b6724","d212ae042ba720b9f0d12913dce2b44b");
			//String s="fMt6xoYZYtC4JfL9jKuzWI21ZgMAqZz_jw2f1BSTXBgHZo0RvBbYkHwqc3hFkZDA8uuZ_RRvVGM1QhFwSThNwG_qL2M7gjToaBhpfLiDEVjmTyLVBcLBpeTOBeQTT5BWWYVjAAAFHE";
//			System.out.println("AccessToken="+s);
			WxuserUnFocus wxuser = JwUserAPI.getWxuserUnFocus("PP8oZB_2bZVv6rou8UwY0OJd9v15WZ6lEDNZf74TBuZkGMthifCqURlRBQMpbQoQCO2Ifap558Kzh9q1XbLcSYzMyBzuvZTbk5NhkuUVGBs", "osSvowWSArBB8ZWYn5G-4w_d7K40");
			System.out.println(wxuser.getNickname());
			//JwGroupAPI.createGroup(s, "清华大厦");
			
			
//			int aa=JwGroupAPI.getAllGroup(s).size();
//			for(int i=0;i<aa;i++){
//				System.out.print(JwGroupAPI.getAllGroup(s).get(i).getName()+",");
//			}
//			System.out.println();
//			
//			
//			String groupid=JwGroupAPI.getUserGroup(s, "oKi_PtxPE4ZQq7Mn4FD6MPuIH0CM");
//			System.out.println(groupid);
//			
//			JwGroupAPI.updateGroup(s,"5", "黑名单2");
//			
			
			
		} catch (WexinReqException e) {
			e.printStackTrace();
		}
	}
}
