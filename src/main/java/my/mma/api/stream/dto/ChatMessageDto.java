package my.mma.api.stream.dto;

import lombok.Builder;

import java.time.LocalDateTime;


public class ChatMessageDto {

    public record ChatJoinRequest(long userId, String nickname, int earnedBetSucceedPoint) {
    }

    public record ChatMessageRequest(String message) {}

    @Builder
    public record ChatMessageResponse(String message,
                                      String nickname,
                                      String messageId,
                                      String profileImgUrl,
                                      long userId,
                                      int earnedBetSucceedPoint,
                                      LocalDateTime createdAt) {}

}