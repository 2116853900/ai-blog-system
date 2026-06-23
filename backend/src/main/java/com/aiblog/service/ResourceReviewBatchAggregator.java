package com.aiblog.service;

import com.aiblog.entity.ResourceReview;
import com.aiblog.repository.ResourceReviewRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aiblog.entity.ResourceReview.ReviewStatus.NORMAL;

@Service
public class ResourceReviewBatchAggregator {

    private final ResourceReviewRepository reviewRepo;

    public ResourceReviewBatchAggregator(ResourceReviewRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    public Map<Long, RatingAggregate> forRefType(ResourceReview.RefType refType, Collection<Long> refIds) {
        Map<Long, RatingAggregate> out = new HashMap<>();
        if (refIds == null || refIds.isEmpty()) {
            return out;
        }
        List<Long> ids = refIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return out;
        }
        for (Object[] row : reviewRepo.aggregateByRefIds(refType, ids, NORMAL)) {
            Long refId = ((Number) row[0]).longValue();
            double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0d;
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            out.put(refId, new RatingAggregate(round(avg), count));
        }
        return out;
    }

    public void apply(ResourceReview.RefType refType,
                      Collection<? extends ReviewRatingTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return;
        }
        List<Long> ids = targets.stream().map(ReviewRatingTarget::getReviewRefId).toList();
        Map<Long, RatingAggregate> map = forRefType(refType, ids);
        for (ReviewRatingTarget target : targets) {
            RatingAggregate agg = map.get(target.getReviewRefId());
            if (agg != null && agg.count() > 0) {
                target.setAverageRating(agg.average());
                target.setReviewCount(agg.count());
            } else {
                target.setAverageRating(0);
                target.setReviewCount(0);
            }
        }
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record RatingAggregate(double average, long count) {}

    public interface ReviewRatingTarget {
        Long getReviewRefId();

        void setAverageRating(double averageRating);

        void setReviewCount(long reviewCount);
    }
}