package com.github.georgeTseng.apiPerformanceTest.utils;

import com.github.georgeTseng.apiPerformanceTest.exception.CustomApplicationException;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceRequestData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceRequestFileData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceResultFileData;
import com.github.georgeTseng.apiPerformanceTest.model.PerformanceTestData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtils {

  private static Gson getGsonInstance() {
    return new GsonBuilder()
        .setPrettyPrinting()
        .create();
  }

  private static Logger getLoggerInstance() {
    return LoggerFactory.getLogger(JsonUtils.class);
  }

  /**
   * 將請求設定json寫入檔案, 失敗時拋出例外
   *
   * @param filePath 要儲存的檔案路徑
   * @param requestData 要儲存的請求設定
   */
  public static void writeRequestProfileJsonIntoFile(Path filePath, PerformanceRequestData requestData) throws CustomApplicationException {

    Gson gson = getGsonInstance();
    Logger jsonUtilsLogger = getLoggerInstance();

    PerformanceRequestFileData requestJsonData = DataTransferUtils.transIntoPerformanceRequestFileData(requestData);

    try {
      /* 開啟writer */
      Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);

      /* json序列化後儲存 */
      gson.toJson(requestJsonData, writer);

      /* 關閉writer */
      writer.close();

      jsonUtilsLogger.info("設定值已儲存於 {} ", filePath);

    } catch (JsonIOException e) {
      jsonUtilsLogger.error("寫入json資料時發生異常, 原因為: {} ", e.getMessage());
      throw new CustomApplicationException("讀取json資料時發生異常", e);
    } catch (IOException e) {
      jsonUtilsLogger.error("發生IO Exception, 原因為: {} , 所使用的參數有: {} ", e.getMessage(), new Object[]{filePath});
      throw new CustomApplicationException("發生IO Exception", e, new Object[]{filePath});
    }

  }

  /**
   * 從檔案中讀取json的測試設定檔, 失敗時拋出例外
   *
   * @param filePath 要讀取的檔案路徑
   */
  public static PerformanceRequestData readRequestSettingJsonFromFile(Path filePath) throws CustomApplicationException {

    Gson gson = getGsonInstance();
    Logger jsonUtilsLogger = getLoggerInstance();

    try {
      /* 開啟reader */
      Reader reader = Files.newBufferedReader(filePath);

      /* 取出json反序列化後的物件 */
      PerformanceRequestFileData requestJsonData = gson.fromJson(reader, PerformanceRequestFileData.class);

      /* 關閉reader */
      reader.close();

      /* 進行處理/檢查 */
      return DataTransferUtils.transIntoPerformanceRequestData(requestJsonData, filePath.toString());

    } catch (CustomApplicationException e) {
      throw e;
    } catch (JsonIOException e) {
      jsonUtilsLogger.error("讀取json資料時發生異常, 原因為: {} ", e.getMessage());
      throw new CustomApplicationException("讀取json資料時發生異常", e);
    } catch (IOException e) {
      jsonUtilsLogger.error("發生IO Exception, 原因為: {} , 所使用的參數有: {} ", e.getMessage(), new Object[]{filePath});
      throw new CustomApplicationException("發生IO Exception", e, new Object[]{filePath});
    }

  }

  /**
   * 將請求結果json寫入檔案, 失敗時拋出例外
   *
   * @param filePath 要儲存的檔案路徑
   * @param taskResults 要儲存的請求結果
   */
  public static void writeRequestTestingResultJsonIntoFile(Path filePath, List<Future<PerformanceTestData>> taskResults) {

    Gson gson = getGsonInstance();
    Logger jsonUtilsLogger = getLoggerInstance();

    try {
      /* 將輸入物件轉換為json所需的物件 */
      PerformanceResultFileData resultJsonData = DataTransferUtils.transIntoPerformanceResultFileData(taskResults);

      /* 開啟writer */
      Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8);

      /* json序列化後儲存 */
      gson.toJson(resultJsonData, writer);

      /* 關閉writer */
      writer.close();

      jsonUtilsLogger.info("測試結果已儲存於 {} ", filePath);

    } catch (CustomApplicationException e) {
      throw e;
    } catch (JsonIOException e) {
      jsonUtilsLogger.error("寫入json資料時發生異常, 原因為: {} ", e.getMessage());
      throw new CustomApplicationException("讀取json資料時發生異常", e);
    } catch (IOException e) {
      jsonUtilsLogger.error("發生IO Exception, 原因為: {} , 所使用的參數有: {} ", e.getMessage(), new Object[]{filePath});
      throw new CustomApplicationException("發生IO Exception", e, new Object[]{filePath});
    }

  }

}
