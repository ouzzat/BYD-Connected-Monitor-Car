// Jeu éducatif : alphabet Amazigh (Tifinagh)
// Alphabet Neo-Tifinagh (norme IRCAM), 33 lettres

const ALPHABET = [
  { t: "ⴰ", l: "a", nom: "ya", exemple: "comme le 'a' de « papa »" },
  { t: "ⴱ", l: "b", nom: "yab", exemple: "comme le 'b' de « bébé »" },
  { t: "ⴳ", l: "g", nom: "yag", exemple: "comme le 'g' de « gare »" },
  { t: "ⴳⵯ", l: "gʷ", nom: "yagʷ", exemple: "'g' labialisé (gw)" },
  { t: "ⴷ", l: "d", nom: "yad", exemple: "comme le 'd' de « dodo »" },
  { t: "ⴹ", l: "ḍ", nom: "yaḍ", exemple: "'d' emphatique" },
  { t: "ⴻ", l: "e", nom: "yey", exemple: "comme le 'e' de « le »" },
  { t: "ⴼ", l: "f", nom: "yaf", exemple: "comme le 'f' de « feu »" },
  { t: "ⴽ", l: "k", nom: "yak", exemple: "comme le 'k' de « kilo »" },
  { t: "ⴽⵯ", l: "kʷ", nom: "yakʷ", exemple: "'k' labialisé (kw)" },
  { t: "ⵀ", l: "h", nom: "yah", exemple: "comme le 'h' aspiré anglais" },
  { t: "ⵃ", l: "ḥ", nom: "yaḥ", exemple: "'h' emphatique (gorge)" },
  { t: "ⵄ", l: "ɛ", nom: "yaɛ", exemple: "consonne pharyngale (ayn)" },
  { t: "ⵅ", l: "x", nom: "yax", exemple: "comme le 'j' espagnol (kh)" },
  { t: "ⵇ", l: "q", nom: "yaq", exemple: "'k' prononcé au fond de la gorge" },
  { t: "ⵉ", l: "i", nom: "yi", exemple: "comme le 'i' de « ici »" },
  { t: "ⵊ", l: "j", nom: "yaj", exemple: "comme le 'j' de « jardin »" },
  { t: "ⵍ", l: "l", nom: "yal", exemple: "comme le 'l' de « lune »" },
  { t: "ⵎ", l: "m", nom: "yam", exemple: "comme le 'm' de « maman »" },
  { t: "ⵏ", l: "n", nom: "yan", exemple: "comme le 'n' de « nid »" },
  { t: "ⵓ", l: "u", nom: "yu", exemple: "comme le 'ou' de « fou »" },
  { t: "ⵔ", l: "r", nom: "yar", exemple: "comme le 'r' roulé" },
  { t: "ⵕ", l: "ṛ", nom: "yaṛ", exemple: "'r' emphatique" },
  { t: "ⵖ", l: "ɣ", nom: "yaɣ", exemple: "comme le 'r' grasseyé français (gh)" },
  { t: "ⵙ", l: "s", nom: "yas", exemple: "comme le 's' de « soleil »" },
  { t: "ⵚ", l: "ṣ", nom: "yaṣ", exemple: "'s' emphatique" },
  { t: "ⵛ", l: "c", nom: "yac", exemple: "comme le 'ch' de « chat »" },
  { t: "ⵜ", l: "t", nom: "yat", exemple: "comme le 't' de « table »" },
  { t: "ⵟ", l: "ṭ", nom: "yaṭ", exemple: "'t' emphatique" },
  { t: "ⵡ", l: "w", nom: "yaw", exemple: "comme le 'w' de « wapiti »" },
  { t: "ⵢ", l: "y", nom: "yay", exemple: "comme le 'y' de « yaourt »" },
  { t: "ⵣ", l: "z", nom: "yaz", exemple: "comme le 'z' de « zèbre »" },
  { t: "ⵥ", l: "ẓ", nom: "yaẓ", exemple: "'z' emphatique" },
];

