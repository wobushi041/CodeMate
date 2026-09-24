package com.wobushi041.codemate.once.importuser;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 星球表格用户信息映射模型
 *
 * @author wobushi041
 */
@Data
public class XingQiuTableUserInfo {

    /**
     * 星球成员编号
     */
    @ExcelProperty("成员编号")
    private String planetCode;

    /**
     * 用户昵称
     */
    @ExcelProperty("成员昵称")
    private String username;

}