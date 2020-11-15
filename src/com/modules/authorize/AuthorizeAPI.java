package com.modules.authorize;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.jeewx.api.core.exception.WexinReqException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.modules.db.C3P0ConnentionProvider;
import com.modules.db.SQLHelper;
import com.modules.utils.AjaxJson;
import com.modules.utils.ConfigurationFileHelper;
import com.modules.utils.WxCommonAPI;

import net.sf.json.JSONObject;

/**
 * 网页授权登录
 * @author stephen
 */
@RestController
@RequestMapping(value = "tf-api/mobile/oauth2")
public class AuthorizeAPI {
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String ExceptionSecret = ConfigurationFileHelper.getExceptionSecret();
	private static String WarningSecret = ConfigurationFileHelper.getWarningSecret();
	private static String JumpStationSecret = ConfigurationFileHelper.getJumpStationSecret();
	private static String SaleQualitySecret = ConfigurationFileHelper.getSaleQualitySecret();
	private static String IQCExceptionSecret = ConfigurationFileHelper.getIQCExceptionSecret();
	private static String NewExceptionSecret = ConfigurationFileHelper.getNewExceptionSecret();
	private static String OvertimeApplicationSecret = ConfigurationFileHelper.getOvertimeApplicationSecret();
   /**
	 * 1、OAuth2.0网页授权获取
	 * @param description
	 * @throws WexinReqException
	 */
	@RequestMapping(value = "authorize")
	@Transactional
	@ResponseBody
	public synchronized AjaxJson authorize(HttpServletRequest request) throws WexinReqException{
		//判断微信企业号应用类型
		String type = request.getParameter("type");
		String Secret = "";
		if("1".equals(type)) {//异常通报
			Secret = ExceptionSecret;
		}else if("5".equals(type)) {//预警通知
			Secret = WarningSecret;
		}else if("6".equals(type)) {//SFC跳站
			Secret = JumpStationSecret;
		}else if("8".equals(type)) {//QIS质量信息系统
			Secret = SaleQualitySecret;
		}else if("9".equals(type)) {//物料异常通报
			Secret = IQCExceptionSecret;
		}else if("10".equals(type)) {//新版异常通报
			Secret = NewExceptionSecret;
		}else if("11".equals(type)) {//加班申请单
			Secret = OvertimeApplicationSecret;
		}
		AjaxJson ajax = new AjaxJson();
		String userId = null;
		String memberId = null;
		boolean isSuccess = true;
		
		String code = request.getParameter("code");
		String access_token = WxCommonAPI.getAccessToken(CorpID,Secret);//获取access_token
		JSONObject userInfo;
		try {
			//根据code获取成员信息
			userInfo = WxCommonAPI.getUserInfo(access_token,code);
			userId = String.valueOf(userInfo.get("UserId"));
		} catch (WexinReqException e) {
			ajax.setSuccess(false);
			ajax.setErrorCode("-3");
			ajax.setMessage("未加入企业微信，请联系管理员！");
			return ajax;
		}

				
		if (userId != null) {
			// 查询下我们的表中是否存在此数据
			memberId = getMemberId(userId);
			JSONObject userDetail = WxCommonAPI.getUserDetail(access_token,userId);//使用user_ticket获取成员详情
			String name = userDetail.getString("name");
			String department = userDetail.getString("department");
			String position = userDetail.getString("position");
			String isleader = userDetail.getString("isleader");
			String mobile = userDetail.getString("mobile");
			String gender = userDetail.getString("gender");
			String email = userDetail.getString("email");
			String avatar = userDetail.getString("avatar");
			String qr_code = userDetail.getString("qr_code");
			String status = userDetail.getString("status");
					
			if (memberId != null && !"null".equals(memberId)) {
				// 若存在就更新下数据，防止出现更换微信头像昵称的情况出现
				isSuccess = updateMemberInfo(memberId,userId,name,department,position,isleader,mobile,gender,email,avatar,qr_code,status);				
			} else {// 若不存在此数据
				// 将这些数据插入到数据库，并获取memberId
				memberId = addMemberInfo(userId,name,department,position,isleader,mobile,gender,email,avatar,qr_code,status);
			}
			
			// 两个都能获取到才能算成功
			if (memberId != null && userId != null && isSuccess) {
				LinkedHashMap<String, Object> body = new LinkedHashMap<String, Object>();
				body.put("memberId", memberId);
				body.put("userId", userId);
				body.put("name", name);
				body.put("department", department);
				body.put("position", position);
				body.put("isleader", isleader);
				body.put("mobile", mobile);
				body.put("gender", gender);
				body.put("email", email);
				body.put("avatar", avatar);
				body.put("qr_code", qr_code);
				body.put("status", status);
				ajax.setBody(body);
			} else {
				ajax.setSuccess(false);
				ajax.setErrorCode("-1");
				ajax.setMessage("获取企业成员信息失败！");
			}
	
		}else {
			ajax.setSuccess(false);
			ajax.setErrorCode("-2");
			ajax.setMessage("微信获取企业成员信息失败！");
		}
		
		return ajax;
	}
	
