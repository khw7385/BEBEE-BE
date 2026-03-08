package com.lgcns.bebee.match.application.usecase;

import com.lgcns.bebee.common.application.Params;
import com.lgcns.bebee.common.application.UseCase;
import com.lgcns.bebee.common.exception.InvalidParamException;
import com.lgcns.bebee.common.util.ParamValidator;
import com.lgcns.bebee.match.common.exception.MatchInvalidParamErrors;
import com.lgcns.bebee.match.domain.entity.Agreement;
import com.lgcns.bebee.match.domain.entity.Match;
import com.lgcns.bebee.match.domain.repository.MatchRepository;
import com.lgcns.bebee.match.domain.service.AgreementReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.lgcns.bebee.match.common.exception.MatchErrors.MATCH_NOT_FOUND;

/**
 * 결제 실패 등의 이유로 Agreement와 Match를 취소하는 UseCase (보상 트랜잭션)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelAgreementUseCase implements UseCase<CancelAgreementUseCase.Param, Void> {
    private final AgreementReader agreementReader;
    private final MatchRepository matchRepository;

    @Transactional
    @Override
    public Void execute(Param param) {
        param.validate();

        // 1. Agreement 취소
        Agreement agreement = agreementReader.getById(param.getAgreementId());
        log.info("Agreement 취소 처리 - agreementId = {}", param.getAgreementId());
        agreement.cancel();

        // 2. Match 취소
        Match match = matchRepository.findById(param.getMatchId())
                .orElseThrow(MATCH_NOT_FOUND::toException);
        log.info("Match 취소 처리 - matchId: {}", param.getMatchId());
        match.cancel();

        return null;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Param implements Params {
        private final Long matchId;
        private final Long agreementId;

        @Override
        public boolean validate() {
            if (!ParamValidator.isValidId(matchId)) {
                throw new InvalidParamException(MatchInvalidParamErrors.REQUIRED_FIELD, "matchId");
            }
            if (!ParamValidator.isValidId(agreementId)) {
                throw new InvalidParamException(MatchInvalidParamErrors.REQUIRED_FIELD, "agreementId");
            }
            return true;
        }
    }
}