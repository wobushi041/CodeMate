package com.wobushi041.codemate.once.importuser;

import com.alibaba.excel.EasyExcel;

import java.util.List;

/**
 * Excel 用户数据读取测试类
 *
 * @author wobushi041
 */
public class ImportExcel {

    /**
     * 执行 Excel 表格数据读取主入口
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        // TODO: 记得改为自己的测试文件
        String fileName = "src/main/resources/testExcel.xlsx";

        // 执行同步读取
        synchronousRead(fileName);
    }

    /**
     * 通过自定义监听器逐行解析 Excel 文件数据
     *
     * @param fileName 待读取的 Excel 文件路径
     */
    public static void readByListener(String fileName) {
        // 注册 TableListener 监听器并读取首个工作表
        EasyExcel.read(fileName, XingQiuTableUserInfo.class, new TableListener()).sheet().doRead();
    }

    /**
     * 同步读取 Excel 文件全部数据并打印输出
     *
     * @param fileName 待读取的 Excel 文件路径
     */
    public static void synchronousRead(String fileName) {
        // 指定表头映射类并同步读取首个工作表的全部数据
        List<XingQiuTableUserInfo> totalDataList =
                EasyExcel.read(fileName).head(XingQiuTableUserInfo.class).sheet().doReadSync();

        // 遍历打印每条解析出的表格用户信息
        for (XingQiuTableUserInfo xingQiuTableUserInfo : totalDataList) {
            System.out.println(xingQiuTableUserInfo);
        }
    }

}
