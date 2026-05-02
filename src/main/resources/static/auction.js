let stompClient = null;
let currentGameId = null;
let myPlayerId = null;
let myPlayerName = null;
let gameState = null;

function getCardName(card) {
    if (!card) return "Unknown Card";
    if (card.faceDown) return "Mystery Card";
    let suit = '♠';
    if(card.suit === 'HEART') suit = '♥';
    else if(card.suit === 'DIAMOND') suit = '♦';
    else if(card.suit === 'CLUB') suit = '♣';

    let rank = card.rank;
    if (card.rank === 1) rank = 'A';
    if (card.rank === 11) rank = 'J';
    if (card.rank === 12) rank = 'Q';
    if (card.rank === 13) rank = 'K';

    return `[${rank}${suit}]`;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
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
    if (!logContent) return;
    const entry = document.createElement('div');
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    entry.innerHTML = `<span style="color:var(--primary); font-size:0.7rem;">[${time}]</span> ${message}`;
    logContent.appendChild(entry);
    logContent.scrollTop = logContent.scrollHeight;
}

// DOM Elements
const lobbyUI = document.getElementById('lobby');
const gameUI = document.getElementById('gameUI');
const displayGameId = document.getElementById('displayGameId');
const displayPlayerName = document.getElementById('displayPlayerName');
const turnIndicator = document.getElementById('turnIndicator');
const marketCardsDiv = document.getElementById('marketCards');
const masterCardZone = document.getElementById('masterCardZone');
const activeBidsZone = document.getElementById('activeBidsZone');
const deckCount = document.getElementById('deckCount');
const playersArea = document.getElementById('playersArea');
const btnStartGame = document.getElementById('btnStartGame');

// J Skill DOM
const jSkillModal = document.getElementById('jSkillModal');
const jSkillDeckCards = document.getElementById('jSkillDeckCards');
const jSkillMarketCards = document.getElementById('jSkillMarketCards');
const btnConfirmJSkill = document.getElementById('btnConfirmJSkill');
let selectedJDeckCardId = null;
let selectedJMarketCardId = null;

// Settings DOM
const hostSettings = document.getElementById('hostSettings');
const inputHandSize = document.getElementById('inputHandSize');
const checkAutoHandSize = document.getElementById('checkAutoHandSize');

// Scoreboard DOM
const scoreModal = document.getElementById('scoreModal');
const scoreTitle = document.getElementById('scoreTitle');
const btnBackToPortal = document.getElementById('btnBackToPortal');
const btnShowRules = document.getElementById('btnShowRules');
const btnCloseRules = document.getElementById('btnCloseRules');
const rulesModal = document.getElementById('rulesModal');
const btnToggleLang = document.getElementById('btnToggleLang');

