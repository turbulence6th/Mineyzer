package com.codeyzer.mine.model;

/**
 * Oyunun farklı durumlarını temsil eden enum.
 */
public enum GameStatus {
    WAITING_FOR_PLAYERS, // İkinci oyuncu bekleniyor
    WAITING_FOR_READY,  // Oyuncuların hazır olması bekleniyor
    IN_PROGRESS,          // Oyun devam ediyor
    GAME_OVER             // Oyun bitti
}