package com.modules.utils;

import java.io.FileInputStream;
import java.util.Properties;

import com.modules.db.C3P0ConnentionProvider;

public class ConfigurationFileHelper {

	private static final String SERVICE_URL = "serviceUrl";
	private static final String SAVEPICTURE_PATH = "savePicturePath";//保存图片路径
	private static final String CORPID = "CorpID";//微信企业的CorpID
	private static final String EXCEPTIONSECRET = "ExceptionSecret";//微信企业的异常通报应用的Secret
	private static final String EXCEPTIONAGENTID = "ExceptionAgentId";//微信企业的异常通报应用的AgentId
	private static final String WARNINGSECRET = "WarningSecret";//微信企业的预警应用的Secret
	private static final String WARNINGAGENTID = "WarningAgentId";//微信企业的预警应用的AgentId
	private static final String JUMPSTATIONSECRET = "JumpStationSecret";//微信企业的跳站应用的Secret
	private static final String JUMPSTATIONAGENTID = "JumpStationAgentId";//微信企业的跳站应用的AgentId
	private static final String SALEQUALITYSECRET = "SaleQualitySecret";//微信企业的售后质量应用的Secret
	private static final String SALEQUALITYAGENTID = "SaleQualityAgentId";//微信企业的售后质量应用的AgentId
	private static final String IQCEXCEPTIONSECRET = "IQCExceptionSecret";//微信企业的物料异常通报应用的Secret
	private static final String IQCEXCEPTIONAGENTID = "IQCExceptionAgentId";//微信企业的物料异常通报应用的AgentId
	private static final String NEWEXCEPTIONSECRET = "NewExceptionSecret";//微信企业的新版异常通报应用的Secret
	private static final String NEWEXCEPTIONAGENTID = "NewExceptionAgentId";//微信企业的新版异常通报应用的AgentId
	private static final String OVERTIMEAPPLICATIONSECRET = "OvertimeApplicationSecret";//微信企业的加班申请单应用的Secret
	private static final String OVERTIMEAPPLICATIONAGENTID = "OvertimeApplicationAgentId";//微信企业的加班申请单应用的AgentId
	
	private static final String Another_CORPID = "Another_CorpID";//微信企业的CorpID
	private static final String IMPROVEMENT_SECRET = "ImprovementSecret";//改善提案应用的Secret
	private static final String IMPROVEMENT_AGENTID = "ImprovementAgentId";//改善提案应用的AgentId

	private static String serviceUrl;
	private static String savePicturePath;
	private static String corpID;
	private static String exceptionSecret;
	private static String exceptionAgentId;
	private static String warningSecret;
	private static String warningAgentId;
	private static String jumpStationSecret;
	private static String jumpStationAgentId;
	private static String saleQualitySecret;
	private static String saleQualityAgentId;
	private static String iqcExceptionSecret;
	private static String iqcExceptionAgentId;
	private static String newExceptionSecret;
	private static String newExceptionAgentId;
	private static String overtimeApplicationSecret;
	private static String overtimeApplicationAgentId;
	
	private static String another_CorpID;
	private static String improvementSecret;
	private static String improvementAgentId;
	
	/**
	 * 初始化
	 */
	static {
		initConfigurationSource();
	}

	private static final void initConfigurationSource() {
		Properties config = new Properties();
		try {
			// 加载配置文件
			String path = C3P0ConnentionProvider.class.getResource("/").getPath();
			String websiteURL = ("/" + path + "configuration.properties").replaceFirst("/", "");
			FileInputStream in = new FileInputStream(websiteURL);
			config.load(in);
		} catch (Exception e) {
			e.printStackTrace();
		}

		serviceUrl = config.getProperty(SERVICE_URL);
		savePicturePath = config.getProperty(SAVEPICTURE_PATH);
		corpID = config.getProperty(CORPID);
		exceptionSecret = config.getProperty(EXCEPTIONSECRET);
		exceptionAgentId = config.getProperty(EXCEPTIONAGENTID);
		warningSecret = config.getProperty(WARNINGSECRET);
		warningAgentId = config.getProperty(WARNINGAGENTID);
		jumpStationSecret = config.getProperty(JUMPSTATIONSECRET);
		jumpStationAgentId = config.getProperty(JUMPSTATIONAGENTID);
		saleQualitySecret = config.getProperty(SALEQUALITYSECRET);
		saleQualityAgentId = config.getProperty(SALEQUALITYAGENTID);
		iqcExceptionSecret = config.getProperty(IQCEXCEPTIONSECRET);
		iqcExceptionAgentId = config.getProperty(IQCEXCEPTIONAGENTID);
		newExceptionSecret = config.getProperty(NEWEXCEPTIONSECRET);
		newExceptionAgentId = config.getProperty(NEWEXCEPTIONAGENTID);
		overtimeApplicationSecret = config.getProperty(OVERTIMEAPPLICATIONSECRET);
		overtimeApplicationAgentId = config.getProperty(OVERTIMEAPPLICATIONAGENTID);
		
		another_CorpID = config.getProperty(Another_CORPID);
		improvementSecret = config.getProperty(IMPROVEMENT_SECRET);
		improvementAgentId = config.getProperty(IMPROVEMENT_AGENTID);
	}

	public static synchronized String getServiceUrl() {
		return serviceUrl;
	}
	public static synchronized String getSavePicturePath() {
		return savePicturePath;
	}
	public static synchronized String getCorpID() {
		return corpID;
	}
	public static synchronized String getExceptionSecret() {
		return exceptionSecret;
	}
	public static synchronized String getExceptionAgentId() {
		return exceptionAgentId;
	}
	public static synchronized String getWarningSecret() {
		return warningSecret;
	}
	public static synchronized String getWarningAgentId() {
		return warningAgentId;
	}
	public static synchronized String getJumpStationSecret() {
		return jumpStationSecret;
	}
	public static synchronized String getJumpStationAgentId() {
		return jumpStationAgentId;
	}
	public static synchronized String getSaleQualitySecret() {
		return saleQualitySecret;
	}
	public static synchronized String getSaleQualityAgentId() {
		return saleQualityAgentId;
	}
	public static synchronized String getIQCExceptionSecret() {
		return iqcExceptionSecret;
	}
	public static synchronized String getIQCExceptionAgentId() {
		return iqcExceptionAgentId;
	}
	public static synchronized String getNewExceptionSecret() {
		return newExceptionSecret;
	}
	public static synchronized String getOvertimeApplicationAgentId() {
		return overtimeApplicationAgentId;
	}
	public static synchronized String getOvertimeApplicationSecret() {
		return overtimeApplicationSecret;
	}
	public static synchronized String getNewExceptionAgentId() {
		return newExceptionAgentId;
	}
	
	public static synchronized String getAnotherCorpID() {
		return another_CorpID;
	}
	public static synchronized String getImprovementSecret() {
		return improvementSecret;
	}
	public static synchronized String getImprovementAgentId() {
		return improvementAgentId;
	}
}
