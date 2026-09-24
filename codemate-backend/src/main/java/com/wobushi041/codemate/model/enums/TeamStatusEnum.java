package com.wobushi041.codemate.model.enums;

/**
 * 队伍状态枚举
 *
 * @author wobushi041
 */
public enum TeamStatusEnum {

    /**
     * 公开队伍状态枚举实例
     */
    PUBLIC(0, "公开"),

    /**
     * 私有队伍状态枚举实例
     */
    PRIVATE(1, "私有"),

    /**
     * 加密队伍状态枚举实例
     */
    SECRET(2, "加密"),

    ;

    /**
     * 状态码值
     */
    private int value;

    /**
     * 状态描述文本
     */
    private String text;

    /**
     * 内部状态构造方法
     *
     * @param value 状态码值
     * @param text  状态描述文本
     */
    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据状态码值获取对应的队伍状态枚举
     *
     * @param value 状态码值
     * @return 对应的队伍状态枚举（未匹配时返回 null）
     */
    public static TeamStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values) {
            if (teamStatusEnum.getValue() == value) {
                return teamStatusEnum;
            }
        }
        return null;
    }

    /**
     * 获取状态码值
     *
     * @return 状态码值
     */
    public int getValue() {
        return value;
    }

    /**
     * 设置状态码值
     *
     * @param value 状态码值
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * 获取状态描述文本
     *
     * @return 状态描述文本
     */
    public String getText() {
        return text;
    }

    /**
     * 设置状态描述文本
     *
     * @param text 状态描述文本
     */
    public void setText(String text) {
        this.text = text;
    }

}