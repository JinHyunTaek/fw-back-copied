package my.mma.api.stream.dto;

import lombok.*;

@Builder
public record SocketConnectedUserDto(String nickname, Long id, int earnedBetSucceedPoint, String profileImgUrl) {

}
