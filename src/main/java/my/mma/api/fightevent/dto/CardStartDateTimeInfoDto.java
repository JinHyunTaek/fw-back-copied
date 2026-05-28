package my.mma.api.fightevent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import my.mma.api.fightevent.entity.property.CardStartDateTimeInfo;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record CardStartDateTimeInfoDto(LocalDate date, @JsonFormat(pattern = "HH:mm") LocalTime time) {

    public CardStartDateTimeInfo toDto(){
        return CardStartDateTimeInfo.builder()
                .date(this.date)
                .time(this.time)
                .build();
    }

    public static CardStartDateTimeInfoDto toDto(CardStartDateTimeInfo info){
        return CardStartDateTimeInfoDto.builder()
                .date(info.getDate())
                .time(info.getTime())
                .build();
    }

}