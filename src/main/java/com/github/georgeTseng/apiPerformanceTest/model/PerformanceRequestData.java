package com.github.georgeTseng.apiPerformanceTest.model;

import com.github.georgeTseng.apiPerformanceTest.enums.SupportedHttpMethod;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceRequestData {

  /* API的base uri */
  private String url;

  /* 使用的Http Method, 見此enum */
  private SupportedHttpMethod httpMethod;

  /* 指定的測試次數，至少為1 */
  private Integer taskLimit;

  /* 指定的測試間格(s) */
  private Long waitTime;

  /* 放於Http Header中的authorization，通常用於驗證時 */
  private String authorization;

  /* 放於Http Header中的content-type，常用如application/json */
  private String contentType;

  /* 此項亦放於Http Header，考慮是否在contentType有值時自動同步帶入 */
  private String accept;

  /* 用來存其餘自定義的Header */
  private Map<String, Object> otherHeadersParams;

  /* 用來存其餘自定義的參數 */
  private Map<String, Object> otherParams;

  /* 讀取的json檔案路徑 */
  private String filePath;

  /* 此為規劃中的功能-用來標記執行的順序，自1開始 */
  private Integer runOrder;

  /* 此為規劃中的功能-用來哪些請求時需要本次API測試裡將會返回的key/value值，使用包含自己在內的runOrder作為此項的key */
  private Map<Integer, Object> requestParamKeys;  

}
