import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import './GameSetup.css';
import StrategyModal from './StrategyModal';
import JoinGameById from './JoinGameById';
import CreateOrJoinGame from './CreateOrJoinGame';

interface GameSetupProps {
    onGameStart: (game: any, username: string) => void;
}

const GameSetup: React.FC<GameSetupProps> = ({ onGameStart }) => {
    const { gameId } = useParams<{ gameId?: string }>();
    const [username, setUsername] = useState('');
    const [isStrategyModalOpen, setIsStrategyModalOpen] = useState(false);

    return (
        <div className="game-setup">
            <div className="game-setup-header">
                <div className="title-container">
                    <h1 className="game-setup-title">Mayın Tarlası Çevrimiçi</h1>
                </div>
            </div>
            <button 
                className="strategy-button"
                onClick={() => setIsStrategyModalOpen(true)}
            >
                Stratejiler
            </button>
            
            <div className="username-input">
                <label htmlFor="username" className="username-input-label">Kullanıcı Adı:</label>
                <input 
                    type="text"
                    id="username"
                    className="username-input-field"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                />
            </div>
            
            {gameId ? (
                <JoinGameById 
                    gameId={gameId} 
                    username={username} 
                    onGameStart={onGameStart} 
                />
            ) : (
                <CreateOrJoinGame 
                    username={username} 
                    onGameStart={onGameStart} 
                />
            )}
            
            <StrategyModal 
                isOpen={isStrategyModalOpen}
                onClose={() => setIsStrategyModalOpen(false)}
            />
        </div>
    );
};

export default GameSetup; 