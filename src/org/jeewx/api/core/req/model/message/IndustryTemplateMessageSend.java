package org.jeewx.api.core.req.model.message;

import org.jeewx.api.core.annotation.ReqType;
import org.jeewx.api.core.req.model.WeixinReqParam;

/**
 * 取多媒体文件
 * 
 * @author sfli.sir
 * 
 */
@ReqType("industryTemplateMessageSend")
public class IndustryTemplateMessageSend extends WeixinReqParam {
	
	//添加一个tipe识别模板消息是哪种形式（1：switch-case方式；2：自定义方式）
	private String template_type;
	
	//添加一个模板编号，用来识别是哪个编号产生的id (用于type 1;)
	private String template_no;
	
	private String touser;
	
	private String template_id;
	
	private String url;
	
	private String topcolor;
	
	private TemplateMessage data;
	
	private TemplateDataKey dataKey;
	
	public String getTemplate_type() {
		return template_type;
	}

	public void setTemplate_type(String template_type) {
		this.template_type = template_type;
	}

	public String getTemplate_no() {
		return template_no;
	}

	public void setTemplate_no(String template_no) {
		this.template_no = template_no;
	}

	public String getTouser() {
		return touser;
	}

	public void setTouser(String touser) {
		this.touser = touser;
	}

	public String getTemplate_id() {
		return template_id;
	}

	public void setTemplate_id(String template_id) {
		this.template_id = template_id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTopcolor() {
		return topcolor;
	}

	public void setTopcolor(String topcolor) {
		this.topcolor = topcolor;
	}

	public TemplateMessage getData() {
		return data;
	}

	public void setData(TemplateMessage data) {
		this.data = data;
	}

	public TemplateDataKey getDataKey() {
		return dataKey;
	}

	public void setDataKey(TemplateDataKey dataKey) {
		this.dataKey = dataKey;
	}

	
}
