package com.wobushi041.codemate.once.importuser;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;

/**
 * Excel 星球用户表格读取监听器
 *
 * @author wobushi041
 */
@Slf4j
public class TableListener implements ReadListener<XingQiuTableUserInfo> {

    /**
     * 逐行解析 Excel 数据时触发回调处理
     *
     * @param data    当前行解析出的星球用户信息对象
     * @param context EasyExcel 解析上下文对象
     */
    @Override
    public void invoke(XingQiuTableUserInfo data, AnalysisContext context) {
        // 打印当前行解析出的用户数据
        System.out.println(data);
    }

    /**
     * 全部 Excel 数据解析完成后触发收尾回调
     *
     * @param context EasyExcel 解析上下文对象
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 打印解析完成提示信息
        System.out.println("已解析完成");
    }

}
