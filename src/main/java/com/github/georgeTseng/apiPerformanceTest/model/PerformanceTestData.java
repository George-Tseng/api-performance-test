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
public class PerformanceTestData {

  private Long operateTime;
  private Integer statusCode;
  private Map<String, Object> responseDatas;

}