let currentLang = 'TH';
const translations = {
    TH: {
        rulesBtn: "📜 กฎการเล่น",
        handSize: "จำนวนไพ่:",
        autoBalance: "ปรับอัตโนมัติ",
        startBtn: "เริ่มการประมูล",
        deckTitle: "สำรับ",
        masterTitle: "ใบสั่งการ",
        marketTitle: "ตลาด",
        activeBids: "การประมูลปัจจุบัน",
        rulesTitle: "📜 กฎการเล่น",
        closeRulesBtn: "เข้าใจแล้ว!",
        roundResultsTitle: "🏆 สรุปผลการประมูล",
        nextRoundBtn: "รอบต่อไป",
        playAgainBtn: "เล่นอีกครั้ง",
        backToPortalBtn: "กลับหน้าหลัก",
        waitingStatus: "กำลังรอผู้เล่น...",
        waitingToStart: "รอเริ่มเกม...",
        yourHand: "ไพ่บนมือ",
        scorePile: "กองคะแนน",
        jackTitle: "เล่ห์เหลี่ยมของ Jack",
        jackPick: "เลือก 1 ใบจากกองเพื่อสลับลงตลาด",
        legacyTitle: "เลือกไพ่มรดก",
        legacyPick: "ทุกคนเลือกไพ่ 1 ใบเพื่อเก็บไว้ใช้ต่อ",
        scoreDetails: "รายละเอียดคะแนน",
        pts: "แต้ม",
        rulesHTML: `
            <h3 style="color:#60a5fa;">1. ใบสั่งการ (Master Card)</h3>
            <p>จั่วตอนเริ่มรอบใหญ่ เพื่อกำหนด:</p>
            <ul>
                <li><strong>เลขนำโชค:</strong> ใครเก็บเลขนี้ได้ รับ <strong>+10 แต้ม</strong></li>
                <li><strong>ดอกนำโชค:</strong> ใครสะสมดอกนี้ได้มากที่สุดตอนจบ รับ <strong>+20 แต้ม</strong></li>
                <li><strong>เหตุการณ์พิเศษ (10, J, Q, K, A):</strong>
                    <ul>
                        <li>🔴 สีแดง: ตลาดคว่ำหน้า (Mystery Market)</li>
                        <li>⚫ สีดำ: ทุกคนได้ไพ่ (ไม่มีแต้ม x2)</li>
                    </ul>
                </li>
            </ul>
            <h3 style="color:#60a5fa;">2. ขั้นตอนการเล่น (Round Flow)</h3>
            <p><strong>STEP 1: การประมูล (BIDDING)</strong></p>
            <ul>
                <li>ทุกคนลงไพ่คว่ำ 1 ใบแล้วหงายพร้อมกัน</li>
                <li>ความใหญ่: K > Q > J > 10... > 2 > A (เล็กสุด)</li>
                <li>ถ้าเลขเท่ากันดูดอก: ♠ > ♥ > ♦ > ♣</li>
            </ul>
            <p><strong>STEP 2: สรุปผล (RESOLUTION)</strong></p>
            <ul>
                <li><strong>อันดับ 1:</strong> หยิบของก่อน 1 ใบ + ใช้พลังขุนพล (ถ้ามี)</li>
                <li><strong>อันดับกลาง:</strong> หยิบของที่เหลือตามลำดับความใหญ่</li>
                <li><strong>ที่โหล่ (Underdog):</strong> ไม่ได้หยิบของตลาด แต่เอาไพ่ตัวเองไป <strong>"คิดคะแนน x2"</strong></li>
            </ul>
            <h3 style="color:#60a5fa;">3. พลังขุนพล (เฉพาะที่ 1)</h3>
            <ul>
                <li><strong>J:</strong> เลือกสลับไพ่จากกองจั่วลงตลาด</li>
                <li><strong>Q:</strong> จั่วไพ่เพิ่ม 1 ใบขึ้นมือ</li>
                <li><strong>K:</strong> จั่วไพ่เพิ่ม 1 ใบลงกองคะแนนฟรีๆ</li>
                <li><strong>A:</strong> ถ้าเป็นที่โหล่ (Underdog) รับ 20 แต้มทันที!</li>
            </ul>
            <h3 style="color:#60a5fa;">4. การคิดคะแนน (จบรอบใหญ่)</h3>
            <ul>
                <li><strong>แต้มปกติ:</strong> ตามหน้าไพ่ (J,Q,K=10 / A=1) และแต้มที่โหล่ x2</li>
                <li><strong>เลขนำโชค:</strong> ใบละ +10 แต้ม</li>
                <li><strong>ดอกนำโชค:</strong> ใครมีดอกนั้นเยอะที่สุด รับ +20 แต้มต่อดอก</li>
            </ul>
            <h3 style="color:#60a5fa;">5. ระบบเชื่อมต่อรอบ (Legacy & Status)</h3>
            <ul>
                <li><strong>Legacy Card:</strong> ทุกคนเลือกไพ่ 1 ใบจากกองคะแนนขึ้นมือไว้ใช้ต่อ</li>
                <li><strong>เจ้าสัว (ที่ 1):</strong> รอบหน้าจั่วไพ่น้อยลง 1 ใบ</li>
                <li><strong>ยาจก (ที่โหล่):</strong> รอบหน้าจั่วไพ่เพิ่มขึ้น 1 ใบ</li>
            </ul>
        `
    },
    EN: {
        rulesBtn: "📜 Rules",
        handSize: "Hand Size:",
        autoBalance: "Auto Balance",
        startBtn: "Start Auction",
        deckTitle: "DECK",
        masterTitle: "MASTER",
        marketTitle: "MARKET",
        activeBids: "ACTIVE BIDS",
        rulesTitle: "📜 Game Rules",
        closeRulesBtn: "Got it!",
        roundResultsTitle: "🏆 Round Results",
        nextRoundBtn: "Next Round",
        playAgainBtn: "Play Again",
        backToPortalBtn: "Back to Portal",
        waitingStatus: "Waiting for players...",
        waitingToStart: "Waiting to start...",
        yourHand: "Hand",
        scorePile: "Score Pile",
        jackTitle: "Jack's Trickery",
        jackPick: "Pick 1 from deck to swap",
        legacyTitle: "Pick Legacy Card",
        legacyPick: "Everyone picks 1 card to keep",
        scoreDetails: "Score Details",
        pts: "pts",
        rulesHTML: `
            <h3 style="color:#60a5fa;">1. MasterCard (The Order)</h3>
            <p>Drawn at the start of each round. Determines:</p>
            <ul>
                <li><strong>Lucky Rank:</strong> Collecting this rank gives <strong>+10 pts</strong>.</li>
                <li><strong>Lucky Suit:</strong> The player with the most cards of this suit at the end gets <strong>+20 pts</strong>.</li>
                <li><strong>Events (10, J, Q, K, A):</strong>
                    <ul>
                        <li>🔴 Red: Mystery Market (Face down)</li>
                        <li>⚫ Black: Everyone gets a card (No Underdog x2)</li>
                    </ul>
                </li>
            </ul>
            <h3 style="color:#60a5fa;">2. Round Flow</h3>
            <p><strong>STEP 1: BIDDING</strong></p>
            <ul>
                <li>Everyone plays 1 card face down, then reveals.</li>
                <li>Power: K > Q > J > 10... > 2 > A (Smallest).</li>
                <li>Tie-break (Suits): ♠ > ♥ > ♦ > ♣.</li>
            </ul>
            <p><strong>STEP 2: RESOLUTION</strong></p>
            <ul>
                <li><strong>1st Place:</strong> Pick 1 card + Hero skill (if any).</li>
                <li><strong>Middle:</strong> Pick remaining cards in order.</li>
                <li><strong>Underdog:</strong> No market card, but their own card scores <strong>x2 points</strong>.</li>
            </ul>
            <h3 style="color:#60a5fa;">3. Hero Skills (Rank 1st)</h3>
            <ul>
                <li><strong>J:</strong> Swap 1 card from deck with 1 in market.</li>
                <li><strong>Q:</strong> Draw 1 extra card to your hand.</li>
                <li><strong>K:</strong> Draw 1 extra card directly to your score pile.</li>
                <li><strong>A:</strong> If Underdog, Ace scores 20 pts!</li>
            </ul>
            <h3 style="color:#60a5fa;">4. Scoring (End of Round)</h3>
            <ul>
                <li><strong>Base Points:</strong> Face value (J,Q,K=10 / A=1) and Underdog x2.</li>
                <li><strong>Lucky Rank:</strong> +10 pts per card.</li>
                <li><strong>Lucky Suit:</strong> Player with the most cards of that suit gets +20 pts.</li>
            </ul>
            <h3 style="color:#60a5fa;">5. Legacy & Status</h3>
            <ul>
                <li><strong>Legacy Card:</strong> Pick 1 card from your score pile to keep for next round.</li>
                <li><strong>Rich (1st Place):</strong> Start next round with -1 card.</li>
                <li><strong>Poor (Underdog):</strong> Start next round with +1 card.</li>
            </ul>
        `
    }
};

