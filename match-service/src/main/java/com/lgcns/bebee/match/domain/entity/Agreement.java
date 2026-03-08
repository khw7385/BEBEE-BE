package com.lgcns.bebee.match.domain.entity;

import com.lgcns.bebee.common.data.domain.BaseTimeEntity;
import com.lgcns.bebee.match.domain.entity.vo.AgreementStatus;
import com.lgcns.bebee.match.domain.entity.vo.EngagementType;
import com.lgcns.bebee.match.presentation.dto.AgreementScheduleDTO;
import com.lgcns.bebee.match.presentation.dto.DayEngagementTimeDTO;
import com.lgcns.bebee.match.presentation.dto.TermEngagementTimeDTO;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.lgcns.bebee.match.common.exception.MatchErrors.ALREADY_CONFIRMED_AGREEMENT;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Agreement extends BaseTimeEntity {
    @Id
    @Tsid
    @Column(name = "agreement_id")
    private Long id;

    @Column
    private Long postId;

    @Column(nullable = false)
    private Long disabledId;

    @Column(nullable = false)
    private Long helperId;

    @Column(nullable = false)
    private Long unitHoney;

    @Column(nullable = false)
    private Long totalHoney;

    @Column(nullable = false, length = 50)
    private String region;

    @Enumerated(EnumType.STRING)
    private EngagementType type;

    @Column(nullable = false)
    private LocalDate confirmationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementStatus status = AgreementStatus.BEFORE;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 10)
    private List<AgreementHelpCategory> helpCategories= new ArrayList<>();

    @OneToOne(mappedBy = "agreement", cascade = CascadeType.ALL)
    private AgreementPeriod period;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 10)
    private List<AgreementSchedule> schedules = new ArrayList<>();

    @Column
    private Boolean isVolunteer;

    public static Agreement create(
            Long postId,
            Long disabledId,
            Long helperId,
            EngagementType type,
            Boolean isVolunteer,
            Long unitHoney,
            Long totalHoney,
            String region,
            DayEngagementTimeDTO dayTime,
            TermEngagementTimeDTO termTime,
            List<Long> helpCategoryIds
    ) {
        if (isVolunteer) {
            unitHoney = 0L;
            totalHoney = 0L;
        }

        Agreement agreement = new Agreement();
        agreement.postId = postId;
        agreement.disabledId = disabledId;
        agreement.helperId = helperId;
        agreement.type = type;
        agreement.isVolunteer = isVolunteer;
        agreement.unitHoney = unitHoney;
        agreement.totalHoney = totalHoney;
        agreement.region = region;
        agreement.confirmationDate = LocalDate.now();
        agreement.status = AgreementStatus.BEFORE;
        agreement.confirmationDate = LocalDate.now();

        if (type == EngagementType.DAY && dayTime != null) {
            // DAY 타입: period 생성 및 주입
            AgreementPeriod period = AgreementPeriod.create(
                    dayTime.getDate(),
                    dayTime.getDate()
            );
            period.assignToAgreement(agreement);
            agreement.period = period;
            
            AgreementScheduleDTO scheduleDTO = dayTime.getSchedule();
            AgreementSchedule schedule = AgreementSchedule.create(
                    scheduleDTO.getDayOfWeek(),
                    scheduleDTO.getStartTime(),
                    scheduleDTO.getEndTime()
            );
            schedule.assignToAgreement(agreement);
            agreement.schedules.add(schedule);
        } else if (type == EngagementType.TERM && termTime != null) {
            AgreementPeriod period = AgreementPeriod.create(
                    termTime.getStartDate(),
                    termTime.getEndDate()
            );
            period.assignToAgreement(agreement);
            agreement.period = period;

            // TERM 타입: schedules 생성 및 주입
            termTime.getSchedules().forEach(scheduleDTO -> {
                AgreementSchedule schedule = AgreementSchedule.create(
                        scheduleDTO.getDayOfWeek(),
                        scheduleDTO.getStartTime(),
                        scheduleDTO.getEndTime()
                );
                schedule.assignToAgreement(agreement);
                agreement.schedules.add(schedule);
            });
        }

        helpCategoryIds.forEach(helpCategoryId -> {
            String categoryName = com.lgcns.bebee.match.domain.entity.vo.HelpCategoryType.getNameById(helpCategoryId);
            AgreementHelpCategory agreementHelpCategory = AgreementHelpCategory.create(agreement, helpCategoryId, categoryName);
            agreement.helpCategories.add(agreementHelpCategory);
        });

        return agreement;
    }

    public void refuse() {
        if (this.status == AgreementStatus.CONFIRMED) {
            throw ALREADY_CONFIRMED_AGREEMENT.toException();
        }
        this.status = AgreementStatus.REFUSED;
    }

    public void confirm() {
        if (this.status == AgreementStatus.CONFIRMED) {
            throw ALREADY_CONFIRMED_AGREEMENT.toException();
        }
        this.status = AgreementStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = AgreementStatus.CANCELLED;
    }

    /**
     * 주어진 기간 내에서 이 Agreement의 활동이 있는 날짜들을 반환
     * <p>
     * DAY 타입: 활동 날짜 하나만 반환 (범위 내에 있는 경우)
     * TERM 타입: 기간 내 활동 요일에 해당하는 모든 날짜 반환
     *
     * @param rangeStart 조회 범위 시작일
     * @param rangeEnd 조회 범위 종료일
     * @return 활동이 있는 날짜들의 Set
     */
    public Set<LocalDate> getActiveDatesInRange(LocalDate rangeStart, LocalDate rangeEnd) {
        Set<LocalDate> activeDates = new HashSet<>();

        if (this.type == EngagementType.DAY) {
            // DAY: 활동 날짜 하나만
            LocalDate activeDate = this.period.getStartDate();

            if (!activeDate.isBefore(rangeStart) && !activeDate.isAfter(rangeEnd)) {
                activeDates.add(activeDate);
            }
        } else {
            // TERM: 기간 내 활동 요일에 해당하는 모든 날짜
            LocalDate periodStart = this.period.getStartDate();
            LocalDate periodEnd = this.period.getEndDate();

            LocalDate calcStart = periodStart.isBefore(rangeStart) ? rangeStart : periodStart;
            LocalDate calcEnd = periodEnd.isAfter(rangeEnd) ? rangeEnd : periodEnd;

            Set<DayOfWeek> activityDays = this.schedules.stream()
                    .map(AgreementSchedule::getDayOfWeek)
                    .collect(Collectors.toSet());

            LocalDate current = calcStart;
            while (!current.isAfter(calcEnd)) {
                if (activityDays.contains(current.getDayOfWeek())) {
                    activeDates.add(current);
                }
                current = current.plusDays(1);
            }
        }

        return activeDates;
    }

    /**
     * Agreement의 전체 기간에 대한 활동 날짜들을 반환
     * <p>
     * DAY 타입: 활동 날짜 하나만 반환
     * TERM 타입: 시작~끝 날짜 사이에서 활동 요일에 해당하는 모든 날짜들을 반환
     *
     * @return 활동이 있는 날짜들의 List (정렬됨)
     */
    public List<LocalDate> getEngagementDates(){
        if (this.period == null) {
            return new ArrayList<>();
        }

        List<LocalDate> engagetmentDates = new ArrayList<>();

        if (this.type == EngagementType.DAY) {
            // DAY 타입: 활동 날짜 하나만 추가
            engagetmentDates.add(this.period.getStartDate());
        } else if (this.type == EngagementType.TERM) {
            // TERM 타입: 시작~끝 날짜 사이에서 활동 요일에 해당하는 모든 날짜 추출
            LocalDate startDate = this.period.getStartDate();
            LocalDate endDate = this.period.getEndDate();

            // schedules에서 활동 요일들 추출
            Set<DayOfWeek> activityDays = this.schedules.stream()
                    .map(AgreementSchedule::getDayOfWeek)
                    .collect(Collectors.toSet());

            // 시작 날짜부터 끝 날짜까지 순회하며 활동 요일에 해당하는 날짜들 수집
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                if (activityDays.contains(currentDate.getDayOfWeek())) {
                    engagetmentDates.add(currentDate);
                }
                currentDate = currentDate.plusDays(1);
            }
        }

        return engagetmentDates;
    }
}
