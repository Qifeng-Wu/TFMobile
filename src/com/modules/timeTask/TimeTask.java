package com.modules.timeTask;  
  
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.jeewx.api.core.exception.WexinReqException;
import org.springframework.scheduling.annotation.Scheduled;  
import org.springframework.stereotype.Component;

import com.modules.db.SQLHelper;
import com.modules.entity.message.Textcard;
import com.modules.entity.message.TextcardMessage;
import com.modules.utils.ConfigurationFileHelper;
import com.modules.utils.WxCommonAPI;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;  
  
@Component  
public class TimeTask {  
	private static String serviceUrl = ConfigurationFileHelper.getServiceUrl();
	private static String CorpID = ConfigurationFileHelper.getCorpID();
	private static String Secret = ConfigurationFileHelper.getExceptionSecret();
	private static String AgentId = ConfigurationFileHelper.getExceptionAgentId();
	/**
	 *每天下午4:30统计未完成的通报，推送到各部门经理及所有人。
	 * @throws WexinReqException 
	 * @throws ParseException 
	 */
    @SuppressWarnings({ "unchecked", "unused" })
	@Scheduled(cron = "0 30 16 * * ?")  
    public void sendUnfinishedException() throws WexinReqException, ParseException{  
    	SQLHelper sqlhe = new SQLHelper();		
		String sql1 = "select serialNumber from tf_exception where deleteFlag = 0 and (state = 0 or state = 1 or state = 5)";//待发文部门处理
		String sql2 = "select serialNumber from tf_exception where deleteFlag = 0 and (state = 2 or state = 3 or state = 8 or state = 10 or state = 11)";//待质量部处理
		String sql3 = "select serialNumber from tf_exception where deleteFlag = 0 and (state = 4 or state = 6)";//待工程部门处理
		String sql4 = "select serialNumber,needSignDepartment,signDepartment from tf_exception where deleteFlag = 0 and (state = 7 or state = 9)";//待会签部门处理
		List<Object> list1 = sqlhe.query(sql1);
		List<Object> list2 = sqlhe.query(sql2);
		List<Object> list3 = sqlhe.query(sql3);
		List<Object> list4 = sqlhe.query(sql4);
		String serialNumber1 = "";//待生产部门处理的异常序列号
		String serialNumber2 = "";//待工程部门处理的异常序列号
		String serialNumber3 = "";//待质量部门处理的异常序列号
		String serialNumber4 = "";//待研发部门处理的异常序列号
		String serialNumber5 = "";//待工程技术部门处理的异常序列号
		String serialNumber6 = "";//待运营部门处理的异常序列号
		String serialNumber7 = "";//待产品部门处理的异常序列号
		
		//1.待发文部门处理
		if(list1!=null && list1.size()>0) {
			for(int i = 0;i<list1.size();i++) {
				String serialNumber = String.valueOf(((Map<String, Object>) list1.get(i)).get("serialNumber"));
				if(serialNumber.startsWith("PD")) {
					serialNumber1 += serialNumber +",";
				}else if(serialNumber.startsWith("QC")) {
					serialNumber3 += serialNumber +",";
				}else if(serialNumber.startsWith("ME")) {
					serialNumber2 += serialNumber +",";
				}
			}
		}
		
		//2.待质量控制部或品质保障部处理
		if(list2!=null && list2.size()>0) {
			for(int i = 0;i<list2.size();i++) {
				String serialNumber = String.valueOf(((Map<String, Object>) list2.get(i)).get("serialNumber"));	
				serialNumber3 += serialNumber +",";
			}
		}
		
		//3.待工程部门处理
		if(list3!=null && list3.size()>0) {
			for(int i = 0;i<list3.size();i++) {
				String serialNumber = String.valueOf(((Map<String, Object>) list3.get(i)).get("serialNumber"));
				serialNumber2 += serialNumber +",";
			}
		}
		
		//4.待会签部门处理
		if(list4!=null && list4.size()>0) {
			for(int i = 0;i<list4.size();i++) {
				String serialNumber = String.valueOf(((Map<String, Object>) list4.get(i)).get("serialNumber"));
				String needSignDepartment = String.valueOf(((Map<String, Object>) list4.get(i)).get("needSignDepartment"));//需要会签部门
				String signDepartment = String.valueOf(((Map<String, Object>) list4.get(i)).get("signDepartment"));//已会签部门
				if(needSignDepartment!=null&&!"".equals(needSignDepartment)&&!"null".equals(needSignDepartment)&&!"无".equals(needSignDepartment)) {
					String needSignDepartmentArray[] = needSignDepartment.split(",");				
					if(signDepartment!=null&&!"".equals(signDepartment)&&!"null".equals(signDepartment)&&!"无".equals(signDepartment)) {
						String signDepartmentArray[] = signDepartment.split(",");
						for (int j=0;j<needSignDepartmentArray.length;j++) {
							for (int k=0;k<signDepartmentArray.length;k++) {
								if (needSignDepartmentArray[j].equals(signDepartmentArray[k])) {
									break;
								}
								if (k == signDepartmentArray.length - 1) {
									if("生产".equals(needSignDepartmentArray[j])) {
										serialNumber1 += serialNumber +",";
									}else if("工程".equals(needSignDepartmentArray[j])) {
										serialNumber2 += serialNumber +",";
									}else if("质量".equals(needSignDepartmentArray[j])) {
										serialNumber3 += serialNumber +",";
									}else if("工业技术".equals(needSignDepartmentArray[j])) {
										serialNumber5 += serialNumber +",";
									}else if("研发".equals(needSignDepartmentArray[j])) {
										serialNumber4 += serialNumber +",";
									}else if("运营".equals(needSignDepartmentArray[j])) {
										serialNumber6 += serialNumber +",";
									}else if("产品".equals(needSignDepartmentArray[j])) {
										serialNumber7 += serialNumber +",";
									}
								}

							}
						}
					}else {
						for (int j=0;j<needSignDepartmentArray.length;j++) {
							if("生产".equals(needSignDepartmentArray[j])) {
								serialNumber1 += serialNumber +",";
							}else if("工程".equals(needSignDepartmentArray[j])) {
								serialNumber2 += serialNumber +",";
							}else if("质量".equals(needSignDepartmentArray[j])) {
								serialNumber3 += serialNumber +",";
							}else if("工业技术".equals(needSignDepartmentArray[j])) {
								serialNumber5 += serialNumber +",";
							}else if("研发".equals(needSignDepartmentArray[j])) {
								serialNumber4 += serialNumber +",";
							}else if("运营".equals(needSignDepartmentArray[j])) {
								serialNumber6 += serialNumber +",";
							}else if("产品".equals(needSignDepartmentArray[j])) {
								serialNumber7 += serialNumber +",";
							}
						}
					}					
				}
			}
		}
				
		
		//推送消息给各个部门经理
		//获取各部门经理userId
		String touser1 = getManagerUserIdByDepartment("[4]");//生产部
		String touser2 = getManagerUserIdByDepartment("[3]");//工程部
		String touser3 = getManagerUserIdByDepartment("[5]");//质量部
		String touser4 = getManagerUserIdByDepartment("[7]");//研发部
		String touser5 = getManagerUserIdByDepartment("[2]");//工程技术部
		String touser6 = getManagerUserIdByDepartment("[8]");//运营部
		String touser7 = getManagerUserIdByDepartment("[9]");//产品部
		String description1 = "";
		String description2 = "";
		String description3 = "";
		String description4 = "";
		String description5 = "";
		String description6 = "";
		String description7 = "";
		int exceptionNumber1 = 0;//生产部
		int exceptionNumber2 = 0;//工程部
		int exceptionNumber3 = 0;//质量控部
		int exceptionNumber4 = 0;//研发部
		int exceptionNumber5 = 0;//工程技术部
		if(serialNumber1!=null&&!"".equals(serialNumber1)) {
			description1 = "待处理异常通报文件序号："+serialNumber1+"\n请及时处理，点击查看！";
			exceptionNumber1 = serialNumber1.split(",").length;
		}else {
			description1 = "部门目前没有异常通报待处理！";
		}
		if(serialNumber2!=null&&!"".equals(serialNumber2)) {
			description2 = "待处理异常通报文件序号："+serialNumber2+"\n请及时处理，点击查看！";
			exceptionNumber2 = serialNumber2.split(",").length;
		}else {
			description2 = "部门目前没有异常通报待处理！";
		}
		if(serialNumber3!=null&&!"".equals(serialNumber3)) {
			description3 = "待处理异常通报文件序号："+serialNumber3+"\n请及时处理，点击查看！";
			exceptionNumber3 = serialNumber3.split(",").length;
		}else {
			description3 = "部门目前没有异常通报待处理！";
		}
		if(serialNumber4!=null&&!"".equals(serialNumber4)) {
			description4 = "待处理异常通报文件序号："+serialNumber4+"\n请及时处理，点击查看！";
			exceptionNumber4 = serialNumber4.split(",").length;
		}else {
			description4 = "部门目前没有异常通报待处理！";
		}
		if(serialNumber5!=null&&!"".equals(serialNumber5)) {
			description5 = "待处理异常通报文件序号："+serialNumber5+"\n请及时处理，点击查看！";
			exceptionNumber5 = serialNumber5.split(",").length;
		}else {
			description5 = "部门目前没有异常通报待处理！";
		}
		if(serialNumber6!=null&&!"".equals(serialNumber6)) {
			description6 = "待处理异常通报文件序号："+serialNumber6+"\n请及时处理，点击查看！";
		}else {
			description6 = "部门目前没有异常通报待处理！";
		}
		if(serialNumber7!=null&&!"".equals(serialNumber7)) {
			description7 = "待处理异常通报文件序号："+serialNumber7+"\n请及时处理，点击查看！";
		}else {
			description7 = "部门目前没有异常通报待处理！";
		}
		
		
		String access_token = WxCommonAPI.getAccessToken(CorpID,Secret);
		String title1 = "待处理异常通报提醒通知";
		if(touser1!=null && !"".equals(touser1)) {
			sendMessageToManager(access_token,title1,touser1,description1);
		}
		if(touser2!=null && !"".equals(touser2)) {
			sendMessageToManager(access_token,title1,touser2,description2);
		}
		if(touser3!=null && !"".equals(touser3)) {
			sendMessageToManager(access_token,title1,touser3,description3);
		}
		if(touser4!=null && !"".equals(touser4)) {
			sendMessageToManager(access_token,title1,touser4,description4);
		}
		if(touser5!=null && !"".equals(touser5)) {
			sendMessageToManager(access_token,title1,touser5,description5);
		}
		if(touser6!=null && !"".equals(touser6)) {
			sendMessageToManager(access_token,title1,touser6,description6);
		}
		if(touser7!=null && !"".equals(touser7)) {
			sendMessageToManager(access_token,title1,touser7,description7);
		}
		
		/*统计超过24小时未结案数量*/
		String overtime_24h_Number1 = "0";//生产部
		String overtime_24h_Number2 = "0";//工程部
		String overtime_24h_Number3 = "0";//质量部
		String overtime_24h_Number4 = "0";//研发部
		String overtime_24h_Number5 = "0";//工业技术部
		String overtimeSql1 = "select count(*) from tf_exception where deleteFlag = 0 and submitDepartment='生产部' and state <>12 and HOUR(timediff( now(),reporterTime))>24";//获取生产部超过24小时未结案数量
		String overtimeSql2 = "select count(*) from tf_exception where deleteFlag = 0 and submitDepartment='工程部' and state <>12 and HOUR(timediff( now(),reporterTime))>24";//获取工程部超过24小时未结案数量
		String overtimeSql3 = "select count(*) from tf_exception where deleteFlag = 0 and submitDepartment='质量部' and state <>12 and HOUR(timediff( now(),reporterTime))>24";//获取质量部超过24小时未结案数量
		List<Object> overtime_24h_list1 = sqlhe.query(overtimeSql1);
		List<Object> overtime_24h_list2 = sqlhe.query(overtimeSql2);
		List<Object> overtime_24h_list3 = sqlhe.query(overtimeSql3);
		Map<String, Object> map1 = (Map<String, Object>) overtime_24h_list1.get(0);
		Map<String, Object> map2 = (Map<String, Object>) overtime_24h_list2.get(0);
		Map<String, Object> map3 = (Map<String, Object>) overtime_24h_list3.get(0);
		overtime_24h_Number1 = String.valueOf(map1.get("count(*)"));
		overtime_24h_Number2 = String.valueOf(map2.get("count(*)"));
		overtime_24h_Number3 = String.valueOf(map3.get("count(*)"));
		
		
		
		//统计未结案的异常通报各部门间超时2小时
		int overtimeNumber1 = 0;//生产部
		int overtimeNumber2 = 0;//工程部
		int overtimeNumber3 = 0;//质量部
		int overtimeNumber4 = 0;//研发部
		int overtimeNumber5 = 0;//工程技术部
		String overtimeSql = "select * from tf_exception where state <>12 and deleteFlag = 0";//获取未完结的数据
		List<Object> overtimeList = sqlhe.query(overtimeSql);
		if(overtimeList!=null && overtimeList.size()>0) {
			for(int i=0;i<overtimeList.size();i++) {
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String serialNumber = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("serialNumber"));
				
				//1.发文审核
				String reporterTime = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("reporterTime"));
				String reportManagerTime = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("reportManagerTime"));
				if(reportManagerTime!=null && !reportManagerTime.isEmpty() && !"null".equals(reportManagerTime)) {
					long reporterTimeDate = sdf.parse(reporterTime).getTime()+(2*60*60*1000);//加两小时（处理时间为两小时内，超出则为超时）				
					if(serialNumber.startsWith("PD")) {//生产发文					
						if(sdf.parse(reportManagerTime).getTime()>reporterTimeDate) {
							overtimeNumber1 += 1;
						}
					}else if(serialNumber.startsWith("QC")) {//质控发文
						if(sdf.parse(reportManagerTime).getTime()>reporterTimeDate) {
							overtimeNumber3 += 1;
						}
					}else if(serialNumber.startsWith("ME")) {//工程发文
						if(sdf.parse(reportManagerTime).getTime()>reporterTimeDate) {
							overtimeNumber2 += 1;
						}		
					}
					
					//2.质量判定审核
					long reportManagerTimeDate = sdf.parse(reportManagerTime).getTime()+(2*60*60*1000);
					String determineManagerTime = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("determineManagerTime"));
					if(determineManagerTime!=null && !determineManagerTime.isEmpty() && !"null".equals(determineManagerTime)) {
						//质量判定审核（质量部门）
						if(sdf.parse(determineManagerTime).getTime()>reportManagerTimeDate) {
							overtimeNumber3 += 1;
						}
						
						//3.责任部门（工程部门）审核
						long determineManagerTimeDate = sdf.parse(determineManagerTime).getTime()+(2*60*60*1000);
						String responsibilityManagerTime = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("responsibilityManagerTime"));		
						if(responsibilityManagerTime!=null && !responsibilityManagerTime.isEmpty() && !"null".equals(responsibilityManagerTime)) {
							if(sdf.parse(responsibilityManagerTime).getTime()>determineManagerTimeDate) {
								overtimeNumber2 += 1;
							}
														
							//6.会签
							//6.1判断是否需要会签
							String needSignDepartment = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("needSignDepartment"));//需要会签部门
							if(needSignDepartment!=null&&!"".equals(needSignDepartment)&&!"null".equals(needSignDepartment)&&!"无".equals(needSignDepartment)) {
								
								//1.会签处理信息
								long responsibilityManagerTimeDate = sdf.parse(responsibilityManagerTime).getTime()+(2*60*60*1000);
								String signHandlerInfo = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("signHandlerInfo"));//会签处理信息
								if(signHandlerInfo!=null&&!"".equals(signHandlerInfo)&&!"null".equals(signHandlerInfo)) {
									String signManagerInfo = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("signManagerInfo"));//会签经理审核信息
									JSONArray signHandlerInfoJsonArray = JSONArray.fromObject(signHandlerInfo); 
									if(signHandlerInfoJsonArray!=null && signHandlerInfoJsonArray.size()>0) {
										for(int j=0;j<signHandlerInfoJsonArray.size();j++) {
											String department = String.valueOf(JSONArray.fromObject(signHandlerInfoJsonArray.get(j)).get(1));
											String signHandlerTime = String.valueOf(JSONArray.fromObject(signHandlerInfoJsonArray.get(j)).get(2));
											if("2".equals(department)) {//工业技术
												/*if(sdf.parse(signHandlerTime).getTime()>responsibilityManagerTimeDate) {
													overtimeNumber5 += 1;
												}*/
												
												//解析会签经理审核信息
												if(signManagerInfo!=null&&!"".equals(signManagerInfo)&&!"null".equals(signManagerInfo)) {
													JSONArray signManagerInfoJsonArray = JSONArray.fromObject(signManagerInfo); 
													if(signManagerInfoJsonArray!=null && signManagerInfoJsonArray.size()>0) {
														for(int k=0;k<signManagerInfoJsonArray.size();k++) {
															String dep= String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(1));
															String signManagerTime = String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(2));
															if("2".equals(dep)) {//工业技术
																if(sdf.parse(signManagerTime).getTime()>responsibilityManagerTimeDate) {
																	overtimeNumber5 += 1;
																}
															}
														}
													}
												}
											}else if("3".equals(department)) {//工程
												/*if(sdf.parse(signHandlerTime).getTime()>responsibilityManagerTimeDate) {
													overtimeNumber2 += 1;
												}*/
												
												//解析会签经理审核信息
												if(signManagerInfo!=null&&!"".equals(signManagerInfo)&&!"null".equals(signManagerInfo)) {
													JSONArray signManagerInfoJsonArray = JSONArray.fromObject(signManagerInfo); 
													if(signManagerInfoJsonArray!=null && signManagerInfoJsonArray.size()>0) {
														for(int k=0;k<signManagerInfoJsonArray.size();k++) {
															String dep= String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(1));
															String signManagerTime = String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(2));
															if("3".equals(dep)) {//工程
																if(sdf.parse(signManagerTime).getTime()>responsibilityManagerTimeDate) {
																	overtimeNumber2 += 1;
																}
															}
														}
													}
												}
											}else if("4".equals(department)) {//生产
												/*if(sdf.parse(signHandlerTime).getTime()>responsibilityManagerTimeDate) {
													overtimeNumber1 += 1;
												}*/
												
												//解析会签经理审核信息
												if(signManagerInfo!=null&&!"".equals(signManagerInfo)&&!"null".equals(signManagerInfo)) {
													JSONArray signManagerInfoJsonArray = JSONArray.fromObject(signManagerInfo); 
													if(signManagerInfoJsonArray!=null && signManagerInfoJsonArray.size()>0) {
														for(int k=0;k<signManagerInfoJsonArray.size();k++) {
															String dep= String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(1));
															String signManagerTime = String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(2));
															if("4".equals(dep)) {//生产
																if(sdf.parse(signManagerTime).getTime()>responsibilityManagerTimeDate) {
																	overtimeNumber1 += 1;
																}
															}
														}
													}
												}
												
											}else if("5".equals(department)) {//质量
												/*if(sdf.parse(signHandlerTime).getTime()>responsibilityManagerTimeDate) {
													overtimeNumber3 += 1;
												}*/
												
												//解析会签经理审核信息
												if(signManagerInfo!=null&&!"".equals(signManagerInfo)&&!"null".equals(signManagerInfo)) {
													JSONArray signManagerInfoJsonArray = JSONArray.fromObject(signManagerInfo); 
													if(signManagerInfoJsonArray!=null && signManagerInfoJsonArray.size()>0) {
														for(int k=0;k<signManagerInfoJsonArray.size();k++) {
															String dep= String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(1));
															String signManagerTime = String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(2));
															if("5".equals(dep)) {//质量
																if(sdf.parse(signManagerTime).getTime()>responsibilityManagerTimeDate) {
																	overtimeNumber3 += 1;
																}
															}
														}
													}
												}
											}else if("7".equals(department)) {//研发
												/*if(sdf.parse(signHandlerTime).getTime()>responsibilityManagerTimeDate) {
													overtimeNumber4 += 1;
												}*/
												
												//解析会签经理审核信息
												if(signManagerInfo!=null&&!"".equals(signManagerInfo)&&!"null".equals(signManagerInfo)) {
													JSONArray signManagerInfoJsonArray = JSONArray.fromObject(signManagerInfo); 
													if(signManagerInfoJsonArray!=null && signManagerInfoJsonArray.size()>0) {
														for(int k=0;k<signManagerInfoJsonArray.size();k++) {
															String dep= String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(1));
															String signManagerTime = String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(2));
															if("7".equals(dep)) {//研发
																if(sdf.parse(signManagerTime).getTime()>responsibilityManagerTimeDate) {
																	overtimeNumber4 += 1;
																}
															}
														}
													}
												}
											}

										}
									}	
								}
						}
						
						//7.质量验证审核
						//7.1.需要会签时
						if(needSignDepartment!=null&&!"".equals(needSignDepartment)&&!"null".equals(needSignDepartment)&&!"无".equals(needSignDepartment)) {
							//获取最后一个会签审核的时间
							long lastTime = 0 ;
							String signManagerInfo = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("signManagerInfo"));//会签经理审核信息
							if(signManagerInfo!=null&&!"".equals(signManagerInfo)&&!"null".equals(signManagerInfo)) {
								JSONArray signManagerInfoJsonArray = JSONArray.fromObject(signManagerInfo); 
								if(signManagerInfoJsonArray!=null && signManagerInfoJsonArray.size()>0) {
									lastTime = sdf.parse(String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(0)).get(2))).getTime();
									for(int k=1;k<signManagerInfoJsonArray.size();k++) {
										String signManagerTime = String.valueOf(JSONArray.fromObject(signManagerInfoJsonArray.get(k)).get(2));
										if(sdf.parse(signManagerTime).getTime()>lastTime) {
											lastTime = sdf.parse(signManagerTime).getTime();
										}
									}
								}
							}
							
							//质量验证审核
							long lastTimeDate = lastTime+(2*60*60*1000);
							String verifyHandlerTime = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("verifyHandlerTime"));
							String verifyManagerTime = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("verifyManagerTime"));
							if(verifyHandlerTime!=null&&!"".equals(verifyHandlerTime)&&!"null".equals(verifyHandlerTime)&&
									verifyManagerTime!=null&&!"".equals(verifyManagerTime)&&!"null".equals(verifyManagerTime)) {								
								//质量验证审核
								if(sdf.parse(verifyManagerTime).getTime()>lastTimeDate) {
									overtimeNumber3 += 1;
								}	
							}
						}else {//7.2.不需要会签时
							long responsibilityManagerTimeDate = sdf.parse(responsibilityManagerTime).getTime()+(2*60*60*1000);
							String verifyHandlerTime = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("verifyHandlerTime"));
							String verifyManagerTime = String.valueOf(((Map<String, Object>) overtimeList.get(i)).get("verifyManagerTime"));
							if(verifyHandlerTime!=null&&!"".equals(verifyHandlerTime)&&!"null".equals(verifyHandlerTime)&&
									verifyManagerTime!=null&&!"".equals(verifyManagerTime)&&!"null".equals(verifyManagerTime)) {								
								//质量验证审核
								if(sdf.parse(verifyManagerTime).getTime()>responsibilityManagerTimeDate) {
									overtimeNumber3 += 1;
								}	
							}

						}
													
					}	
				}
			}		
		}
	}
		
		//发送消息给送有人
		String title2 = "部门异常通报汇总提醒"; 
		String description = "部   门          待处理     2H超时     24H未结案"+ 
							 "\n生产部：             "+exceptionNumber1+"               "+overtimeNumber1+"               "+overtime_24h_Number1+
						     "\n工程部：             "+exceptionNumber2+"               "+overtimeNumber2+"               "+overtime_24h_Number2+
						     "\n质量部：             "+exceptionNumber3+"               "+overtimeNumber3+"               "+overtime_24h_Number3+
						     "\n研发部：             "+exceptionNumber4+"               "+overtimeNumber4+"               "+overtime_24h_Number4+
						     "\n工业技术部：     "+exceptionNumber5+"               "+overtimeNumber5+"               "+overtime_24h_Number5;
		sendMessageToManager(access_token,title2,"@all",description);
    }  
    
	/**
	 * 根据不同变量推送消息
	 * @param access_token
	 * @return 
	 * @throws WexinReqException 
	 */
	public static void sendMessageToManager(String access_token,String title,String touser,String description) throws WexinReqException {
		try {
			if(access_token!=null && !"".equals(access_token)) {											
				TextcardMessage textcardMessage = new TextcardMessage();
				Textcard textcard = new Textcard();
				//推送质量验证通知
				textcard.setTitle(title);
				textcard.setUrl(serviceUrl+"/TFMobile/webpage/exception/exceptionList.html");
				textcard.setDescription(description);
				textcardMessage.setTouser(touser);
				textcardMessage.setToparty("");
				textcardMessage.setMsgtype("textcard");
				textcardMessage.setAgentid(Integer.parseInt(AgentId));
				textcardMessage.setTextcard(textcard);
				JSONObject result = WxCommonAPI.sendMssage(access_token, textcardMessage);
				if (result.has("errcode") && result.getString("errcode").equals("0") && result.getString("errmsg").equals("ok")) {
				}else {			
					throw new WexinReqException("发送消息！errcode=" + result.getString("errcode") + ",errmsg = " + result.getString("errmsg"));
				}			
			}
			
		} catch (Exception e) {
			throw new WexinReqException("发送消息出错啦！");
		}
	}
	
	/**
	 * 根据部门编号获取该部门经理userId
	 * @param toparty
	 * @return managerUserId
	 */
	@SuppressWarnings("unchecked")
	public static String getManagerUserIdByDepartment(String department) {
		//根据部门号找到对应经理领导发送消息
		SQLHelper sqlhe = new SQLHelper();		
		String sql = "select userId from tf_member where deleteFlag = 0 and isleader = 1 and department = '" + department + "'";
		List<Object> userIdList = sqlhe.query(sql);
		String userIds = "";
		if (userIdList!=null && userIdList.size()>0) {
			for(int i=0;i<userIdList.size();i++) {				
				Map<String, Object> map = (Map<String, Object>) userIdList.get(i);
				String userId = String.valueOf(map.get("userId"));
				userIds += userId+"|";					
			  }
			if(userIds.substring(userIds.length()-1).equals("|")){
				userIds = userIds.substring(0,userIds.length()-1);
			}
		}
		return userIds;
	}
	public static void main(String[] args) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(sdf.parse("2016-06-28 12:00:00").getTime());
		System.out.println(sdf.parse("2016-06-28 12:00:00").getTime()+(2*60*60*1000));
		System.out.println(sdf.parse("2016-06-28 14:00:00").getTime());
		System.out.println("123,123,".split(",").length);
	}
}
