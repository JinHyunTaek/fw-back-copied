package my.mma.api.inquiry.repository;

import my.mma.api.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findAllByUserIdOrderByCreatedDateTimeDesc(Long userId, Pageable pageable);
    @Query("select i from Inquiry i join fetch i.user where i.answer is null")
    Page<Inquiry> findAllWithUsersByAnswerIsNull(Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("delete from Inquiry i where i.user.id=:userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
