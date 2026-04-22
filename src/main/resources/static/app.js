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
    
    // Auto-join after creation
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

// Rules Modal
document.getElementById('btnRules').onclick = () => {
    document.getElementById('rulesModal').classList.remove('hidden');
};
document.getElementById('btnCloseRules').onclick = () => {
    document.getElementById('rulesModal').classList.add('hidden');
};

// Graveyard Modal Logic
document.getElementById('btnCloseGraveyard').onclick = () => {
    document.getElementById('graveyardModal').classList.add('hidden');
};

function openGraveyardModal() {
    if (!gameState || !gameState.graveyard) return;
    const modal = document.getElementById('graveyardModal');
    const grid = document.getElementById('graveyardGrid');
    const msg = document.getElementById('graveyardMessage');
    
    grid.innerHTML = '';
    
    // Determine if we are actively selecting for 9 or 10
    let isSelecting = false;
    let validRanks = [];
    if (selectedSourceCardId) {
        let sourceCard = null;
        gameState.players.forEach(p => {
            if (p.id === myPlayerId) {
                sourceCard = p.board.find(c => c.id === selectedSourceCardId);
            }
        });
        if (sourceCard && sourceCard.rank === 9) {
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
        
        if (isValidTarget) {
            // Also need to check if they already have the char card for 9
            if (c.rank >= 11 && c.rank <= 13) {
                let myChar = gameState.players.find(p => p.id === myPlayerId).board.find(bc => bc.rank === c.rank);
                if (myChar) isValidTarget = false; // Already have this character
            }
        }

        if (isSelecting) {
            if (isValidTarget) {
                el.style.cursor = 'pointer';
                el.style.boxShadow = "0 0 10px 2px #3b82f6";
                el.onclick = () => {
                    selectedTargetCardId = c.id;
                    selectedTargetPlayerId = myPlayerId; // self
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

// Action Bar Logic
const actionBar = document.getElementById('actionBar');
const actionStatus = document.getElementById('actionStatus');

function updateActionBar() {
    if (selectedSourceCardId) {
        actionBar.classList.remove('hidden');
        actionStatus.innerText = `Skill Ready. ${selectedTargetCardId || selectedTargetPlayerId ? '(Target Selected)' : '(Select target if required)'}`;
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
    stompClient.debug = null; // Disable spammy logs
    stompClient.connect({}, function (frame) {
        stompClient.subscribe('/topic/game/' + currentGameId, function (message) {
            gameState = JSON.parse(message.body);
            renderBoard();
        });
        stompClient.subscribe('/topic/events/' + currentGameId, function (message) {
            showEvent(message.body);
        });
        
        // Initial fetch to sync rendering
        fetch(`/api/game/${currentGameId}`)
            .then(r => r.json())
            .then(game => {
                gameState = game;
                renderBoard();
            });
    });
}

function renderBoard() {
    if (!gameState) return;
    
    // Global Status
    const isMyTurn = gameState.players[gameState.currentPlayerIdx]?.id === myPlayerId;
    const isGameStarted = gameState.deck.length > 0 || gameState.market.length > 0;
    
    if (gameState.globalSilence) {
        document.getElementById('globalSilenceBadge').classList.remove('hidden');
    } else {
        document.getElementById('globalSilenceBadge').classList.add('hidden');
    }

    if (!isGameStarted) {
        btnStartGame.classList.remove('hidden');
        turnIndicator.innerText = `Waiting to start... (${gameState.players.length} joined)`;
    } else {
        btnStartGame.classList.add('hidden');
        let currentPName = gameState.players[gameState.currentPlayerIdx].name;
        turnIndicator.innerText = isMyTurn ? `Your Turn (Actions: ${gameState.players[gameState.currentPlayerIdx].actionsLeft})` : `${currentPName}'s Turn`;
        
        if (isMyTurn) btnEndTurn.classList.remove('hidden');
        else btnEndTurn.classList.add('hidden');
        
        deckCount.innerText = gameState.deck.length;

        // Render Top of Graveyard
        const graveCount = document.getElementById('graveCount');
        const gravePile = document.getElementById('gravePile');
        
        // Ensure gravePile binds to open modal
        gravePile.onclick = () => openGraveyardModal();
        gravePile.style.cursor = 'pointer';
        
        // Make Zone glow if we have 9 or 10 selected
        let highlightGraveZone = false;
        if (selectedSourceCardId) {
            let myP = gameState.players.find(p => p.id === myPlayerId);
            if (myP) {
                let sc = myP.board.find(c => c.id === selectedSourceCardId);
                if (sc && (sc.rank === 9 || sc.rank === 10)) highlightGraveZone = true;
            }
        }
        
        if (highlightGraveZone) {
            gravePile.style.boxShadow = "0 0 20px 5px #eab308"; // Yellow pulse
            gravePile.title = "Click to select a target!";
        } else {
            gravePile.style.boxShadow = "none";
            gravePile.title = "";
        }

        if (gameState.graveyard && gameState.graveyard.length > 0) {
            graveCount.innerText = gameState.graveyard.length;
            let topGrave = gameState.graveyard[gameState.graveyard.length - 1];
            // Render it properly inside gravePile
            let mockGraveCard = createCardDOM(topGrave, false);
            gravePile.innerHTML = mockGraveCard.innerHTML;
            gravePile.className = mockGraveCard.className + " card-disabled"; // ensure disabled styling
        } else {
            graveCount.innerText = 0;
            gravePile.className = "card card-disabled";
            gravePile.innerHTML = "Empty";
        }
    }

    // Market
    marketCardsDiv.innerHTML = '';
    gameState.market.forEach(c => {
        let el = createCardDOM(c, true);
        el.onclick = () => {
            if (isMyTurn) sendAction('TAKE_MARKET', c.id, null, null);
        };
        marketCardsDiv.appendChild(el);
    });

    // Players
    playersArea.innerHTML = '';
    gameState.players.forEach((p, idx) => {
        let isCurrent = idx === gameState.currentPlayerIdx;
        let isMe = p.id === myPlayerId;
        
        let pdiv = document.createElement('div');
        pdiv.className = `player-dashboard glass-panel ${isCurrent ? 'active-turn' : ''}`;
        
        // --- Duplicate Detection ---
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
        // ---------------------------
        
        let tags = "";
        if(p.silenced) tags += `<span class="badge badge-silence">Silenced</span> `;
        if(p.slowed) tags += `<span class="badge badge-slow">Slowed</span>`;

        pdiv.innerHTML = `
            <div class="player-header">
                <span>${p.name} ${isMe ? '(You)' : ''} ${tags}</span>
                <span>Score: ${p.score} | AP: ${p.actionsLeft}</span>
            </div>
            <div class="player-board" id="board-${p.id}"></div>
        `;
        playersArea.appendChild(pdiv);

        let boardArea = document.getElementById(`board-${p.id}`);
        p.board.forEach(c => {
            let el = createCardDOM(c, false);
            
            // Skill Targeting Logic
            el.onclick = () => {
                if (!isMyTurn) return;
                
                if (isMe) {
                    // Selecting my card as Source
                    if (selectedSourceCardId === c.id) {
                        selectedSourceCardId = null; // deselect
                        selectedTargetCardId = null;
                        selectedTargetPlayerId = null;
                    } else {
                        selectedSourceCardId = c.id;
                        selectedTargetCardId = null;
                        selectedTargetPlayerId = null;
                    }
                    renderBoard();
                    updateActionBar();
                } else if (selectedSourceCardId) {
                    // Selected opponent's card as Target
                    selectedTargetCardId = c.id;
                    selectedTargetPlayerId = p.id;
                    renderBoard();
                    updateActionBar();
                }
            };

            if (selectedSourceCardId === c.id) {
                el.style.boxShadow = "0 0 15px 5px #22c55e"; // Green glow for selected source
                el.style.transform = "translateY(-10px)";
            }
            if (selectedTargetCardId === c.id) {
                el.style.boxShadow = "0 0 15px 5px #ef4444"; // Red glow for selected target
                el.style.transform = "translateY(-10px)";
            }
            
            boardArea.appendChild(el);
        });
        
        // Allow targeting the player directly
        pdiv.onclick = (e) => {
            if (!isMyTurn || isMe || !selectedSourceCardId) return;
            if (e.target.closest('.card')) return; // handled by card click
            
            selectedTargetPlayerId = p.id;
            selectedTargetCardId = null;
            renderBoard();
            updateActionBar();
        }
        pdiv.style.cursor = selectedSourceCardId && !isMe ? 'crosshair' : 'default';
        if(selectedTargetPlayerId === p.id && !selectedTargetCardId) {
            pdiv.style.borderColor = "#ef4444";
            pdiv.style.boxShadow = "0 0 15px rgba(239, 68, 68, 0.4)";
        }
    });
}

function createCardDOM(card, isMarket) {
    let div = document.createElement('div');
    
    // Classes
    let isRed = card.suit === 'HEART' || card.suit === 'DIAMOND';
    div.className = `card ${isRed ? 'red' : 'black'}`;
    if (card.tapped) div.classList.add('card-tapped');
    if (card.resurrected) div.classList.add('card-resurrected');
    if (card.disabled) div.classList.add('card-disabled');

    // Display values
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
            // Send discard action
            sendAction('DISCARD_DUPLICATE', c.id, null, null);
            modal.classList.add('hidden'); // optimistic hide
        };
        container.appendChild(el);
    });
    
    modal.classList.remove('hidden');
}

function showEvent(text) {
    const overlay = document.getElementById('eventOverlay');
    document.getElementById('eventText').innerText = text;
    overlay.classList.remove('hidden');
    
    // Hide after 5 seconds
    setTimeout(() => {
        overlay.classList.add('hidden');
    }, 5000);
}
