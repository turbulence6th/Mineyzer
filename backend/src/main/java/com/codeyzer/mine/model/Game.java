package com.codeyzer.mine.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.codeyzer.mine.model.GameStatus.*;

@Getter
@Setter
public class Game {

    private String id;
    private int rows;
    private int columns;
    private int mineCount;
    private GameStatus status;
    private String currentTurn;
    private List<Player> players = new ArrayList<>();
    private List<List<Cell>> board = new ArrayList<>();
    private String lastEventMessage = "";
    private int lastMoveRow = -1; // Son hamle koordinatları
    private int lastMoveCol = -1; // Son hamle koordinatları
    // Yeni/Güncellenen Zaman Alanları
    private long initialPlayerTimeMillis; // Oyuna özel başlangıç süresi
    private long player1TimeLeftMillis; // Default initialization kaldırıldı
    private long player2TimeLeftMillis; // Default initialization kaldırıldı
    private long turnStartTimeMillis = 0L;
    private String winnerId = null; // Kazanan oyuncunun ID'si (null ise berabere veya devam ediyor)

    public Game() {
        // Boş constructor
    }

    public Game(int rows, int columns, int mineCount) {
        this.id = UUID.randomUUID().toString();
        this.rows = rows;
        this.columns = columns;
        this.mineCount = mineCount;
        this.status = WAITING_FOR_PLAYERS;
        this.lastEventMessage = "Oyun oluşturuldu, rakip bekleniyor.";
    }
    
    /**
     * Oyun için başlangıç süresini ve oyuncuların kalan sürelerini ayarlar.
     * Genellikle GameService tarafından çağrılır.
     */
    public void initializeTime(long initialTimeMillis) {
        this.initialPlayerTimeMillis = initialTimeMillis;
        this.player1TimeLeftMillis = initialTimeMillis;
        this.player2TimeLeftMillis = initialTimeMillis;
        // turnStartTimeMillis, ikinci oyuncu katıldığında set edilecek.
    }

    public boolean joinGame(Player player) {
        if (players.size() < 2 && this.status == WAITING_FOR_PLAYERS) {
            players.add(player);
            
            // İlk oyuncu eklendiğinde sırayı ona ver
            if (players.size() == 1) {
                currentTurn = player.getId();
                this.lastEventMessage = player.getUsername() + " oyuna katıldı. Rakip bekleniyor.";
            } else if (players.size() == 2) {
                // İkinci oyuncu katıldığında durumu WAITING_FOR_READY yap
                this.status = WAITING_FOR_READY;
                // turnStartTimeMillis VE currentTurn GameService.startGameIfReady tarafından ayarlanacak
                this.lastEventMessage = player.getUsername() + " oyuna katıldı. Hazır olmanız bekleniyor.";
            }
            
            return true;
        }
        return false;
    }

    /**
     * Sıradaki oyuncuya geçer.
     */
    public void switchTurn() {
        if (players.size() == 2 && this.status == IN_PROGRESS) {
            if (currentTurn.equals(players.get(0).getId())) {
                currentTurn = players.get(1).getId();
            } else {
                currentTurn = players.get(0).getId();
            }
            // Yeni turun başlangıç zamanını GameService ayarlayacak.
        }
    }

    /**
     * Verilen ID'ye sahip oyuncuyu bulur.
     * @param playerId Aranacak oyuncunun ID'si.
     * @return Oyuncu bulunursa Player nesnesi, bulunamazsa null.
     */
    public Player getPlayerById(String playerId) {
        return players.stream()
                      .filter(p -> p.getId().equals(playerId))
                      .findFirst()
                      .orElse(null);
    }
    
    public boolean isGameFinished() {
        if (this.status != IN_PROGRESS) {
            return false;
        }
        for (List<Cell> row : board) {
            for (Cell cell : row) {
                if (!cell.isMine() && !cell.isRevealed()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * @deprecated Kazanan artık winnerId alanı üzerinden yönetiliyor.
     * Bu metod skor bazlı kazananı döndürürdü, ancak oyun bitiş mantığı değişti.
     */
    @Deprecated
    public Player getWinner() {
        if (players.isEmpty()) return null;
        if (players.size() == 1) return players.get(0);
        // Skor bazlı basit kontrol (eski mantık, kullanılmamalı)
        if (players.get(0).getScore() > players.get(1).getScore()) return players.get(0);
        if (players.get(1).getScore() > players.get(0).getScore()) return players.get(1);
        return null; // Berabere veya diğer durumlar
    }

    /**
     * Belirtilen hücreye bayrak koyar veya kaldırır.
     * Sadece oyuncu kendi bayrağını kaldırabilir.
     * @param playerId Bayrağı değiştiren oyuncunun ID'si.
     * @param row Satır
     * @param col Sütun
     * @return İşlem başarılıysa true (bayrak kondu veya kaldırıldı), aksi halde false.
     */
    public boolean toggleFlag(String playerId, int row, int col) {
        if (this.status != IN_PROGRESS) { return false; }
        if (row < 0 || row >= rows || col < 0 || col >= columns) { return false; }
        Cell cell = board.get(row).get(col);
        if (cell.isRevealed()) { return false; } // Açık hücreye bayrak konmaz

        String currentFlagOwner = cell.getFlaggedByPlayerId();
        boolean toggled = false;

        if (currentFlagOwner == null) {
            // Hücre boş, bayrak koy
            cell.setFlaggedByPlayerId(playerId);
            toggled = true;
        } else if (currentFlagOwner.equals(playerId)) {
            // Kendi bayrağı, kaldır
            cell.setFlaggedByPlayerId(null);
            toggled = true;
        } else {
            // Rakibin bayrağı, değiştirme
            toggled = false;
        }
        // Bayrak değiştirme mesajı GameService'te loglanabilir.
        return toggled;
    }

    /** 
     * Oyunun bitip bitmediğini kontrol eder. (status alanına göre)
     * @return Oyun bitmişse true, aksi halde false.
     */
    public boolean isGameOver() {
        return this.status == GAME_OVER;
    }
} 