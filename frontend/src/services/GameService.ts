import axios from 'axios';

// API URL'sini ortam değişkeninden al
const BASE_API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'; // Fallback
const API_ENDPOINT = `${BASE_API_URL}/api/games`;

export interface Cell {
    row: number;
    column: number;
    mine: boolean;
    revealed: boolean;
    adjacentMines: number;
    revealedByPlayerId?: string;
    flaggedByPlayerId?: string | null;
}

export interface Player {
    id: string;
    username: string;
    score: number;
}

export interface Game {
    id: string;
    rows: number;
    columns: number;
    mineCount: number;
    gameOver: boolean;
    currentTurn: string;
    players: Player[];
    board: Cell[][];
    lastEventMessage?: string;
    lastMoveRow: number;
    lastMoveCol: number;
    player1TimeLeftMillis: number;
    player2TimeLeftMillis: number;
    winnerId?: string | null;
    turnStartTimeMillis?: number;
}

export const GameService = {
    createGame: async (rows: number = 8, columns: number = 8, mineCount: number = 10): Promise<Game> => {
        const response = await axios.post(API_ENDPOINT, { rows, columns, mineCount });
        return response.data;
    },

    getGame: async (gameId: string): Promise<Game> => {
        const response = await axios.get(`${API_ENDPOINT}/${gameId}`);
        return response.data;
    },

    getAllGames: async (): Promise<Game[]> => {
        const response = await axios.get(API_ENDPOINT);
        return response.data;
    },

    joinGame: async (gameId: string, username: string): Promise<Game> => {
        const response = await axios.post(`${API_ENDPOINT}/${gameId}/join`, { username });
        return response.data;
    },

    makeMove: async (gameId: string, playerId: string, row: number, col: number): Promise<Game> => {
        const response = await axios.post(`${API_ENDPOINT}/${gameId}/move`, { playerId, row, col });
        return response.data;
    },

    toggleFlag: async (gameId: string, playerId: string, row: number, col: number): Promise<Game> => {
        const response = await axios.post(`${API_ENDPOINT}/${gameId}/flag`, { playerId, row, col });
        return response.data;
    }
}; 