function updateLanguage() {
    const t = translations[currentLang];
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (t[key]) el.innerText = t[key];
    });
    document.getElementById('rulesContent').innerHTML = t.rulesHTML;
}

// Setup UI Handlers
document.getElementById('btnCreateGame').onclick = async () => {
    myPlayerName = document.getElementById('playerName').value || 'Bidder 1';
    let res = await fetch('/api/auction/create', { method: 'POST' });
    let game = await res.json();
    currentGameId = game.gameId;
    
    res = await fetch(`/api/auction/join/${currentGameId}?playerName=${myPlayerName}`, { method: 'POST' });
    game = await res.json();
    myPlayerId = game.players[0].id;
    
    enterGameRoom();
};

document.getElementById('btnJoinGame').onclick = async () => {
    myPlayerName = document.getElementById('playerName').value || 'Bidder 2';
    currentGameId = document.getElementById('joinGameId').value;
    if(!currentGameId) return alert('Enter Room ID');
    
    let res = await fetch(`/api/auction/join/${currentGameId}?playerName=${myPlayerName}`, { method: 'POST' });
    let game = await res.json();
    myPlayerId = game.players[game.players.length - 1].id;
    
    enterGameRoom();
};

btnStartGame.onclick = async () => {
    await fetch(`/api/auction/start/${currentGameId}`, { method: 'POST' });
};

btnConfirmJSkill.onclick = () => {
    stompClient.send("/app/auction.action", {}, JSON.stringify({
        gameId: currentGameId,
        playerId: myPlayerId,
        command: { actionType: 'USE_SKILL_J', sourceCardId: selectedJDeckCardId, targetCardId: selectedJMarketCardId }
    }));
};

btnNextRound.onclick = async () => {
    if (gameState.phase === "LEGACY_PICK") {
        let unpicked = gameState.players.filter(p => p.scorePile.length > 0 && !p.legacyCardId);
        if (unpicked.length > 0) {
            let names = unpicked.map(p => p.name).join(', ');
            alert(`⚠️ Wait! ${names} still picking their Legacy Card.`);
            return;
        }
    }
    await fetch(`/api/auction/next/${currentGameId}`, { method: 'POST' });
};

btnResetToLobby.onclick = async () => {
    if (confirm("Reset game back to lobby? All scores will be cleared.")) {
        await fetch(`/api/auction/reset/${currentGameId}`, { method: 'POST' });
    }
};

btnShowRules.onclick = () => rulesModal.classList.remove('hidden');
btnCloseRules.onclick = () => rulesModal.classList.add('hidden');

btnToggleLang.onclick = () => {
    currentLang = currentLang === 'TH' ? 'EN' : 'TH';
    updateLanguage();
};

updateLanguage(); // Initial load

inputHandSize.onchange = () => syncSettings();
checkAutoHandSize.onchange = () => syncSettings();

