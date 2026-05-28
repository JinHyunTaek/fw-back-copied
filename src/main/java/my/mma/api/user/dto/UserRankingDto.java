package my.mma.api.user.dto;

import java.util.List;

public record UserRankingDto(int myRanking, List<RankedUserDto> rankedUsers) {
}
