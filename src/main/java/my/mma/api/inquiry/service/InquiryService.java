package my.mma.api.inquiry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.inquiry.dto.InquiryBodyResponse;
import my.mma.api.inquiry.dto.InquiryResponse;
import my.mma.api.inquiry.dto.InquirySaveRequest;
import my.mma.api.inquiry.entity.Inquiry;
import my.mma.api.inquiry.repository.InquiryRepository;
import my.mma.api.user.entity.User;
import my.mma.api.user.repository.UserRepository;
import my.mma.api.exception.CustomException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static my.mma.api.exception.ErrorCode.BAD_REQUEST_400;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    public Page<InquiryResponse> inquiries(String email, Pageable pageable) {
        User user = getUser(email);
        Page<Inquiry> inquiries = inquiryRepository.findAllByUserIdOrderByCreatedDateTimeDesc(user.getId(), pageable);
        return inquiries.map(InquiryResponse::toDto);
    }

    public InquiryBodyResponse inquiryBody(Long inquiryId){
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(
                () -> new CustomException(BAD_REQUEST_400));
        return InquiryBodyResponse.builder()
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .build();
    }

    @Transactional
    public void save(InquirySaveRequest request, String email) {
        User user = getUser(email);
        inquiryRepository.save(Inquiry.builder()
                .user(user)
                .category(request.category())
                .content(request.content())
                .build());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(BAD_REQUEST_400));
    }

}
