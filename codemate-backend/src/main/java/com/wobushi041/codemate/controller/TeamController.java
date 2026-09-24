package com.wobushi041.codemate.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wobushi041.codemate.common.BaseResponse;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.common.ResultUtils;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.model.domain.Team;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.domain.UserTeam;
import com.wobushi041.codemate.model.dto.TeamQuery;
import com.wobushi041.codemate.model.request.TeamAddRequest;
import com.wobushi041.codemate.model.request.TeamJoinRequest;
import com.wobushi041.codemate.model.request.TeamQuitRequest;
import com.wobushi041.codemate.model.request.TeamUpdateRequest;
import com.wobushi041.codemate.model.vo.TeamUserVO;
import com.wobushi041.codemate.service.TeamService;
import com.wobushi041.codemate.service.UserService;
import com.wobushi041.codemate.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 队伍管理控制层
 *
 * @author wobushi041
 */
@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入队伍服务依赖
     */
    @Resource
    private TeamService teamService;

    /**
     * 注入用户队伍关联服务依赖
     */
    @Resource
    private UserTeamService userTeamService;

    /// 队伍基础管理相关接口 ///

    /**
     * 创建队伍接口
     *
     * @param teamAddRequest 创建队伍请求参数
     * @param request        HTTP 请求对象
     * @return 新创建队伍的 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        // 参数检查
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户并执行队伍创建
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long result = teamService.addTeam(team, loginUserFromRequest);
        return ResultUtils.success(result);
    }

    /**
     * 解散并删除队伍接口
     *
     * @param id      待删除队伍的 id
     * @param request HTTP 请求对象
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody Long id, HttpServletRequest request) {
        // 参数检查
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户并执行队伍删除
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        boolean result = teamService.deleteTeam(id, loginUserFromRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "插入失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 更新队伍信息接口
     *
     * @param teamUpdateRequest 更新队伍请求参数
     * @param request           HTTP 请求对象
     * @return 是否更新成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        // 参数检查
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍更新信息不能为空");
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未登录");
        }

        // 获取当前登录用户并执行队伍信息更新
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUserFromRequest);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    /// 队伍查询相关接口 ///

    /**
     * 根据 id 获取队伍详情接口
     *
     * @param id 队伍 id
     * @return 队伍实体信息
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(@RequestParam(value = "id") long id) {
        // 参数检查
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询队伍信息并校验是否存在
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(team);
    }

    /**
     * 条件查询队伍列表接口（附带当前用户加入状态与队伍已加入人数统计）
     *
     * @param teamQuery 队伍查询条件封装对象
     * @param request   HTTP 请求对象
     * @return 队伍及成员状态视图列表
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(@ParameterObject TeamQuery teamQuery, HttpServletRequest request) {
        // 参数检查与管理员权限判定
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);

        // 查询符合条件的队伍列表
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);
        if (teamList == null || CollectionUtils.isEmpty(teamList)) {
            return ResultUtils.success(new ArrayList<>());
        }

        // 标注当前登录用户是否已加入查询出的各个队伍
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        try {
            User loginUserFromRequest = userService.getLoginUserFromRequest(request);
            queryWrapper.in("teamId", teamIdList);
            queryWrapper.eq("userId", loginUserFromRequest.getId());
            List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
        }

        // 统计并填充每个队伍的当前已加入成员数量
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        List<UserTeam> userTeamJoinList = userTeamService.list(userTeamJoinQueryWrapper);
        Map<Long, List<UserTeam>> teamIdUserTeamMap = userTeamJoinList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team -> {
            team.setHasJoinNum(teamIdUserTeamMap.getOrDefault(team.getId(), new ArrayList<>()).size());
        });
        return ResultUtils.success(teamList);
    }

    /**
     * 分页查询队伍列表接口
     *
     * @param teamQuery 队伍分页查询条件封装对象
     * @return 队伍分页查询结果
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(@ParameterObject TeamQuery teamQuery) {
        // 参数检查
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 构造分页与查询条件并执行分页查询
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    /// 队伍成员变更与个人队伍相关接口 ///

    /**
     * 加入队伍接口
     *
     * @param teamJoinRequest 加入队伍请求参数
     * @param request         HTTP 请求对象
     * @return 是否加入成功
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        // 参数检查
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户并执行加入队伍逻辑
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUserFromRequest);
        return ResultUtils.success(result);
    }

    /**
     * 退出队伍接口
     *
     * @param teamQuitRequest 退出队伍请求参数
     * @param request         HTTP 请求对象
     * @return 是否退出成功
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        // 参数检查
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求体不能为空");
        }

        // 获取当前登录用户并执行退出队伍逻辑
        User loginUserFromRequest = userService.getLoginUserFromRequest(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUserFromRequest);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户创建的队伍列表接口
     *
     * @param teamQuery 队伍查询条件封装对象
     * @param request   HTTP 请求对象
     * @return 当前用户创建的队伍视图列表
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyCreateTeams(@ParameterObject TeamQuery teamQuery, HttpServletRequest request) {
        // 参数检查
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户并查询其创建的队伍列表
        User loginUser = userService.getLoginUserFromRequest(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取当前登录用户已加入的队伍列表接口
     *
     * @param teamQuery 队伍查询条件封装对象
     * @param request   HTTP 请求对象
     * @return 当前用户已加入的队伍视图列表
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(@ParameterObject TeamQuery teamQuery, HttpServletRequest request) {
        // 参数检查
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询当前登录用户关联的所有队伍记录
        User loginUser = userService.getLoginUserFromRequest(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);

        // 提取去重后的队伍 id 列表并查询队伍详情
        Map<Long, List<UserTeam>> listMap = userTeamList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }

}
