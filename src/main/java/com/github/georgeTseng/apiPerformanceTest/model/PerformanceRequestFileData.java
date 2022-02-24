package com.github.georgeTseng.apiPerformanceTest.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceRequestFileData {

  private String url;
  private String httpMethod;
  private String taskLimit;
  private String waitTime;
  private String authorization;
  private String contentType;
  private String accept;
  private Map<String, Object> otherHeadersParams;
  private Map<String, Object> otherParams;

}
