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
public class PerformanceResultFileData {

  private Map<Integer, PerformanceTestData> performanceTestResults;
  private Integer totalCount;
  private Long averageOperateTime;
  private Long bestOperateTime;
  private Long worstOperateTime;
  private Integer okCount;
  private Integer ngCount;
  private Double okPercent;

}
