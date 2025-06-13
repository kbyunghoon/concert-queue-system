package com.concertqueuesystem.flow.service;

import com.concertqueuesystem.flow.EmbeddedRedis;
import com.concertqueuesystem.flow.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test")
class UserQueueServiceTest {
    @Autowired
    private UserQueueService userQueueService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @BeforeEach
    void setUp() {
        ReactiveRedisConnection reactiveRedisConnection = reactiveRedisTemplate.getConnectionFactory()
                .getReactiveConnection();
        reactiveRedisConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    @DisplayName("사용자들이 순차적으로 대기열에 등록되면 올바른 순번 부여")
    void registerWaitQueue() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default", 101L))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default", 102L))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("동일한 사용자가 같은 대기열에 중복 등록 시 예외 발생")
    void alreadyRegisterWaitQueue() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
                .expectError(ApplicationException.class)
                .verify();
    }

    @Test
    @DisplayName("대기 중인 사용자가 없으면 허용 요청 시 0명으로 처리")
    void emptyAllowUser() {
        StepVerifier.create(userQueueService.allowUser("default", 3L))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열 크기가 허용 인원 수보다 클 때 허용 인원 수만큼만 허용")
    void allowUser() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue("default", 100L)
                                .then(userQueueService.registerWaitQueue("default", 101L))
                                .then(userQueueService.registerWaitQueue("default", 102L))
                                .then(userQueueService.allowUser("default", 2L)))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열 크기가 허용 인원 수보다 작을 때 대기열 크기만큼만 허용")
    void allowUser2() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue("default", 100L)
                                .then(userQueueService.registerWaitQueue("default", 101L))
                                .then(userQueueService.registerWaitQueue("default", 102L))
                                .then(userQueueService.allowUser("default", 10L)))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    @DisplayName("사용자들이 허용된 후 새로운 사용자 등록 시 올바른 순번 부여")
    void allowUserAfterRegisterWaitQueue() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue("default", 100L)
                                .then(userQueueService.registerWaitQueue("default", 101L))
                                .then(userQueueService.registerWaitQueue("default", 102L))
                                .then(userQueueService.allowUser("default", 3L))
                                .then(userQueueService.registerWaitQueue("default", 104L)))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    @DisplayName("미등록 사용자의 허용 상태 조회 시 false를 반환")
    void isNotAllowed() {
        StepVerifier.create(
                        userQueueService.isAllowed("default", 100L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열에 없는 사용자는 다른 사용자 허용 처리와 무관하게 false를 반환")
    void isNotAllowed2() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                        .then(userQueueService.allowUser("default", 5L))
                        .then(userQueueService.isAllowed("default", 101L)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("대기열 등록 및 허용 처리 완료된 사용자의 허용 상태는 true")
    void isAllowed() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                        .then(userQueueService.allowUser("default", 5L))
                        .then(userQueueService.isAllowed("default", 100L)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("첫 번째 등록된 사용자의 순번은 1번으로 조회")
    void getRank() {
        StepVerifier.create(
                        userQueueService.registerWaitQueue("default", 100L)
                                .then(userQueueService.getRank("default", 100L)))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(
                        userQueueService.registerWaitQueue("default", 101L)
                                .then(userQueueService.getRank("default", 101L)))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("등록되지 않은 사용자의 순번은 -1을 반환")
    void getRank2() {
        StepVerifier.create(userQueueService.getRank("default", 100L))
                .expectNext(-1L)
                .verifyComplete();
    }
}