function syncSettings() {
    if (gameState.players[0].id === myPlayerId) {
        stompClient.send("/app/auction.action", {}, JSON.stringify({
            gameId: currentGameId,
            playerId: myPlayerId,
            command: { 
                actionType: 'UPDATE_SETTINGS', 
                handSize: parseInt(inputHandSize.value), 
                autoHandSize: checkAutoHandSize.checked 
            }
        }));
    }
}

function enterGameRoom() {
    lobbyUI.classList.add('hidden');
    gameUI.classList.remove('hidden');
    displayGameId.innerText = currentGameId;
    displayPlayerName.innerText = myPlayerName;
    connectWebSocket();
}

function connectWebSocket() {
    console.log("Connecting to WebSocket...");
    let socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; 
    
    stompClient.connect({}, function (frame) {
        console.log("Connected: " + frame);
        stompClient.subscribe('/topic/auction/' + currentGameId, function (message) {
            const newState = JSON.parse(message.body);
            handleStateTransition(newState);
            gameState = newState;
            renderBoard();
        });
        
        // Initial state fetch
        fetch(`/api/auction/${currentGameId}`)
            .then(r => r.json())
            .then(data => {
                console.log("Initial state loaded", data);
                gameState = data;
                renderBoard();
            })
            .catch(err => console.error("Error fetching state:", err));
    }, function(error) {
        console.error("STOMP error", error);
    });
}

function handleStateTransition(newState) {
    if (!gameState) return;

    // 1. Detect Individual Actions (Bidding) FIRST while still in BIDDING phase or transitioning out
    newState.players.forEach(newP => {
        const oldP = gameState.players.find(p => p.id === newP.id);
        if (oldP && newP.hasBid && !oldP.hasBid) {
            addToActionLog(`✉️ <b>${newP.name}</b> ลงประมูลแล้ว`);
            if (newP.id === myPlayerId) showToast("Bid placed!", "success");
        }
    });

    // 2. Phase Change Notifications
    if (newState.phase !== gameState.phase) {
        if (newState.phase === "RESOLUTION") {
            // Log that bidding is complete BEFORE showing results
            showToast("Bidding Complete! Revealing cards...", "success");
            addToActionLog("🔮 <b>การประมูลสิ้นสุด</b> เริ่มการตัดสินผล!");

            // Detect Auto-skills (Q, K) for the winner - only if transition just happened
            const winner = newState.players.find(p => p.isFirstPlace);
            if (winner && winner.currentBid) {
                const rank = winner.currentBid.rank;
                if (rank === 12) { // Queen
                    addToActionLog(`👸 <b>${winner.name}</b> ใช้พลัง Queen จั่วไพ่เพิ่ม 1 ใบขึ้นมือ!`);
                    if (winner.id === myPlayerId) showToast("Queen's Power: +1 Card in hand", "success");
                } else if (rank === 13) { // King
                    addToActionLog(`🤴 <b>${winner.name}</b> ใช้พลัง King จั่วไพ่เพิ่ม 1 ใบลงกองคะแนน!`);
                    if (winner.id === myPlayerId) showToast("King's Power: +1 Card in score pile", "success");
                }
            }

            // Detect Ace Underdog
            const underdog = newState.players.find(p => p.isUnderdog);
            if (underdog && underdog.currentBid && underdog.currentBid.rank === 1 && !newState.blackEvent) {
                addToActionLog(`💀 <b>${underdog.name}</b> เปิดตำนาน Ace Underdog รับแต้มพิเศษ!`);
                if (underdog.id === myPlayerId) showToast("Ace Underdog: 20 pts!", "warning");
            }
        } else if (newState.phase === "BIDDING") {
            showToast("New Bidding Round Started!", "info");
            addToActionLog("🎭 <b>รอบใหม่เริ่มขึ้นแล้ว</b> โปรดวางหมากของท่าน");
        } else if (newState.phase === "J_SKILL") {
            const pickerId = newState.pickOrderPlayerIds[0];
            const picker = newState.players.find(p => p.id === pickerId);
            if (picker) {
                showToast(`${picker.name} is using Jack's Skill!`, "warning");
                addToActionLog(`🃏 <b>${picker.name}</b> กำลังใช้พลังของ Jack`);
            }
        } else if (newState.phase === "ROUND_END") {
            addToActionLog("🏆 <b>จบรอบใหญ่</b> กำลังสรุปคะแนน...");
        }
    }

    // 3. Picking from Market detection
    if (gameState.phase === "RESOLUTION" && newState.market.length < gameState.market.length) {
        const lastPickerId = gameState.pickOrderPlayerIds[0];
        const picker = newState.players.find(p => p.id === lastPickerId);
        const pickedCard = gameState.market.find(c => !newState.market.some(nc => nc.id === c.id));
        if (picker && pickedCard) {
            addToActionLog(`🎁 <b>${picker.name}</b> เลือก ${getCardName(pickedCard)} เข้ากองคะแนน`);
            if (picker.id === myPlayerId) showToast(`You picked ${getCardName(pickedCard)}`, "success");
        }
    }

    // 4. J Skill completion
    if (gameState.phase === "J_SKILL" && newState.phase !== "J_SKILL") {
        const pickerId = gameState.pickOrderPlayerIds[0];
        const picker = newState.players.find(p => p.id === pickerId);
        if (picker) {
            addToActionLog(`🃏 <b>${picker.name}</b> ใช้เล่ห์เหลี่ยมสลับไพ่ในตลาดเสร็จสิ้น`);
        }
    }
}

