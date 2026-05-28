package my.mma.api.stream.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.global.redis.key.RedisKey;
import my.mma.api.global.redis.prefix.RedisKeyPrefix;
import my.mma.api.global.redis.utils.RedisListUtils;
import my.mma.api.global.redis.utils.RedisUtils;
import my.mma.api.global.s3.service.S3ImgService;
import my.mma.api.stream.dto.ChatMessageDto;
import my.mma.api.stream.dto.ChatMessageDto.ChatMessageResponse;
import my.mma.api.stream.dto.StreamDto.RecentMessagesResponse;
import my.mma.api.stream.dto.StreamDto.ResponseMessageType;
import my.mma.api.stream.dto.StreamDto.StreamRequest;
import my.mma.api.stream.dto.StreamDto.StreamResponse;
import my.mma.api.stream.dto.SocketConnectedUserDto;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static my.mma.api.stream.dto.StreamDto.ResponseMessageType.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class GlobalWebSocketHandler extends TextWebSocketHandler {

    private static final int RECENT_MESSAGE_RANGE = 30;

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String, SocketConnectedUserDto> connectedUsers = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> blockedUsersMap = new ConcurrentHashMap<>();
    private final RedisListUtils recentChatLogRedisUtils;
    private final RedisUtils<CurrentEventDto> streamFightEventRedisUtils;
    private final ObjectMapper objectMapper;
    private final S3ImgService s3Service;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessions.add(session);
        StreamResponse response = StreamResponse.builder()
                .responseMessageType(CONNECTION_COUNT)
                .connectionCount(sessions.size())
                .build();
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(response));
        for (WebSocketSession ws : sessions) {
            if (ws.isOpen()) {
                ws.sendMessage(textMessage);
            }
        }
        sendRecentChatLogsToConnectedUser(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        String sessionId = session.getId();
        StreamRequest request = objectMapper.readValue(payload, StreamRequest.class);
        switch (request.getRequestMessageType()) {
            case JOIN -> handleJoin(sessionId, request.getChatJoinRequest());
            case TALK -> handleTalk(sessionId, request.getChatMessageRequest());
            case BLOCK -> handleBlock(sessionId, request.getUserIdToBlock());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        sessions.remove(session);
        connectedUsers.remove(session.getId());
        blockedUsersMap.remove(session.getId());
        StreamResponse response = StreamResponse.builder()
                .responseMessageType(ResponseMessageType.CONNECTION_COUNT)
                .connectionCount(sessions.size())
                .build();
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(response));
        for (WebSocketSession ws : sessions) {
            if (ws.isOpen()) {
                ws.sendMessage(textMessage);
            }
        }
    }

    private void handleBlock(String sessionId, Long userIdToBlock) {
        blockedUsersMap.computeIfAbsent(sessionId, k -> new HashSet<>()).add(userIdToBlock);
    }

    private void handleTalk(String sessionId, ChatMessageDto.ChatMessageRequest chatRequest) throws IOException {
        SocketConnectedUserDto user = connectedUsers.get(sessionId);
        ChatMessageResponse chatMessageResponse = ChatMessageResponse.builder()
                .userId(user.id())
                .nickname(user.nickname())
                .message(chatRequest.message())
                .messageId(UUID.randomUUID().toString())
                .earnedBetSucceedPoint(user.earnedBetSucceedPoint())
                .profileImgUrl(user.profileImgUrl())
                .createdAt(LocalDateTime.now())
                .build();
        StreamResponse response = StreamResponse.builder()
                .responseMessageType(TALK)
                .chatMessageResponse(chatMessageResponse)
                .build();
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(response));
        for (WebSocketSession ws : sessions) {
            if (ws.isOpen()) {
                SocketConnectedUserDto receiver = connectedUsers.get(ws.getId());
                if (receiver == null)
                    continue;
                Set<Long> blocked = blockedUsersMap.get(ws.getId());
                if (blocked == null || !blocked.contains(user.id()))
                    ws.sendMessage(textMessage);
            }
        }
        saveChatMessage(chatMessageResponse);
    }

    private void handleJoin(String sessionId, ChatMessageDto.ChatJoinRequest joinRequest) {
        connectedUsers.put(sessionId, SocketConnectedUserDto.builder()
                .id(joinRequest.userId())
                .nickname(joinRequest.nickname())
                .earnedBetSucceedPoint(joinRequest.earnedBetSucceedPoint())
                .profileImgUrl(s3Service.generateUserImgUrlOrNull(joinRequest.userId()))
                .build()
        );
    }

    private void sendRecentChatLogsToConnectedUser(WebSocketSession session) throws IOException {
        List<String> rawRecentChatLogs = recentChatLogRedisUtils.getRecent(getRecentChatLogKey(), RECENT_MESSAGE_RANGE);
        List<ChatMessageResponse> recentChatLogs = rawRecentChatLogs.stream()
                .map(message -> {
                    try {
                        return objectMapper.readValue(message, ChatMessageResponse.class);
                    } catch (JsonProcessingException e) {
                        log.error("error while serializing recent chat logs. e=", e);
                        throw new CustomException(ErrorCode.SERVER_ERROR_500);
                    }
                }).toList();
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                StreamResponse.builder()
                        .responseMessageType(RECENT_MESSAGES)
                        .recentMessages(new RecentMessagesResponse(recentChatLogs))
                        .build()
        )));
    }

    private void saveChatMessage(ChatMessageResponse chatMessage) throws JsonProcessingException {
        recentChatLogRedisUtils.pushRightWithRange(
                getRecentChatLogKey(),
                objectMapper.writeValueAsString(chatMessage),
                RECENT_MESSAGE_RANGE);
    }

    private String getRecentChatLogKey() {
        return RedisKeyPrefix.CHAT_LOG_PREFIX.getPrefix() +
                streamFightEventRedisUtils.getData(RedisKey.CURRENT_EVENT.getKey()).getId();
    }

    public void broadcastFightEvent(CurrentEventDto fe) {
        try {
            StreamResponse response = StreamResponse.builder()
                    .responseMessageType(FIGHT)
                    .streamFightEvent(fe)
                    .build();
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(response));
            for (WebSocketSession ws : sessions) {
                if (ws.isOpen()) {
                    ws.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            log.info("io exception while broadcasting stream fight event, e = ", e);
            throw new CustomException(ErrorCode.SERVER_ERROR_500);
        }
    }

    public int getSocketConnectedUserCnt() {
        return sessions.size();
    }

}
