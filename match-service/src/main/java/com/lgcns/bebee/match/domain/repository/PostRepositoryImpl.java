package com.lgcns.bebee.match.domain.repository;

import com.lgcns.bebee.match.domain.entity.Post;
import com.lgcns.bebee.match.domain.entity.sync.Gender;
import com.lgcns.bebee.match.domain.entity.vo.EngagementType;
import com.lgcns.bebee.match.domain.entity.vo.PostStatus;
import com.lgcns.bebee.match.domain.repository.dto.PostSearchCond;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;

import static com.lgcns.bebee.match.domain.entity.QPost.post;
import static com.lgcns.bebee.match.domain.entity.QPostHelpCategory.postHelpCategory;
import static com.lgcns.bebee.match.domain.entity.QPostPeriod.postPeriod;
import static com.lgcns.bebee.match.domain.entity.QPostSchedule.postSchedule;
import static com.lgcns.bebee.match.domain.entity.sync.QMemberSync.memberSync;
import static com.lgcns.bebee.match.domain.entity.sync.QMemberDisabilityCategorySync.memberDisabilityCategorySync;

@Component
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    // 쿼리 최적화 하기 전 게시글 조회
//    @Override
//    public List<Post> findPosts(PostSearchCond cond) {
//        return queryFactory
//                .selectFrom(post)
//                .distinct()
//                .leftJoin(post.period, postPeriod).fetchJoin()
//                .leftJoin(post.helpCategories, postHelpCategory)
//                .leftJoin(memberSync).on(memberSync.id.eq(post.memberId))
//                .leftJoin(memberSync.disabilityCategories, memberDisabilityCategorySync)
//                .leftJoin(postSchedule).on(postSchedule.post.id.eq(post.id))
//                .where(
//                        eqEngagementType(cond.engagementType()),
//                        inLegalDongCodes(cond.legalDongCode()),
//                        inHelpCategoryIds(cond.helpCategoryIds()),
//                        eqGender(cond.gender()),
//                        betweenHoney(cond.minHoney(), cond.maxHoney()),
//                        inDisabilityCategoryIds(cond.disabilityCategoryId()),
//                        inDayOfWeeks(cond.dayOfWeeks()),
//                        inPostStatuses(cond.postStatuses()),
//                        ltPostId(cond.lastPostId())
//                )
//                .orderBy(post.id.desc())
//                .limit(cond.count())
//                .fetch();
//    }
        // 세미 조인 & 쿼리 분리를 통한 최적화한 게시글 조회
