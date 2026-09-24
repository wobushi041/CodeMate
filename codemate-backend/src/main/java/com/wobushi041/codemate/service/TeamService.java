package com.wobushi041.codemate.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wobushi041.codemate.model.domain.Team;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.dto.TeamQuery;
import com.wobushi041.codemate.model.request.TeamJoinRequest;
import com.wobushi041.codemate.model.request.TeamQuitRequest;
import com.wobushi041.codemate.model.request.TeamUpdateRequest;
import com.wobushi041.codemate.model.vo.TeamUserVO;

import java.util.List;

/**
 * 队伍服务
 *
 * @author wobushi041
 */
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team      待创建的队伍实体
     * @param loginUser 当前登录用户
     * @return 新创建队伍的 id
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍列表
     *
     * @param teamQuery 队伍查询条件封装
     * @param isAdmin   是否为管理员
     * @return 队伍与关联成员视图列表
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest 队伍更新请求参数
     * @param loginUser         当前登录用户
     * @return 是否更新成功
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     *
     * @param teamJoinRequest 加入队伍请求参数
     * @param loginUser       当前登录用户
     * @return 是否加入成功
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     *
     * @param teamQuitRequest      退出队伍请求参数
     * @param loginUserFromRequest 当前登录用户
     * @return 是否退出成功
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUserFromRequest);

    /**
     * 删除（解散）队伍
     *
     * @param id        待解散队伍的 id
     * @param loginUser 当前登录用户
     * @return 是否解散成功
     */
    boolean deleteTeam(Long id, User loginUser);

}
