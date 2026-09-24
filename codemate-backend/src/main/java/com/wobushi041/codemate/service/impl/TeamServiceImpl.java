package com.wobushi041.codemate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wobushi041.codemate.common.ErrorCode;
import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.mapper.TeamMapper;
import com.wobushi041.codemate.model.domain.Team;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.domain.UserTeam;
import com.wobushi041.codemate.model.dto.TeamQuery;
import com.wobushi041.codemate.model.enums.TeamStatusEnum;
import com.wobushi041.codemate.model.request.TeamJoinRequest;
import com.wobushi041.codemate.model.request.TeamQuitRequest;
import com.wobushi041.codemate.model.request.TeamUpdateRequest;
import com.wobushi041.codemate.model.vo.TeamUserVO;
import com.wobushi041.codemate.model.vo.UserVO;
import com.wobushi041.codemate.service.TeamService;
import com.wobushi041.codemate.service.UserService;
import com.wobushi041.codemate.service.UserTeamService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 队伍服务实现
 *
 * @author wobushi041
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    /**
     * 注入用户队伍关联服务依赖
     */
    @Resource
    private UserTeamService userTeamService;

    /**
     * 注入用户服务依赖
     */
    @Resource
    private UserService userService;

    /**
     * 注入 Redisson 客户端依赖
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 校验队伍参数并在事务中写入队伍主表与用户队伍关系表
     *
     * @param team      待创建的队伍实体
     * @param loginUser 当前登录用户
     * @return 新创建队伍的 id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 校验请求参数与当前登录状态
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        final long userId = loginUser.getId();

        // 校验队伍人数上限、标题长度与描述长度
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }

        // 校验队伍公开状态及加密房间密码规则
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }

        // 校验队伍过期时间是否晚于当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }

        // 校验当前用户已创建队伍数量是否超过 5 个上限
        // TODO: 存在并发创建超限问题，待引入分布式锁控制并发创建
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建 5 个队伍");
        }

        // 插入队伍信息至队伍主表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

        // 插入创建人与队伍的关联记录至关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return teamId;
    }

    /**
     * 构建动态查询条件从 MySQL 检索未过期队伍并关联脱敏创建人信息
     *
     * @param teamQuery 队伍查询条件封装
     * @param isAdmin   当前用户是否为管理员
     * @return 队伍与关联创建人视图列表
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        // 组装队伍动态筛选查询条件
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            Integer status = teamQuery.getStatus();
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "非管理员无权查看私有队伍");
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }

        // 过滤已过期的队伍并执行数据库查询
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        // 关联查询队伍创建人信息并组装脱敏视图列表
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    /**
     * 校验操作权限与房间状态规则并按主键更新 MySQL 队伍记录
     *
     * @param teamUpdateRequest 队伍更新请求参数
     * @param loginUser         当前登录用户
     * @return 是否更新成功
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        // 校验更新参数与目标队伍存在性
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }

        // 校验当前操作者是否为队长或管理员
        if (oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        // 校验变更后的队伍状态与加密房间密码规则
        Integer status = teamUpdateRequest.getStatus();
        if (status != null) {
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不合法");
            }
            if (statusEnum.equals(TeamStatusEnum.SECRET) && StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须要设置密码");
            }
        }

        // 填充非空更新字段并执行主键更新
        Team updateTeam = new Team();
        updateTeam.setId(id);
        if (teamUpdateRequest.getName() != null) {
            updateTeam.setName(teamUpdateRequest.getName());
        }
        if (teamUpdateRequest.getDescription() != null) {
            updateTeam.setDescription(teamUpdateRequest.getDescription());
        }
        if (teamUpdateRequest.getExpireTime() != null) {
            updateTeam.setExpireTime(teamUpdateRequest.getExpireTime());
        }
        if (teamUpdateRequest.getStatus() != null) {
            updateTeam.setStatus(teamUpdateRequest.getStatus());
        }
        if (teamUpdateRequest.getPassword() != null) {
            updateTeam.setPassword(teamUpdateRequest.getPassword());
        }
        return this.updateById(updateTeam);
    }

    /**
     * 基于 Redisson 分布式锁校验加队上限与房间容量并写入用户队伍关联记录
     *
     * @param teamJoinRequest 加入队伍请求参数
     * @param loginUser       当前登录用户
     * @return 是否加入成功
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        // 校验请求参数及目标队伍的有效期、公开状态与访问密码
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }

        // 获取 Redisson 分布式锁，串行校验用户加队数量、重复加队与队伍满员状态
        long userId = loginUser.getId();
        RLock lock = redissonClient.getLock("codemate:joinTeam:join_team");
        try {
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinNum > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                    }
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 仅释放当前线程持有的分布式锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    /**
     * 在事务中处理成员退出、单人队伍自动解散或队长顺位转移并移除关联关系
     *
     * @param teamQuitRequest      退出队伍请求参数
     * @param loginUserFromRequest 当前登录用户
     * @return 是否退出成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUserFromRequest) {
        // 校验退队请求参数、队伍存在性及当前用户的入队状态
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        long userId = loginUserFromRequest.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("teamId", teamId);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }

        // 根据队伍剩余人数执行解散队伍或队长顺位转让
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        if (teamHasJoinNum == 1) {
            this.removeById(teamId);
        }
        // 队伍剩余多人且当前退出者为队长时，将队长职位转移给最早加入的下一位成员
        else {
            if (team.getUserId() == userId) {
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }

        // 移除当前用户与队伍的关联关系
        return userTeamService.remove(queryWrapper);
    }

    /**
     * 校验队长权限并在事务中级联删除用户队伍关联记录与队伍主表记录
     *
     * @param id        待解散队伍的 id
     * @param loginUser 当前登录用户
     * @return 是否解散成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteTeam(Long id, User loginUser) {
        // 校验队伍是否存在及当前用户是否为队长
        Team team = getTeamById(id);
        long teamId = team.getId();
        if (team.getUserId() != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH, "你不是创建者，无访问权限");
        }

        // 移除该队伍下所有成员关联记录并删除队伍主记录
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(queryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }
        return this.removeById(teamId);
    }

    /**
     * 根据主键查询队伍实体并校验其存在性
     *
     * @param teamId 队伍 id
     * @return 队伍实体对象
     */
    private Team getTeamById(Long teamId) {
        // 校验队伍 id 合法性并查询数据库
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 统计指定队伍在用户队伍关系表中的当前成员总数
     *
     * @param teamId 队伍 id
     * @return 队伍当前成员总数
     */
    private long countTeamUserByTeamId(long teamId) {
        // 按队伍 id 统计关联表记录数
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }

}