function sendAction(type, sourceId) {
    let cmd = { actionType: type, sourceCardId: sourceId };
    stompClient.send("/app/auction.action", {}, JSON.stringify({
        gameId: currentGameId,
        playerId: myPlayerId,
        command: cmd
    }));
}

function renderBoard() {
    displayPlayerName.innerText = myPlayerName;
    
    const eventDiv = document.getElementById('eventDescription');
    const eventBanner = document.getElementById('eventBanner');
    if (gameState.eventDescription) {
        eventDiv.innerText = gameState.eventDescription;
        eventBanner.classList.remove('hidden');
        eventBanner.classList.add('active-event-glow');
    } else {
        eventDiv.innerText = "";
        eventBanner.classList.add('hidden');
        eventBanner.classList.remove('active-event-glow');
    }
    
    if (!gameState) return;
    
    const isGameStarted = gameState.phase !== "WAITING_FOR_PLAYERS";
    
    if (!isGameStarted) {
        const isHost = gameState.players[0].id === myPlayerId;
        
        if (isHost) {
            btnStartGame.classList.remove('hidden');
        } else {
            btnStartGame.classList.add('hidden');
            // Only update inputs for non-hosts to avoid flickering/interruption for host
            inputHandSize.value = gameState.initialHandSize;
            checkAutoHandSize.checked = gameState.autoHandSize;
        }
        
        inputHandSize.disabled = !isHost || checkAutoHandSize.checked;
        checkAutoHandSize.disabled = !isHost;
        hostSettings.classList.remove('hidden'); // Always show in waiting room
        
        const t = translations[currentLang];
        turnIndicator.innerText = `${t.waitingToStart} (${gameState.players.length} joined)`;
        
        playersArea.innerHTML = '';
        gameState.players.forEach(p => {
            let pdiv = document.createElement('div');
            pdiv.className = `player-dashboard glass-panel`;
            pdiv.style.minWidth = "250px";
            pdiv.innerHTML = `
                <div class="player-header">
                    <span>${p.name} ${p.id === myPlayerId ? '(You)' : ''}</span>
                </div>
                <div style="font-size:0.9rem; color:#94a3b8; margin-top:10px; text-align:center;">Status: Ready</div>
            `;
            playersArea.appendChild(pdiv);
        });
        
        masterCardZone.innerHTML = '<div class="card card-disabled" style="font-size:1rem;">Wait</div>';
        marketCardsDiv.innerHTML = '';
        activeBidsZone.innerHTML = '';
        return;
    }
    
    btnStartGame.classList.add('hidden');
    hostSettings.classList.add('hidden');
    
    // Status text
    if (gameState.phase === "BIDDING") {
        turnIndicator.innerText = "Phase: Secret Bidding! Select a card.";
    } else if (gameState.phase === "RESOLUTION") {
        let currentPickerId = gameState.pickOrderPlayerIds[0];
        let pName = gameState.players.find(p => p.id === currentPickerId)?.name;
        turnIndicator.innerText = currentPickerId === myPlayerId ? "YOUR TURN TO PICK FROM MARKET!" : `Waiting for ${pName} to pick...`;
    } else if (gameState.phase === "J_SKILL") {
        let currentPickerId = gameState.pickOrderPlayerIds[0];
        let pName = gameState.players.find(p => p.id === currentPickerId)?.name;
        turnIndicator.innerText = currentPickerId === myPlayerId ? "Use your Jack Skill!" : `Waiting for ${pName} to use Jack Skill...`;
    } else if (gameState.phase === "LEGACY_PICK") {
        turnIndicator.innerText = "Pick 1 Legacy Card to keep for next round!";
        renderScoreboard();
    } else if (gameState.phase === "ROUND_END" || gameState.phase === "GAME_OVER") {
        turnIndicator.innerText = gameState.phase === "GAME_OVER" ? "GAME OVER! Final Tally." : `Round ${gameState.round} Ended!`;
        renderScoreboard();
    }

    // Update deck count
    if (deckCount) {
        deckCount.innerText = gameState.deck ? gameState.deck.length : 0;
    }

    // Master Card
    masterCardZone.innerHTML = '';
    if (gameState.masterCard) {
        masterCardZone.appendChild(createCardDOM(gameState.masterCard));
    }

    // Market
    marketCardsDiv.innerHTML = '';
    gameState.market.forEach(c => {
        let el = createCardDOM(c);
        if (gameState.phase === "RESOLUTION" && gameState.pickOrderPlayerIds[0] === myPlayerId) {
            el.style.boxShadow = "0 0 15px 5px #22c55e";
            el.onclick = () => sendAction('PICK_MARKET', c.id);
        }
        marketCardsDiv.appendChild(el);
    });

    // Active Bids
    activeBidsZone.innerHTML = '';
    gameState.players.forEach(p => {
        if (p.currentBid) {
            let bidContainer = document.createElement('div');
            bidContainer.style.textAlign = "center";
            let nameTag = document.createElement('div');
            nameTag.innerText = p.name;
            nameTag.style.marginBottom = "5px";
            nameTag.style.fontSize = "0.8rem";
            nameTag.style.color = p.isUnderdog ? "#ef4444" : (p.isFirstPlace ? "#eab308" : "#ccc");
            
            let cardEl;
            // If in bidding phase, hide other people's bids
            if (gameState.phase === "BIDDING" && p.id !== myPlayerId) {
                cardEl = document.createElement('div');
                cardEl.className = "card card-back";
            } else {
                cardEl = createCardDOM(p.currentBid);
            }
            
            if (p.isFirstPlace) {
                cardEl.style.boxShadow = "0 0 20px 5px #eab308";
                cardEl.classList.add("highlight-target");
            }
            if (p.isUnderdog) {
                cardEl.style.filter = "grayscale(80%)";
                cardEl.style.boxShadow = "0 0 15px #ef4444";
            }

            bidContainer.appendChild(nameTag);
            bidContainer.appendChild(cardEl);
            activeBidsZone.appendChild(bidContainer);
        }
    });

    // Players Area
    playersArea.innerHTML = '';
    gameState.players.forEach(p => {
        let isMe = p.id === myPlayerId;
        
        let pdiv = document.createElement('div');
        pdiv.className = `player-dashboard glass-panel`;
        pdiv.style.minWidth = "250px";
        
        let tags = p.isFirstPlace ? "👑 " : (p.isUnderdog ? "💀 " : "");
        pdiv.innerHTML = `
            <div class="player-header">
                <span>${tags}${p.name} ${isMe ? '(You)' : ''}</span>
            </div>
            <div style="font-size:0.8rem; color:#94a3b8; margin-bottom:5px;">Hand</div>
            <div class="player-board" id="hand-${p.id}" style="min-height: 80px;"></div>
            <div style="font-size:0.8rem; color:#94a3b8; margin-top:10px; margin-bottom:5px;">Score Pile</div>
            <div class="player-board" id="score-${p.id}" style="min-height: 50px; background:rgba(0,0,0,0.5);"></div>
        `;
        playersArea.appendChild(pdiv);

        let handArea = document.getElementById(`hand-${p.id}`);
        if (isMe || gameState.phase === "GAME_OVER") {
            p.hand.forEach(c => {
                let el = createCardDOM(c);
                if (gameState.phase === "BIDDING" && isMe && !p.hasBid) {
                    el.onclick = () => sendAction('BID', c.id);
                    el.style.boxShadow = "0 0 10px #3b82f6";
                }
                handArea.appendChild(el);
            });
        } else {
            // Show card backs for others
            for(let i=0; i<p.hand.length; i++) {
                let el = document.createElement('div');
                el.className = "card card-back";
                el.style.transform = "scale(0.8)";
                handArea.appendChild(el);
            }
        }

        let scoreArea = document.getElementById(`score-${p.id}`);
        p.scorePile.forEach(c => {
            let el = createCardDOM(c);
            el.style.transform = "scale(0.6)";
            el.style.margin = "-15px";
            
            if (gameState.phase === "LEGACY_PICK" && isMe) {
                el.style.cursor = "pointer";
                el.onclick = () => sendAction('PICK_LEGACY', c.id);
                if (p.legacyCardId === c.id) {
                    el.style.boxShadow = "0 0 15px 5px #fbbf24";
                    el.style.border = "2px solid #fbbf24";
                }
            } else if (p.legacyCardId === c.id) {
                 el.style.border = "1px solid #fbbf24";
            }

            scoreArea.appendChild(el);
        });
    });

    // J Skill Modal Handling
    if (gameState.phase === "J_SKILL" && gameState.pickOrderPlayerIds[0] === myPlayerId) {
        if (jSkillModal.classList.contains('hidden')) {
            jSkillModal.classList.remove('hidden');
            renderJSkillModal();
        }
    } else {
        jSkillModal.classList.add('hidden');
    }

    // Scoreboard Modal Handling
    if (gameState.phase === "ROUND_END" || gameState.phase === "GAME_OVER" || gameState.phase === "LEGACY_PICK") {
        scoreModal.classList.remove('hidden');
    } else {
        scoreModal.classList.add('hidden');
    }
}

