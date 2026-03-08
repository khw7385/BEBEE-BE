package com.lgcns.bebee.match.domain.entity;

import com.lgcns.bebee.common.data.domain.BaseTimeEntity;
import com.lgcns.bebee.match.domain.entity.vo.MatchStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.lgcns.bebee.match.domain.entity.vo.ReviewDirection;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "`match`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseTimeEntity {
    @Id
    @Tsid
    private Long matchId;

    @Column(nullable = false)
    private Long helperId;

    @Column(nullable = false)
    private Long disabledId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Long chatRoomId;

    @Column(nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PAYMENT_PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false, unique = true)
    private Agreement agreement;

    @OneToMany(mappedBy = "match")
    private List<Review> reviews = new ArrayList<>();

    public Long getAgreementId() {
        return agreement != null ? agreement.getId() : null;
    }

    // 도우미가 작성한 리뷰
    public Review getHelperReview() {
        return reviews.stream()
                .filter(r -> r.getReviewDirection() == ReviewDirection.HELPER_TO_DISABLED)
                .findFirst()
                .orElse(null);
    }

    // 장애인이 작성한 리뷰
    public Review getDisabledReview() {
        return reviews.stream()
                .filter(r -> r.getReviewDirection() == ReviewDirection.DISABLED_TO_HELPER)
                .findFirst()
                .orElse(null);
    }

    public static Match create(
            Long helperId,
            Long disabledId,
            String title,
            String imageUrl,
            Long chatRoomId,
            Agreement agreement
    ) {
        Match match = new Match();
        match.helperId = helperId;
        match.disabledId = disabledId;
        match.title = title;
        match.imageUrl = imageUrl;
        match.chatRoomId = chatRoomId;
        match.agreement = agreement;

        return match;
    }

    public void completePayment() {
        this.status = MatchStatus.PAYMENT_COMPLETED;
    }

    public void cancel() {
        this.status = MatchStatus.CANCELLED;
    }
}
