package com.rail.api.controller;

import com.rail.api.component.UserResolver;
import com.rail.api.dto.SchedulingProfileDto;
import com.rail.api.dto.SchedulingProfileRequest;
import com.rail.api.service.OnboardingService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/scheduling-profile")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final UserResolver userResolver;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchedulingProfileDto create(
        @AuthenticationPrincipal UUID userPid,
        @Valid @RequestBody SchedulingProfileRequest req
    ) {
        return onboardingService.createProfile(
            userResolver.resolve(userPid),
            req
        );
    }

    @GetMapping
    public SchedulingProfileDto get(@AuthenticationPrincipal UUID userPid) {
        return onboardingService.getProfile(userResolver.resolve(userPid));
    }

    @PutMapping
    public SchedulingProfileDto update(
        @AuthenticationPrincipal UUID userPid,
        @Valid @RequestBody SchedulingProfileRequest req
    ) {
        return onboardingService.updateProfile(
            userResolver.resolve(userPid),
            req
        );
    }
}
