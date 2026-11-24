package com.wobushi041.codemate.service.impl;

import com.wobushi041.codemate.exception.BusinessException;
import com.wobushi041.codemate.mapper.UserMapper;
import com.wobushi041.codemate.model.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateUserRejectsPhoneThatIsNotElevenDigits() {
        User loginUser = user(1L, null);
        User updateUser = user(1L, "12");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.updateUser(updateUser, loginUser)
        );

        assertEquals("手机号必须为11位数字", exception.getDescription());
    }

    @Test
    void updateUserAcceptsElevenDigitPhone() {
        User loginUser = user(1L, null);
        User updateUser = user(1L, "13800138000");
        when(userMapper.selectById(1L)).thenReturn(user(1L, null));
        when(userMapper.updateById(updateUser)).thenReturn(1);

        int result = userService.updateUser(updateUser, loginUser);

        assertEquals(1, result);
        assertEquals("13800138000", updateUser.getPhone());
        verify(userMapper).updateById(updateUser);
    }

    private User user(long id, String phone) {
        User user = new User();
        user.setId(id);
        user.setUserRole(0);
        user.setPhone(phone);
        return user;
    }
}
