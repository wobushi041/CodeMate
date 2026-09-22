package com.wobushi041.codemate.model.dto;

import com.wobushi041.codemate.model.enums.RecommendCacheStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendCacheSnapshot {

    private String redisKey;

    private RecommendCacheStatus status;

    private RecommendCacheValue cacheValue;
}
