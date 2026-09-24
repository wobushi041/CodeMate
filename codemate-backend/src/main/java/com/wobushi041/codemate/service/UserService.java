package com.wobushi041.codemate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wobushi041.codemate.model.domain.User;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
 *
 * @author wobushi041
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新注册用户的 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP 请求对象
     * @return 脱敏后的登录用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 更新用户信息
     *
     * @param user      待更新的用户信息
     * @param loginUser 当前登录用户
     * @return 更新影响的记录行数
     */
    int updateUser(User user, User loginUser);

    /**
     * 用户注销登录
     *
     * @param request HTTP 请求对象
     * @return 注销操作结果标识
     */
    int userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的安全用户信息
     *
     * @param originUser 原始用户实体
     * @return 脱敏后的用户实体
     */
    User getSafetyUser(User originUser);

    /**
     * 从当前请求中获取登录用户信息
     *
     * @param request HTTP 请求对象
     * @return 当前登录用户实体
     */
    User getLoginUserFromRequest(HttpServletRequest request);

    /**
     * 根据标签列表搜索匹配的用户列表
     *
     * @param tagNamelist 标签名称列表
     * @return 脱敏后的匹配用户列表
     */
    List<User> searchUsersByTagId(List<String> tagNamelist);

    /**
     * 按全量标签集合筛选匹配的用户列表
     *
     * @param tagNameList 标签名称列表
     * @return 脱敏后的匹配用户列表
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 根据当前请求判断操作用户是否为管理员
     *
     * @param request HTTP 请求对象
     * @return 是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 判断指定用户是否为管理员
     *
     * @param loginUser 当前登录用户
     * @return 是否为管理员
     */
    boolean isAdmin(User loginUser);

    /**
     * 获取与当前登录用户兴趣标签最匹配的推荐伙伴列表
     *
     * @param num       期望匹配的用户数量
     * @param loginUser 当前登录用户
     * @return 脱敏后的高匹配度用户列表
     */
    List<User> matchUsers(long num, User loginUser);

    /**
     * 强制重新计算并获取与当前登录用户最匹配的推荐伙伴列表
     *
     * @param num       期望匹配的用户数量
     * @param loginUser 当前登录用户
     * @return 脱敏后的高匹配度用户列表
     */
    List<User> matchUsersWithoutRedis(long num, User loginUser);

}