function renderScoreboard() {
    scoreTitle.innerText = gameState.phase === "GAME_OVER" ? "🏁 Final Results" : (gameState.phase === "LEGACY_PICK" ? "🌟 Pick Legacy Card" : `🏆 Round ${gameState.round} Results`);
    scoreList.innerHTML = '';
    scoreList.className = "scoreboard-container";
    
    let sortedPlayers = [...gameState.players].sort((a, b) => b.totalScore - a.totalScore);
    
    sortedPlayers.forEach((p, index) => {
        let pRow = document.createElement('div');
        pRow.className = `score-item ${index === 0 ? 'winner' : ''} ${p.poor ? 'underdog-row' : ''}`;
        
        let rankBadgeClass = `rank-badge rank-${Math.min(index + 1, 3)}`;
        let rankLabel = index + 1;
        
        let legacyBadge = p.rich ? ' <span class="badge-legacy legacy-rich">เจ้าสัว</span>' : (p.poor ? ' <span class="badge-legacy legacy-poor">ยาจก</span>' : '');
        let pickedStatus = p.legacyCardId ? ' <span style="color:#22c55e; font-size:0.7rem;">✓</span>' : (gameState.phase === "LEGACY_PICK" ? ' <span style="color:#94a3b8; font-size:0.7rem;">⏳</span>' : '');

        pRow.innerHTML = `
            <div style="display:flex; align-items:center;">
                <div class="score-rank">${rankLabel}</div>
                <div style="margin-left: 15px;">
                    <div style="font-weight:bold; font-size:1.1rem;">${p.name}${legacyBadge}</div>
                    <div style="font-size:0.7rem; color:#94a3b8; font-family:'Outfit', sans-serif;">Click to view cards ${pickedStatus}</div>
                </div>
            </div>
            <div style="text-align:right;">
                <div style="font-size:0.85rem; color:#94a3b8;">Round: +${p.roundScore}</div>
                <div style="font-weight:bold; color:#fbbf24; font-size:1.2rem; font-family:'Cinzel', serif;">${p.totalScore} <span style="font-size:0.6rem;">PTS</span></div>
            </div>
        `;

        pRow.onclick = () => showScoreBreakdown(p);
        scoreList.appendChild(pRow);
    });

    const isHost = gameState.players[0].id === myPlayerId;
    if (gameState.phase === "GAME_OVER") {
        btnNextRound.classList.add('hidden');
        btnResetToLobby.classList.toggle('hidden', !isHost);
        btnBackToPortal.classList.remove('hidden');
    } else {
        btnNextRound.classList.toggle('hidden', !isHost);
        btnResetToLobby.classList.add('hidden');
        btnBackToPortal.classList.add('hidden');
    }
}