	/**
	 * 从数据库获取对应userId的memberId
	 * @param userId
	 * @return memberId
	 */
	@SuppressWarnings("unchecked")
	public String getMemberId(String userId) {
		SQLHelper sqlhe = new SQLHelper();
		String sql = "select * from tf_member where userId = '" + userId + "'";
		List<Object> result = sqlhe.query(sql);
		if (!result.isEmpty() && result != null) {
			Map<String, Object> map = (Map<String, Object>) result.get(0);
			String memberId = String.valueOf(map.get("memberId"));
			return memberId;
		}
		return null;
	}
	
	/**
	 * 更新企业成员信息
	 * @param memberId，deviceId，name，mobile，gender，email，avatar
	 * @return Boolean
	 */
	public boolean updateMemberInfo(String memberId,String userId,String name,String department,String position,String isleader,
			String mobile,String gender,String email,String avatar,String qr_code,String status) {
		SQLHelper sqlhe = new SQLHelper();
		List<String> sqlist = new ArrayList<>();
		
		String upfanSql = "UPDATE tf_member SET userId ='" + userId + "', name= '" + name + "', department='" + department + "', position='" + 
			   position + "', isleader='" + isleader+ "', mobile='" + mobile+ "', gender='" + gender+ "', email='" + email + "', avatar='" + 
			   avatar + "', qr_code='" + qr_code+ "', status='" + status+ "',updateTime = now() where memberId = " + memberId;
		
		sqlist.add(upfanSql);

		boolean isSuccess = sqlhe.update(sqlist);
		return isSuccess;
	}
	
	/**
	 * 新增成员信息，并返回memberId(主键)
	 * @param userId,deviceId,name,mobile,gender,email,avatar
	 * @return memberId
	 */
	public String addMemberInfo(String userId,String name,String department,String position,String isleader,String mobile,
			String gender,String email,String avatar,String qr_code,String status) {
		String sql = "INSERT INTO tf_member (userId,name,department,position,isleader,mobile,gender,email,avatar,qr_code,status,addTime,updateTime) VALUES "+
				"('"+ userId +"','"+ name +"','"+ department +"','"+ position +"','"+ isleader +"','"+ mobile+ "','"+ gender+
				"','"+ email +"','"+ avatar +"','"+ qr_code +"','"+ status +"',now(),now())";
		String memberId = String.valueOf(getId(sql));
		return memberId;
	}

	/**
	 * 插入并获取刚插入的数据的自增主键Id
	 * 
	 * @param insertSql
	 * @return
	 */
	@SuppressWarnings("resource")
	public Object getId(String insertSql) {
		Object id = 0;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = C3P0ConnentionProvider.getConnection();
			ps = conn.prepareStatement(insertSql);
			int j = ps.executeUpdate();
			if (j == 1) {
				ps = conn.prepareStatement("SELECT @@IDENTITY");
				rs = ps.executeQuery();
				ResultSetMetaData rsmd = rs.getMetaData();
				// 可以得到有多少列
				int columnNum = rsmd.getColumnCount();
				// 将数据封装到list中
				while (rs.next()) {
					for (int i = 0; i < columnNum; i++) {
						id = rs.getObject(i + 1);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
					rs = null;
				}
				if (ps != null) {
					ps.close();
					ps = null;
				}
				if (conn != null) {
					conn.close();
					conn = null;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return id;
	}
	
}