//    @Override
//    public List<Post> findPosts(PostSearchCond cond){
//        List<Long> ids = queryFactory.select(post.id)
//                .from(post)
//                .where(
//                        eqEngagementType(cond.engagementType()),
//                        inLegalDongCodes(cond.legalDongCode()),
//                        betweenHoney(cond.minHoney(), cond.maxHoney()),
//                        ltPostId(cond.lastPostId()),
//                        inHelpCategoryIdsSemiJoin(cond.helpCategoryIds()),
//                        inDisabilityCategoryIdsSemiJoin(cond.disabilityCategoryId()),
//                        inDayOfWeeksSemiJoin(cond.dayOfWeeks())
//                )
//                .orderBy(post.id.desc())
//                .limit(cond.count())
//                .fetch();
//
//        return queryFactory.selectFrom(post)
//                .leftJoin(post.period, postPeriod).fetchJoin()
//                .where(post.id.in(ids))
//                .orderBy(post.id.desc())
//                .fetch();
//    }

    @Override
    public List<Post> findPosts(PostSearchCond cond){
        List<Long> postIds = queryFactory.select(post.id)
                .from(post)
                .setHint("org.hibernate.comment", "STRAIGHT_JOIN_HINT")
                .where(
                        ltPostId(cond.lastPostId()),
                        eqEngagementType(cond.engagementType()),
                        inLegalDongCodes(cond.legalDongCode()),
                        betweenHoney(cond.minHoney(), cond.maxHoney()),
                        inHelpCategoryIdsSemiJoin(cond.helpCategoryIds()),
                        inDisabilityCategoryIdsSemiJoin(cond.disabilityCategoryId()),
                        inDayOfWeeksSemiJoin(cond.dayOfWeeks())
                ).orderBy(post.id.desc())
                .limit(cond.count())
                .fetch();

        return queryFactory
                .selectFrom(post)
                .leftJoin(post.period, postPeriod).fetchJoin()
                .where(post.id.in(postIds))
                .orderBy(post.id.desc())
                .fetch();
    }

    private BooleanExpression eqEngagementType(EngagementType type) {
        return type != null ? post.type.eq(type) : null;
    }

    private BooleanExpression inLegalDongCodes(List<String> legalDongCodes) {
        return legalDongCodes != null && !legalDongCodes.isEmpty()
                ? post.legalDongCode.in(legalDongCodes)
                : null;
    }

    private BooleanExpression inHelpCategoryIds(List<Long> helpCategoryIds) {
        return helpCategoryIds != null && !helpCategoryIds.isEmpty()
                ? postHelpCategory.id.helpCategoryId.in(helpCategoryIds)
                : null;
    }

    private BooleanExpression inHelpCategoryIdsSemiJoin(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;

        return JPAExpressions
                .selectOne()
                .from(postHelpCategory)
                .where(postHelpCategory.post.id.eq(post.id).and(postHelpCategory.id.helpCategoryId.in(categoryIds)))
                .exists();
    }

    private BooleanExpression eqGender(Gender gender) {
        return gender != null ? memberSync.gender.eq(gender) : null;
    }

    private BooleanExpression inDisabilityCategoryIds(List<Long> disabilityCategoryIds) {
        return disabilityCategoryIds != null && !disabilityCategoryIds.isEmpty()
                ? memberDisabilityCategorySync.id.disabilityCategoryId.in(disabilityCategoryIds)
                : null;
    }

    private BooleanExpression inDisabilityCategoryIdsSemiJoin(List<Long> categoryIds){
        if (categoryIds == null || categoryIds.isEmpty()) return null;

        return JPAExpressions
                .selectOne()
                .from(memberDisabilityCategorySync)
                .where(memberDisabilityCategorySync.member.id.eq(post.memberId).and(memberDisabilityCategorySync.id.disabilityCategoryId.in(categoryIds)))
                .exists();
    }

    private BooleanExpression betweenHoney(Long minHoney, Long maxHoney) {
        if (minHoney != null && maxHoney != null) {
            return post.totalHoney.between(minHoney, maxHoney);
        } else if (minHoney != null) {
            return post.totalHoney.goe(minHoney);
        } else if (maxHoney != null) {
            return post.totalHoney.loe(maxHoney);
        }
        return null;
    }

    private BooleanExpression inDayOfWeeks(List<DayOfWeek> dayOfWeeks) {
        return dayOfWeeks != null && !dayOfWeeks.isEmpty()
                ? postSchedule.dayOfWeek.in(dayOfWeeks)
                : null;
    }

    private BooleanExpression inDayOfWeeksSemiJoin(List<DayOfWeek> dayOfWeeks) {
        if (dayOfWeeks == null || dayOfWeeks.isEmpty()) return null;

        return JPAExpressions
                .selectOne()
                .from(postSchedule)
                .where(postSchedule.post.id.eq(post.id).and(postSchedule.dayOfWeek.in(dayOfWeeks)))
                .exists();
    }


    private BooleanExpression inPostStatuses(List<PostStatus> postStatuses) {
        return postStatuses != null && !postStatuses.isEmpty()
                ? post.status.in(postStatuses)
                : null;
    }

    private BooleanExpression ltPostId(Long lastPostId) {
        return lastPostId != null ? post.id.lt(lastPostId) : null;
    }
}