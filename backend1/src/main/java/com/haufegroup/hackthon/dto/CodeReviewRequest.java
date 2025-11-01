package com.haufegroup.hackthon.dto;

public record CodeReviewRequest(
        String code,
        String language) {}