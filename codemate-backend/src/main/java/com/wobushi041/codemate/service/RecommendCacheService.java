package com.wobushi041.codemate.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.dto.RecommendCacheSnapshot;

public interface RecommendCacheService {

    Page<User> getRecommendUsers(long pageNum, long pageSize, long userId);

    Page<User> refreshRecommendUsers(long userId, long pageNum, long pageSize);

    RecommendCacheSnapshot refreshRecommendCache(long userId, long pageNum, long pageSize);

    RecommendCacheSnapshot getRecommendCacheSnapshot(long userId, long pageNum, long pageSize);

    long calculateNextDelayMillis(RecommendCacheSnapshot cacheSnapshot);

    String buildRecommendCacheKey(long userId, long pageNum, long pageSize);
}
