let stompClient = null;
let currentGameId = null;
let myPlayerId = null;
let myPlayerName = null;
let gameState = null;

let selectedSourceCardId = null;
let selectedTargetCardId = null;
let selectedTargetPlayerId = null;

// DOM Elements
const lobbyUI = document.getElementById('lobby');
const gameUI = document.getElementById('gameUI');
const displayGameId = document.getElementById('displayGameId');
const displayPlayerName = document.getElementById('displayPlayerName');
const turnIndicator = document.getElementById('turnIndicator');
const marketCardsDiv = document.getElementById('marketCards');
const playersArea = document.getElementById('playersArea');
const btnStartGame = document.getElementById('btnStartGame');
const btnEndTurn = document.getElementById('btnEndTurn');
const deckCount = document.getElementById('deckCount');

// Setup UI Handlers
document.getElementById('btnCreateGame').onclick = async () => {
    myPlayerName = document.getElementById('playerName').value || 'Mage 1';
    let res = await fetch('/api/game/create', { method: 'POST' });
    let game = await res.json();
    currentGameId = game.gameId;
    
    res = await fetch(`/api/game/join/${currentGameId}?playerName=${myPlayerName}`, { method: 'POST' });
    game = await res.json();
    myPlayerId = game.players[0].id;
    
    enterGameRoom();
};

document.getElementById('btnJoinGame').onclick = async () => {
    myPlayerName = document.getElementById('playerName').value || 'Mage 2';
    currentGameId = document.getElementById('joinGameId').value;
    if(!currentGameId) return alert('Enter Room ID');
    
    let res = await fetch(`/api/game/join/${currentGameId}?playerName=${myPlayerName}`, { method: 'POST' });
    let game = await res.json();
    myPlayerId = game.players[game.players.length - 1].id;
    
    enterGameRoom();
};

btnStartGame.onclick = async () => {
    await fetch(`/api/game/start/${currentGameId}`, { method: 'POST' });
};

btnEndTurn.onclick = () => {
    sendAction('END_TURN', null, null, null);
};

document.getElementById('btnRules').onclick = () => {
    document.getElementById('rulesModal').classList.remove('hidden');
};
document.getElementById('btnCloseRules').onclick = () => {
    document.getElementById('rulesModal').classList.add('hidden');
};

document.getElementById('btnCloseGraveyard').onclick = () => {
    document.getElementById('graveyardModal').classList.add('hidden');
};

function openGraveyardModal() {
    if (!gameState || !gameState.graveyard) return;
    const modal = document.getElementById('graveyardModal');
    const grid = document.getElementById('graveyardGrid');
    const msg = document.getElementById('graveyardMessage');
    
    grid.innerHTML = '';
    
    let isSelecting = false;
    let validRanks = [];
    if (selectedSourceCardId) {
        let sourceCard = null;
        gameState.players.forEach(p => {
            if (p.id === myPlayerId) {
                sourceCard = p.board.find(c => c.id === selectedSourceCardId);
            }
        });
        if (sourceCard && sourceCard.rank === 6) {
            isSelecting = true;
            validRanks = [3, 5, 7, 9];
        } else if (sourceCard && sourceCard.rank === 9) {
            isSelecting = true;
            validRanks = [11, 12, 13];
        } else if (sourceCard && sourceCard.rank === 10) {
            isSelecting = true;
            validRanks = [2, 4, 8];
        }
    }
    
    if (isSelecting) {
        msg.innerText = "⚡ โปรดเลือกการ์ดเป้าหมายที่ต้องการขุดขึ้นมา...";
        msg.style.color = "#22c55e";
    } else {
        msg.innerText = "รายชื่อไพ่ที่อยู่ในสุสานทั้งหมด";
        msg.style.color = "#ccc";
    }

    gameState.graveyard.forEach(c => {
        let el = createCardDOM(c, false);
        let isValidTarget = isSelecting && validRanks.includes(c.rank);
        
        if (isSelecting) {
            if (isValidTarget) {
                el.style.cursor = 'pointer';
                el.style.boxShadow = "0 0 10px 2px #3b82f6";
                el.onclick = () => {
                    selectedTargetCardId = c.id;
                    selectedTargetPlayerId = myPlayerId; 
                    modal.classList.add('hidden');
                    renderBoard();
                    updateActionBar();
                };
            } else {
                el.style.opacity = "0.3";
                el.style.cursor = 'not-allowed';
            }
        }
        
        grid.appendChild(el);
    });
    
    modal.classList.remove('hidden');
}

const actionBar = document.getElementById('actionBar');
const actionStatus = document.getElementById('actionStatus');