function showScoreBreakdown(p) {
    let overlay = document.createElement('div');
    overlay.className = "event-overlay";
    overlay.onclick = () => document.body.removeChild(overlay);

    let box = document.createElement('div');
    box.className = "glass-panel bounce-in";
    box.style.maxWidth = "800px";
    box.style.width = "90%";
    box.style.padding = "20px";
    box.onclick = (e) => e.stopPropagation();
    
    let html = `
        <h2 style="color:#fbbf24; margin-top:0; text-align:center;">🔍 ${p.name}'s Collection</h2>
        <div id="breakdownCards" class="score-details-grid" style="max-height:450px; overflow-y:auto; background:rgba(0,0,0,0.3); border-radius:15px; margin-bottom:20px;"></div>
    `;
    
    if (p.scoreBreakdown && p.scoreBreakdown.some(s => s.includes("Lucky Suit"))) {
        html += `<div style="text-align:center; color:#fbbf24; font-weight:bold; margin-bottom:15px; font-size:1.2rem; text-shadow:0 0 10px #fbbf24;">🌟 Lucky Suit Bonus: +20 pts</div>`;
    }
    
    html += `<div style="text-align:center;"><button class="btn-primary" style="width:200px;">Close</button></div>`;
    box.innerHTML = html;
    
    box.querySelector('button').onclick = () => document.body.removeChild(overlay);
    
    let cardContainer = box.querySelector('#breakdownCards');
    if (p.scorePile.length === 0) {
        cardContainer.innerHTML = '<div style="grid-column: 1/-1; text-align:center; color:#94a3b8; padding:40px;">No cards collected this round.</div>';
    }

    p.scorePile.forEach((c, idx) => {
        let cardWrapper = document.createElement('div');
        cardWrapper.className = "breakdown-card";
        cardWrapper.style.animationDelay = `${idx * 0.05}s`;
        
        let el = createCardDOM(c);
        el.style.transform = "scale(0.8)";
        el.style.margin = "0";
        
        let pts = c.rank;
        if (pts > 10) pts = 10;
        let bonusTags = [];
        
        if (c.aceUnderdog) {
            pts = 20;
            bonusTags.push('<span style="color:#fbbf24;">Ace!</span>');
        } else {
            if (c.doubleScore) {
                pts *= 2;
                bonusTags.push('<span style="color:#ef4444;">x2</span>');
            }
            if (gameState.masterCard && c.rank === gameState.masterCard.rank) {
                pts += 10;
                bonusTags.push('<span style="color:#60a5fa;">+10</span>');
            }
        }
        
        let label = document.createElement('div');
        label.style.fontSize = "0.8rem";
        label.style.fontWeight = "bold";
        label.innerHTML = `${pts} <span style="font-size:0.6rem; color:#94a3b8;">PTS</span><br>${bonusTags.join(' ')}`;
        
        cardWrapper.appendChild(el);
        cardWrapper.appendChild(label);
        cardContainer.appendChild(cardWrapper);
    });

    overlay.appendChild(box);
    document.body.appendChild(overlay);
}

