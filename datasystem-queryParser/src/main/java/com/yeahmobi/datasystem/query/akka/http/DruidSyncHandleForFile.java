package com.yeahmobi.datasystem.query.akka.http;

/**
 * Created by ellis.wang on 3/17/15.
 */

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import akka.actor.ActorRef;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.yeahmobi.datasystem.query.akka.cache.CacheTool;
import com.yeahmobi.datasystem.query.akka.cache.Ttls;
import com.yeahmobi.datasystem.query.akka.cache.db.CacheLogic;
import com.yeahmobi.datasystem.query.akka.cache.db.CacheLogicFactory;
import com.yeahmobi.datasystem.query.akka.cache.db.CacheRecord;
import com.yeahmobi.datasystem.query.akka.cache.db.CacheStatus;
import com.yeahmobi.datasystem.query.antlr4.DruidReportParser;
import com.yeahmobi.datasystem.query.assist.NetService;
import com.yeahmobi.datasystem.query.aws.AWSFileStore;
import com.yeahmobi.datasystem.query.exception.ReportRuntimeException;
import com.yeahmobi.datasystem.query.jersey.ReportServiceResult;
import com.yeahmobi.datasystem.query.landingpage.H2InMemoryDbUtil;
import com.yeahmobi.datasystem.query.meta.ReportResult;
import com.yeahmobi.datasystem.query.process.QueryContext;
import com.yeahmobi.datasystem.query.queue.DetailConstant;
import com.yeahmobi.datasystem.query.reportrequest.ReportParam;
import com.yeahmobi.datasystem.query.skeleton.DataBaseDataSet;
import com.yeahmobi.datasystem.query.skeleton.DataSet;
import com.yeahmobi.datasystem.query.skeleton.DataSetHandler;
import com.yeahmobi.datasystem.query.skeleton.InMemoryDataSet;
import com.yeahmobi.datasystem.query.skeleton.PostContext;
import com.yeahmobi.datasystem.query.skeleton.ReportResultDataSet;
import com.yeahmobi.datasystem.query.timedimension.TimeDimension;

/**
 * sync handler<br>
 * 在onCompleted中处理数据<br>
 * 
 */
public class DruidSyncHandleForFile implements AsyncHandler<Boolean> {


	// the result that returned to Client
	private PostContext reportFeatures;
	private DataSet dataSet;
	private DataSetHandler dataSetHandler;

	// 
	private boolean isFirstPart = true;
	private boolean isDruidErrorOccured = false;
	
	/**
	 * constructor
	 * 
	 * @param parser
	 * @param ctx
	 * @param cacheTool
	 * @param sender
	 * @param receiver
	 * @param dataSetHandler
	 * @param landingPage
	 */
	public DruidSyncHandleForFile(PostContext reportFeatures, DataSet dataSet,
			 DataSetHandler dataSetHandler) {
		this.reportFeatures = reportFeatures;
		this.dataSet = dataSet;
		this.dataSetHandler = dataSetHandler;
	}

	/**
	 * when druid finished store result into in-memory db <br>
	 * will invoke this
	 */
	public Boolean onCompleted() throws Exception {

		if(isDruidErrorOccured){
			throw new ReportRuntimeException("Error occured: the druid result exceeds the max limit[500000] or other reason");
		}
		
		DataSet retDataSet = dataSetHandler.processDataSet(dataSet);
		// 这边需要调整。对于实现DataSet接口的具象类没有统一继承。
	    Map<String, String> map = retDataSet.getInfo();
	    if (map.containsKey(DetailConstant.FILE_URL) && map.containsKey(DetailConstant.CALLBACK_URL)) {
	    	uploadFile(map.get(DetailConstant.FILE_URL), map.get(DetailConstant.CALLBACK_URL));
		}
	    
	    // 下载这种情况不要做cache，故删除这些表
	    if(map.get("lastTableName") != null){
	    	deleteLastTable(map.get("firstTableName"));
	    	dropLastTable(map.get("lastTableName"));
	    }
		return true;
	}

	public STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
		if(isFirstPart){
			// 第一个http 片段返回
			String str = new String(content.getBodyPartBytes(), "UTF-8").toLowerCase();
			if(str.contains("failure")){
				isDruidErrorOccured = true;
			}
			// 设置为false
			isFirstPart = false;
		}

		return STATE.CONTINUE;
	}

	public STATE onStatusReceived(final HttpResponseStatus status) throws Exception {
		logger.debug("the status from druid is " + status.getStatusCode());
    	if(status.getStatusCode() != 200){
    		throw new ReportRuntimeException("Error occured: the druid result exceeds the max limit[500000] or other reason");
    	}
		return STATE.CONTINUE;
	}

	public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
		return STATE.CONTINUE;
	}

	@Override
	public void onThrowable(Throwable throwable) {
	}
	
	public void uploadFile(String filePath, String callbackUrl) {
        try {
            File file = new File(filePath);
            String report_id = reportFeatures.getReportParam().getSettings().getReport_id();
            String fileStorePath = AWSFileStore.uploadDataAsCSV(file).toString();
            FileUtils.deleteQuietly(file);
            String verificationCode = DigestUtils.md5Hex(report_id + fileStorePath + "Yeahmobif3899843bc09ff972ab6252ab3c3cac6");
            if (logger.isDebugEnabled()) {
                logger.debug("file_url=" + fileStorePath);
            }
            String bsYeahmobiUrl = String.format(callbackUrl, report_id, URLEncoder.encode(fileStorePath, "UTF-8"), verificationCode);
            NetService.yeahMobiCallBack(bsYeahmobiUrl);
        } catch (IOException e) {
            logger.error("", e);
        }
    }
	
	private void deleteLastTable(String timeTablename) {
		String sql = "delete from lp_meta where tablename = '" + timeTablename + "'";
		String errorMsg = "execute sql failed: " + sql;
		try {
			H2InMemoryDbUtil.executeDbStatement(sql, errorMsg);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
	
	private void dropLastTable(String timeTablename) {
		String sql = "drop table if exists " + timeTablename;
		String errorMsg = "execute sql failed: " + sql;

		try {
			H2InMemoryDbUtil.executeDbStatement(sql, errorMsg);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	private final static Logger logger = Logger.getLogger(DruidSyncHandleForFile.class);
}