function updateActionBar() {
    if (selectedSourceCardId) {
        actionBar.classList.remove('hidden');
        actionStatus.innerText = `เตรียมร่ายมนตรา: ${selectedTargetCardId || selectedTargetPlayerId ? '(เลือกเป้าหมายแล้ว)' : '(โปรดเลือกเป้าหมายที่ต้องการ)'}`;
    } else {
        actionBar.classList.add('hidden');
    }
}

document.getElementById('btnExecuteSkill').onclick = () => {
    if (selectedSourceCardId) {
        sendAction('USE_SKILL', selectedSourceCardId, selectedTargetPlayerId, selectedTargetCardId);
        selectedSourceCardId = null;
        selectedTargetCardId = null;
        selectedTargetPlayerId = null;
        updateActionBar();
    }
};

document.getElementById('btnCancelAction').onclick = () => {
    selectedSourceCardId = null;
    selectedTargetCardId = null;
    selectedTargetPlayerId = null;
    renderBoard();
    updateActionBar();
};

function enterGameRoom() {
    lobbyUI.classList.add('hidden');
    gameUI.classList.remove('hidden');
    displayGameId.innerText = currentGameId;
    displayPlayerName.innerText = myPlayerName;
    connectWebSocket();
}

function connectWebSocket() {
    let socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; 
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/topic/game/' + currentGameId, function (message) {
            const newState = JSON.parse(message.body);
            if (gameState) {
                processStateChanges(gameState, newState);
            }
            gameState = newState;
            renderBoard();
        });
        stompClient.subscribe('/topic/events/' + currentGameId, function (message) {
            showEvent(message.body);
        });
        
        fetch(`/api/game/${currentGameId}`)
            .then(r => r.json())
            .then(game => {
                gameState = game;
                renderBoard();
            });
    });
}

function processStateChanges(oldState, newState) {
    if (oldState.currentPlayerIdx !== newState.currentPlayerIdx) {
        const nextPlayer = newState.players[newState.currentPlayerIdx];
        if (nextPlayer) {
            showToast(`ตาของ ${nextPlayer.name} แล้ว!`, 'info');
            addToActionLog(`🔄 <b>${nextPlayer.name}</b> เริ่มเทิร์นใหม่`);
        }
    }

    newState.players.forEach((newP, idx) => {
        const oldP = oldState.players.find(p => p.id === newP.id);
        if (!oldP) return;

        newP.board.forEach(newC => {
            const wasOnBoard = oldP.board.some(oldC => oldC.id === newC.id);
            if (!wasOnBoard) {
                const wasInMarket = oldState.market.some(mC => mC.id === newC.id);
                if (wasInMarket) {
                    addToActionLog(`🎴 <b>${newP.name}</b> หยิบ ${getCardName(newC)} จากตลาด`);
                    highlightCard(newC.id, 'highlight-target');
                } else {
                    let stolenFrom = null;
                    oldState.players.forEach(op => {
                        if (op.id !== newP.id && op.board.some(oc => oc.id === newC.id)) {
                            stolenFrom = op;
                        }
                    });
                    if (stolenFrom) {
                        addToActionLog(`💸 <b>${newP.name}</b> ชิง ${getCardName(newC)} มาจาก ${stolenFrom.name}!`);
                        showToast(`${newP.name} ชิง ${getCardName(newC)} มาได้สำเร็จ!`, 'warning');
                        highlightCard(newC.id, 'highlight-target');
                    }
                }
            } else {
                const oldC = oldP.board.find(c => c.id === newC.id);
                if (!oldC.tapped && newC.tapped) {
                    addToActionLog(`⚡ <b>${newP.name}</b> ร่ายอาคม ${getCardName(newC)}`);
                    highlightCard(newC.id, 'highlight-target');
                }
            }
        });
    });

    if (newState.graveyard.length > oldState.graveyard.length) {
        const newGraveCard = newState.graveyard[newState.graveyard.length - 1];
        let previousOwner = null;
        oldState.players.forEach(p => {
            if (p.board.some(c => c.id === newGraveCard.id)) previousOwner = p;
        });
        
        if (previousOwner) {
            addToActionLog(`💀 ${getCardName(newGraveCard)} ของ <b>${previousOwner.name}</b> สลายไปในสุสาน`);
        }
    }
}

function getCardName(card) {
    let rankStr = card.rank;
    if (card.rank === 1) rankStr = 'A';
    if (card.rank === 11) rankStr = 'J';
    if (card.rank === 12) rankStr = 'Q';
    if (card.rank === 13) rankStr = 'K';
    
    let suitSymbol = '♠️';
    if(card.suit === 'HEART') suitSymbol = '♥️';
    else if(card.suit === 'DIAMOND') suitSymbol = '♦️';
    else if(card.suit === 'CLUB') suitSymbol = '♣️';
    
    return `${rankStr}${suitSymbol}`;
}

