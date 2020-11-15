//1.获取url中的参数
function getUrlParam(name) {
	var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
	var r = window.location.search.substr(1).match(reg);
	if (r != null) {
		return unescape(r[2]);
	} else {
		return null;
	}
} 

//2.js解析时间（格式：YYYY-MM-DD）
function findDate(tm){
	if(tm != ""){
		var date = new Date(tm);
		Y = date.getFullYear() + '-';
		M =((date.getMonth()+1) < 10 ? '0'+(date.getMonth()+1) : (date.getMonth()+1)) + '-';
		D = (date.getDate() < 10 ? ('0'+date.getDate()) : date.getDate());
		var ndate = Y+M+D
		return ndate;
	}
	return "";
}

//3.js解析时间（格式：YYYY-MM-DD HH-MM）
function getDate(tm){
	if(tm != ""){
		var date = new Date(tm);
		Y = date.getFullYear() + '-';
		M =((date.getMonth()+1) < 10 ? '0'+(date.getMonth()+1) : (date.getMonth()+1)) + '-';
		D = (date.getDate() < 10 ? ('0'+date.getDate()) : date.getDate()) + ' ';
		h = (date.getHours() < 10 ? ('0'+date.getHours()) : date.getHours())+ ':';
		m = (date.getMinutes() < 10 ? ('0'+date.getMinutes()) : date.getMinutes());
		s =date.getSeconds() < 10 ? ('0'+date.getSeconds()) : date.getSeconds();
		var ndate = Y+M+D+h+m
		return ndate;
	}
	return "";
}

//4.js解析时间（格式：YYYY.MM.DD）
function parseDate(tm){
	if(tm != ""){
		var date = new Date(tm);
		Y = date.getFullYear() + '.';
		M =((date.getMonth()+1) < 10 ? '0'+(date.getMonth()+1) : (date.getMonth()+1)) + '.';
		D = (date.getDate() < 10 ? ('0'+date.getDate()) : date.getDate()) + ' ';
		var ndate = Y+M+D
		return ndate;
	}
	return "";
}

