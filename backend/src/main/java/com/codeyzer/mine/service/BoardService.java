package com.codeyzer.mine.service;

import com.codeyzer.mine.model.Cell;
import com.codeyzer.mine.model.Game;
import com.codeyzer.mine.model.Player; // Gerekirse eklenebilir
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class BoardService {

    private static final Logger log = LoggerFactory.getLogger(BoardService.class);

    /**
     * Verilen boyutlarda boş bir oyun tahtası listesi oluşturur.
     * @param rows Satır sayısı
     * @param columns Sütun sayısı
     * @return Boş hücrelerden oluşan 2D liste (tahta).
     */
    public List<List<Cell>> initializeBoard(int rows, int columns) {
        log.debug("Initializing board {}x{}", rows, columns);
        List<List<Cell>> board = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            List<Cell> rowList = new ArrayList<>();
            for (int j = 0; j < columns; j++) {
                rowList.add(new Cell(i, j));
            }
            board.add(rowList);
        }
        return board;
    }

    /**
     * Verilen tahtaya rastgele mayınları yerleştirir ve komşu hücrelerin sayaçlarını günceller.
     * @param board Mayınların yerleştirileceği tahta.
     * @param rows Satır sayısı
     * @param columns Sütun sayısı
     * @param mineCount Yerleştirilecek mayın sayısı.
     */
    public void placeMines(List<List<Cell>> board, int rows, int columns, int mineCount) {
        log.debug("Placing {} mines on {}x{} board", mineCount, rows, columns);
        Random random = new Random();
        int minesPlaced = 0;
        int totalCells = rows * columns;
        if (mineCount >= totalCells) {
             log.warn("Mine count ({}) is greater than or equal to total cells ({}), placing mines in all cells except one if possible.", mineCount, totalCells);
             mineCount = Math.max(0, totalCells -1); // Tüm hücrelere mayın koyma durumunu engelle
        }

        while (minesPlaced < mineCount) {
            int row = random.nextInt(rows);
            int col = random.nextInt(columns);
            Cell cell = board.get(row).get(col);
            if (!cell.isMine()) {
                cell.setMine(true);
                minesPlaced++;
                // Komşu hücrelerin sayaçlarını artır
                updateAdjacentCells(board, rows, columns, row, col);
            }
        }
    }

    /**
     * Bir mayının yerleştirildiği koordinat etrafındaki komşu hücrelerin mayın sayısını artırır.
     * @param board Tahta
     * @param rows Satır sayısı
     * @param columns Sütun sayısı
     * @param mineRow Mayının satırı
     * @param mineCol Mayının sütunu
     */
    public void updateAdjacentCells(List<List<Cell>> board, int rows, int columns, int mineRow, int mineCol) {
        for (int i = Math.max(0, mineRow - 1); i <= Math.min(rows - 1, mineRow + 1); i++) {
            for (int j = Math.max(0, mineCol - 1); j <= Math.min(columns - 1, mineCol + 1); j++) {
                // Mayın hücresinin kendisini atla
                if (i == mineRow && j == mineCol) {
                    continue;
                }
                Cell adjacentCell = board.get(i).get(j);
                // Diğer mayınların sayacını artırma
                if (!adjacentCell.isMine()) {
                    adjacentCell.setAdjacentMines(adjacentCell.getAdjacentMines() + 1);
                }
            }
        }
    }

    /**
     * Açılan '0' değerli bir hücrenin etrafındaki güvenli komşu hücreleri rekürsif olarak açar (kaskad).
     * Bayraklı hücreler açılmaz.
     * @param board Tahta
     * @param rows Satır sayısı
     * @param columns Sütun sayısı
     * @param row Başlangıç satırı ('0' olan hücre)
     * @param col Başlangıç sütunu ('0' olan hücre)
     */
    public void revealAdjacentCells(List<List<Cell>> board, int rows, int columns, int row, int col) {
        Cell currentCell = board.get(row).get(col);
        // Kaskad sadece zaten açılmış ve 0 değerli hücrelerden başlamalı (güvenlik kontrolü)
        if (!currentCell.isRevealed() || currentCell.getAdjacentMines() != 0 || currentCell.isMine()) {
            return;
        }

        log.trace("Cascading reveal from ({}, {})", row, col);

        for (int i = Math.max(0, row - 1); i <= Math.min(rows - 1, row + 1); i++) {
            for (int j = Math.max(0, col - 1); j <= Math.min(columns - 1, col + 1); j++) {
                // Kendisini atla
                if (i == row && j == col) {
                    continue;
                }

                Cell neighbor = board.get(i).get(j);

                // Sadece açılmamış, mayın olmayan ve bayraksız komşuları aç
                if (!neighbor.isRevealed() && !neighbor.isMine() && neighbor.getFlaggedByPlayerId() == null) {
                    log.trace("Revealing neighbor ({}, {}) during cascade", i, j);
                    neighbor.setRevealed(true);
                    // Eğer açılan komşu da '0' ise, kaskadı oradan devam ettir
                    if (neighbor.getAdjacentMines() == 0) {
                        revealAdjacentCells(board, rows, columns, i, j);
                    }
                }
            }
        }
    }

    /**
     * Bir hücreyi açma işleminin sonucunu tutan DTO.
     */
    public static class RevealResult {
        public boolean moveSuccess = false; // Hamle geçerli miydi? (Koordinat, zaten açık vb.)
        public boolean mineHit = false; // Mayına mı basıldı?
        public int pointsGained = 0; // Hücrenin kendisinden kazanılan puan (varsa)
        public boolean cascadeTriggered = false; // '0' açılarak kaskad tetiklendi mi?
        public String revealedOpponentFlagOwnerId = null; // Açılan hücrede rakip bayrağı var mıydı? Varsa sahibinin ID'si.
        public Cell revealedCell = null; // Açılan hücre nesnesi.
    }

    /**
     * Belirtilen koordinattaki hücreyi açar ve sonucu döndürür.
     * Gerekirse kaskad açılımını tetikler.
     * @param game Mevcut oyun nesnesi
     * @param playerId Hamleyi yapan oyuncunun ID'si
     * @param row Açılacak hücrenin satırı
     * @param col Açılacak hücrenin sütunu
     * @return RevealResult nesnesi içinde işlemin sonucunu döndürür.
     */
    public RevealResult revealCell(Game game, String playerId, int row, int col) {
        RevealResult result = new RevealResult();
        List<List<Cell>> board = game.getBoard();
        int rows = game.getRows();
        int columns = game.getColumns();

        // Temel koordinat kontrolü
        if (row < 0 || row >= rows || col < 0 || col >= columns) {
            log.warn("Invalid coordinates ({}, {}) in revealCell", row, col);
            return result; // moveSuccess = false
        }

        Cell cell = board.get(row).get(col);
        result.revealedCell = cell; // Hedef hücreyi kaydet

        // Zaten açıksa veya oyuncunun kendi bayrağı varsa geçersiz hamle
        if (cell.isRevealed() || playerId.equals(cell.getFlaggedByPlayerId())) {
             log.debug("Cell ({}, {}) already revealed or flagged by player {}", row, col, playerId);
            return result; // moveSuccess = false
        }

        // Rakip bayrağı var mı kontrol et
        if (cell.getFlaggedByPlayerId() != null && !playerId.equals(cell.getFlaggedByPlayerId())) {
            result.revealedOpponentFlagOwnerId = cell.getFlaggedByPlayerId();
            log.debug("Revealing opponent's ({}) flag at ({}, {})", result.revealedOpponentFlagOwnerId, row, col);
        }

        // Hücreyi aç
        cell.setRevealed(true);
        cell.setRevealedByPlayerId(playerId); // Kimin açtığını kaydet
        cell.setFlaggedByPlayerId(null); // Varsa bayrağı kaldır

        // Ne açıldı?
        if (cell.isMine()) {
            log.debug("Player {} hit a mine at ({}, {})", playerId, row, col);
            result.mineHit = true;
            result.moveSuccess = true; // Mayına basmak geçerli bir hamle sonucudur
        } else {
            // Güvenli hücre
            int adjacentMines = cell.getAdjacentMines();
            if (adjacentMines > 0) {
                result.pointsGained = adjacentMines; // Puanı ayarla
                 log.debug("Player {} revealed safe cell ({}, {}) with {} points", playerId, row, col, adjacentMines);
            } else {
                // '0' hücresi, kaskadı tetikle
                 log.debug("Player {} revealed 0-cell ({}, {}), triggering cascade", playerId, row, col);
                revealAdjacentCells(board, rows, columns, row, col); // Kaskadı çağır
                result.cascadeTriggered = true;
            }
            result.moveSuccess = true;
        }

        return result;
    }

    /**
     * Tüm mayın olmayan hücrelerin açılıp açılmadığını kontrol eder.
     * @param board Kontrol edilecek tahta.
     * @param mineCount Tahtadaki toplam mayın sayısı.
     * @return Tüm güvenli hücreler açıldıysa true, aksi halde false.
     */
    public boolean isGameFinished(List<List<Cell>> board, int mineCount) {
        if (board == null || board.isEmpty() || board.get(0).isEmpty()) {
            return false; // Geçersiz tahta durumu
        }
        int rows = board.size();
        int columns = board.get(0).size();
        int revealedSafeCells = 0;
        int totalCells = rows * columns;
        int totalSafeCells = totalCells - mineCount;

        // Tahtada mayın sayısı beklenenden fazla veya az ise (potansiyel hata), bitirme
        if (totalSafeCells < 0) {
             log.error("Calculated totalSafeCells is negative ({}), check mineCount ({}) vs totalCells ({}).", totalSafeCells, mineCount, totalCells);
             return false;
        }

        for (List<Cell> row : board) {
            for (Cell cell : row) {
                if (cell.isRevealed() && !cell.isMine()) {
                    revealedSafeCells++;
                }
                // Eğer açılmamış güvenli bir hücre varsa, oyun bitmemiştir
                else if (!cell.isRevealed() && !cell.isMine()) {
                    return false;
                }
            }
        }

        // Tüm güvenli hücreler açıldıysa oyun bitmiştir.
        boolean finished = revealedSafeCells >= totalSafeCells;
        log.trace("isGameFinished check: Revealed safe cells = {}, Total safe cells = {}, Finished = {}", revealedSafeCells, totalSafeCells, finished);
        return finished;
    }

    /**
     * Tahtada kalan (açılmamış, mayın olmayan) hücrelerin toplam puan değerini hesaplar.
     * @param board Oyun tahtası.
     * @return Kalan toplam puan.
     */
    public int calculateRemainingPoints(List<List<Cell>> board) {
        if (board == null || board.isEmpty()) {
            return 0;
        }
        int remainingPoints = 0;
        log.debug("Calculating remaining points on the board.");
        for (List<Cell> row : board) {
            for (Cell cell : row) {
                // Sadece açılmamış ve mayın olmayan hücreleri say
                if (!cell.isRevealed() && !cell.isMine()) {
                    // Kalan puan hücrenin komşu mayın sayısıdır
                    remainingPoints += cell.getAdjacentMines();
                }
            }
        }
        log.debug("Total remaining points calculated: {}", remainingPoints);
        return remainingPoints;
    }
} 