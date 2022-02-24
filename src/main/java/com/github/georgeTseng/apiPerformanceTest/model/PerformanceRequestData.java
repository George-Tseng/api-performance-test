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

  private String url;
  private SupportedHttpMethod httpMethod;
  private Integer taskLimit;
  private Long waitTime;
  private String authorization;
  private String contentType;
  private String accept;
  private Map<String, Object> otherHeadersParams;
  private Map<String, Object> otherParams;
  private String filePath;

}