//5.网页授权获取userId
function getMemberInfo(type){
	var CorpID = "wwaae8a082900de6d1";
	if(type==1){
		var agentid = "1000002";//异常通报
	}else if(type==5){
		var agentid = "1000005";//预警通知
	}else if(type==6){
		var agentid = "1000006";//SFC跳站
	}else if(type==8){
		var agentid = "1000008";//QIS质量信息系统
	}else if(type==9){
		var agentid = "1000009";//物料异常通报
	}else if(type==10){
		var agentid = "1000010";//新版异常通报
	}else if(type==11){
		var agentid = "1000011";//加班申请单
	}
	
	//获取缓存(用本地存取，经测试，若切换微信账号，微信会自动清理本地缓存，包括缓存等)
	var memberId = sessionStorage.getItem("memberId-"+CorpID+agentid);
 	var userId = sessionStorage.getItem("userId-"+CorpID+agentid);
	var name = sessionStorage.getItem("name-"+CorpID+agentid);
	var department = sessionStorage.getItem("department-"+CorpID+agentid);
	var position = sessionStorage.getItem("position-"+CorpID+agentid);
	var isleader = sessionStorage.getItem("isleader-"+CorpID+agentid);
	var mobile = sessionStorage.getItem("mobile-"+CorpID+agentid);
	var gender = sessionStorage.getItem("gender-"+CorpID+agentid);
	var avatar = sessionStorage.getItem("avatar-"+CorpID+agentid);
	var email = sessionStorage.getItem("email-"+CorpID+agentid);
	var qr_code = sessionStorage.getItem("qr_code-"+CorpID+agentid);
	var status = sessionStorage.getItem("status-"+CorpID+agentid);
		
	/*var memberId = localStorage .getItem("memberId-"+CorpID+agentid);
	var userId = localStorage .getItem("userId-"+CorpID+agentid);
	var name = localStorage .getItem("name-"+CorpID+agentid);
	var department = localStorage .getItem("department-"+CorpID+agentid);
	var position = localStorage .getItem("position-"+CorpID+agentid);
	var isleader = localStorage .getItem("isleader-"+CorpID+agentid);
	var mobile = localStorage .getItem("mobile-"+CorpID+agentid);
	var gender = localStorage .getItem("gender-"+CorpID+agentid);
	var avatar = localStorage .getItem("avatar-"+CorpID+agentid);
	var email = localStorage .getItem("email-"+CorpID+agentid);
	var qr_code = localStorage .getItem("qr_code-"+CorpID+agentid);
	var status = localStorage .getItem("status-"+CorpID+agentid);*/
	var access_code = getUrlParam('code');
	
		if (userId == null || userId == "") {
			//----------------------------获取code--------------------------------------
			if(access_code == null || access_code == "" || access_code =="null"){
				var fromurl = location.href;			
				var url = 'https://open.weixin.qq.com/connect/oauth2/authorize?appid='+CorpID+'&redirect_uri=' + encodeURIComponent(fromurl)
						+ '&response_type=code&scope=snsapi_userinfo&agentid='+agentid+'&state=STATE#wechat_redirect';
				location.href = url;
			} else {
				mui.ajax('/TFMobile/tf-api/mobile/oauth2/authorize', {
					data : {
						code : access_code,
						type : type
					},
					async : false,
					cache : false,
					dataType : 'json',//服务器返回json格式数据
					type : 'post',//HTTP请求类型
					success : function(result) {
						if (result.success) {
							 //将企业成员信息放在缓存中
							sessionStorage.setItem("memberId-"+CorpID+agentid, result.body.memberId);
							sessionStorage.setItem("userId-"+CorpID+agentid, result.body.userId);							
							sessionStorage.setItem("name-"+CorpID+agentid, result.body.name);
							sessionStorage.setItem("department-"+CorpID+agentid, result.body.department);
							sessionStorage.setItem("position-"+CorpID+agentid, result.body.position);
							sessionStorage.setItem("isleader-"+CorpID+agentid, result.body.isleader);
							sessionStorage.setItem("mobile-"+CorpID+agentid, result.body.mobile);
							sessionStorage.setItem("gender-"+CorpID+agentid, result.body.gender);
							sessionStorage.setItem("email-"+CorpID+agentid, result.body.email);
							sessionStorage.setItem("avatar-"+CorpID+agentid, result.body.avatar);
							sessionStorage.setItem("qr_code-"+CorpID+agentid, result.body.qr_code);
							sessionStorage.setItem("status-"+CorpID+agentid, result.body.status);
							
							/*localStorage .setItem("memberId-"+CorpID+agentid, result.body.memberId);
							localStorage .setItem("userId-"+CorpID+agentid, result.body.userId);							
							localStorage .setItem("name-"+CorpID+agentid, result.body.name);
							localStorage .setItem("department-"+CorpID+agentid, result.body.department);
							localStorage .setItem("position-"+CorpID+agentid, result.body.position);
							localStorage .setItem("isleader-"+CorpID+agentid, result.body.isleader);
							localStorage .setItem("mobile-"+CorpID+agentid, result.body.mobile);
							localStorage .setItem("gender-"+CorpID+agentid, result.body.gender);
							localStorage .setItem("email-"+CorpID+agentid, result.body.email);
							localStorage .setItem("avatar-"+CorpID+agentid, result.body.avatar);
							localStorage .setItem("qr_code-"+CorpID+agentid, result.body.qr_code);
							localStorage .setItem("status-"+CorpID+agentid, result.body.status);*/
							
							memberId = result.body.memberId;
							userId = result.body.userId;
							name = result.body.name;
							department = result.body.department;
							position = result.body.position;
							isleader = result.body.isleader;
							mobile = result.body.mobile;
							gender = result.body.gender;
							email = result.body.email;
							avatar = result.body.avatar;
							qr_code = result.body.qr_code;
							status = result.body.status;
				
						} else {
							mui.toast(result.message);
						}
					},
					error : function(xhr, type, errorThrown) {
						mui.toast("请退出重新进入！");
					}
				});
			}
		}
	return [memberId, userId, name, department, position, isleader, mobile, gender, email, avatar, qr_code, status];
}

//6.通过主键获取企业成员信息
function getInfo(memberId){
	var userId = null;
	var name = null;
	var department = null;
	var position = null;
	var isleader = null;
	var avatar = null;
	if(memberId==null || memberId=="" || memberId=="null"){
		return [userId, name, department, position, isleader, avatar];
	}
	mui.ajax('/TFMobile/tf-api/member/getInfo', {
		data : {
			memberId : memberId			
		},
		dataType : 'json',//服务器返回json格式数据
		type : 'post',//HTTP请求类型
		async : false,
		success : function(result) {
			if(result.success == true){
				userId = result.body.member.userId;
				name = result.body.member.name;
				department = result.body.member.department;
				position = result.body.member.position;
				isleader = result.body.member.isleader;
				avatar = result.body.member.avatar;
			}else{
				mui.toast(result.message);
			}				
		},
		error : function(xhr, type, errorThrown) {
			mui.toast("系统出错啦，请联系管理员");
		}
	});
	return [userId, name, department, position, isleader, avatar];
}

