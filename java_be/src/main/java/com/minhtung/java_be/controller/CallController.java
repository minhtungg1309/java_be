package com.minhtung.java_be.controller;

import com.minhtung.java_be.service.SignalingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/call")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CallController {
    SignalingService signalingService;

    @GetMapping("/status/{userId}")
    public ResponseEntity<String> checkUserStatus(@PathVariable String userId) {
        boolean isOnline = signalingService.isUserOnline(userId);
        return ResponseEntity.ok(isOnline ? "online" : "offline");
    }
}