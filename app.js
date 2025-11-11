const ACCESS_KEY = "8T9sQ5opWQpNamo80b2dtn7upQuQ-jI6amp6YEdmSSo";

const state = { query: "", page: 1, perPage: 12, total: 0, method: "xhr" };

const form = document.getElementById("search-form");
const input = document.getElementById("query");
const perPageEl = document.getElementById("per-page");
const grid = document.getElementById("grid");
const statusEl = document.getElementById("status");
const prevBtn = document.getElementById("prev");
const nextBtn = document.getElementById("next");
const pageIndicator = document.getElementById("page-indicator");
const methodRadios = [...document.querySelectorAll('input[name="method"]')];
const clearBtn = document.getElementById("clear");

function headers() {
  return { Authorization: `Client-ID ${ACCESS_KEY}` };
}

function url() {
  const q = encodeURIComponent(state.query || "");
  return `https://api.unsplash.com/search/photos?query=${q}&page=${state.page}&per_page=${state.perPage}`;
}

function setStatus(t) {
  statusEl.textContent = t || "";
}

function setPaging() {
  pageIndicator.textContent = `Page ${state.page}`;
  prevBtn.disabled = state.page <= 1;
  const maxPages = Math.ceil((state.total || 0) / state.perPage);
  nextBtn.disabled = !state.total || state.page >= maxPages;
}

function render(results) {
  grid.innerHTML = "";
  const frag = document.createDocumentFragment();
  results.forEach(p => {
    const card = document.createElement("div");
    card.className = "card";
    const a = document.createElement("a");
    a.href = p.links.html;
    a.target = "_blank";
    const img = document.createElement("img");
    img.src = p.urls.small;
    img.alt = p.alt_description || "Photo";
    img.decoding = "async";
    img.loading = "lazy";
    const meta = document.createElement("div");
    meta.className = "meta";
    const left = document.createElement("div");
    left.className = "left";
    const av = document.createElement("img");
    av.className = "av";
    av.src = p.user.profile_image.small;
    av.alt = p.user.name;
    const nm = document.createElement("div");
    nm.className = "name";
    nm.textContent = p.user.name;
    const likes = document.createElement("div");
    likes.className = "likes";
    likes.textContent = `❤ ${p.likes}`;
    left.appendChild(av);
    left.appendChild(nm);
    meta.appendChild(left);
    meta.appendChild(likes);
    a.appendChild(img);
    card.appendChild(a);
    card.appendChild(meta);
    card.addEventListener("mouseenter", () => card.style.boxShadow = "0 6px 24px rgba(0,0,0,.35)");
    card.addEventListener("mouseleave", () => card.style.boxShadow = "");
    frag.appendChild(card);
  });
  grid.appendChild(frag);
}

function xhrGet(u) {
  return new Promise((resolve, reject) => {
    const x = new XMLHttpRequest();
    x.open("GET", u, true);
    x.setRequestHeader("Authorization", `Client-ID ${ACCESS_KEY}`);
    x.onreadystatechange = () => {
      if (x.readyState !== 4) return;
      if (x.status >= 200 && x.status < 300) resolve(JSON.parse(x.responseText));
      else reject(new Error(`HTTP ${x.status}`));
    };
    x.onerror = () => reject(new Error("Network error"));
    x.send();
  });
}

function fetchPromise(u) {
  return fetch(u, { headers: headers() }).then(r => {
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    return r.json();
  });
}

async function fetchAsync(u) {
  const r = await fetch(u, { headers: headers() });
  if (!r.ok) throw new Error(`HTTP ${r.status}`);
  return r.json();
}

async function search() {
  if (!ACCESS_KEY || ACCESS_KEY.includes("YOUR_")) {
    setStatus("Add your Unsplash access key in app.js");
    return;
  }
  if (!state.query.trim()) {
    setStatus("Type a query and press Enter");
    grid.innerHTML = "";
    state.total = 0;
    setPaging();
    return;
  }
  setStatus("Loading…");
  try {
    let data;
    if (state.method === "xhr") data = await xhrGet(url());
    else if (state.method === "fetch") data = await fetchPromise(url());
    else data = await fetchAsync(url());
    state.total = data.total || 0;
    render(data.results || []);
    setPaging();
    setStatus(`${state.total.toLocaleString()} results`);
  } catch (e) {
    grid.innerHTML = "";
    setStatus(String(e.message || e));
  }
}

form.addEventListener("submit", e => {
  e.preventDefault();
  state.query = input.value.trim();
  state.page = 1;
  state.perPage = parseInt(perPageEl.value, 10) || 12;
  search();
});

perPageEl.addEventListener("change", () => {
  state.perPage = parseInt(perPageEl.value, 10) || 12;
  state.page = 1;
  search();
});

prevBtn.addEventListener("click", () => {
  if (state.page > 1) {
    state.page -= 1;
    search();
  }
});

nextBtn.addEventListener("click", () => {
  const maxPages = Math.ceil((state.total || 0) / state.perPage);
  if (state.page < maxPages) {
    state.page += 1;
    search();
  }
});

methodRadios.forEach(r => r.addEventListener("change", () => {
  state.method = document.querySelector('input[name="method"]:checked').value;
  state.page = 1;
  search();
}));

clearBtn.addEventListener("click", () => {
  input.value = "";
  state.query = "";
  state.page = 1;
  state.total = 0;
  grid.innerHTML = "";
  setPaging();
  setStatus("");
  input.focus();
});

document.addEventListener("keydown", e => {
  if (e.key === "/" && document.activeElement !== input) {
    e.preventDefault();
    input.focus();
    input.select();
  } else if (e.key === "Enter" && document.activeElement === input) {
    e.preventDefault();
    document.getElementById("submit").click();
  } else if (e.key === "ArrowRight") {
    nextBtn.click();
  } else if (e.key === "ArrowLeft") {
    prevBtn.click();
  }
});

input.addEventListener("input", e => {
  if (!e.target.value.trim()) {
    grid.innerHTML = "";
    state.total = 0;
    setPaging();
    setStatus("");
  }
});

setPaging();
