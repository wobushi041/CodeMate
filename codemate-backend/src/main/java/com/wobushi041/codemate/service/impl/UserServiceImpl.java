package com.wobushi041.codemate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.contant.UserConstant;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.mapper.UserMapper;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.service.UserService;
import com.wobushi041.codemate.utils.AlgorithmUtils;
import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.wobushi041.codemate.contant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现
 *
 * @author wobushi041
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 密码加密混淆盐值
     */
    private static final String SALT = "041";

    /**
     * 注入用户持久层依赖
     */
    @Resource
    private UserMapper userMapper;

    /**
     * 注入 Redisson 客户端依赖
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 校验账号与星球编号唯一性并通过 MD5 加盐加密后写入 MySQL 用户表
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode    星球编号
     * @return 新注册用户的 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 校验账号特殊字符与两次输入密码一致性
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码不相同");
        }

        // 查询数据库校验用户账号与星球编号是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编号重复");
        }

        // 使用 MD5 加盐加密密码并保存新用户记录至数据库
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }

    /**
     * 校验 MD5 加盐密码匹配性并将脱敏后的用户信息写入 HTTP Session 登录态
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP 请求对象
     * @return 脱敏后的登录用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 校验账号格式并对输入密码执行 MD5 加盐摘要计算
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 查询数据库验证账号与加密密码是否匹配
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }

        // 执行用户信息脱敏并写入 Session 登录态
        User safetyUser = getSafetyUser(user);
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 剥离用户密码等敏感字段并构造全新的脱敏用户对象
     *
     * @param originUser 原始用户实体
     * @return 脱敏后的用户实体
     */
    @Override
    public User getSafetyUser(User originUser) {
        // 判空并拷贝公开的非敏感用户属性
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    /**
     * 从 HTTP Session 中读取当前登录用户属性并校验登录状态
     *
     * @param request HTTP 请求对象
     * @return 当前登录用户实体
     */
    @Override
    public User getLoginUserFromRequest(HttpServletRequest request) {
        // 从 Session 获取登录态对象并校验非空
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户未登录");
        }
        return (User) userObj;
    }

    /**
     * 从 HTTP Session 中移除当前用户的登录态属性
     *
     * @param request HTTP 请求对象
     * @return 注销操作成功标识
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除 Session 中的用户登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 基于 MySQL LIKE 模糊查询拼接标签条件检索用户并执行脱敏处理
     *
     * @param tagNamelist 标签名称列表
     * @return 脱敏后的匹配用户列表
     */
    @Override
    public List<User> searchUsersByTagId(List<String> tagNamelist) {
        // 初始化查询包装器并拼接每个标签的 LIKE 查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : tagNamelist) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }

        // 执行数据库查询并将结果集映射为脱敏用户列表
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream()
                .map(this::getSafetyUser)
                .collect(Collectors.toList());
    }

    /**
     * 全量查询 MySQL 用户并在内存中通过 Gson 反序列化与并行流筛选包含目标标签的用户
     *
     * @param tagNameList 标签名称列表
     * @return 脱敏后的匹配用户列表
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        // 校验标签列表是否为空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询数据库所有用户并在内存中通过并行流解析 JSON 标签集合进行匹配
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        return userList.parallelStream()
                .filter(user -> {
                    String tagsStr = user.getTags();
                    if (StringUtils.isBlank(tagsStr)) {
                        return false;
                    }
                    Set<String> tmpTagNameList = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {}.getType());
                    for (String tagName : tagNameList) {
                        if (!tmpTagNameList.contains(tagName)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(this::getSafetyUser)
                .collect(Collectors.toList());
    }

    /**
     * 从 HTTP Session 提取登录用户并比对用户角色是否为管理员常量值
     *
     * @param request HTTP 请求对象
     * @return 是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 尝试从请求中获取登录用户并校验管理员角色
        try {
            User user = this.getLoginUserFromRequest(request);
            return user.getUserRole() == UserConstant.ADMIN_ROLE;
        } catch (BusinessException e) {
            return false;
        }
    }

    /**
     * 校验给定用户实体非空且角色标识等于管理员常量值
     *
     * @param loginUser 当前登录用户
     * @return 是否为管理员
     */
    @Override
    public boolean isAdmin(User loginUser) {
        // 判断用户非空且角色为管理员
        return loginUser != null && loginUser.getUserRole() == UserConstant.ADMIN_ROLE;
    }

    /**
     * 优先从 Redisson 缓存获取匹配伙伴列表，未命中时执行多算法相似度匹配并缓存
     *
     * @param num       期望匹配的用户数量
     * @param loginUser 当前登录用户
     * @return 脱敏后的高匹配度用户列表
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<User> matchUsers(long num, User loginUser) {
        // 检查 Redisson 缓存中是否已存在当前用户的匹配结果
        Long loginUserId = loginUser.getId();
        String redisKey = buildMatchUsersCacheKey(loginUserId);
        RBucket<Object> bucket = redissonClient.getBucket(redisKey);
        Object cachedValue = bucket.get();
        if (cachedValue instanceof List) {
            return (List<User>) cachedValue;
        }

        // 缓存未命中时回源执行标签相似度计算并写入缓存
        return queryAndCacheMatchUsers(num, loginUserId, bucket);
    }

    /**
     * 跳过缓存直接执行多算法标签相似度匹配并将最新结果更新至 Redisson 缓存
     *
     * @param num       期望匹配的用户数量
     * @param loginUser 当前登录用户
     * @return 脱敏后的高匹配度用户列表
     */
    @Override
    public List<User> matchUsersWithoutRedis(long num, User loginUser) {
        // 构造 Redisson 缓存桶并强制重新计算匹配用户列表
        Long loginUserId = loginUser.getId();
        String redisKey = buildMatchUsersCacheKey(loginUserId);
        RBucket<Object> bucket = redissonClient.getBucket(redisKey);
        return queryAndCacheMatchUsers(num, loginUserId, bucket);
    }

    /**
     * 查询带标签候选用户并计算综合标签相似度，取前 N 名脱敏后写入 Redisson 缓存
     *
     * @param num         期望匹配的用户数量
     * @param loginUserId 当前登录用户 id
     * @param bucket      Redisson 缓存桶对象
     * @return 按相似度降序排列的脱敏用户列表
     */
    private List<User> queryAndCacheMatchUsers(long num, long loginUserId, RBucket<Object> bucket) {
        // 查询当前登录用户最新标签，若无标签则清理缓存并返回空列表
        User loginUser;
        loginUser = this.getById(loginUserId);
        if (loginUser == null || StringUtils.isBlank(loginUser.getTags())) {
            bucket.delete();
            return Collections.emptyList();
        }

        // 仅查询数据库中非空标签用户的 id 与 tags 字段以降低内存开销
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags");
        queryWrapper.select("id", "tags");
        List<User> userList = this.list(queryWrapper);

        // 解析当前登录用户的标签 JSON 列表
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {}.getType());
        if (CollectionUtils.isEmpty(tagList)) {
            bucket.delete();
            return Collections.emptyList();
        }

        // 遍历候选用户并计算综合标签匹配得分
        List<Pair<User, Double>> list = new ArrayList<>();
        for (User user : userList) {
            String userTags = user.getTags();
            if (StringUtils.isBlank(userTags) || Objects.equals(user.getId(), loginUser.getId())) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {}.getType());
            if (CollectionUtils.isEmpty(userTagList)) {
                continue;
            }
            double score = calculateFriendMatchScore(tagList, userTagList);
            list.add(new Pair<>(user, score));
        }

        // 按相似度分数从高到低排序并截取前 num 名用户的 id
        List<Pair<User, Double>> topUserPairList = list.stream()
                .sorted((o1, o2) -> Double.compare(o2.getSecond(), o1.getSecond()))
                .limit(num)
                .collect(Collectors.toList());
        List<Long> userListVo = topUserPairList.stream()
                .map(pair -> pair.getFirst().getId())
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userListVo)) {
            bucket.delete();
            return Collections.emptyList();
        }

        // 批量回查完整用户信息并按匹配度顺序组装脱敏列表写入缓存
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userListVo);
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(this::getSafetyUser)
                .collect(Collectors.groupingBy(User::getId));
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userListVo) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        bucket.set(finalUserList, 7, TimeUnit.DAYS);
        return finalUserList;
    }

    /**
     * 融合编辑距离、Jaccard 相似度、余弦相似度与 IK 分词计算两组标签的加权匹配分数
     *
     * @param list1 第一组用户标签列表
     * @param list2 第二组用户标签列表
     * @return 加权综合相似度分数
     */
    private double calculateFriendMatchScore(List<String> list1, List<String> list2) {
        // 统一转小写并识别中英文混合类型以执行分词预处理
        List<String> resultList1 = list1.stream()
                .filter(Objects::nonNull)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        List<String> resultList2 = list2.stream()
                .filter(Objects::nonNull)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        int strType1 = AlgorithmUtils.getStrType(resultList1);
        int strType2 = AlgorithmUtils.getStrType(resultList2);
        if (strType1 == AlgorithmUtils.MIXED_CHINESE_ENGLISH) {
            resultList1 = AlgorithmUtils.tokenize(resultList1);
        }
        if (strType2 == AlgorithmUtils.MIXED_CHINESE_ENGLISH) {
            resultList2 = AlgorithmUtils.tokenize(resultList2);
        }

        // 分别计算 IK 分词、编辑距离、Jaccard 与余弦相似度并按权重汇总
        double ikScore = calculateIkScore(list1, list2, strType1, strType2);
        int editDistanceScore = AlgorithmUtils.calculateEditDistance(resultList1, resultList2);
        double maxEditDistance = Math.max(resultList1.size(), resultList2.size());
        double editDistance = maxEditDistance == 0 ? 0 : 1 - editDistanceScore / maxEditDistance;
        double jaccardScore = AlgorithmUtils.calculateJaccardSimilarity(resultList1, resultList2);
        double similarityScore = AlgorithmUtils.cosineSimilarity(resultList1, resultList2);
        return editDistance * 0.5 + jaccardScore * 0.3 + similarityScore * 0.2 + ikScore * 0.3;
    }

    /**
     * 提取中文字符并通过 IK 分词器计算两组标签的 Jaccard 语义相似度
     *
     * @param list1    第一组原始标签列表
     * @param list2    第二组原始标签列表
     * @param strType1 第一组标签字符类型
     * @param strType2 第二组标签字符类型
     * @return IK 分词后的 Jaccard 相似度得分
     */
    private double calculateIkScore(List<String> list1, List<String> list2, int strType1, int strType2) {
        // 若任一标签组为纯英文则跳过中文 IK 分词
        if (strType1 == AlgorithmUtils.ENGLISH || strType2 == AlgorithmUtils.ENGLISH) {
            return 0D;
        }

        // 提取中文字符并调用 IK 分词计算 Jaccard 相似度
        try {
            List<String> quotedList1 = list1.stream()
                    .filter(Objects::nonNull)
                    .map(item -> "\"" + item.toLowerCase(Locale.ROOT) + "\"")
                    .collect(Collectors.toList());
            List<String> quotedList2 = list2.stream()
                    .filter(Objects::nonNull)
                    .map(item -> "\"" + item.toLowerCase(Locale.ROOT) + "\"")
                    .collect(Collectors.toList());
            String tags1 = AlgorithmUtils.collectChineseChars(quotedList1);
            String tags2 = AlgorithmUtils.collectChineseChars(quotedList2);
            List<String> analyzedTags1 = AlgorithmUtils.analyzeText(tags1);
            List<String> analyzedTags2 = AlgorithmUtils.analyzeText(tags2);
            return AlgorithmUtils.calculateJaccardSimilarity(analyzedTags1, analyzedTags2);
        } catch (IOException e) {
            log.warn("IK analyze failed, fallback ikScore to 0", e);
            return 0D;
        }
    }

    /**
     * 校验管理员或本人修改权限后通过 MyBatis-Plus 按主键更新数据库用户记录
     *
     * @param user                 待更新的用户信息
     * @param loginUserFromRequest 当前登录用户
     * @return 更新操作影响的数据库记录数
     */
    @Override
    public int updateUser(User user, User loginUserFromRequest) {
        // 校验目标用户 id 与修改权限（仅允许管理员或本人修改）
        long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!isAdmin(loginUserFromRequest) && loginUserFromRequest.getId() != userId) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        // 校验目标用户是否存在并执行主键更新
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    /**
     * 构建指定用户的伙伴匹配结果 Redis 缓存键名
     *
     * @param userId 用户 id
     * @return 伙伴匹配 Redis 缓存键名
     */
    private String buildMatchUsersCacheKey(long userId) {
        // 格式化拼接用户 id 与匹配缓存前缀
        return String.format("user:match:%d", userId);
    }

}
