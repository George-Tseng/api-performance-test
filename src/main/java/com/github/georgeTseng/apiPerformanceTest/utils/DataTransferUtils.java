package com.github.georgeTseng.apiPerformanceTest.utils;

import com.github.georgeTseng.apiPerformanceTest.ApiPerformanceTestApplication.HeadersParamValue;
import com.github.georgeTseng.apiPerformanceTest.enums.SupportedHttpMethod;
import com.github.georgeTseng.apiPerformanceTest.exception.CustomApplicationException;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceRequestData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceRequestFileData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceResultFileData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceTestData;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;

public class DataTransferUtils {

  public final static String DEFAULT_ERROR_MESSAGE = "發生異常, 錯誤的輸入內容, 程式已停止";
  public final static String UNSUPPORTED_CONTENT_TYPE_MESSAGE = "本程式僅接受 application/json 或 application/x-www-form-urlencoded ...";
  public final static String UNSUPPORTED_ACCEPT_MESSAGE = "本程式僅接受 application/json ...";
  public final static String INTEGER_SAVE_IN_DOUBLE_PATTERN = ".0";
  public final static int ONE = 1;
  public final static int ZERO = 0;
  public final static int HTTP_OK_STATUS_OK = 200;

  /**
   * 將輸入的物件轉換為 PerformanceRequestFileData 物件
   *
   * @param requestData 輸入的 PerformanceRequestData 物件
   */
  public static PerformanceRequestFileData transIntoPerformanceRequestFileData(PerformanceRequestData requestData) {
    return PerformanceRequestFileData.builder()
        .url(requestData.getUrl())
        .httpMethod(requestData.getHttpMethod().toString())
        .taskLimit(requestData.getTaskLimit().toString())
        .waitTime(requestData.getWaitTime().toString())
        .contentType(requestData.getContentType())
        .authorization(requestData.getAuthorization())
        .accept(requestData.getAccept())
        .otherHeadersParams(requestData.getOtherHeadersParams())
        .otherParams(requestData.getOtherParams())
        .build();
  }

  /**
   * 將輸入的物件轉換為 PerformanceResultFileData 物件
   *
   * @param taskResults 輸入的 List<Future<PerformanceTestData>> 物件
   */
  public static PerformanceResultFileData transIntoPerformanceResultFileData(List<Future<PerformanceTestData>> taskResults) {

    Map<Integer, PerformanceTestData> performanceTestResults = new HashMap<>();
    long averageOperateTime = ZERO, bestOperateTime = ZERO, worstOperateTime = ZERO;
    int okCount = ZERO, ngCount = ZERO;
    double okPercent = ZERO;

    if (taskResults == null) {
      throw new CustomApplicationException("empty performance test result...");
    } else {
      long totalOperateTime = ZERO;

      int totalTestCount = taskResults.size();

      for (int index = 0; index < totalTestCount; index++) {
        Future<PerformanceTestData> currentTest = taskResults.get(index);
        try {
          PerformanceTestData currentTestData = currentTest.get();

          long currentOperateTime = currentTestData.getOperateTime();
          if (worstOperateTime < currentOperateTime) {
            worstOperateTime = currentOperateTime;
          }

          if (bestOperateTime == ZERO || bestOperateTime > currentOperateTime) {
            bestOperateTime = currentOperateTime;
          }

          if (HTTP_OK_STATUS_OK == currentTestData.getStatusCode()) {
            okCount++;
          } else {
            ngCount++;
          }

          performanceTestResults.put((index + 1), currentTestData);

          totalOperateTime = totalOperateTime + currentOperateTime;
          if (index == totalTestCount - 1) {
            averageOperateTime = totalOperateTime / totalTestCount;
            double okRate = (100 * okCount) / (totalTestCount * 1.0);
            double decimalPlaceFactor = Math.pow(10, 2);
            okPercent = Math.round((okRate / decimalPlaceFactor) * decimalPlaceFactor);


          }

        } catch (InterruptedException | ExecutionException e) {
          throw new CustomApplicationException(System.lineSeparator() + "fail to get TaskData in currentTask" + System.lineSeparator(), e);
        }
      }

      return PerformanceResultFileData.builder()
          .performanceTestResults(performanceTestResults)
          .totalCount(totalTestCount)
          .averageOperateTime(averageOperateTime)
          .bestOperateTime(bestOperateTime)
          .worstOperateTime(worstOperateTime)
          .okPercent(okPercent)
          .okCount(okCount)
          .ngCount(ngCount)
          .build();

    }

  }

