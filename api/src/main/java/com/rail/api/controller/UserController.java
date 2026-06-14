package com.rail.api.controller;

import com.rail.api.component.UserResolver;
import com.rail.api.dto.AuthResponse;
import com.rail.api.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class UserController {

    private final UserResolver userResolver;

    @GetMapping
    public AuthResponse.UserInfo me(@AuthenticationPrincipal UUID userPid) {
        User user = userResolver.resolve(userPid);
        return new AuthResponse.UserInfo(
            user.getPid(),
            user.getEmail(),
            user.getDisplayName()
        );
    }
}
