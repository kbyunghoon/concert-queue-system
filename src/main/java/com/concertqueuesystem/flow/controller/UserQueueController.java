package com.concertqueuesystem.flow.controller;

import com.concertqueuesystem.flow.dto.AllowUserResponse;
import com.concertqueuesystem.flow.dto.AllowedUserResponse;
import com.concertqueuesystem.flow.dto.RankNumberResponse;
import com.concertqueuesystem.flow.dto.RegisterUserResponse;
import com.concertqueuesystem.flow.service.UserQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class UserQueueController {
    private final UserQueueService userQueueService;

    @PostMapping("")
    public Mono<RegisterUserResponse> registerUser(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.registerWaitQueue(queue, userId)
                .map(RegisterUserResponse::new);
    }

    @PostMapping("/allow")
    public Mono<AllowUserResponse> allowUsers(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "count") Long count) {
        return userQueueService.allowUser(queue, count)
                .map(allowed -> new AllowUserResponse(count, allowed));
    }

    @GetMapping("/allowed")
    public Mono<AllowedUserResponse> isAllwoed(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.isAllowed(queue, userId)
                .map(AllowedUserResponse::new);
    }

    @GetMapping("/rank")
    public Mono<RankNumberResponse> getRankUser(
            @RequestParam(name = "queue", defaultValue = "default") String queue,
            @RequestParam(name = "user_id") Long userId) {
        return userQueueService.getRank(queue, userId)
                .map(RankNumberResponse::new);
    }
}
