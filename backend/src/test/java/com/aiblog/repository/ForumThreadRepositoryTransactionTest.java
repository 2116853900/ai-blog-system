package com.aiblog.repository;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ForumThreadRepositoryTransactionTest {

    @Test
    void counterMutationQueriesDeclareTheirOwnTransactions() throws Exception {
        assertTransactional("incrementViewCount", Long.class);
        assertTransactional("incrementViewCountBy", Long.class, long.class);
        assertTransactional("incrementLikeCount", Long.class);
        assertTransactional("decrementLikeCount", Long.class);
        assertTransactional("incrementFavoriteCount", Long.class);
        assertTransactional("decrementFavoriteCount", Long.class);
        assertTransactional("incrementReplyCount", Long.class, Long.class, Instant.class);
        assertTransactional("decrementReplyCount", Long.class);
        assertTransactional("incrementReportCount", Long.class);
    }

    private void assertTransactional(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = ForumThreadRepository.class.getMethod(methodName, parameterTypes);
        assertThat(method.isAnnotationPresent(Transactional.class))
                .as(methodName + " must be transactional when called from scheduled/non-transactional code")
                .isTrue();
    }
}
