package com.lgcns.bebee.match.application.usecase;

import com.lgcns.bebee.common.application.Params;
import com.lgcns.bebee.common.application.UseCase;
import com.lgcns.bebee.common.exception.InvalidParamException;
import com.lgcns.bebee.common.util.ParamValidator;
import com.lgcns.bebee.match.common.exception.MatchInvalidParamErrors;
import com.lgcns.bebee.match.domain.entity.Match;
import com.lgcns.bebee.match.domain.repository.MatchRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.lgcns.bebee.match.common.exception.MatchErrors.MATCH_NOT_FOUND;

/**
 * 결제 완료 후 Match 상태를 PAYMENT_COMPLETED로 변경하는 UseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteMatchPaymentUseCase implements UseCase<CompleteMatchPaymentUseCase.Param, Void> {
    private final MatchRepository matchRepository;

    @Transactional
    @Override
    public Void execute(Param param) {
        param.validate();

        Match match = matchRepository.findById(param.getMatchId())
                .orElseThrow(MATCH_NOT_FOUND::toException);

        log.info("Match 결제 완료 처리 - matchId: {}", param.getMatchId());

        match.completePayment();

        return null;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Param implements Params {
        private final Long matchId;

        @Override
        public boolean validate() {
            if (!ParamValidator.isValidId(matchId)) {
                throw new InvalidParamException(MatchInvalidParamErrors.REQUIRED_FIELD, "matchId");
            }
            return true;
        }
    }
}