// File: /W_demo/backend/src/main/resources/static/public/js/app.js

(() => {
  const API_BASE = "/api"; // nếu đổi, sửa ở đây
  const WS_URL =
    (location.protocol === "https:" ? "wss://" : "ws://") +
    location.host +
    "/realtime";
  const token = localStorage.getItem("authToken"); // hoặc lấy từ cookie

  // --- Helpers fetch with auth
  async function apiGet(path) {
    const res = await fetch(API_BASE + path, {
      headers: token ? { Authorization: "Bearer " + token } : {},
    });
    if (!res.ok) throw new Error(`API ${path} failed: ${res.status}`);
    return res.json();
  }
  async function apiPost(path, body, isFormData = false) {
    const headers =
      token && !isFormData
        ? {
            Authorization: "Bearer " + token,
            "Content-Type": "application/json",
          }
        : token
        ? { Authorization: "Bearer " + token }
        : {};
    const res = await fetch(API_BASE + path, {
      method: "POST",
      headers,
      body: isFormData ? body : JSON.stringify(body),
    });
    if (!res.ok) {
      const t = await res.text();
      throw new Error("POST " + path + " -> " + res.status + " " + t);
    }
    return res.json();
  }

  // --- DOM shortcuts
  const friendsListEl = document.getElementById("friends");
  const groupsListEl = document.getElementById("groups");
  const mainImageEl = document.getElementById("mainImage");
  const postCaptionEl = document.querySelector(".post-caption");
  const profileNameEl = document.querySelector(".profile-name");

  // --- Render helpers
  function avatarEl(url, size = 36) {
    const img = document.createElement("img");
    img.src = url || "/static/default-avatar.png";
    img.alt = "avatar";
    img.style.width = `${size}px`;
    img.style.height = `${size}px`;
    img.style.borderRadius = "50%";
    img.style.objectFit = "cover";
    img.style.border = "2px solid rgba(0,0,0,0.12)";
    return img;
  }

  function renderFriends(friends) {
    friendsListEl.innerHTML = "";
    if (!friends || friends.length === 0) {
      const li = document.createElement("li");
      li.className = "empty";
      li.textContent = "Chưa có bạn bè. Khi có bạn bè sẽ hiện ở đây.";
      friendsListEl.appendChild(li);
      return;
    }
    friends.forEach((f) => {
      const li = document.createElement("li");
      li.style.display = "flex";
      li.style.alignItems = "center";
      li.style.gap = "12px";
      li.style.padding = "8px";
      // avatar
      const avWrap = document.createElement("div");
      avWrap.style.position = "relative";
      const av = avatarEl(f.avatarUrl, 40);
      avWrap.appendChild(av);
      // online dot
      const dot = document.createElement("span");
      dot.style.position = "absolute";
      dot.style.right = "-2px";
      dot.style.bottom = "-2px";
      dot.style.width = "12px";
      dot.style.height = "12px";
      dot.style.borderRadius = "50%";
      dot.style.border = "2px solid var(--panel)"; // border to blend
      dot.style.background = f.isOnline ? "#39d353" : "#6b6b6b";
      avWrap.appendChild(dot);

      // name
      const name = document.createElement("div");
      name.style.flex = "1";
      name.textContent = f.displayName || f.username;
      name.style.fontWeight = "600";
      name.style.color = "var(--text)";

      li.appendChild(avWrap);
      li.appendChild(name);

      friendsListEl.appendChild(li);
    });
  }

  function renderGroups(groups) {
    groupsListEl.innerHTML = "";
    if (!groups || groups.length === 0) {
      const li = document.createElement("li");
      li.className = "empty";
      li.textContent =
        "Chưa có nhóm. Tạo nhóm hoặc được mời sẽ hiển thị ở đây.";
      groupsListEl.appendChild(li);
      return;
    }
    groups.forEach((g) => {
      const li = document.createElement("li");
      li.style.display = "flex";
      li.style.alignItems = "center";
      li.style.gap = "12px";
      const av = avatarEl(g.avatarUrl, 40);
      const name = document.createElement("div");
      name.textContent = g.name || "Group " + g.id;
      name.style.fontWeight = "600";
      name.style.color = "var(--text)";
      li.appendChild(av);
      li.appendChild(name);
      groupsListEl.appendChild(li);
    });
  }

  function renderMainPost(post) {
    if (!post) {
      mainImageEl.src = "/static/placeholder.png";
      postCaptionEl.textContent = "caption hiển thị ở đây";
      return;
    }
    mainImageEl.src = post.imageUrl || "/static/placeholder.png";
    postCaptionEl.textContent = post.caption || "";
  }

  // --- WebSocket realtime
  let ws;
  function connectRealtime() {
    try {
      const url = new URL(WS_URL, location.origin);
      if (token) url.searchParams.set("token", token);
      ws = new WebSocket(url.toString());
      ws.addEventListener("open", () => console.log("Realtime connected"));
      ws.addEventListener("message", (ev) => {
        try {
          const msg = JSON.parse(ev.data);
          handleRealtimeEvent(msg);
        } catch (e) {
          console.warn("Bad realtime message", e);
        }
      });
      ws.addEventListener("close", () => {
        console.log("Realtime disconnected, reconnect in 2s");
        setTimeout(connectRealtime, 2000);
      });
      ws.addEventListener("error", (err) => console.error("WS error", err));
    } catch (e) {
      console.error("WS connect failed", e);
    }
  }

  function handleRealtimeEvent(msg) {
    // msg: { type, payload }
    switch (msg.type) {
      case "presence": // payload: { userId, isOnline }
        updateFriendPresence(msg.payload.userId, msg.payload.isOnline);
        break;
      case "friend_online":
      case "friend_offline":
        updateFriendPresence(msg.payload.userId, msg.type === "friend_online");
        break;
      case "new_post":
        // payload is the new post object: show it immediately as main
        renderMainPost(msg.payload);
        break;
      case "new_message":
        // could update conversation preview / unread count
        // not implemented here
        break;
      default:
        console.log("unhandled realtime", msg.type);
    }
  }

  // update friend presence in DOM
  function updateFriendPresence(userId, isOnline) {
    // scan list items and update dot by matching dataset-userid
    const lis = friendsListEl.querySelectorAll("li");
    lis.forEach((li) => {
      const name = li.querySelector("div:nth-child(2)");
      if (!name) return;
      // we don't have dataset by default, so compare displayName text (best-effort)
      // Ideally items should have data-userid attribute
      if (
        li.dataset &&
        li.dataset.userid &&
        String(li.dataset.userid) === String(userId)
      ) {
        const dot = li.querySelector("div > span") || li.querySelector("span");
        if (dot) dot.style.background = isOnline ? "#39d353" : "#6b6b6b";
      }
    });
  }

  // --- Init: load data
  async function init() {
    try {
      // load current user
      const me = await apiGet("/users/me").catch(() => null);
      if (me) {
        profileNameEl.textContent = me.displayName || me.username;
        document.querySelector(
          ".profile-avatar"
        ).style.backgroundImage = `url(${
          me.avatarUrl || "/static/default-avatar.png"
        })`;
      }

      // friends
      const friends = await apiGet(`/users/${me ? me.id : "me"}/friends`).catch(
        () => []
      );
      // For safety, map to expected fields
      const fdata = (friends || []).map((f) => ({
        id: f.id,
        username: f.username,
        displayName: f.displayName,
        avatarUrl: f.avatarUrl,
        isOnline: !!f.isOnline,
      }));
      // attach data-userid attribute to li creation step - modify renderFriends to set dataset
      renderFriendsWithDataset(fdata);

      // groups
      const groups = await apiGet(
        `/users/${me ? me.id : "me"}/conversations`
      ).catch(() => []);
      renderGroups(groups || []);

      // load main post(s)
      const posts = await apiGet("/posts/latest").catch(() => []);
      renderMainPost((posts && posts[0]) || null);

      // connect realtime
      connectRealtime();
    } catch (err) {
      console.error("Init error", err);
    }
  }

  // small improvement: set data-userid in render
  function renderFriendsWithDataset(friends) {
    friendsListEl.innerHTML = "";
    if (!friends || friends.length === 0) {
      const li = document.createElement("li");
      li.className = "empty";
      li.textContent = "Chưa có bạn bè. Khi có bạn bè sẽ hiện ở đây.";
      friendsListEl.appendChild(li);
      return;
    }
    friends.forEach((f) => {
      const li = document.createElement("li");
      li.style.display = "flex";
      li.style.alignItems = "center";
      li.style.gap = "12px";
      li.style.padding = "8px";
      li.dataset.userid = String(f.id);

      const avWrap = document.createElement("div");
      avWrap.style.position = "relative";
      avWrap.style.width = "40px";
      avWrap.style.height = "40px";
      const av = avatarEl(f.avatarUrl, 40);
      avWrap.appendChild(av);

      const dot = document.createElement("span");
      dot.style.position = "absolute";
      dot.style.right = "-2px";
      dot.style.bottom = "-2px";
      dot.style.width = "12px";
      dot.style.height = "12px";
      dot.style.borderRadius = "50%";
      dot.style.border = "2px solid var(--panel)";
      dot.style.background = f.isOnline ? "#39d353" : "#6b6b6b";
      avWrap.appendChild(dot);

      const name = document.createElement("div");
      name.style.flex = "1";
      name.textContent = f.displayName || f.username;
      name.style.fontWeight = "600";
      name.style.color = "var(--text)";

      li.appendChild(avWrap);
      li.appendChild(name);

      friendsListEl.appendChild(li);
    });
  }

  // --- Post creation UI (simple modal)
  function createPostUI() {
    const modal = document.createElement("div");
    modal.id = "postModal";
    Object.assign(modal.style, {
      position: "fixed",
      left: "0",
      right: "0",
      top: "0",
      bottom: "0",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      background: "rgba(0,0,0,0.5)",
    });
    const card = document.createElement("div");
    Object.assign(card.style, {
      width: "640px",
      background: "var(--panel)",
      padding: "18px",
      borderRadius: "12px",
    });

    const h = document.createElement("h3");
    h.textContent = "Đăng ảnh / bài viết";
    const file = document.createElement("input");
    file.type = "file";
    file.accept = "image/*";
    const caption = document.createElement("textarea");
    caption.rows = 4;
    caption.style.width = "100%";
    const btn = document.createElement("button");
    btn.textContent = "Đăng";
    btn.style.marginTop = "8px";
    const close = document.createElement("button");
    close.textContent = "Hủy";
    close.style.marginLeft = "8px";

    btn.onclick = async () => {
      if (!file.files.length) {
        alert("Chọn ảnh");
        return;
      }
      const fd = new FormData();
      fd.append("image", file.files[0]);
      fd.append("caption", caption.value || "");
      try {
        const res = await apiPost("/posts", fd, true);
        // broadcast handled by backend; but immediately show
        renderMainPost(res);
        document.body.removeChild(modal);
      } catch (e) {
        alert("Đăng thất bại: " + e.message);
      }
    };
    close.onclick = () => document.body.removeChild(modal);

    card.appendChild(h);
    card.appendChild(file);
    card.appendChild(caption);
    card.appendChild(btn);
    card.appendChild(close);
    modal.appendChild(card);
    document.body.appendChild(modal);
  }

  // hook up top + create buttons
  document.addEventListener("DOMContentLoaded", () => {
    // theme toggle (simple)
    const themeToggle = document.getElementById("themeToggle");
    themeToggle?.addEventListener("click", () => {
      const link = document.querySelector('link[href$="style.css"]');
      const darkLink = "/static/css/style-dark.css"; // your path
      if (document.body.classList.toggle("dark")) {
        // switch to dark stylesheet (if exists)
        // for simplicity, change root class
        document.body.classList.add("dark");
      } else {
        document.body.classList.remove("dark");
      }
    });

    // create post
    document
      .getElementById("btnCreate")
      ?.addEventListener("click", () => createPostUI());

    // init data
    init();
  });
})();