function highlightCard(cardId, className) {
    setTimeout(() => {
        const elements = document.querySelectorAll(`[data-card-id="${cardId}"]`);
        elements.forEach(el => {
            el.classList.add(className);
            setTimeout(() => el.classList.remove(className), 3000);
        });
    }, 150);
}

function renderBoard() {
    if (!gameState) return;
    
    if (gameState.isGameOver || gameState.gameOver) {
        showGameOver(gameState.winnerId);
    }

    const isMyTurn = gameState.players[gameState.currentPlayerIdx]?.id === myPlayerId;
    const isGameStarted = gameState.deck.length > 0 || gameState.market.length > 0;
    
    if (gameState.globalSilence) {
        document.getElementById('globalSilenceBadge').classList.remove('hidden');
    } else {
        document.getElementById('globalSilenceBadge').classList.add('hidden');
    }

    if (!isGameStarted) {
        btnStartGame.classList.remove('hidden');
        turnIndicator.innerText = `รอเริ่มการแข่งขัน... (${gameState.players.length} คนเข้าร่วมแล้ว)`;
    } else {
        btnStartGame.classList.add('hidden');
        let currentPName = gameState.players[gameState.currentPlayerIdx].name;
        turnIndicator.innerText = isMyTurn ? `ตาของคุณ (เหลือ Actions: ${gameState.players[gameState.currentPlayerIdx].actionsLeft})` : `ตาของ ${currentPName}`;
        
        if (isMyTurn) btnEndTurn.classList.remove('hidden');
        else btnEndTurn.classList.add('hidden');
        
        deckCount.innerText = gameState.deck.length;

        const gravePile = document.getElementById('gravePile');
        gravePile.onclick = () => openGraveyardModal();
        gravePile.style.cursor = 'pointer';
        
        let highlightGraveZone = false;
        if (selectedSourceCardId) {
            let myP = gameState.players.find(p => p.id === myPlayerId);
            if (myP) {
                let sc = myP.board.find(c => c.id === selectedSourceCardId);
                if (sc && (sc.rank === 6 || sc.rank === 9 || sc.rank === 10)) highlightGraveZone = true;
            }
        }
        
        if (highlightGraveZone) {
            gravePile.style.boxShadow = "0 0 20px 5px #eab308";
            gravePile.title = "คลิกเพื่อเลือกเป้าหมายจากสุสาน!";
            
            let arrow = document.createElement('div');
            arrow.className = 'target-arrow';
            arrow.innerText = '👇';
            gravePile.appendChild(arrow);
        } else {
            gravePile.style.boxShadow = "none";
            gravePile.title = "";
            const existingArrow = gravePile.querySelector('.target-arrow');
            if (existingArrow) existingArrow.remove();
        }

        if (gameState.graveyard && gameState.graveyard.length > 0) {
            document.getElementById('graveCount').innerText = gameState.graveyard.length;
            let topGrave = gameState.graveyard[gameState.graveyard.length - 1];
            let mockGraveCard = createCardDOM(topGrave, false);
            gravePile.innerHTML = mockGraveCard.innerHTML;
            if (highlightGraveZone) {
                let arrow = document.createElement('div');
                arrow.className = 'target-arrow';
                arrow.innerText = '👇';
                gravePile.appendChild(arrow);
            }
            gravePile.className = mockGraveCard.className + " card-disabled";
        } else {
            document.getElementById('graveCount').innerText = 0;
            gravePile.className = "card card-disabled";
            gravePile.innerHTML = "Empty";
            if (highlightGraveZone) {
                let arrow = document.createElement('div');
                arrow.className = 'target-arrow';
                arrow.innerText = '👇';
                gravePile.appendChild(arrow);
            }
        }
    }

    marketCardsDiv.innerHTML = '';
    gameState.market.forEach(c => {
        let el = createCardDOM(c, true);
        el.onclick = () => {
            if (isMyTurn && !gameState.isGameOver && !gameState.gameOver) sendAction('TAKE_MARKET', c.id, null, null);
        };
        marketCardsDiv.appendChild(el);
    });

    playersArea.innerHTML = '';
    gameState.players.forEach((p, idx) => {
        let isCurrent = idx === gameState.currentPlayerIdx;
        let isMe = p.id === myPlayerId;
        
        let pdiv = document.createElement('div');
        pdiv.className = `player-dashboard glass-panel ${isCurrent ? 'active-turn' : ''}`;
        
        let hasDuplicates = false;
        if (isMe) {
            let rankCounts = { 11: [], 12: [], 13: [] };
            p.board.forEach(c => {
                if (c.rank >= 11 && c.rank <= 13) rankCounts[c.rank].push(c);
            });
            
            for (let rank in rankCounts) {
                if (rankCounts[rank].length > 1) {
                    hasDuplicates = true;
                    showDuplicateDiscardModal(rankCounts[rank]);
                    break;
                }
            }
        }
        
        if (isMe && !hasDuplicates) {
            document.getElementById('duplicateDiscardModal').classList.add('hidden');
        }
        
        let tags = "";
        if(p.silenced) tags += `<span class="badge badge-silence">ถูกใบ้</span> `;
        if(p.slowed) tags += `<span class="badge badge-slow">สโลว์</span>`;

        pdiv.innerHTML = `
            <div class="player-header">
                <span>${p.name} ${isMe ? '(คุณ)' : ''} ${tags}</span>
                <span>คะแนน: ${p.score} | AP: ${p.actionsLeft}</span>
            </div>
            <div class="player-board" id="board-${p.id}"></div>
        `;
        playersArea.appendChild(pdiv);

        let boardArea = document.getElementById(`board-${p.id}`);
        p.board.forEach(c => {
            let el = createCardDOM(c, false);
            
            el.onclick = (e) => {
                e.stopPropagation();
                if (!isMyTurn || gameState.isGameOver || gameState.gameOver) return;
                
                if (isMe) {
                    if (selectedSourceCardId === c.id) {
                        selectedSourceCardId = null;
                        selectedTargetCardId = null;
                        selectedTargetPlayerId = null;
                    } else {
                        if (c.tapped) {
                            showToast("ไพ่ใบนี้ถูกใช้งานไปแล้วในเทิร์นนี้", "warning");
                            return;
                        }
                        if (c.disabled) {
                            showToast("ไพ่ใบนี้ติดสถานะคำสาป ไม่สามารถใช้งานได้", "warning");
                            return;
                        }
                        selectedSourceCardId = c.id;
                        selectedTargetCardId = null;
                        selectedTargetPlayerId = null;
                    }
                    renderBoard();
                    updateActionBar();
                } else if (selectedSourceCardId) {
                    if (isValidTarget(c, p.id)) {
                        selectedTargetCardId = c.id;
                        selectedTargetPlayerId = p.id;
                        renderBoard();
                        updateActionBar();
                    } else {
                        showToast("เป้าหมายนี้ไม่สามารถรับผลของสกิลนี้ได้", "warning");
                    }
                }
            };

            if (selectedSourceCardId === c.id) {
                el.style.boxShadow = "0 0 15px 5px #22c55e";
                el.style.transform = "translateY(-10px)";
            } else if (selectedSourceCardId && !isMe && isValidTarget(c, p.id)) {
                el.style.boxShadow = "0 0 15px #fbbf24";
                let arrow = document.createElement('div');
                arrow.className = 'target-arrow';
                arrow.innerText = '👇';
                el.appendChild(arrow);
            }

            if (selectedTargetCardId === c.id) {
                el.style.boxShadow = "0 0 15px 5px #ef4444";
                el.style.transform = "translateY(-10px)";
            }
            
            boardArea.appendChild(el);
        });
        
        pdiv.onclick = (e) => {
            if (!isMyTurn || isMe || !selectedSourceCardId || gameState.isGameOver || gameState.gameOver) return;
            
            if (isValidPlayerTarget(p.id)) {
                selectedTargetPlayerId = p.id;
                selectedTargetCardId = null;
                renderBoard();
                updateActionBar();
            } else {
                showToast("สกิลนี้จำเป็นต้องเลือกเป้าหมายเป็น 'ไพ่' บนบอร์ดเท่านั้น", "warning");
            }
        }
        
        if (selectedSourceCardId && !isMe && isValidPlayerTarget(p.id)) {
             pdiv.classList.add('valid-player-target');
             let arrow = document.createElement('div');
             arrow.className = 'target-arrow';
             arrow.style.top = "-50px";
             arrow.innerText = '🎯';
             pdiv.appendChild(arrow);
        }

        if(selectedTargetPlayerId === p.id && !selectedTargetCardId) {
            pdiv.style.borderColor = "#ef4444";
            pdiv.style.boxShadow = "0 0 15px rgba(239, 68, 68, 0.4)";
        }
    });
}

