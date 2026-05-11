package com.lgcns.bebee.match.application.usecase;

import com.lgcns.bebee.common.application.Params;
import com.lgcns.bebee.common.application.UseCase;
import com.lgcns.bebee.common.data.event.DomainEventPublisher;
import com.lgcns.bebee.common.data.event.match.EngagementCompletedEvent;
import com.lgcns.bebee.match.domain.entity.Agreement;
import com.lgcns.bebee.match.domain.entity.Engagement;
import com.lgcns.bebee.match.domain.entity.Match;
import com.lgcns.bebee.match.domain.entity.sync.MemberSync;
import com.lgcns.bebee.match.domain.entity.sync.Role;
import com.lgcns.bebee.match.domain.service.EngagementManager;
import com.lgcns.bebee.match.domain.service.MemberManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteEngagementUseCase implements UseCase<CompleteEngagementUseCase.Param, CompleteEngagementUseCase.Result> {
    private final MemberManager memberManager;
    private final EngagementManager engagementManager;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Result execute(Param param) {
        MemberSync member = memberManager.findExistingMember(param.currentMemberId);
        Engagement engagement = engagementManager.findExistingEngagement(param.engagementId);

        check(member, engagement);
        engagement.complete();

        if(engagement.getIsDisabledCheck() == true) {
            Match match = engagement.getMatch();
            eventPublisher.publish(new EngagementCompletedEvent(
                    engagement.getId(),
                    match.getAgreementId(),
                    match.getMatchId(),
                    match.getHelperId(),
                    match.getDisabledId(),
                    engagement.getDate()
            ));
        }

        Agreement agreement = engagement.getMatch().getAgreement();

        boolean isLastEngagement = false;
        if(agreement.getPeriod().getEndDate() == engagement.getDate()){
            isLastEngagement = true;
        }

        return new Result(isLastEngagement);
    }

    private void check(MemberSync member, Engagement engagement) {
        Role role = member.getRole();

        if(role == Role.HELPER){
            engagement.checkHelper();
            return;
        }
        engagement.checkDisabled();
    }


    @Getter
    @RequiredArgsConstructor
    public static class Param implements Params {
        private final Long currentMemberId;
        private final Long engagementId;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Result {
        private final Boolean isLastEngagement;
    }
}