package my.mma.api.inquiry.dto;

import lombok.Builder;
import my.mma.api.inquiry.entity.Inquiry;
import my.mma.api.inquiry.entity.constant.InquiryCategory;

import java.time.LocalDate;

@Builder
public record InquiryResponse(long id, InquiryCategory category, LocalDate answeredDate, LocalDate createdDate) {

    public static InquiryResponse toDto(Inquiry inquiry){
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .category(inquiry.getCategory())
                .answeredDate(inquiry.getAnswer() != null ? inquiry.getLastModifiedDateTime().toLocalDate() : null)
                .createdDate(inquiry.getCreatedDateTime().toLocalDate())
                .build();
    }

}
