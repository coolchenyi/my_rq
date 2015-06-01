package com.yeahmobi.datasystem.query.meta;


import java.util.LinkedHashMap;
import java.util.LinkedList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yeahmobi.datasystem.query.serializer.ObjectSerializer;

/**
 * the configuration file for landing page calculation
 * 
 */
public class TimeDimensionCfg {

	private LinkedHashMap<String, LinkedHashMap<String, String>> metric;

	private static final String JSON_FILE = "timeDimensionPageCfg.json";
	private static TimeDimensionCfg cfg = null;

	// parse the json file
	static {
		cfg = ObjectSerializer.read(JSON_FILE,
				new TypeReference<TimeDimensionCfg>() {
				}, TimeDimensionCfg.class.getClassLoader());
	}

	// get the LandingPageTableCfg
	public static TimeDimensionCfg getInstance(){
		return cfg;
	}

	private LinkedHashMap<String, LinkedHashMap<String, String>> getMetric() {
		return metric;
	}

	public void setMetric(
			LinkedHashMap<String, LinkedHashMap<String, String>> metric) {
		this.metric = metric;
	}

	public LinkedHashMap<String, String> getMetric(String dataSource) {
		return getMetric().get(dataSource);
	}
	
	public static void main(String[] args) {
		System.out.println(TimeDimensionCfg.getInstance().getMetric("native_report_datasource"));
	}
}
