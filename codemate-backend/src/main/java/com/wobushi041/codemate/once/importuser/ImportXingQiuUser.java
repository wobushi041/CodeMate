package com.wobushi041.codemate.once.importuser;

import com.alibaba.excel.EasyExcel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 星球用户 Excel 导入与去重分析工具类
 *
 * @author wobushi041
 */
public class ImportXingQiuUser {

    /**
     * 读取星球用户生产 Excel 文件并统计不重复用户昵称数量
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        // TODO: 记得改为自己的测试文件
        String fileName = "src/main/resources/prodExcel.xlsx";

        // 同步读取首个工作表的全部星球用户数据
        List<XingQiuTableUserInfo> userInfoList =
                EasyExcel.read(fileName).head(XingQiuTableUserInfo.class).sheet().doReadSync();
        System.out.println("总数 = " + userInfoList.size());

        // 过滤空昵称记录并按用户昵称分组统计重复项
        Map<String, List<XingQiuTableUserInfo>> listMap =
                userInfoList.stream()
                        .filter(userInfo -> StringUtils.isNotEmpty(userInfo.getUsername()))
                        .collect(Collectors.groupingBy(XingQiuTableUserInfo::getUsername));
        for (Map.Entry<String, List<XingQiuTableUserInfo>> stringListEntry : listMap.entrySet()) {
            if (stringListEntry.getValue().size() > 1) {
                System.out.println("username = " + stringListEntry.getKey());
                System.out.println("1");
            }
        }
        System.out.println("不重复昵称数 = " + listMap.keySet().size());
    }

}
