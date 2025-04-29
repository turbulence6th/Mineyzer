import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import './App.css'
import GameSetup from './components/GameSetup'
import GameBoard from './components/GameBoard'
import { Game, Player, GameService } from './services/GameService'
import WebSocketService from './services/WebSocketService'

function App() {
  const [currentGame, setCurrentGame] = useState<Game | null>(null)
  const [currentPlayer, setCurrentPlayer] = useState<Player | null>(null)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    return () => {
      // Bileşen unmount olduğunda WebSocket bağlantısını kapat
      WebSocketService.disconnect()
    }
  }, [])

  const handleGameStart = (game: Game, playerUsername: string) => {
    setCurrentGame(game)
    
    const player = game.players.find(p => p.username === playerUsername)
    if (player) {
      setCurrentPlayer(player)
      // WebSocket bağlantısını başlatırken playerId'yi ekle
      WebSocketService.init(game.id, player.id, handleGameUpdate) 
    } else {
        console.error("Oyuncu bulunamadı!")
        // Hata durumu yönetimi eklenebilir
    }
  }

  const handleGameUpdate = (updatedGame: Game) => {
    setCurrentGame(updatedGame)
    
    // Mevcut oyuncuyu güncellenmiş oyun verilerinden bulup güncelle
    if (currentPlayer) {
      const updatedPlayer = updatedGame.players.find(p => p.id === currentPlayer.id)
      if (updatedPlayer) {
        setCurrentPlayer(updatedPlayer)
      }
    }
  }

  const handleMakeMove = async (row: number, col: number) => {
    if (!currentGame || !currentPlayer) return
    
    try {
      await GameService.makeMove(currentGame.id, currentPlayer.id, row, col)
      // Yanıt beklemiyoruz, WebSocket üzerinden güncellemeler alacağız
    } catch (err) {
      setError('Hamle yapılırken bir hata oluştu')
      console.error(err)
    } 
  }

  const handleExitGame = () => {
    WebSocketService.disconnect()
    setCurrentGame(null)
    setCurrentPlayer(null)
    navigate('/')
  }

  return (
    <div className="App">
      {error && <div className="error-message">{error}</div>}
      
      {currentGame ? (
        <div className="game-container">
          <GameBoard 
            game={currentGame}
            currentPlayer={currentPlayer}
            onMakeMove={handleMakeMove}
            onExitGame={handleExitGame}
          />
          
        </div>
      ) : (
        <GameSetup onGameStart={handleGameStart} />
      )}
    </div>
  )
}

export default App