function isValidTarget(card, ownerId) {
    if (!selectedSourceCardId) return false;
    let myP = gameState.players.find(p => p.id === myPlayerId);
    let srcCard = myP.board.find(c => c.id === selectedSourceCardId);
    if (!srcCard) return false;

    switch (srcCard.rank) {
        case 11: // Jack
            return true; 
        case 12: // Queen
            return true;
        case 2: // Curse
            return false;
    }
    return false;
}

function isValidPlayerTarget(targetPId) {
    if (!selectedSourceCardId) return false;
    let myP = gameState.players.find(p => p.id === myPlayerId);
    let srcCard = myP.board.find(c => c.id === selectedSourceCardId);
    if (!srcCard) return false;

    return [2, 3, 7, 5, 11, 12].includes(srcCard.rank);
}

function showGameOver(winnerId) {
    const winner = gameState.players.find(p => p.id === winnerId);
    const winnerName = winner ? winner.name : "Unknown";
    showEvent(`🏆 จบการแข่งขัน! 🏆\nผู้ชนะคือ: ${winnerName}`);
}

function createCardDOM(card, isMarket) {
    let div = document.createElement('div');
    div.setAttribute('data-card-id', card.id);
    
    let isRed = card.suit === 'HEART' || card.suit === 'DIAMOND';
    div.className = `card ${isRed ? 'red' : 'black'}`;
    if (card.tapped) div.classList.add('card-tapped');
    if (card.resurrected) div.classList.add('card-resurrected');
    if (card.disabled) div.classList.add('card-disabled');

    let suitSymbol = '♠️';
    if(card.suit === 'HEART') suitSymbol = '♥️';
    else if(card.suit === 'DIAMOND') suitSymbol = '♦️';
    else if(card.suit === 'CLUB') suitSymbol = '♣️';

    let rankStr = card.rank;
    let skillStr = "";
    if (card.rank === 1) { rankStr = 'A'; skillStr = 'Energy'; }
    if (card.rank === 11) { rankStr = 'J'; skillStr = 'Destroy'; }
    if (card.rank === 12) { rankStr = 'Q'; skillStr = 'Steal'; }
    if (card.rank === 13) { rankStr = 'K'; skillStr = 'Finisher'; }
    if (card.rank === 2) skillStr = 'Curse';
    if (card.rank === 3) skillStr = 'Silence';
    if (card.rank === 4) skillStr = 'Draw';
    if (card.rank === 5) skillStr = 'Refresh';
    if (card.rank === 6) skillStr = 'Odd Dig';
    if (card.rank === 7) skillStr = 'Slow';
    if (card.rank === 8) skillStr = 'Haste';
    if (card.rank === 9) skillStr = 'Revive';
    if (card.rank === 10) skillStr = 'Even Dig';

    div.innerHTML = `
        <div class="suit" style="position: absolute; top: 5px; left: 5px; font-size:1rem;">${suitSymbol}</div>
        <div class="rank">${rankStr}</div>
        <div class="skill-name">${skillStr}</div>
    `;
    return div;
}