//7.另一个网页授权获取userId
function getAnotherMemberInfo(type){
	var CorpID = "ww44ec0bf79d7538e0";
	if(type==2){
		var agentid = "1000002";//改善提案
	}
	
	//获取缓存(用本地存取，经测试，若切换微信账号，微信会自动清理本地缓存，包括缓存等)
	var memberId = sessionStorage.getItem("memberId-"+CorpID+agentid);
 	var userId = sessionStorage.getItem("userId-"+CorpID+agentid);
	var name = sessionStorage.getItem("name-"+CorpID+agentid);
	var department = sessionStorage.getItem("department-"+CorpID+agentid);
	var position = sessionStorage.getItem("position-"+CorpID+agentid);
	var isleader = sessionStorage.getItem("isleader-"+CorpID+agentid);
	var mobile = sessionStorage.getItem("mobile-"+CorpID+agentid);
	var gender = sessionStorage.getItem("gender-"+CorpID+agentid);
	var avatar = sessionStorage.getItem("avatar-"+CorpID+agentid);
	var email = sessionStorage.getItem("email-"+CorpID+agentid);
	var qr_code = sessionStorage.getItem("qr_code-"+CorpID+agentid);
	var status = sessionStorage.getItem("status-"+CorpID+agentid);
	var access_code = getUrlParam('code');
	
		if (userId == null || userId == "") {
			//----------------------------获取code--------------------------------------
			if(access_code == null || access_code == "" || access_code =="null"){
				var fromurl = location.href;			
				var url = 'https://open.weixin.qq.com/connect/oauth2/authorize?appid='+CorpID+'&redirect_uri=' + encodeURIComponent(fromurl)
						+ '&response_type=code&scope=snsapi_userinfo&agentid='+agentid+'&state=STATE#wechat_redirect';
				location.href = url;
			} else {
				mui.ajax('/TFMobile/tf-api/mobile/oauth2/another_authorize', {
					data : {
						code : access_code,
						type : type
					},
					async : false,
					cache : false,
					dataType : 'json',//服务器返回json格式数据
					type : 'post',//HTTP请求类型
					success : function(result) {
						if (result.success) {
							 //将企业成员信息放在缓存中
							sessionStorage.setItem("memberId-"+CorpID+agentid, result.body.memberId);
							sessionStorage.setItem("userId-"+CorpID+agentid, result.body.userId);							
							sessionStorage.setItem("name-"+CorpID+agentid, result.body.name);
							sessionStorage.setItem("department-"+CorpID+agentid, result.body.department);
							sessionStorage.setItem("position-"+CorpID+agentid, result.body.position);
							sessionStorage.setItem("isleader-"+CorpID+agentid, result.body.isleader);
							sessionStorage.setItem("mobile-"+CorpID+agentid, result.body.mobile);
							sessionStorage.setItem("gender-"+CorpID+agentid, result.body.gender);
							sessionStorage.setItem("email-"+CorpID+agentid, result.body.email);
							sessionStorage.setItem("avatar-"+CorpID+agentid, result.body.avatar);
							sessionStorage.setItem("qr_code-"+CorpID+agentid, result.body.qr_code);
							sessionStorage.setItem("status-"+CorpID+agentid, result.body.status);
							
							memberId = result.body.memberId;
							userId = result.body.userId;
							name = result.body.name;
							department = result.body.department;
							position = result.body.position;
							isleader = result.body.isleader;
							mobile = result.body.mobile;
							gender = result.body.gender;
							email = result.body.email;
							avatar = result.body.avatar;
							qr_code = result.body.qr_code;
							status = result.body.status;
				
						} else {
							mui.toast(result.message);
						}
					},
					error : function(xhr, type, errorThrown) {
						mui.toast("请退出重新进入！");
					}
				});
			}
		}
	return [memberId, userId, name, department, position, isleader, mobile, gender, email, avatar, qr_code, status];
}