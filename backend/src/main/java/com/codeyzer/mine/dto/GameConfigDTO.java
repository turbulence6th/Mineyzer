package com.codeyzer.mine.dto;

import lombok.Data;

@Data
public class GameConfigDTO {
    private int rows;
    private int columns;
    private int mineCount;
}