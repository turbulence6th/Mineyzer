package com.codeyzer.mine.dto;

import lombok.Data;

@Data
public class ToggleFlagRequestDTO {
    private String playerId;
    private Integer row;
    private Integer col;
} 