const state = {
  quiz: { order: [], index: 0, score: 0, total: 0 },
  memory: { cards: [], firstPick: null, lock: false, matched: 0, moves: 0 },
};

function shuffle(arr) {
  const a = arr.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

function pickRandomOptions(correct, count) {
  const others = shuffle(ALPHABET.filter((x) => x !== correct)).slice(0, count - 1);
  return shuffle([correct, ...others]);
}

// ---------- Navigation ----------
const views = document.querySelectorAll(".view");
const navButtons = document.querySelectorAll(".nav-btn");

function showView(name) {
  views.forEach((v) => v.classList.toggle("active", v.id === "view-" + name));
  navButtons.forEach((b) => b.classList.toggle("active", b.dataset.view === name));
  if (name === "apprendre") renderApprendre();
  if (name === "quiz") startQuiz();
  if (name === "memoire") startMemory();
}

navButtons.forEach((btn) => {
  btn.addEventListener("click", () => showView(btn.dataset.view));
});

// ---------- Vue Apprendre ----------
function renderApprendre() {
  const grid = document.getElementById("lettres-grid");
  if (grid.dataset.rendered) return;
  grid.dataset.rendered = "1";
  ALPHABET.forEach((entry) => {
    const card = document.createElement("button");
    card.className = "lettre-card";
    card.innerHTML = `<span class="tifinagh">${entry.t}</span>`;
    card.addEventListener("click", () => showDetail(entry));
    grid.appendChild(card);
  });
}

function showDetail(entry) {
  const detail = document.getElementById("lettre-detail");
  detail.innerHTML = `
    <div class="detail-tifinagh">${entry.t}</div>
    <div class="detail-latin">${entry.l}</div>
    <div class="detail-nom">Nom : ${entry.nom}</div>
    <div class="detail-exemple">${entry.exemple}</div>
  `;
  detail.classList.add("show");
}

// ---------- Vue Quiz ----------
const QUIZ_LENGTH = 12;

function startQuiz() {
  state.quiz.order = shuffle(ALPHABET).slice(0, QUIZ_LENGTH);
  state.quiz.index = 0;
  state.quiz.score = 0;
  state.quiz.total = QUIZ_LENGTH;
  document.getElementById("quiz-result").classList.remove("show");
  document.getElementById("quiz-question").style.display = "";
  nextQuizQuestion();
}

function nextQuizQuestion() {
  const q = state.quiz;
  if (q.index >= q.order.length) return endQuiz();

  const entry = q.order[q.index];
  document.getElementById("quiz-progress").textContent = `Question ${q.index + 1} / ${q.total}`;
  document.getElementById("quiz-score").textContent = `Score : ${q.score}`;
  document.getElementById("quiz-lettre").textContent = entry.t;

  const options = pickRandomOptions(entry, 4);
  const optionsEl = document.getElementById("quiz-options");
  optionsEl.innerHTML = "";
  const feedback = document.getElementById("quiz-feedback");
  feedback.textContent = "";
  feedback.className = "quiz-feedback";

  options.forEach((opt) => {
    const btn = document.createElement("button");
    btn.className = "quiz-option";
    btn.textContent = opt.l;
    btn.addEventListener("click", () => answerQuiz(btn, opt, entry, options));
    optionsEl.appendChild(btn);
  });
}

function answerQuiz(btn, chosen, correct, allOptions) {
  const optionsEl = document.getElementById("quiz-options");
  const buttons = optionsEl.querySelectorAll(".quiz-option");
  buttons.forEach((b) => (b.disabled = true));

  const feedback = document.getElementById("quiz-feedback");
  if (chosen === correct) {
    state.quiz.score++;
    btn.classList.add("correct");
    feedback.textContent = `Bravo ! ${correct.t} se prononce « ${correct.nom} ».`;
    feedback.classList.add("ok");
  } else {
    btn.classList.add("wrong");
    buttons.forEach((b) => {
      if (b.textContent === correct.l) b.classList.add("correct");
    });
    feedback.textContent = `Ce n'était pas ça. ${correct.t} se lit « ${correct.l} » (${correct.nom}).`;
    feedback.classList.add("ko");
  }

  document.getElementById("quiz-score").textContent = `Score : ${state.quiz.score}`;

  setTimeout(() => {
    state.quiz.index++;
    nextQuizQuestion();
  }, 1400);
}

function endQuiz() {
  document.getElementById("quiz-question").style.display = "none";
  const result = document.getElementById("quiz-result");
  const { score, total } = state.quiz;
  const best = Number(localStorage.getItem("tifinagh-best-score") || 0);
  if (score > best) localStorage.setItem("tifinagh-best-score", String(score));
  const nouveauRecord = score > best;

  result.innerHTML = `
    <h3>Manche terminée !</h3>
    <p class="result-score">${score} / ${total}</p>
    <p>${nouveauRecord ? "Nouveau record personnel !" : "Meilleur score : " + Math.max(score, best) + " / " + total}</p>
    <button id="quiz-restart" class="primary-btn">Rejouer</button>
  `;
  result.classList.add("show");
  document.getElementById("quiz-restart").addEventListener("click", startQuiz);
}

// ---------- Vue Mémoire ----------
const MEMORY_PAIRS = 8;

function startMemory() {
  const m = state.memory;
  m.firstPick = null;
  m.lock = false;
  m.matched = 0;
  m.moves = 0;

  const chosen = shuffle(ALPHABET).slice(0, MEMORY_PAIRS);
  const deck = shuffle(
    chosen.flatMap((entry) => [
      { key: entry.l, display: entry.t, kind: "tifinagh" },
      { key: entry.l, display: entry.l, kind: "latin" },
    ])
  );
  m.cards = deck;

  document.getElementById("memory-status").textContent = `Paires trouvées : 0 / ${MEMORY_PAIRS} — Coups : 0`;
  document.getElementById("memory-win").classList.remove("show");

  const board = document.getElementById("memory-board");
  board.innerHTML = "";
  deck.forEach((card, i) => {
    const cardEl = document.createElement("button");
    cardEl.className = "memory-card";
    cardEl.dataset.index = i;
    cardEl.innerHTML = `<span class="memory-face memory-back">?</span><span class="memory-face memory-front ${card.kind}">${card.display}</span>`;
    cardEl.addEventListener("click", () => flipMemoryCard(i, cardEl));
    board.appendChild(cardEl);
  });
}

function flipMemoryCard(index, cardEl) {
  const m = state.memory;
  if (m.lock || cardEl.classList.contains("flipped") || cardEl.classList.contains("matched")) return;

  cardEl.classList.add("flipped");

  if (!m.firstPick) {
    m.firstPick = { index, el: cardEl };
    return;
  }

  m.moves++;
  const first = m.firstPick;
  const second = { index, el: cardEl };
  m.firstPick = null;

  const firstCard = m.cards[first.index];
  const secondCard = m.cards[second.index];

  if (firstCard.key === secondCard.key && firstCard.kind !== secondCard.kind) {
    first.el.classList.add("matched");
    second.el.classList.add("matched");
    m.matched++;
    updateMemoryStatus();
    if (m.matched === MEMORY_PAIRS) {
      setTimeout(() => {
        document.getElementById("memory-win").classList.add("show");
        document.getElementById("memory-win-moves").textContent = m.moves;
      }, 400);
    }
  } else {
    m.lock = true;
    setTimeout(() => {
      first.el.classList.remove("flipped");
      second.el.classList.remove("flipped");
      m.lock = false;
    }, 800);
  }
  updateMemoryStatus();
}

function updateMemoryStatus() {
  document.getElementById("memory-status").textContent =
    `Paires trouvées : ${state.memory.matched} / ${MEMORY_PAIRS} — Coups : ${state.memory.moves}`;
}

document.getElementById("memory-restart").addEventListener("click", startMemory);

// ---------- Initialisation ----------
showView("accueil");
