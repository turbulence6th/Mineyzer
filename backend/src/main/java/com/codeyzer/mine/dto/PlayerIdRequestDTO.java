package com.codeyzer.mine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sadece oyuncu ID'sini içeren istekler için DTO.
 */
@Data
@NoArgsConstructor
public class PlayerIdRequestDTO {
    private String playerId;
} 