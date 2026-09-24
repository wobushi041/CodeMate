package com.wobushi041.codemate.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.common.BaseResponse;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.common.ResultUtils;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.request.UserLoginRequest;
import com.wobushi041.codemate.model.request.UserRegisterRequest;
import com.wobushi041.codemate.service.RecommendCacheService;
import com.wobushi041.codemate.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;
import static com.wobushi041.codemate.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Validated
public class UserController {

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入推荐缓存服务依赖
     */
    @Resource
    private RecommendCacheService recommendCacheService;

    /// 认证相关接口 ///

    /**
     * 用户注册接口
     *
     * @param userRegisterRequest 用户注册请求参数
     * @return 新注册用户的 id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody @Validated UserRegisterRequest userRegisterRequest) {
        // 执行用户注册
        long result = userService.userRegister(
                userRegisterRequest.getUserAccount(),
                userRegisterRequest.getUserPassword(),
                userRegisterRequest.getCheckPassword(),
                userRegisterRequest.getPlanetCode()
        );
        return ResultUtils.success(result);
    }

    /**
     * 用户登录接口
     *
     * @param userLoginRequest 用户登录请求参数
     * @param request          HTTP 请求对象
     * @return 脱敏后的登录用户信息
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody @Validated UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 执行用户登录
        User user = userService.userLogin(userLoginRequest.getUserAccount(), userLoginRequest.getUserPassword(), request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销接口
     *
     * @param request HTTP 请求对象
     * @return 注销结果状态码
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        // 参数检查
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        // 执行用户注销
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户接口
     *
     * @param request HTTP 请求对象
     * @return 脱敏后的当前登录用户信息
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        // 获取并校验登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 查询最新用户信息并脱敏
        long userId = currentUser.getId();
        // TODO: 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /// 用户管理相关接口 ///

    /**
     * 根据用户名搜索用户列表接口
     *
     * @param username 用户名关键字
     * @param request  HTTP 请求对象
     * @return 脱敏后的匹配用户列表
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(@RequestParam(value = "username") String username, HttpServletRequest request) {
        // 权限校验
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }

        // 构造查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }

        // 执行查询并脱敏返回
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream()
                .map(user -> userService.getSafetyUser(user))
                .collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 根据标签列表搜索用户接口
     *
     * @param tagNameList 标签名称列表（可选参数，未携带时绑定为 null）
     * @return 脱敏后的匹配用户列表
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTagId(@RequestParam(required = false, value = "tagNameList") List<String> tagNameList) {
        // 参数检查
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }

        // 根据标签查询用户
        List<User> userList = userService.searchUsersByTagId(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 删除用户接口
     *
     * @param id      待删除的用户 id
     * @param request HTTP 请求对象
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestParam(value = "id") @Min(value = 1, message = "id 非法") long id, HttpServletRequest request) {
        // 权限校验
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        // 执行逻辑删除
        boolean result = userService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息接口
     *
     * @param user    待更新的用户信息
     * @param request HTTP 请求对象
     * @return 更新记录行数
     */
    @PostMapping("/update")
    public BaseResponse<Long> updateUser(@RequestBody User user, HttpServletRequest request) {
        // 获取当前登录用户并执行更新
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        long result = userService.updateUser(user, loginUserFromRequest);
        return ResultUtils.success(result);
    }

    /// 推荐与匹配相关接口 ///

    /**
     * 获取推荐用户分页列表接口（优先读取 Redis 缓存）
     *
     * @param pageSize 每页显示的用户数
     * @param pageNum  请求的页码
     * @param request  HTTP 请求对象
     * @return 脱敏后的推荐用户分页列表
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(@RequestParam(value = "pageSize") long pageSize, @RequestParam(value = "pageNum") long pageNum, HttpServletRequest request) {
        // 获取当前登录用户并查询推荐缓存
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        Page<User> userPage = recommendCacheService.getRecommendUsers(pageNum, pageSize, loginUserFromRequest.getId());
        return ResultUtils.success(userPage);
    }

    /**
     * 获取智能匹配用户列表接口（基于 Redis 缓存）
     *
     * @param num     期望匹配的用户数量
     * @param request HTTP 请求对象
     * @return 匹配度最高的用户列表
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(@RequestParam(value = "num") long num, HttpServletRequest request) {
        // 参数检查
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "匹配人数必须在 1 到 20 之间");
        }

        // 获取当前登录用户并执行匹配
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        List<User> matchUsers = userService.matchUsers(num, loginUserFromRequest);
        return ResultUtils.success(matchUsers);
    }

    /**
     * 获取智能匹配用户列表接口（不使用 Redis 缓存）
     *
     * @param num     期望匹配的用户数量
     * @param request HTTP 请求对象
     * @return 匹配度最高的用户列表
     */
    @GetMapping("/match/withoutRedis")
    public BaseResponse<List<User>> matchUsersWithoutRedis(@RequestParam(value = "num") long num, HttpServletRequest request) {
        // 参数检查
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "匹配人数必须在 1 到 20 之间");
        }

        // 获取当前登录用户并执行无缓存匹配
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        List<User> matchUsers = userService.matchUsersWithoutRedis(num, loginUserFromRequest);
        return ResultUtils.success(matchUsers);
    }

}
