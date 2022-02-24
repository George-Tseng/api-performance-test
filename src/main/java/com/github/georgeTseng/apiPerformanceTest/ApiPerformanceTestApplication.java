package com.github.georgeTseng.apiPerformanceTest;

import com.github.georgeTseng.apiPerformanceTest.enums.SupportedHttpMethod;
import com.github.georgeTseng.apiPerformanceTest.exception.CustomApplicationException;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceRequestData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceTestData;
import com.github.georgeTseng.apiPerformanceTest.task.PerformanceTask;
import com.github.georgeTseng.apiPerformanceTest.utils.JsonUtils;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class ApiPerformanceTestApplication {

  public final static int ZERO = 0;
  public final static int ONE = 1;
  public final static int HTTP_OK_STATUS_OK = 200;

  public final static String DEFAULT_ERROR_MESSAGE = "發生異常, 錯誤的輸入內容, 程式已停止";
  public final static String DEFAULT_STOP_INPUT_MESSAGE = "使用者本階段已輸入完畢..." + System.lineSeparator();
  public final static String UNSUPPORTED_CONTENT_TYPE_MESSAGE = "本程式僅接受 application/json 或 application/x-www-form-urlencoded ...";
  public final static String UNSUPPORTED_ACCEPT_MESSAGE = "本程式僅接受 application/json ...";
  public final static String TEST_API_URI_I118N_ZH_TW = "目標API URI";
  public final static String TEST_LIMIT_TIME_I18N_ZH_TW = "總測試次數";
  public final static String HTTP_METHOD_I18N_ZH_TW = "使用的HTTP方法";
  public final static String TEST_WAIT_TIME_PERIOD_I18N_ZH_TW = "測試間的等待時間(s)";
  public final static String DEFAULT_YES = "Y";
  public final static String DEFAULT_NO = "N";
  public final static String INVALID_FILE_PATH = "無效的檔案路徑! 程式已停止";

  /* Set the basic condition */
  public enum BasicParamKey {
    TARGET_URL_KEY("url"),
    HTTP_METHOD("httpMethod"),
    TASK_LIMIT("taskLimit"),
    WAIT_TIME("waitTime");

    String key;

    BasicParamKey(String key) {
      this.key = key;
    }

    public String getKey() {
      return this.key;
    }

  }

  /* Set the key usually be used in headers */
  public enum HeadersParamKey {
    AUTHORIZATION("authorization"),
    CONTENT_TYPE("Content-Type"),
    ACCEPT("accept");

    String key;

    HeadersParamKey(String key) {
      this.key = key;
    }

    public String getKey() {
      return this.key;
    }

  }

  /* Define the value usually be used in headers */
  public enum HeadersParamValue {
    JSON("application/json"),
    FORM_URI("application/x-www-form-urlencoded");

    String value;

    HeadersParamValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return this.value;
    }

    public static boolean isNotEqualToDefaultValue(String inputValue, HeadersParamValue defaultValue) {
      return !defaultValue.getValue().equals(inputValue);
    }

  }

  public static void main(String[] args) {

    Scanner scanner = new Scanner(System.in);
    Logger mainLogger = LoggerFactory.getLogger(ApiPerformanceTestApplication.class);
    mainLogger.info("開始執行本程式...{} ", System.lineSeparator());

    try {
      /* 是否讀取既有的設定檔, Y->是, N->否 */
      String testModel = checkRequestSettingFile(mainLogger, scanner);

      /* 設定請求物件 */
      PerformanceRequestData requestData;
      if (DEFAULT_YES.equals(testModel)) {
        requestData = setRequestDataFromFile(mainLogger, scanner);
        mainLogger.info("自 {} 讀取請求設定檔...", requestData.getFilePath());
      } else {
        requestData = setRequestDataByHand(mainLogger, scanner);
      }

      /* 檢視所有參數內容 */
      viewParams(mainLogger, requestData);

      /* 是否將此次設定值存入檔案 */
      String profileModel = checkRequestSettingProfile(mainLogger, scanner);
      if (DEFAULT_YES.equals(profileModel)) {
        saveRequestSettingProfile(mainLogger, scanner, requestData);
      } else {
        mainLogger.info("本次使用的請求設定將不予儲存...");
      }

      mainLogger.info("即將開始以相關設定連線至指定uri... {} ", System.lineSeparator());

      /* 取出總執行次數 */
      int taskLimit = requestData.getTaskLimit();

      /* 指定所有排程的集合 */
      List<PerformanceTask> performanceTasks = new ArrayList<>();
      for (int index = ZERO; index < taskLimit; index++) {
        PerformanceTask performanceTask = PerformanceTask.builder()
            .taskCount(index)
            .totalCount(taskLimit)
            .requestData(requestData)
            .build();
        performanceTasks.add(performanceTask);
      }

      /* 僅測試用, 單執行緒來跑排程即可 */
      ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

      List<Future<PerformanceTestData>> taskResults = doInvokeAll(executor, performanceTasks);

      /* 結束排程 */
      executor.shutdown();

      /* 顯示量測結果 */
      showTestResult(mainLogger, taskResults);

      /* 是否輸出量測結果為檔案 */
      String testResultFileModel = checkTestingResult(mainLogger, scanner);
      if (DEFAULT_YES.equals(testResultFileModel)) {
        saveRequestTestingResult(mainLogger, scanner, taskResults);
      }

      endingProgram(mainLogger, scanner, "測試程式已順利完成工作並結束");

    } catch (CustomApplicationException e) {
      if (Optional.ofNullable(e.getErrorCause()).isPresent()) {
        endingProgramInError(mainLogger, scanner, e.getErrorMessage(), e.getErrorCause());
      } else {
        endingProgramInError(mainLogger, scanner, e.getErrorMessage());
      }
    }

  }

  /**
   * 由使用者在每次操作中手動輸入測試所需的參數
   */
  public static PerformanceRequestData setRequestDataByHand(Logger logger, Scanner scanner) throws CustomApplicationException{

    logger.info("以下請設定 重複測試 相關的參數... ");
    try {
      String targetUrl = getUrlInput(logger, scanner);
      if (StringUtils.isBlank(targetUrl)) {
        throw new CustomApplicationException("無效的url , 程式已停止");
      }

      String usedMethod = getHttpMethodInput(logger, scanner);
      SupportedHttpMethod targetMethod;
      if (StringUtils.isBlank(usedMethod)) {
        targetMethod = SupportedHttpMethod.GET;
      } else {
        switch (usedMethod) {
          case "1":
            targetMethod = SupportedHttpMethod.GET;
            break;
          case "2":
            targetMethod = SupportedHttpMethod.POST;
            break;
          default:
            throw new CustomApplicationException(DEFAULT_ERROR_MESSAGE);
        }
      }

      int taskLimit = getTaskLimitInput(logger, scanner);
      if (taskLimit < ONE) {
        throw new CustomApplicationException("無效的輸入次數 - " + taskLimit + " , 程式已停止");
      }

      long waitTime = getWaitTimeInput(logger, scanner);
      if (waitTime < ONE) {
        throw new CustomApplicationException("無效的等待時間 - " + waitTime + " , 程式已停止");
      }

      logger.info(DEFAULT_STOP_INPUT_MESSAGE);
      logger.info("以下請設定 Http Headers 相關的參數... ");

      scanner.nextLine();

      String contentTypeValue = getContentTypeInput(logger, scanner);
      if (StringUtils.isNotBlank(contentTypeValue) &&
          (HeadersParamValue.isNotEqualToDefaultValue(contentTypeValue, HeadersParamValue.JSON) &&
              HeadersParamValue.isNotEqualToDefaultValue(contentTypeValue, HeadersParamValue.FORM_URI))) {
        throw new CustomApplicationException(UNSUPPORTED_CONTENT_TYPE_MESSAGE);
      }

      String authorizationValue = getAuthorizationInput(logger, scanner);

      String acceptValue = getAcceptInput(logger, scanner);
      if (StringUtils.isNotBlank(acceptValue) && HeadersParamValue.isNotEqualToDefaultValue(acceptValue, HeadersParamValue.JSON)) {
        throw new CustomApplicationException(UNSUPPORTED_ACCEPT_MESSAGE);
      }

      Map<String, Object> otherHeadersParams = new HashMap<>();
      String otherHeadersKey;
      do {
        /* 提供其他自定義的 headers 輸入, 如果 key 為空則停止迴圈 */
        otherHeadersKey = getOtherHeadersParamKey(logger, scanner);
        if (StringUtils.isBlank(otherHeadersKey)) {
          logger.info(DEFAULT_STOP_INPUT_MESSAGE);
          break;
        }

        String otherHeadersValue = getOtherHeadersParamValue(logger, otherHeadersKey ,scanner);
        if (StringUtils.isNotBlank(otherHeadersValue)) {
          otherHeadersParams.put(otherHeadersKey, otherHeadersValue);
        }

        /* 如果使用者本輪有輸入有效的參數, 則繼續讓其輸入下一個 */
      } while (StringUtils.isNotBlank(otherHeadersKey));

      logger.info(DEFAULT_STOP_INPUT_MESSAGE);
      Map<String, Object> otherParams = new HashMap<>();
      logger.info("以下請設定 其餘 的參數... ");

      String otherRequestKey;
      do {
        /* 提供其他自定義的 request 輸入, 如果 key 為空則停止迴圈 */
        otherRequestKey = getOtherRequestParamKey(logger, scanner);
        if (StringUtils.isBlank(otherRequestKey)) {
          logger.info(DEFAULT_STOP_INPUT_MESSAGE);
          break;
        }

        String otherRequestValue = getOtherRequestParamValue(logger, otherRequestKey, scanner);
        if (StringUtils.isNotBlank(otherRequestValue)) {
          otherParams.put(otherRequestKey, otherRequestValue);
        }

        /* 如果使用者本輪有輸入有效的參數, 則繼續讓其輸入下一個 */
      } while (StringUtils.isNotBlank(otherRequestKey));

      return PerformanceRequestData.builder()
          .url(targetUrl)
          .httpMethod(targetMethod)
          .taskLimit(taskLimit)
          .waitTime(waitTime)
          .contentType(contentTypeValue)
          .authorization(authorizationValue)
          .accept(acceptValue)
          .otherHeadersParams(otherHeadersParams)
          .otherParams(otherParams)
          .build();

    } catch (NoSuchElementException | IllegalStateException e) {
      throw new CustomApplicationException(DEFAULT_ERROR_MESSAGE, e);
    }

  }

  public static String getUrlInput(Logger logger, Scanner scanner) {
    logger.info("請輸入要執行的url(不用含參數):");
    return scanner.nextLine();
  }

  public static String getHttpMethodInput(Logger logger, Scanner scanner) {
    logger.info("請輸入要使用的方式(請輸入'數字', 1.GET/2.POST, 預設為GET)");
    return scanner.nextLine();
  }

  public static int getTaskLimitInput(Logger logger, Scanner scanner) {
    logger.info("請輸入要執行的總次數(正整數):");
    return scanner.nextInt();
  }

  public static long getWaitTimeInput(Logger logger, Scanner scanner) {
    logger.info("請輸入要執行的等待時間(秒數):");
    return scanner.nextLong();
  }

  public static String getContentTypeInput(Logger logger, Scanner scanner) {
    logger.info("是否需要指定 Content-Type ? 需要則輸入對應的值, 否則直接按enter跳過即可");
    return scanner.nextLine();
  }

  public static String getAuthorizationInput(Logger logger, Scanner scanner) {
    logger.info("是否需要在Header中加入 authorization ? 需要則輸入對應的值, 否則直接按enter跳過即可");
    return scanner.nextLine();
  }

  public static String getAcceptInput(Logger logger, Scanner scanner) {
    logger.info("是否需要指定 accept ? 需要則輸入對應的值, 否則直接按enter跳過即可");
    return scanner.nextLine();
  }

  public static String getOtherHeadersParamKey(Logger logger, Scanner scanner) {
    logger.info("是否需要指定其他 Header 所需的參數 ? 需要則先輸入對應的參數名稱, 否則直接按enter跳過即可");
    return scanner.nextLine();
  }

  public static String getOtherHeadersParamValue(Logger logger, String otherHeadersKey, Scanner scanner) {
    logger.info("是否需要指定參數 {} 的值 ? 需要則輸入對應的值, 否則直接按enter跳過即可", otherHeadersKey);
    return scanner.nextLine();
  }

  public static String getOtherRequestParamKey(Logger logger, Scanner scanner) {
    logger.info("是否需要指定其他 request 所需的參數 ? 需要則先輸入對應的參數名稱, 否則直接按enter跳過即可");
    return scanner.nextLine();
  }

  public static String getOtherRequestParamValue(Logger logger, String otherRequestKey, Scanner scanner) {
    logger.info("是否需要指定參數 {} 的值 ? 需要則輸入對應的值, 否則直接按enter跳過即可", otherRequestKey);
    return scanner.nextLine();
  }

  public static void endingProgram(Logger logger, Scanner scanner, String message) {
    logger.info(message);
    scanner.close();
  }

  public static void endingProgramInError(Logger logger, Scanner scanner, String message) {
    logger.info(message);
    scanner.close();
  }

  public static void endingProgramInError(Logger logger, Scanner scanner, String message, Throwable cause) {
    logger.info("{} , 原因為: {} ", message, cause);
    scanner.close();
  }

  public static void viewParams(Logger logger, PerformanceRequestData requestData) {

    Map<String, Object> otherHeadersParams = requestData.getOtherHeadersParams();
    Map<String, Object> otherParams = requestData.getOtherParams();

    logger.info("以下為本次測試所使用的設定值: {} ", System.lineSeparator());

    logger.info("{} : {} ", TEST_API_URI_I118N_ZH_TW, requestData.getUrl());
    logger.info("{} : {} ", HTTP_METHOD_I18N_ZH_TW, requestData.getHttpMethod());
    logger.info("{} : {} ", TEST_LIMIT_TIME_I18N_ZH_TW, requestData.getTaskLimit());
    logger.info("{} : {} ", TEST_WAIT_TIME_PERIOD_I18N_ZH_TW, requestData.getWaitTime());

    String contentType = requestData.getContentType();
    if (StringUtils.isNotBlank(contentType)) {
      logger.info("{} : {} ", HeadersParamKey.CONTENT_TYPE.getKey(), contentType);
    }

    String authorization = requestData.getAuthorization();
    if (StringUtils.isNotBlank(authorization)) {
      logger.info("{} : {} ", HeadersParamKey.AUTHORIZATION.getKey(), authorization);
    }

    String accept = requestData.getAccept();
    if (StringUtils.isNotBlank(accept)) {
      logger.info("{} : {} ", HeadersParamKey.ACCEPT.getKey(), accept);
    }

    if (Optional.ofNullable(otherHeadersParams).isPresent() && otherHeadersParams.size() > 0) {
      logger.info("使用了以下自定義的Headers...");
      viewExtraParams(logger, otherHeadersParams);
    } else {
      logger.info("未使用其他自定義的Headers...");
    }

    if (Optional.ofNullable(otherParams).isPresent() && otherParams.size() > 0) {
      logger.info("使用了以下自定義的參數...");
      viewExtraParams(logger, otherParams);
    } else {
      logger.info("未使用其他自定義的參數...");
    }

  }

  public static void viewExtraParams(Logger logger, Map<String, Object> paramsMap) {
    for (Map.Entry<String, Object> param: paramsMap.entrySet()) {
      logger.info("{} : {} ", param.getKey(), param.getValue());
    }
  }

  public static List<Future<PerformanceTestData>> doInvokeAll(ScheduledExecutorService executor, List<PerformanceTask> performanceTasks) throws CustomApplicationException {

    try {
      return executor.invokeAll(performanceTasks);
    } catch (InterruptedException e) {
      throw new CustomApplicationException("fail to invoke task data...", e);
    }

  }

  public static void showTestResult(Logger logger, List<Future<PerformanceTestData>> taskResults) {

    if (taskResults == null) {
      throw new CustomApplicationException("empty performance test result...");
    } else {
      logger.info("以下為逐次測試的相關資訊: {} ", System.lineSeparator());

      long totalOperateTime = ZERO, bestOperateTime = ZERO, worstOperateTime = ZERO;
      int okCount = ZERO, ngCount = ZERO;

      int totalTestCount = taskResults.size();

      for (int index = 0; index < totalTestCount; index++) {
        Future<PerformanceTestData> currentTest = taskResults.get(index);
        try {
          PerformanceTestData currentTestData = currentTest.get();

          /* 顯示耗時資訊 */
          long currentOperateTime = currentTestData.getOperateTime();
          logger.info("第 {} 次執行時的耗時資訊為: {} ms", (index + 1), currentOperateTime);

          /* 顯示 api 回應的訊息 */
          String currentResponseText = currentTestData.getResponseText();
          logger.info("第 {} 次執行時的回傳資訊為: {} ", (index + 1), currentResponseText + System.lineSeparator());

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

          totalOperateTime = totalOperateTime + currentOperateTime;
          if (index == totalTestCount - 1) {
            logger.info("共 {} 筆", totalTestCount);

            /* 求出平均耗時 */
            long averageOperateTime = totalOperateTime / totalTestCount;
            double okRate = (100 * okCount) / (totalTestCount * 1.0);
            double decimalPlaceFactor = Math.pow(10, 2);
            double okPercent = Math.round((okRate / decimalPlaceFactor) * decimalPlaceFactor);

            logger.info("平均耗時為 {} ms", averageOperateTime);
            logger.info("最長耗時為 {} ms", worstOperateTime);
            logger.info("最短耗時為 {} ms", bestOperateTime);
            logger.info("Http請求成功次數為 {} 次", okCount);
            logger.info("Http請求失敗次數為 {} 次", ngCount);
            logger.info("Http請求成功率為(到2位小數) {} % {} ", okPercent, System.lineSeparator());
          }

        } catch (InterruptedException | ExecutionException e) {
          throw new CustomApplicationException(System.lineSeparator() + "fail to get TaskData in currentTask" + System.lineSeparator(), e);
        }
      }
    }

  }

  /**
   * 確認是否需要讀取現有的請求設定檔, 回傳Y->是, N->否
   */
  public static String checkRequestSettingFile(Logger logger, Scanner scanner) throws CustomApplicationException {

    logger.info("請問是否要載入可用的測試設定檔？(Y->是, N->否, 預設為否)");
    String testModel = scanner.nextLine();
    if (testModel == null) {
      testModel = "N";
    } else if (testModel.equalsIgnoreCase(DEFAULT_YES)) {
      testModel = "Y";
    } else if (testModel.equalsIgnoreCase(DEFAULT_NO)) {
      testModel = "N";
    } else {
      logger.error(DEFAULT_ERROR_MESSAGE);
      throw new CustomApplicationException(DEFAULT_ERROR_MESSAGE);
    }

    return testModel;

  }

  /**
   * 讀取現有的測試設定檔
   */
  public static PerformanceRequestData setRequestDataFromFile(Logger logger, Scanner scanner) {

    logger.info("請輸入測試設定檔的完整路徑");
    String testConfigFilePath = scanner.nextLine();

    try {
      /* 產生路徑物件 */
      Path testConfigPath = Paths.get(testConfigFilePath);

      /* 成功時回傳請求設定物件 */
      return JsonUtils.readRequestSettingJsonFromFile(testConfigPath);

    } catch (CustomApplicationException e) {
      throw e;
    } catch (Exception e) {
      logger.error(INVALID_FILE_PATH);
      throw new CustomApplicationException(INVALID_FILE_PATH, e);
    }

  }

  /**
   * 確認是否需要儲存現有的請求設定, 回傳Y->是, N->否
   */
  public static String checkRequestSettingProfile(Logger logger, Scanner scanner) throws CustomApplicationException {

    logger.info("請問是否要將測試設定值儲存成檔案？(Y->是, N->否, 預設為否)");
    String profileModel = scanner.nextLine();
    if (profileModel == null) {
      profileModel = "N";
    } else if (profileModel.equalsIgnoreCase(DEFAULT_YES)) {
      profileModel = "Y";
    } else if (profileModel.equalsIgnoreCase(DEFAULT_NO)) {
      profileModel = "N";
    } else {
      logger.error(DEFAULT_ERROR_MESSAGE);
      throw new CustomApplicationException(DEFAULT_ERROR_MESSAGE);
    }

    return profileModel;

  }

  /**
   * 寫入現有的測試設定值
   */
  public static void saveRequestSettingProfile(Logger logger, Scanner scanner, PerformanceRequestData requestData) throws CustomApplicationException {

    logger.info("請輸入欲儲存設定值的檔案完整路徑");
    String testProfileFilePath = scanner.nextLine();

    try {
      File testProfile = new File(testProfileFilePath);
      if (testProfile.exists()) {
        String testProfileFileModel = checkExistingFile(logger, scanner);
        if (DEFAULT_NO.equals(testProfileFileModel)) {
          logger.info("請輸入欲儲存設定值的新檔案完整路徑");
          testProfileFilePath = scanner.nextLine();
        } else {
          logger.info("將新設定值寫入現有的 {} 中", testProfileFilePath);
        }
      }

      /* 產生路徑物件 */
      Path testProfilePath = Paths.get(testProfileFilePath);

      /* 執行儲存請求的設定值 */
      JsonUtils.writeRequestProfileJsonIntoFile(testProfilePath, requestData);

    } catch (CustomApplicationException e) {
      throw e;
    } catch (Exception e) {
      logger.error(INVALID_FILE_PATH);
      throw new CustomApplicationException(INVALID_FILE_PATH, e);
    }

  }

  /**
   * 當欲儲存的檔案已存在時, 詢問是否要另存檔案或直接覆蓋原檔, 回傳Y->是, N->否
   */
  public static String checkExistingFile(Logger logger, Scanner scanner) throws CustomApplicationException {

    logger.info("檔案已存在, 請問是否要覆蓋現有的檔案?(Y->是, N->否, 預設為否)");
    String testProfileFileModel = scanner.nextLine();
    if (testProfileFileModel == null) {
      testProfileFileModel = "N";
    } else if (testProfileFileModel.equalsIgnoreCase(DEFAULT_YES)) {
      testProfileFileModel = "Y";
    } else if (testProfileFileModel.equalsIgnoreCase(DEFAULT_NO)) {
      testProfileFileModel = "N";
    } else {
      logger.error(DEFAULT_ERROR_MESSAGE);
      throw new CustomApplicationException(DEFAULT_ERROR_MESSAGE);
    }

    return testProfileFileModel;

  }

  /**
   * 確認是否需要儲存測試的結果, 回傳Y->是, N->否
   */
  public static String checkTestingResult(Logger logger, Scanner scanner) throws CustomApplicationException {

    logger.info("請問是否要將測試結果輸出成檔案?(Y->是, N->否, 預設為否)");
    String testResultFileModel = scanner.nextLine();
    if (testResultFileModel == null) {
      testResultFileModel = "N";
    } else if (testResultFileModel.equalsIgnoreCase(DEFAULT_YES)) {
      testResultFileModel = "Y";
    } else if (testResultFileModel.equalsIgnoreCase(DEFAULT_NO)) {
      testResultFileModel = "N";
    } else {
      logger.error(DEFAULT_ERROR_MESSAGE);
      throw new CustomApplicationException(DEFAULT_ERROR_MESSAGE);
    }

    return testResultFileModel;

  }

  /**
   * 寫入測試的結果
   */
  public static void saveRequestTestingResult(Logger logger, Scanner scanner, List<Future<PerformanceTestData>> taskResults) throws CustomApplicationException {

    logger.info("請輸入欲儲存測試結果的檔案完整路徑");
    String testResultFilePath = scanner.nextLine();

    try {
      File testResult = new File(testResultFilePath);
      if (testResult.exists()) {
        String testProfileFileModel = checkExistingFile(logger, scanner);
        if (DEFAULT_NO.equals(testProfileFileModel)) {
          logger.info("請輸入欲儲存測試結果的新檔案完整路徑");
          testResultFilePath = scanner.nextLine();
        } else {
          logger.info("將新測試結果寫入現有的 {} 中", testResultFilePath);
        }
      }

      /* 產生路徑物件 */
      Path testProfilePath = Paths.get(testResultFilePath);

      /* 執行儲存請求測試的結果 */
      JsonUtils.writeRequestTestingResultJsonIntoFile(testProfilePath, taskResults);

    } catch (CustomApplicationException e) {
      throw e;
    } catch (Exception e) {
      logger.error(INVALID_FILE_PATH);
      throw new CustomApplicationException(INVALID_FILE_PATH, e);
    }

  }

}