  /**
   * 進行處理/檢查 json 的內容, 成功時回傳整理後的 PerformanceRequestData 物件, 失敗時拋出例外
   *
   * @param requestJsonData 由 json 中取得的資料
   * @param filePath json 檔的所在路徑
   */
  public static PerformanceRequestData transIntoPerformanceRequestData(PerformanceRequestFileData requestJsonData, String filePath) throws CustomApplicationException {

    String targetUrl = requestJsonData.getUrl();
    if (StringUtils.isBlank(targetUrl)) {
      throw new CustomApplicationException("無效的url , 程式已停止");
    }

    String usedMethod = requestJsonData.getHttpMethod();
    SupportedHttpMethod targetMethod;
    if (StringUtils.isBlank(usedMethod)) {
      targetMethod = SupportedHttpMethod.GET;
    } else {
      switch (usedMethod) {
        case "GET":
          targetMethod = SupportedHttpMethod.GET;
          break;
        case "POST":
          targetMethod = SupportedHttpMethod.POST;
          break;
        default:
          throw new CustomApplicationException(DEFAULT_ERROR_MESSAGE);
      }
    }

    String taskLimitValue = requestJsonData.getTaskLimit();
    if (StringUtils.isBlank(taskLimitValue)) {
      throw new CustomApplicationException("無效的測試次數 , 程式已停止");
    } else if (taskLimitValue.endsWith(INTEGER_SAVE_IN_DOUBLE_PATTERN)) {
      taskLimitValue = taskLimitValue.substring(0, taskLimitValue.length() - 2);
    }

    int taskLimit = Integer.parseInt(taskLimitValue);
    if (taskLimit < ONE) {
      throw new CustomApplicationException("無效的輸入次數 - " + taskLimit + " , 程式已停止");
    }

    String waitTimeValue = requestJsonData.getWaitTime();
    if (StringUtils.isBlank(waitTimeValue)) {
      throw new CustomApplicationException("無效的等待時間 , 程式已停止");
    } else if (waitTimeValue.endsWith(INTEGER_SAVE_IN_DOUBLE_PATTERN)) {
      waitTimeValue = waitTimeValue.substring(0, waitTimeValue.length() - 2);
    }

    long waitTime = Long.parseLong(waitTimeValue);
    if (waitTime < ONE) {
      throw new CustomApplicationException("無效的等待時間 - " + waitTime + " , 程式已停止");
    }

    String contentTypeValue = requestJsonData.getContentType();
    if (StringUtils.isNotBlank(contentTypeValue)) {
      if (HeadersParamValue.isNotEqualToDefaultValue(contentTypeValue, HeadersParamValue.JSON) &&
          HeadersParamValue.isNotEqualToDefaultValue(contentTypeValue, HeadersParamValue.FORM_URI)) {
        throw new CustomApplicationException(UNSUPPORTED_CONTENT_TYPE_MESSAGE);
      }
    }

    String acceptValue = requestJsonData.getAccept();
    if (StringUtils.isNotBlank(acceptValue) && HeadersParamValue.isNotEqualToDefaultValue(acceptValue, HeadersParamValue.JSON)) {
      throw new CustomApplicationException(UNSUPPORTED_ACCEPT_MESSAGE);
    }

    return PerformanceRequestData.builder()
        .url(targetUrl)
        .httpMethod(targetMethod)
        .taskLimit(taskLimit)
        .waitTime(waitTime)
        .contentType(contentTypeValue)
        .authorization(requestJsonData.getAuthorization())
        .accept(acceptValue)
        .otherHeadersParams(requestJsonData.getOtherHeadersParams())
        .otherParams(requestJsonData.getOtherParams())
        .filePath(filePath.toString())
        .build();

  }

}
