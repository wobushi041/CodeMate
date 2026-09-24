package com.wobushi041.codemate.chat;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 聊天室连接通道管理器单元测试
 *
 * @author wobushi041
 */
class ChatChannelManagerTest {

    /**
     * 测试绑定用户 id 到通道并在通道移除时清理在线状态
     */
    // 场景：测试用户加入房间后绑定 Channel 并在离开房间后清理在线状态
    @Test
    void bindsUserIdToChannelAndRemovesItWhenChannelIsRemoved() {
        // 1. 准备测试数据
        ChatChannelManager chatChannelManager = new ChatChannelManager();
        EmbeddedChannel channel = new EmbeddedChannel();

        // 2. 调用待测方法执行加入房间与离开房间
        chatChannelManager.joinRoom(1L, 41L, channel);
        assertTrue(chatChannelManager.isOnline(1L, 41L));
        assertSame(channel, chatChannelManager.get(1L, 41L));
        chatChannelManager.leaveRoom(1L, 41L, channel);

        // 3. 断言结果
        assertFalse(chatChannelManager.isOnline(1L, 41L));
    }

    /**
     * 测试最后一个用户离开后自动移除空房间
     */
    // 场景：测试房间内最后一名成员离开后清空房间通道集合
    @Test
    void removesEmptyRoomAfterLastUserLeaves() {
        // 1. 准备测试数据
        ChatChannelManager chatChannelManager = new ChatChannelManager();
        EmbeddedChannel channel = new EmbeddedChannel();

        // 2. 调用待测方法
        chatChannelManager.joinRoom(1L, 41L, channel);
        chatChannelManager.leaveRoom(1L, 41L, channel);

        // 3. 断言结果
        assertTrue(chatChannelManager.getRoomChannels(1L).isEmpty());
    }

    /**
     * 测试同一房间内重复加入时替换并关闭旧通道
     */
    // 场景：测试同一用户在同一房间建立新连接时替换并关闭旧 Channel
    @Test
    void replacesExistingUserChannelInSameRoom() {
        // 1. 准备测试数据
        ChatChannelManager chatChannelManager = new ChatChannelManager();
        EmbeddedChannel oldChannel = new EmbeddedChannel();
        EmbeddedChannel newChannel = new EmbeddedChannel();

        // 2. 调用待测方法
        chatChannelManager.joinRoom(1L, 41L, oldChannel);
        chatChannelManager.joinRoom(1L, 41L, newChannel);

        // 3. 断言结果
        assertSame(newChannel, chatChannelManager.get(1L, 41L));
        assertNull(oldChannel.closeFuture().cause());
        assertFalse(oldChannel.isOpen());
    }

}
