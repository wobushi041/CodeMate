package com.wobushi041.codemate.model.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.model.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 推荐缓存值数据对象
 *
 * @author wobushi041
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendCacheValue implements Serializable {

    /**
     * 用户推荐分页数据
     */
    private Page<User> userPage;

    /**
     * 逻辑过期时间戳（毫秒）
     */
    private Long logicExpireTime;

    /// 序列化字段 ///
    private static final long serialVersionUID = 8743980324169666342L;

}
