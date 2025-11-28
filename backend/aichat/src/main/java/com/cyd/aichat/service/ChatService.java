package com.cyd.aichat.service;


import com.cyd.aichat.model.ChatRoom;
import java.util.List;

public interface ChatService {

    // 通过房间号与特定用户对话
    String doChat(long roomId, String userPrompt);

    // 获取当前所有的聊天房间对话
    List<ChatRoom> getChatRoomList();
}