function sendAction(type, sourceId, targetPId, targetCId) {
    let cmd = {
        actionType: type,
        sourceCardId: sourceId,
        targetPlayerId: targetPId,
        targetCardId: targetCId
    };
    stompClient.send("/app/game.action", {}, JSON.stringify({
        gameId: currentGameId,
        playerId: myPlayerId,
        command: cmd
    }));
}

function showDuplicateDiscardModal(cards) {
    const modal = document.getElementById('duplicateDiscardModal');
    const container = document.getElementById('duplicateCardsContainer');
    container.innerHTML = '';
    
    cards.forEach(c => {
        let el = createCardDOM(c, false);
        el.style.cursor = 'pointer';
        el.onclick = () => {
            sendAction('DISCARD_DUPLICATE', c.id, null, null);
            modal.classList.add('hidden');
        };
        container.appendChild(el);
    });
    
    modal.classList.remove('hidden');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerText = message;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.remove();
    }, 4000);
}

function addToActionLog(message) {
    const logContent = document.getElementById('logContent');
    const entry = document.createElement('div');
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    entry.innerHTML = `<span style="color:var(--primary); font-size:0.7rem;">[${time}]</span> ${message}`;
    logContent.appendChild(entry);
    logContent.scrollTop = logContent.scrollHeight;
}

function showEvent(text) {
    addToActionLog(`✨ ${text}`);
    showToast(text, 'warning');

    const overlay = document.getElementById('eventOverlay');
    document.getElementById('eventText').innerText = text;
    overlay.classList.remove('hidden');
    
    setTimeout(() => {
        overlay.classList.add('hidden');
    }, 5000);
}
