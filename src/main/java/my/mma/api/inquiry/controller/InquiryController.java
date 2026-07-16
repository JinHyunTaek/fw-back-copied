package my.mma.api.inquiry.controller;

import lombok.RequiredArgsConstructor;
import my.mma.api.inquiry.dto.InquiryBodyResponse;
import my.mma.api.inquiry.dto.InquiryResponse;
import my.mma.api.inquiry.dto.InquirySaveRequest;
import my.mma.api.inquiry.service.InquiryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inquiry")
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping("/inquiries")
    public ResponseEntity<Page<InquiryResponse>> inquiries(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault Pageable pageable
            ) {
        return ResponseEntity.ok().body(inquiryService.inquiries(userDetails.getUsername(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InquiryBodyResponse> inquiryBody(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok().body(inquiryService.inquiryBody(id));
    }

    @PostMapping("")
    public ResponseEntity<Void> save(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Validated InquirySaveRequest request
    ) {
        inquiryService.save(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

}
