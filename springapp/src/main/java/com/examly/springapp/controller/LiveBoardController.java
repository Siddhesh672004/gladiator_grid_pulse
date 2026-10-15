package com.examly.springapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.examly.springapp.service.LiveBoardHub;

import lombok.RequiredArgsConstructor;

// live boards pushed by the server (text/event-stream): event "standings" = the whole board, now and after every change
@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveBoardController {

    private final LiveBoardHub liveBoardHub;

    // public like the board itself; 404 for an unknown session, 503 when too many people watch it
    @GetMapping("/session/{sessionId}")
    public SseEmitter watch(@PathVariable Long sessionId) {
        return liveBoardHub.subscribe(sessionId);
    }
}
