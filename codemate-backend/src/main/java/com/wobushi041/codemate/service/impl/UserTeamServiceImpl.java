package com.wobushi041.codemate.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wobushi041.codemate.mapper.UserTeamMapper;
import com.wobushi041.codemate.model.domain.UserTeam;
import com.wobushi041.codemate.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
 * 用户队伍关联服务实现
 *
 * @author wobushi041
 */
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
        implements UserTeamService {

}
