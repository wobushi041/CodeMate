package com.wobushi041.codemate.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求对象
 *
 * @author wobushi041
 */
@Data
public class PageRequest implements Serializable {

    /**
     * 当前页码
     */
    protected int pageNum = 1;

    /**
     * 每页记录数
     */
    protected int pageSize = 10;

    /// 序列化字段 ///
    private static final long serialVersionUID = 6218947034899114472L;

}
