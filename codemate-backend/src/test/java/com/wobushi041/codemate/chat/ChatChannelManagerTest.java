package com.wobushi041.codemate.chat;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatChannelManagerTest {

    @Test
    void bindsUserIdToChannelAndRemovesItWhenChannelIsRemoved() {
        ChatChannelManager chatChannelManager = new ChatChannelManager();
        EmbeddedChannel channel = new EmbeddedChannel();

        chatChannelManager.joinRoom(1L, 41L, channel);

        assertTrue(chatChannelManager.isOnline(1L, 41L));
        assertSame(channel, chatChannelManager.get(1L, 41L));

        chatChannelManager.leaveRoom(1L, 41L, channel);

        assertFalse(chatChannelManager.isOnline(1L, 41L));
    }

    @Test
    void removesEmptyRoomAfterLastUserLeaves() {
        ChatChannelManager chatChannelManager = new ChatChannelManager();
        EmbeddedChannel channel = new EmbeddedChannel();

        chatChannelManager.joinRoom(1L, 41L, channel);
        chatChannelManager.leaveRoom(1L, 41L, channel);

        assertTrue(chatChannelManager.getRoomChannels(1L).isEmpty());
    }

    @Test
    void replacesExistingUserChannelInSameRoom() {
        ChatChannelManager chatChannelManager = new ChatChannelManager();
        EmbeddedChannel oldChannel = new EmbeddedChannel();
        EmbeddedChannel newChannel = new EmbeddedChannel();

        chatChannelManager.joinRoom(1L, 41L, oldChannel);
        chatChannelManager.joinRoom(1L, 41L, newChannel);

        assertSame(newChannel, chatChannelManager.get(1L, 41L));
        assertNull(oldChannel.closeFuture().cause());
        assertFalse(oldChannel.isOpen());
    }
}