function renderJSkillModal() {
    jSkillDeckCards.innerHTML = '';
    jSkillMarketCards.innerHTML = '';
    selectedJDeckCardId = null;
    selectedJMarketCardId = null;
    btnConfirmJSkill.classList.add('hidden');
    
    gameState.jackSkillOptions.forEach(c => {
        let el = createCardDOM(c);
        el.onclick = () => {
            selectedJDeckCardId = c.id;
            Array.from(jSkillDeckCards.children).forEach(child => child.style.boxShadow = "none");
            el.style.boxShadow = "0 0 15px 5px #3b82f6";
            if (selectedJDeckCardId && selectedJMarketCardId) btnConfirmJSkill.classList.remove('hidden');
        };
        jSkillDeckCards.appendChild(el);
    });
    
    gameState.market.forEach(c => {
        let el = createCardDOM(c);
        el.onclick = () => {
            selectedJMarketCardId = c.id;
            Array.from(jSkillMarketCards.children).forEach(child => child.style.boxShadow = "none");
            el.style.boxShadow = "0 0 15px 5px #ef4444";
            if (selectedJDeckCardId && selectedJMarketCardId) btnConfirmJSkill.classList.remove('hidden');
        };
        jSkillMarketCards.appendChild(el);
    });
}

function createCardDOM(card) {
    let div = document.createElement('div');
    if (card.faceDown) {
        div.className = "card card-back";
        return div;
    }
    
    let isRed = card.suit === 'HEART' || card.suit === 'DIAMOND';
    div.className = `card ${isRed ? 'red' : 'black'}`;
    if (card.doubleScore) {
        div.classList.add('card-tapped');
        div.style.boxShadow = "0 0 10px #ef4444";
        if (card.aceUnderdog) {
            let pts = document.createElement('div');
            pts.innerText = "20";
            pts.style.position = "absolute";
            pts.style.top = "10px";
            pts.style.right = "10px";
            pts.style.color = "#fbbf24";
            pts.style.fontWeight = "bold";
            pts.style.fontSize = "1.5rem";
            pts.style.textShadow = "2px 2px 0px #000";
            pts.style.transform = "rotate(-90deg)"; // counter tap
            div.appendChild(pts);
        }
    }

    let suitSymbol = '♠️';
    if(card.suit === 'HEART') suitSymbol = '♥️';
    else if(card.suit === 'DIAMOND') suitSymbol = '♦️';
    else if(card.suit === 'CLUB') suitSymbol = '♣️';

    let rankStr = card.rank;
    if (card.rank === 1) rankStr = 'A';
    if (card.rank === 11) rankStr = 'J';
    if (card.rank === 12) rankStr = 'Q';
    if (card.rank === 13) rankStr = 'K';

    div.innerHTML = `
        <div class="suit" style="position: absolute; top: 5px; left: 5px; font-size:1rem;">${suitSymbol}</div>
        <div class="rank">${rankStr}</div>
    `;
    return div;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerText = message;
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.5s forwards';
        setTimeout(() => toast.remove(), 500);
    }, 4000);
}

function addToActionLog(message) {
    const logContent = document.getElementById('logContent');
    if (!logContent) return;
    const entry = document.createElement('div');
    entry.style.marginBottom = "5px";
    entry.style.borderBottom = "1px solid rgba(255,255,255,0.05)";
    entry.style.paddingBottom = "3px";
    entry.style.color = "#cbd5e1";
    const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    entry.innerHTML = `<span style="color:#fbbf24; font-size:0.7rem;">[${time}]</span> ${message}`;
    logContent.appendChild(entry);
    logContent.scrollTop = logContent.scrollHeight;
}

function getCardName(card) {
    if (!card) return "Unknown Card";
    let suitSymbol = '♠️';
    if(card.suit === 'HEART') suitSymbol = '♥️';
    else if(card.suit === 'DIAMOND') suitSymbol = '♦️';
    else if(card.suit === 'CLUB') suitSymbol = '♣️';
    
    let rankStr = card.rank;
    if (card.rank === 1) rankStr = 'A';
    else if (card.rank === 11) rankStr = 'J';
    else if (card.rank === 12) rankStr = 'Q';
    else if (card.rank === 13) rankStr = 'K';
    
    return `${rankStr}${suitSymbol}`;
}
