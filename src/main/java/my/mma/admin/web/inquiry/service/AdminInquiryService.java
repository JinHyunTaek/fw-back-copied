package my.mma.admin.web.inquiry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.admin.web.inquiry.dto.AdminInquiryAnswerRequest;
import my.mma.admin.web.inquiry.dto.AdminInquiryDetailResponse;
import my.mma.admin.web.inquiry.dto.AdminInquiryResponse;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import my.mma.api.inquiry.entity.Inquiry;
import my.mma.api.inquiry.repository.InquiryRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    public Page<AdminInquiryResponse> inquiries(Pageable pageable){
        Page<Inquiry> inquiries = inquiryRepository.findAllWithUsersByAnswerIsNull(pageable);
        return inquiries.map(
                inquiry -> AdminInquiryResponse.builder()
                        .id(inquiry.getId())
                        .nickname(inquiry.getUser().getNickname())
                        .category(inquiry.getCategory())
                        .createdAt(inquiry.getCreatedDateTime())
                        .build()
        );
    }

    public AdminInquiryDetailResponse inquiryDetail(Long id){
        Inquiry inquiry = inquiryRepository.findById(id).orElseThrow(
                () -> new CustomException(ErrorCode.BAD_REQUEST_400)
        );
        User user = userRepository.findById(inquiry.getUser().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.BAD_REQUEST_400));
        return AdminInquiryDetailResponse.builder()
                .id(inquiry.getId())
                .content(inquiry.getContent())
                .nickname(user.getNickname())
                .category(inquiry.getCategory())
                .createdAt(inquiry.getCreatedDateTime())
                .build();
    }

    @Transactional
    public void updateAnswer(AdminInquiryAnswerRequest request){
        Inquiry inquiry = inquiryRepository.findById(request.id()).orElseThrow(
                () -> new CustomException(ErrorCode.BAD_REQUEST_400)
        );
        inquiry.updateAnswer(request.answer());
    }

}
