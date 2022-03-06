package com.github.georgeTseng.apiPerformanceTest.task;

import com.github.georgeTseng.apiPerformanceTest.exception.CustomApplicationException;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceRequestData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceTestData;
import com.github.georgeTseng.apiPerformanceTest.utils.ApiConnectionUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceTask implements Callable<PerformanceTestData> {

  private Integer taskCount;
  private Integer totalCount;
  private PerformanceRequestData requestData;

  private final static String ERROR_PARAM_KEY = "error";

  @Override
  public PerformanceTestData call() throws CustomApplicationException {

    Logger performanceTaskLogger = LoggerFactory.getLogger(PerformanceTask.class);

    Map<String, Object> responseDatas = new HashMap<>();

    /* 顯示系統當前時間(ms) */
    long startTime = System.currentTimeMillis();
    long endTime;

    PerformanceTestData responseData;

    try {
      /* 執行呼叫 api */
      responseData = ApiConnectionUtils.getApiConnectResult(requestData);
    } catch (UnsupportedEncodingException e) {
      /* 代表 StringEntity 建立失敗 */
      performanceTaskLogger.error("執行失敗, 無法建立 StringEntity ! ");
      responseDatas.put(ERROR_PARAM_KEY, "執行失敗, 無法建立 StringEntity ! ");

      responseData = PerformanceTestData.builder()
          .statusCode(400)
          .responseDatas(responseDatas)
          .build();
    } catch (ClientProtocolException e) {
      /* 代表 CloseableHttpClient物件.execute 執行失敗 */
      performanceTaskLogger.error("執行失敗, execute 失敗 ! ");
      responseDatas.put(ERROR_PARAM_KEY, "執行失敗, execute 失敗 ! ");

      responseData = PerformanceTestData.builder()
          .statusCode(400)
          .responseDatas(responseDatas)
          .build();
    } catch (IOException e) {
      /* IOException */
      performanceTaskLogger.error("執行失敗, 發生 IOException ! ");
      responseDatas.put(ERROR_PARAM_KEY, "執行失敗, 發生 IOException ! ");

      responseData = PerformanceTestData.builder()
          .statusCode(400)
          .responseDatas(responseDatas)
          .build();
    }

    /* 執行完成後紀錄當前時間(ms) */
    endTime = new Date().getTime();

    /* 執行完成後算出耗時 */
    long costTime = endTime - startTime;

    /* 取出執行次數 */
    int currentTaskCount = getTaskCount();

    /* 取出 status code 與回應內文 */
    int currentStatusCode = responseData.getStatusCode();
    responseDatas = responseData.getResponseDatas();

    /* 生成回傳用的 TaskData 物件 */
    PerformanceTestData currentTestData = PerformanceTestData.builder()
        .operateTime(costTime)
        .statusCode(currentStatusCode)
        .responseDatas(responseDatas)
        .build();

    /* 取出總執行次數 */
    int totalTaskCount = getTotalCount();

    /* 算出完成比率 */
    double finishRate = (100 * (currentTaskCount + 1)) / (totalTaskCount * 1.0);
    double decimalPlaceFactor = Math.pow(10, 2);
    double finishPercent = Math.round((finishRate / decimalPlaceFactor) * decimalPlaceFactor);

    /* 印出執行次數讓使用者確認程式尚在運行 */
    performanceTaskLogger.info("已執行第 {} 次, 已完成 {} %...", (currentTaskCount + 1), finishPercent);

    /* 取出等待時間 */
    long waitTime = requestData.getWaitTime();

    try {
      /* 執行後暫停指定時間 */
      TimeUnit.SECONDS.sleep(waitTime);
    } catch (InterruptedException e) {
      performanceTaskLogger.error("執行緒 sleep 失敗..., 原因為: {} ", e.getMessage());
      throw new CustomApplicationException("執行緒 sleep 失敗...", e);
    }

    return currentTestData;
  }

}
