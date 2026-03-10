package com.app.playerservicejava.controller;

import com.app.playerservicejava.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("v1/info")
public class AppInfoController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping
    public ResponseEntity<Map<String, String>> getInfo() {
        return ResponseEntity.ok(Map.of(
                "name", appProperties.getName(),
                "environment", appProperties.getEnvironment()
        ));
    }
}
