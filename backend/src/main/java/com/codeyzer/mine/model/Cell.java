package com.codeyzer.mine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Cell {
    private int row;
    private int column;
    private boolean isMine;
    private boolean isRevealed;
    private int adjacentMines;
    private String revealedByPlayerId;
    private String flaggedByPlayerId;

    public Cell(int row, int column) {
        this.row = row;
        this.column = column;
        this.isMine = false;
        this.isRevealed = false;
        this.adjacentMines = 0;
        this.revealedByPlayerId = null;
        this.flaggedByPlayerId = null;
    }
} 