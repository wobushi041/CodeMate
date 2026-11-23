package com.wobushi041.codemate.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wobushi041.codemate.common.BaseResponse;
import com.wobushi041.codemate.model.domain.User;
import com.wobushi041.codemate.model.domain.UserTeam;
import com.wobushi041.codemate.model.dto.TeamQuery;
import com.wobushi041.codemate.model.vo.TeamUserVO;
import com.wobushi041.codemate.service.TeamService;
import com.wobushi041.codemate.service.UserService;
import com.wobushi041.codemate.service.UserTeamService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 队伍控制层单元测试。
 */
class TeamControllerTest {

    private final UserService userService = mock(UserService.class);
    private final TeamService teamService = mock(TeamService.class);
    private final UserTeamService userTeamService = mock(UserTeamService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final TeamController teamController = new TeamController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(teamController, "userService", userService);
        ReflectionTestUtils.setField(teamController, "teamService", teamService);
        ReflectionTestUtils.setField(teamController, "userTeamService", userTeamService);
    }

    @Test
    void createdTeamsReturnActualMemberCount() {
        User owner = user(1L);
        TeamUserVO team = team(100L);
        when(userService.getLoginUserFromRequest(request)).thenReturn(owner);
        when(teamService.listTeams(any(TeamQuery.class), eq(true))).thenReturn(List.of(team));
        when(userTeamService.list(any(Wrapper.class))).thenReturn(List.of(
                membership(1L, 100L),
                membership(2L, 100L)
        ));

        BaseResponse<List<TeamUserVO>> response =
                teamController.listMyCreateTeams(new TeamQuery(), request);

        assertEquals(2, response.getData().get(0).getHasJoinNum());
        assertTrue(response.getData().get(0).isHasJoin());
    }

    @Test
    void joinedTeamsReturnSameActualMemberCount() {
        User member = user(2L);
        TeamUserVO team = team(100L);
        TeamQuery query = new TeamQuery();
        when(userService.getLoginUserFromRequest(request)).thenReturn(member);
        when(userTeamService.list(any(Wrapper.class))).thenReturn(
                List.of(membership(2L, 100L)),
                List.of(membership(1L, 100L), membership(2L, 100L))
        );
        when(teamService.listTeams(any(TeamQuery.class), eq(true))).thenReturn(List.of(team));

        BaseResponse<List<TeamUserVO>> response = teamController.listMyJoinTeams(query, request);

        assertEquals(List.of(100L), query.getIdList());
        assertEquals(2, response.getData().get(0).getHasJoinNum());
        assertTrue(response.getData().get(0).isHasJoin());
    }

    @Test
    void joinedTeamsReturnEmptyListWhenUserHasNoMemberships() {
        when(userService.getLoginUserFromRequest(request)).thenReturn(user(3L));
        when(userTeamService.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

        BaseResponse<List<TeamUserVO>> response =
                teamController.listMyJoinTeams(new TeamQuery(), request);

        assertTrue(response.getData().isEmpty());
        verify(teamService, never()).listTeams(any(TeamQuery.class), anyBoolean());
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private TeamUserVO team(long id) {
        TeamUserVO team = new TeamUserVO();
        team.setId(id);
        return team;
    }

    private UserTeam membership(long userId, long teamId) {
        UserTeam membership = new UserTeam();
        membership.setUserId(userId);
        membership.setTeamId(teamId);
        return membership;
    }
}
