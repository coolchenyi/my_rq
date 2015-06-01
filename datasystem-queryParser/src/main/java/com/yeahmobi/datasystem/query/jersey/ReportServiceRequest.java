package com.yeahmobi.datasystem.query.jersey;

public class ReportServiceRequest {

	private String param;
	private String style;
	
	public ReportServiceRequest(String param, String style){
		this.param = param;
		this.style = style;
	}
	
	public String getParam() {
		return param;
	}
	public void setParam(String param) {
		this.param = param;
	}
	public String getStyle() {
		return style;
	}
	public void setStyle(String style) {
		this.style = style;
	}
	
	
}
