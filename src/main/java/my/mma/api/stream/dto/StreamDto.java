package my.mma.api.stream.dto;

import lombok.*;
import my.mma.api.fightevent.dto.CurrentEventDto;
import my.mma.api.stream.dto.ChatMessageDto.ChatMessageResponse;

import java.util.List;

public class StreamDto {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class StreamRequest {
        private RequestMessageType requestMessageType;
        private ChatMessageDto.ChatJoinRequest chatJoinRequest;
        private ChatMessageDto.ChatMessageRequest chatMessageRequest;
        private CurrentEventDto streamFightEvent;
        private Long userIdToBlock;
    }

    @Builder
    public record StreamResponse(ResponseMessageType responseMessageType,
                                 ChatMessageResponse chatMessageResponse,
                                 CurrentEventDto streamFightEvent,
                                 RecentMessagesResponse recentMessages,
                                 int connectionCount){}

    public record RecentMessagesResponse(List<ChatMessageResponse> recentMessages){}

    public enum RequestMessageType {
        JOIN,TALK,BLOCK
    }

    public enum ResponseMessageType {
        TALK,CONNECTION_COUNT,FIGHT,RECENT_MESSAGES
    }

}
