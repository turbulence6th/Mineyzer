import React from 'react';
import { Cell as CellType } from '../services/GameService';
import './Cell.css';

interface CellProps {
    cell: CellType;
    onClick: (row: number, col: number) => void;
    onToggleFlag: (row: number, col: number) => void;
    isCurrentPlayerTurn: boolean;
    firstPlayerId?: string;
    currentPlayerId?: string;
    lastMoveRow: number;
    lastMoveCol: number;
}

const Cell: React.FC<CellProps> = ({ 
    cell, 
    onClick, 
    onToggleFlag, 
    isCurrentPlayerTurn, 
    firstPlayerId, 
    currentPlayerId,
    lastMoveRow,
    lastMoveCol 
}) => {
    const handleClick = (_event: React.MouseEvent<HTMLDivElement>) => {
        const isMyFlag = cell.flaggedByPlayerId && cell.flaggedByPlayerId === currentPlayerId;
        
        if (!cell.revealed && !isMyFlag) {
            onClick(cell.row, cell.column);
        }
    };

    const handleContextMenu = (event: React.MouseEvent<HTMLDivElement>) => {
        event.preventDefault();
        if (!cell.revealed && isCurrentPlayerTurn) {
            onToggleFlag(cell.row, cell.column);
        }
    };

    const getCellContent = () => {
        if (cell.flaggedByPlayerId) {
            return '🚩';
        }
        if (!cell.revealed) {
            return '';
        }
        if (cell.mine) {
            return '💣';
        }
        if (cell.adjacentMines > 0) {
            return cell.adjacentMines.toString();
        }
        return '';
    };

    const getCellClassName = () => {
        let className = 'cell';
        const isLastMove = cell.row === lastMoveRow && cell.column === lastMoveCol;

        if (cell.revealed) {
            className += ' revealed';
            if (cell.mine) {
                className += ' mine';
                if (isLastMove) {
                    className += ' mine-hit';
                }
            } else if (cell.adjacentMines > 0) {
                className += ` value-${cell.adjacentMines}`;
            } else {
                className += ' empty';
            }
        } else {
            className += ' hidden';
            if (cell.flaggedByPlayerId) {
                className += ' flagged';
                if (cell.flaggedByPlayerId === firstPlayerId) {
                    className += ' flagged-p1';
                } else {
                    className += ' flagged-p2';
                }
            }
        }
        
        if (isLastMove && !(cell.revealed && cell.mine)) {
             className += ' last-move';
        }

        return className;
    };

    return (
        <div 
            className={getCellClassName()} 
            onClick={handleClick}
            onContextMenu={handleContextMenu}
            style={{
                cursor: cell.revealed ? 'default' : (isCurrentPlayerTurn ? 'pointer' : 'not-allowed')
            }}
        >
            {getCellContent()}
        </div>
    );
};

export default Cell;