const API_BASE = "/api";
const WS_ENDPOINT = "/api/ws"; // nếu WebSocketConfig dùng đường khác thì chỉnh
let stompClient = null;
let wsConnected = false;
let currentSubscription = null;
const seenMessageIds = new Set(); // để tránh hiển thị trùng

const currentUserId = Number(localStorage.getItem("zmnt_user_id") || 0);

function getToken() {
  return localStorage.getItem("zmnt_session_token") || null;
}

async function apiFetch(path, options = {}) {
  // Log request để debug
  console.log("API Request:", { path, options });

  if (path.includes("undefined")) {
    console.error("Invalid API path detected:", path);
    throw { status: 400, body: "Invalid request path" };
  }

  const token = getToken();
  const headers = options.headers || {};

  //KHÔNG phải FormData thì set JSON
  if (!(options.body instanceof FormData)) {
    headers["Content-Type"] = headers["Content-Type"] || "application/json";
  }
  if (token) {
    headers["Authorization"] = "Bearer " + token;
  }
  options.headers = headers;

  const res = await fetch(API_BASE + path, options);

  //Log response status
  console.log("API Response Status:", res.status);

  if (!res.ok) {
    let bodyText = await res.text();
    try {
      bodyText = JSON.parse(bodyText);
    } catch (_) {}

    //Log error
    console.error("API Error:", { status: res.status, body: bodyText });

    throw { status: res.status, body: bodyText };
  }

  //Kiểm tra content-type và response trống
  const contentType = res.headers.get("content-type");
  const contentLength = res.headers.get("content-length");

  if (
    res.status === 204 ||
    contentLength === "0" ||
    !contentType ||
    !contentType.includes("application/json")
  ) {
    return null;
  }

  try {
    const jsonResponse = await res.json();
    // Log success
    console.log("API Success Response:", jsonResponse);
    return jsonResponse;
  } catch (e) {
    console.warn("Response is not JSON, returning null:", e);
    return null;
  }
}

function connectWebSocket() {
  const socket = new SockJS(WS_ENDPOINT);
  stompClient = Stomp.over(socket);

  // Tắt log spam của stomp nếu muốn
  stompClient.debug = null;

  stompClient.connect(
    {},
    (frame) => {
      console.log("WS connected:", frame);
      wsConnected = true;

      // nếu đang mở 1 conversation thì subscribe luôn
      if (currentConversation) {
        subscribeConversation(currentConversation.id);
      }
    },
    (error) => {
      console.error("WS connect error:", error);
      wsConnected = false;
    }
  );
}

function subscribeConversation(convId) {
  if (!stompClient || !wsConnected) {
    console.warn("WS chưa sẵn sàng, bỏ qua subscribe");
    return;
  }

  if (currentSubscription) {
    currentSubscription.unsubscribe();
    currentSubscription = null;
  }

  const destination = `/topic/conversations/${convId}`;
  console.log("Subscribing to", destination);

  currentSubscription = stompClient.subscribe(destination, (msg) => {
    try {
      const payload = JSON.parse(msg.body);
      console.log("WS message:", payload);

      if (payload.id && seenMessageIds.has(payload.id)) {
        return; // đã hiển thị từ REST rồi
      }
      if (payload.id) {
        seenMessageIds.add(payload.id);
      }

      const isMe = String(payload.senderId) === String(currentUserId);
      const msgObj = {
        ...payload,
        isMe,
        senderName: isMe ? "Bạn" : payload.senderName || "Người dùng",
      };

      const key = String(convId);
      if (!messageCache[key]) messageCache[key] = [];
      messageCache[key].push(msgObj);

      appendMessageToChat(
        msgObj,
        msgObj.senderName,
        isMe,
        payload.sentAt || payload.createdAt
      );
    } catch (e) {
      console.error("Parse WS message error:", e);
    }
  });
}
function generateCid() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
}

// upload ẢNH
async function uploadImage(file) {
  const form = new FormData();
  form.append("file", file);
  form.append("mediaType", "PHOTO"); // Hiện tại để cho ảnh, kiểm soát mở rộng thêm

  return apiFetch("/media/upload", {
    method: "POST",
    body: form,
  });
}
// DOM
const profileNameEl = document.querySelector(".profile-name");
const profileAvatarEl = document.querySelector(".profile-avatar");

const friendsList = document.getElementById("friends");
const groupsList = document.getElementById("groups");

const btnCreateGroup = document.getElementById("btnCreate");
const searchInput = document.getElementById("search");

const searchResultList = document.getElementById("searchResults");
const searchResultPanel = document.querySelector(".search-result-panel");

// Phần banif đăng
const mainImageEl = document.getElementById("mainImage");
const captionEl = document.querySelector(".post-caption");

// Cố định tên 1
const vipNameEl =
  document.getElementById("vipName") ||
  document.querySelector(".vip-label") ||
  null;

// nav icons
const navButtons = document.querySelectorAll(".nav-icons .nav-btn");
const btnPost = navButtons[2];
const btnBell = navButtons[3];

const messageCache = {};

let currentConversation = null;
let friendReqPopup = null;

btnBell?.addEventListener("click", async () => {
  if (friendReqPopup) {
    friendReqPopup.remove();
    friendReqPopup = null;
    return;
  }
  friendReqPopup = await createFriendRequestPopup();
});

// Load thông tin cá nhân

async function createFriendRequestPopup() {
  const popup = document.createElement("div");
  popup.style.position = "absolute";
  popup.style.top = "70px";
  popup.style.right = "40px";
  popup.style.width = "320px";
  popup.style.maxHeight = "360px";
  popup.style.background = "var(--panel)";
  popup.style.borderRadius = "18px";
  popup.style.border = "1px solid var(--border)";
  popup.style.boxShadow = "0 12px 30px rgba(0,0,0,0.6)";
  popup.style.zIndex = "999";
  popup.style.display = "flex";
  popup.style.flexDirection = "column";
  popup.innerHTML = `
      <div style="padding:10px 12px;font-weight:700;border-bottom:1px solid rgba(255,255,255,0.06);display:flex;justify-content:space-between;align-items:center;">
        <span>Lời mời kết bạn</span>
        <button id="close-req-popup" style="border:0;background:transparent;cursor:pointer;font-size:16px;">✕</button>
      </div>
      <div id="req-body" style="flex:1;overflow:auto;padding:8px 10px;"></div>
  `;
  document.body.appendChild(popup);

  popup.querySelector("#close-req-popup").onclick = () => {
    popup.remove();
    friendReqPopup = null;
  };

  const body = popup.querySelector("#req-body");
  body.innerHTML = "<div>Đang tải...</div>";

  try {
    const requests = await apiFetch(
      `/friends/requests?currentUserId=${currentUserId}`
    );
    body.innerHTML = "";

    if (!requests || requests.length === 0) {
      body.innerHTML = "<div>Không có lời mời nào</div>";
      return popup;
    }

    // Lọc Chỉ hiển thị requests mà currentUser != actionUser và status là pending
    const incomingRequests = requests.filter((fr) => {
      return fr.status === "PENDING" && fr.actionUserId !== currentUserId;
    });

    if (incomingRequests.length === 0) {
      body.innerHTML = "<div>Không có lời mời nào</div>";
      return popup;
    }

    incomingRequests.forEach((fr) => {
      const sender = {
        id: fr.actionUserId,
        name: fr.actionUserName,
        avatarUrl: "", // Có thể thêm sau, kiểm tra lại uer_avatar
      };

      const item = document.createElement("div");
      item.style.display = "flex";
      item.style.alignItems = "center";
      item.style.justifyContent = "space-between";
      item.style.padding = "6px 0";
      item.style.borderBottom = "1px solid rgba(255,255,255,0.1)";

      item.innerHTML = `
          <div style="display:flex;align-items:center;gap:8px;">
            <div style="
                width:32px;height:32px;border-radius:50%;
                background-size:cover;background-position:center;
                border:2px solid rgba(0,0,0,0.25);
                background-image:url('${sender.avatarUrl || ""}');
                background-color: #555;
            "></div>
            <div>
              <div style="font-weight:600;">${escapeHtml(sender.name)}</div>
              <div style="font-size:12px;color:var(--muted);">Gửi lời mời kết bạn</div>
            </div>
          </div>
      `;

      const btnWrap = document.createElement("div");
      btnWrap.style.display = "flex";
      btnWrap.style.gap = "6px";

      const btnAccept = document.createElement("button");
      btnAccept.textContent = "Đồng ý";
      // Thêm màu cho đặc săc

      const btnReject = document.createElement("button");
      btnReject.textContent = "Từ chối";
      // Tương tự trên

      btnAccept.onclick = async () => {
        try {
          const friendshipId = Number(fr.id);
          if (isNaN(friendshipId)) {
            alert("ID lời mời không hợp lệ");
            return;
          }

          await apiFetch(
            `/friends/requests/${friendshipId}/accept?currentUserId=${currentUserId}`,
            { method: "PUT" }
          );
          item.remove();
          loadFriends();
        } catch (e) {
          console.error(e);
          alert("Lỗi khi chấp nhận lời mời: " + (e.body?.message || e.status));
        }
      };

      btnReject.onclick = async () => {
        try {
          const friendshipId = Number(fr.id);
          if (isNaN(friendshipId)) {
            alert("ID lời mời không hợp lệ");
            return;
          }

          await apiFetch(
            `/friends/requests/${friendshipId}/reject?currentUserId=${currentUserId}`,
            { method: "PUT" }
          );
          item.remove();
        } catch (e) {
          console.error(e);
          alert("Lỗi khi từ chối lời mời: " + (e.body?.message || e.status));
        }
      };

      btnWrap.appendChild(btnAccept);
      btnWrap.appendChild(btnReject);
      item.appendChild(btnWrap);
      body.appendChild(item);
    });
  } catch (e) {
    console.error(e);
    body.innerHTML = "<div>Không tải được danh sách lời mời</div>";
  }

  return popup;
}
async function loadProfile() {
  try {
    const me = await apiFetch("/users/me");
    profileNameEl.textContent = me.displayName || me.username || "Bạn";

    if (me.avatarUrl) {
      profileAvatarEl.style.backgroundImage = `url('${me.avatarUrl}')`;
      profileAvatarEl.style.backgroundSize = "cover";
      profileAvatarEl.style.backgroundPosition = "center";
    }
  } catch (e) {
    console.warn("Không lấy được profile", e);
  }
}

// Các cốt

function renderFriendItem(friend, friendshipId) {
  // Kiểm tra friendshipId có hợp lệ không
  if (!friendshipId || friendshipId === "undefined") {
    console.error("Invalid friendshipId:", friendshipId);
    return document.createElement("li");
  }
  const li = document.createElement("li");
  li.className = "friend-item";
  li.style.display = "flex";
  li.style.alignItems = "center";
  li.style.justifyContent = "space-between";
  li.style.padding = "8px";
  li.style.cursor = "pointer";
  li.style.position = "relative";

  const avatarUrl = friend.avatarUrl || "";

  li.innerHTML = `
        <div style="display:flex;alignItems:center;gap:10px;flex:1;">
            <div style="position:relative;">
                <div style="
                    width:36px;height:36px;border-radius:50%;
                    background-image:url('${avatarUrl}');
                    background-size:cover;background-position:center;
                    border:2px solid rgba(0,0,0,0.25);
                "></div>
                <span style="
                    position:absolute;bottom:-2px;right:-2px;
                    width:12px;height:12px;border-radius:50%;
                    background:${friend.online ? "#2ecc71" : "#95a5a6"};
                    border:2px solid var(--panel);
                "></span>
            </div>
            <div style="flex:1;">
                <div style="font-weight:600;">${escapeHtml(
                  friend.name || ""
                )}</div>
            </div>
        </div>
        <div class="friend-actions" style="display:none;">
            <button class="btn-chat" title="Nhắn tin"><i class="fi fi-rr-comment"></i></button>
            <button class="btn-block" title="Chặn"><i class="fi fi-rr-ban"></i></button>
            <button class="btn-unfriend" title="Xóa bạn"><i class="fi fi-rr-cross-circle"></i></button>
        </div>
    `;

  li.addEventListener("mouseenter", () => {
    li.querySelector(".friend-actions").style.display = "flex";
  });

  li.addEventListener("mouseleave", () => {
    li.querySelector(".friend-actions").style.display = "none";
  });

  li.querySelector(".btn-chat").addEventListener("click", (e) => {
    e.stopPropagation();
    openDirectChat(friend);
  });

  li.querySelector(".btn-block").addEventListener("click", async (e) => {
    e.stopPropagation();

    const fid = Number(friendshipId);
    if (isNaN(fid)) {
      alert("ID kết bạn không hợp lệ");
      return;
    }

    if (confirm(`Chặn ${friend.name}?`)) {
      try {
        await apiFetch(
          `/friends/block?currentUserId=${currentUserId}&targetUserId=${friend.id}`,
          {
            method: "POST",
          }
        );
        loadFriends();
      } catch (error) {
        console.error("Block error:", error);
        alert("Lỗi khi chặn bạn bè: " + (error.body?.message || error.status));
      }
    }
  });

  li.querySelector(".btn-unfriend").addEventListener("click", async (e) => {
    e.stopPropagation();

    const fid = Number(friendshipId);
    if (isNaN(fid)) {
      alert("ID kết bạn không hợp lệ");
      return;
    }

    if (confirm(`Xóa kết bạn với ${friend.name}?`)) {
      try {
        const result = await apiFetch(
          `/friends/${fid}?currentUserId=${currentUserId}`,
          {
            method: "DELETE",
          }
        );
        console.log("Unfriend successful");
        loadFriends();
      } catch (error) {
        console.error("Unfriend error:", error);
        alert(
          "Lỗi khi xóa bạn bè: " +
            (error.body?.message || error.status || "Unknown error")
        );
      }
    }
  });

  li.addEventListener("click", () => openDirectChat(friend));

  return li;
}

async function loadFriends() {
  friendsList.innerHTML = `<li>Đang tải...</li>`;

  if (!currentUserId) {
    friendsList.innerHTML =
      '<li class="empty">Không xác định được user hiện tại</li>';
    return;
  }

  try {
    const friendships = await apiFetch(
      `/friends?currentUserId=${currentUserId}`
    );

    friendsList.innerHTML = "";
    if (!friendships || friendships.length === 0) {
      friendsList.innerHTML =
        '<li class="empty">Chưa có bạn bè. Khi có bạn bè sẽ hiện ở đây.</li>';
      return;
    }

    friendships.forEach((fr) => {
      const isUser1 = fr.user1Id === currentUserId;
      const friend = {
        id: isUser1 ? fr.user2Id : fr.user1Id,
        name: isUser1 ? fr.user2Name : fr.user1Name,
        avatarUrl: "", //kiểm tra lại user_avatar
        online: false,
      };
      friendsList.appendChild(renderFriendItem(friend, fr.id));
    });
  } catch (e) {
    console.error(e);
    friendsList.innerHTML =
      '<li class="empty">Không load được danh sách bạn bè</li>';
  }
}

// Nhóm chat

function renderGroupItem(conv) {
  const li = document.createElement("li");
  li.className = "group-item";
  li.style.padding = "10px";
  li.style.cursor = "pointer";
  li.style.position = "relative";
  li.style.borderBottom = "1px solid rgba(255,255,255,0.05)";

  const avatarUrl = conv.avatarUrl || "";

  li.innerHTML = `
    <div style="display:flex;align-items:center;gap:10px;position:relative;">
      <div style="
        width:40px;height:40px;border-radius:10px;
        background-image:url('${avatarUrl}');
        background-size:cover;background-position:center;
        border:2px solid rgba(255,255,255,0.05);
        background-color: ${avatarUrl ? "transparent" : "#555"};
      "></div>
      <div style="flex:1;">
        <div style="font-weight:700;font-size:14px;">${escapeHtml(
          conv.name || ""
        )}</div>
        <div style="font-size:11px;color:var(--muted);">
          ${conv.memberCount ? conv.memberCount + " thành viên" : ""}
          ${conv.createdBy === currentUserId ? " • Bạn tạo" : ""}
        </div>
      </div>
    </div>
  `;

  // Nếu là người tạo nhóm, cho hiển thị menu
  if (conv.createdBy === currentUserId) {
    const menuBtn = document.createElement("button");
    menuBtn.innerHTML = "⋮";
    menuBtn.title = "Tùy chọn nhóm";
    menuBtn.style.background = "transparent";
    menuBtn.style.border = "none";
    menuBtn.style.cursor = "pointer";
    menuBtn.style.fontSize = "20px";
    menuBtn.style.padding = "0 8px";
    menuBtn.style.color = "var(--muted)";

    const actionsMenu = document.createElement("div");
    actionsMenu.className = "group-actions-menu";
    actionsMenu.style.display = "none";
    actionsMenu.style.position = "absolute";
    actionsMenu.style.right = "0";
    actionsMenu.style.top = "100%";
    actionsMenu.style.background = "var(--panel)";
    actionsMenu.style.border = "1px solid var(--border)";
    actionsMenu.style.borderRadius = "8px";
    actionsMenu.style.padding = "8px 0";
    actionsMenu.style.zIndex = "100";
    actionsMenu.style.minWidth = "120px";
    actionsMenu.style.boxShadow = "0 4px 12px rgba(0,0,0,0.2)";

    const deleteItem = document.createElement("div");
    deleteItem.innerHTML = "🗑 Xóa nhóm";
    deleteItem.style.padding = "8px 12px";
    deleteItem.style.cursor = "pointer";
    deleteItem.style.fontSize = "13px";
    deleteItem.style.display = "flex";
    deleteItem.style.alignItems = "center";
    deleteItem.style.gap = "8px";

    deleteItem.addEventListener("click", async (e) => {
      e.stopPropagation();
      actionsMenu.style.display = "none";

      if (
        confirm(
          `Bạn có chắc muốn xóa nhóm "${conv.name}"? Toàn bộ tin nhắn sẽ bị mất.`
        )
      ) {
        try {
          await apiFetch(`/conversations/${conv.id}`, {
            method: "DELETE",
          });

          // Đóng chat box nếu đang mở
          const chatBox = document.getElementById("floating-chat-box");
          if (chatBox && currentConversation?.id === conv.id) {
            chatBox.remove();
            currentConversation = null;
          }

          // Xóa khỏi danh sách
          li.remove();

          alert("Đã xóa nhóm thành công");
        } catch (error) {
          console.error("Delete group error:", error);
          alert("Lỗi khi xóa nhóm: " + (error.body?.message || error.status));
        }
      }
    });

    actionsMenu.appendChild(deleteItem);
    li.querySelector("div").appendChild(menuBtn);
    li.appendChild(actionsMenu);

    menuBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      actionsMenu.style.display =
        actionsMenu.style.display === "none" ? "block" : "none";
    });

    document.addEventListener("click", (e) => {
      if (!li.contains(e.target)) {
        actionsMenu.style.display = "none";
      }
    });
  }

  li.addEventListener("click", () => {
    currentConversation = {
      id: conv.id,
      name: conv.name,
      type: conv.type,
      createdBy: conv.createdBy,
    };
    openChatBox(conv.id, conv.name);
    loadMessages(conv.id);
    if (wsConnected && stompClient) {
      subscribeConversation(convId);
    }
  });

  return li;
}
async function loadGroups() {
  groupsList.innerHTML = `<li>Đang tải...</li>`;
  try {
    const page = await apiFetch("/conversations/my?page=0&size=50");
    const allConversations = Array.isArray(page) ? page : page.content || [];

    // chỉ hiển thị GROUP, ẩn DIRECT
    const groups = allConversations.filter(
      (conv) => conv.type === "GROUP" || conv.type === "GROUP_CHAT"
    );

    groupsList.innerHTML = "";
    if (!groups || groups.length === 0) {
      groupsList.innerHTML =
        '<li class="empty">Chưa có nhóm. Tạo nhóm hoặc được mời sẽ hiển thị ở đây.</li>';
      return;
    }

    groups.forEach((g) => groupsList.appendChild(renderGroupItem(g)));
  } catch (e) {
    console.error(e);
    groupsList.innerHTML = '<li class="empty">Không load được nhóm.</li>';
  }
}

async function createGroup() {
  try {
    // Load danh sách bạn bè để chọn
    const friendships = await apiFetch(
      `/friends?currentUserId=${currentUserId}`
    );

    if (!friendships || friendships.length < 2) {
      alert("Bạn cần ít nhất 2 bạn bè để tạo nhóm");
      return;
    }

    const selectedFriends = await showFriendSelectionDialog(friendships);

    if (!selectedFriends || selectedFriends.length < 2) {
      alert("Bạn cần chọn ít nhất 2 bạn bè để tạo nhóm");
      return;
    }

    const name = prompt("Tên nhóm mới:");
    if (!name) return;

    const body = {
      type: "GROUP",
      name,
      description: "",
      isPublic: true,
      maxMembers: 200,
      memberIds: selectedFriends,
    };

    const newGroup = await apiFetch("/conversations/group", {
      method: "POST",
      body: JSON.stringify(body),
    });

    alert("Tạo nhóm thành công!");
    loadGroups();
  } catch (e) {
    console.error(e);
    alert("Tạo nhóm lỗi: " + (e.body?.message || e.status));
  }
}

// Hàm quản lý thành viên
async function manageGroupMembers(convId) {
  try {
    // Lấy thông tin conversation
    const conv = await apiFetch(`/conversations/${convId}`);

    // Lấy danh sách thành viên
    const members = await apiFetch(`/conversations/${convId}/members`);

    // Hiển thị dialog quản lý thành viên
    showMemberManagementDialog(conv, members);
  } catch (e) {
    console.error("Manage members error:", e);
    alert(
      "Không thể tải thông tin thành viên: " + (e.body?.message || e.status)
    );
  }
}

function showMemberManagementDialog(conv, members) {
  const dialog = document.createElement("div");
  dialog.style.position = "fixed";
  dialog.style.top = "50%";
  dialog.style.left = "50%";
  dialog.style.transform = "translate(-50%, -50%)";
  dialog.style.background = "var(--panel)";
  dialog.style.padding = "20px";
  dialog.style.borderRadius = "12px";
  dialog.style.zIndex = "1000";
  dialog.style.minWidth = "400px";
  dialog.style.maxHeight = "80vh";
  dialog.style.overflow = "hidden";
  dialog.style.display = "flex";
  dialog.style.flexDirection = "column";
  dialog.style.boxShadow = "0 10px 30px rgba(0,0,0,0.4)";

  dialog.innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:15px;">
      <h3 style="margin:0;">Quản lý thành viên: ${escapeHtml(conv.name)}</h3>
      <button id="close-member-dialog" style="border:0;background:transparent;cursor:pointer;font-size:18px;color:var(--muted);">✕</button>
    </div>
    
    <div style="margin-bottom:15px;">
      <h4 style="margin:0 0 10px 0;font-size:14px;">Thêm thành viên mới</h4>
      <div style="display:flex;gap:8px;">
        <input id="search-new-member" type="text" placeholder="Tìm bạn bè để thêm..." 
               style="flex:1;padding:8px;border-radius:8px;border:1px solid var(--border);background:var(--input-bg);">
        <button id="btn-add-member" style="padding:8px 16px;border-radius:8px;border:none;background:#ff7b1a;color:white;cursor:pointer;">Thêm</button>
      </div>
      <div id="search-results" style="margin-top:10px;max-height:200px;overflow-y:auto;display:none;"></div>
    </div>
    
    <div style="flex:1;overflow-y:auto;border-top:1px solid var(--border);padding-top:15px;">
      <h4 style="margin:0 0 10px 0;font-size:14px;">Thành viên (${
        members.length
      })</h4>
      <div id="member-list"></div>
    </div>
  `;

  document.body.appendChild(dialog);

  // Đóng dialog
  dialog.querySelector("#close-member-dialog").onclick = () => dialog.remove();

  // Render danh sách thành viên
  const memberList = dialog.querySelector("#member-list");
  renderMemberList(memberList, members, conv);

  // Tìm kiếm bạn bè để thêm
  const searchInput = dialog.querySelector("#search-new-member");
  const searchResults = dialog.querySelector("#search-results");
  const btnAddMember = dialog.querySelector("#btn-add-member");

  let selectedFriend = null;

  searchInput.addEventListener("input", async (e) => {
    const keyword = e.target.value.trim();
    if (keyword.length < 2) {
      searchResults.style.display = "none";
      return;
    }

    try {
      const friends = await apiFetch(
        `/users/search?keyword=${encodeURIComponent(keyword)}`
      );

      const existingMemberIds = members.map((m) => m.userId || m.id);
      const availableFriends = friends.filter(
        (f) =>
          !existingMemberIds.includes(f.userId || f.id) &&
          (f.userId || f.id) !== currentUserId
      );

      if (availableFriends.length > 0) {
        searchResults.innerHTML = "";
        availableFriends.forEach((friend) => {
          const item = document.createElement("div");
          item.style.padding = "8px";
          item.style.borderBottom = "1px solid var(--border)";
          item.style.cursor = "pointer";
          item.style.display = "flex";
          item.style.alignItems = "center";
          item.style.gap = "10px";

          item.innerHTML = `
            <div style="width:32px;height:32px;border-radius:50%;background-image:url('${
              friend.avatarUrl || ""
            }');background-size:cover;"></div>
            <div>
              <div style="font-weight:600;">${escapeHtml(
                friend.displayName || friend.username
              )}</div>
              <div style="font-size:11px;color:var(--muted);">ID: ${
                friend.userId || friend.id
              }</div>
            </div>
          `;

          item.addEventListener("click", () => {
            selectedFriend = friend;
            searchInput.value = friend.displayName || friend.username;
            searchResults.style.display = "none";
          });

          searchResults.appendChild(item);
        });
        searchResults.style.display = "block";
      } else {
        searchResults.innerHTML =
          "<div style='padding:10px;text-align:center;color:var(--muted);'>Không tìm thấy bạn bè phù hợp</div>";
        searchResults.style.display = "block";
      }
    } catch (error) {
      console.error("Search friends error:", error);
    }
  });

  btnAddMember.addEventListener("click", async () => {
    if (!selectedFriend) {
      alert("Vui lòng chọn bạn bè để thêm");
      return;
    }

    try {
      const req = {
        userId: selectedFriend.userId || selectedFriend.id,
        role: "MEMBER",
      };

      await apiFetch(`/conversations/${conv.id}/members`, {
        method: "POST",
        body: JSON.stringify(req),
      });

      alert(
        `Đã thêm ${
          selectedFriend.displayName || selectedFriend.username
        } vào nhóm`
      );

      const updatedMembers = await apiFetch(
        `/conversations/${conv.id}/members`
      );
      renderMemberList(memberList, updatedMembers, conv);

      // Reset
      searchInput.value = "";
      selectedFriend = null;
      searchResults.style.display = "none";
    } catch (error) {
      console.error("Add member error:", error);
      alert(
        "Lỗi khi thêm thành viên: " + (error.body?.message || error.status)
      );
    }
  });

  document.addEventListener("click", (e) => {
    if (!dialog.contains(e.target)) {
      searchResults.style.display = "none";
    }
  });
}

function renderMemberList(container, members, conv) {
  container.innerHTML = "";

  members.forEach((member) => {
    const isCreator = member.role === "CREATOR";
    const isMe = (member.userId || member.id) === currentUserId;

    const memberEl = document.createElement("div");
    memberEl.style.display = "flex";
    memberEl.style.alignItems = "center";
    memberEl.style.justifyContent = "space-between";
    memberEl.style.padding = "10px";
    memberEl.style.borderBottom = "1px solid rgba(255,255,255,0.05)";

    memberEl.innerHTML = `
      <div style="display:flex;align-items:center;gap:10px;flex:1;">
        <div style="width:36px;height:36px;border-radius:50%;background-image:url('${
          member.avatarUrl || ""
        }');background-size:cover;background-color:#555;"></div>
        <div style="flex:1;">
          <div style="font-weight:600;font-size:13px;">
            ${escapeHtml(member.displayName || member.username || "Người dùng")}
            ${isMe ? " (Bạn)" : ""}
          </div>
          <div style="font-size:11px;color:var(--muted);">
            ${isCreator ? "👑 Người tạo" : member.role || "Thành viên"}
          </div>
        </div>
      </div>
    `;
    // Xóa thành viên
    if (conv.createdBy === currentUserId && !isCreator && !isMe) {
      const removeBtn = document.createElement("button");
      removeBtn.textContent = "Xóa";
      removeBtn.style.padding = "4px 12px";
      removeBtn.style.borderRadius = "6px";
      removeBtn.style.border = "1px solid #ff4444";
      removeBtn.style.background = "transparent";
      removeBtn.style.color = "#ff4444";
      removeBtn.style.cursor = "pointer";
      removeBtn.style.fontSize = "12px";

      removeBtn.addEventListener("click", async (e) => {
        e.stopPropagation();

        if (
          confirm(`Xóa ${member.displayName || member.username} khỏi nhóm?`)
        ) {
          try {
            await apiFetch(
              `/conversations/${conv.id}/members/${member.userId || member.id}`,
              {
                method: "DELETE",
              }
            );

            alert("Đã xóa thành viên");
            memberEl.remove();
          } catch (error) {
            console.error("Remove member error:", error);
            alert(
              "Lỗi khi xóa thành viên: " + (error.body?.message || error.status)
            );
          }
        }
      });

      memberEl.appendChild(removeBtn);
    }

    container.appendChild(memberEl);
  });
}

// Nút quản lý thành viên vào chat box
function openChatBox(convId, name) {
  const box = ensureChatBox();
  const title = box.querySelector("#chat-title");
  title.textContent = name || "Nhóm " + convId;

  const oldBtns = title.parentNode.querySelectorAll(".chat-action-btn");
  oldBtns.forEach((btn) => btn.remove());

  // Nút quản lý thành viên nếu là nhóm
  if (currentConversation?.type === "GROUP") {
    const memberBtn = document.createElement("button");
    memberBtn.innerHTML = '<i class="fi fi-rr-users"></i>';
    memberBtn.title = "Quản lý thành viên";
    memberBtn.className = "chat-action-btn";
    memberBtn.style.background = "transparent";
    memberBtn.style.border = "none";
    memberBtn.style.cursor = "pointer";
    memberBtn.style.marginLeft = "10px";
    memberBtn.style.fontSize = "18px";

    memberBtn.addEventListener("click", () => {
      manageGroupMembers(convId);
    });

    title.parentNode.insertBefore(memberBtn, title.nextSibling);
  }

  // Thêm nút xóa nhóm nếu là người tạo
  if (
    currentConversation?.createdBy === currentUserId &&
    currentConversation.type === "GROUP"
  ) {
    const deleteBtn = document.createElement("button");
    deleteBtn.innerHTML = "🗑";
    deleteBtn.title = "Xóa nhóm";
    deleteBtn.className = "chat-action-btn";
    deleteBtn.style.background = "transparent";
    deleteBtn.style.border = "none";
    deleteBtn.style.cursor = "pointer";
    deleteBtn.style.marginLeft = "10px";
    deleteBtn.style.fontSize = "18px";
    deleteBtn.style.color = "#ff4444";

    deleteBtn.addEventListener("click", async (e) => {
      e.stopPropagation();

      showDeleteConfirm(name || "Nhóm " + convId, async (confirmed) => {
        if (confirmed) {
          try {
            await apiFetch(`/conversations/${convId}`, {
              method: "DELETE",
            });

            box.remove();
            currentConversation = null;
            loadGroups();

            alert("Đã xóa nhóm thành công");
          } catch (error) {
            console.error("Delete group error:", error);
            alert("Lỗi khi xóa nhóm: " + (error.body?.message || error.status));
          }
        }
      });
    });

    title.parentNode.insertBefore(deleteBtn, title.nextSibling);
  }

  const body = document.getElementById("chat-body");
  body.innerHTML = "";

  const cached = messageCache[String(convId)];
  if (cached && cached.length) {
    cached.forEach((m) => {
      appendMessageToChat(m, m.senderName || "", m.isMe, m.sentAt);
    });
    body.scrollTop = body.scrollHeight;
  }
  if (wsConnected && stompClient) {
    subscribeConversation(convId);
  }
}
function showDeleteConfirm(groupName, callback) {
  const modal = document.createElement("div");
  modal.style.position = "fixed";
  modal.style.top = "0";
  modal.style.left = "0";
  modal.style.width = "100%";
  modal.style.height = "100%";
  modal.style.background = "rgba(0,0,0,0.5)";
  modal.style.display = "flex";
  modal.style.justifyContent = "center";
  modal.style.alignItems = "center";
  modal.style.zIndex = "1000";

  const escapedGroupName = escapeHtml(groupName);

  modal.innerHTML = `
    <div style="background: var(--panel); padding: 24px; border-radius: 12px; width: 320px;">
      <h3 style="margin-top: 0; color: #ff4444;"><i class="fi fi-rr-triangle-warning"></i> Xóa nhóm</h3>
      <p>Bạn có chắc muốn xóa nhóm <strong>"${escapedGroupName}"</strong>?</p>
      <p style="font-size: 14px; color: var(--muted);">
        Tất cả tin nhắn và dữ liệu sẽ bị xóa vĩnh viễn.
      </p>
      <div style="display: flex; gap: 12px; margin-top: 20px;">
        <button id="confirm-delete" style="
          flex: 1; padding: 10px; background: #ff4444; 
          color: white; border: none; border-radius: 8px; cursor: pointer;
        ">Xóa nhóm</button>
        <button id="cancel-delete" style="
          flex: 1; padding: 10px; background: var(--border); 
          color: var(--text); border: none; border-radius: 8px; cursor: pointer;
        ">Hủy</button>
      </div>
    </div>
  `;

  document.body.appendChild(modal);

  modal.querySelector("#confirm-delete").onclick = () => {
    modal.remove();
    callback(true);
  };

  modal.querySelector("#cancel-delete").onclick = () => {
    modal.remove();
    callback(false);
  };

  modal.onclick = (e) => {
    if (e.target === modal) {
      modal.remove();
      callback(false);
    }
  };
}

function escapeHtml(text) {
  if (typeof text !== "string") return text;
  const div = document.createElement("div");
  div.textContent = text;
  return div.innerHTML;
}

function showFriendSelectionDialog(friendships) {
  return new Promise((resolve) => {
    const dialog = document.createElement("div");
    dialog.style.position = "fixed";
    dialog.style.top = "50%";
    dialog.style.left = "50%";
    dialog.style.transform = "translate(-50%, -50%)";
    dialog.style.background = "var(--panel)";
    dialog.style.padding = "20px";
    dialog.style.borderRadius = "12px";
    dialog.style.zIndex = "1000";
    dialog.style.minWidth = "300px";

    const selectedFriends = [];

    dialog.innerHTML = `
            <h3 style="margin-bottom: 15px;">Chọn bạn bè (ít nhất 2)</h3>
            <div id="friend-selection-list" style="max-height: 200px; overflow-y: auto; margin-bottom: 15px;"></div>
            <div style="display: flex; gap: 10px; justify-content: flex-end;">
                <button id="confirm-selection" style="padding: 8px 16px; background: #ff7b1a; color: white; border: none; border-radius: 6px; cursor: pointer;">Xác nhận</button>
                <button id="cancel-selection" style="padding: 8px 16px; background: #666; color: white; border: none; border-radius: 6px; cursor: pointer;">Hủy</button>
            </div>
        `;

    document.body.appendChild(dialog);

    const listEl = dialog.querySelector("#friend-selection-list");

    friendships.forEach((fr) => {
      const friendId = fr.user1Id === currentUserId ? fr.user2Id : fr.user1Id;
      const friendName =
        fr.user1Id === currentUserId ? fr.user2Name : fr.user1Name;

      const item = document.createElement("div");
      item.style.display = "flex";
      item.style.alignItems = "center";
      item.style.padding = "8px";
      item.style.borderBottom = "1px solid var(--border)";

      item.innerHTML = `
                <input type="checkbox" value="${friendId}" style="margin-right: 10px;">
                <span>${escapeHtml(friendName)}</span>
            `;

      listEl.appendChild(item);
    });

    dialog.querySelector("#confirm-selection").addEventListener("click", () => {
      const checkboxes = listEl.querySelectorAll(
        'input[type="checkbox"]:checked'
      );
      const selected = Array.from(checkboxes).map((cb) => Number(cb.value));
      resolve(selected);
      dialog.remove();
    });

    dialog.querySelector("#cancel-selection").addEventListener("click", () => {
      resolve(null);
      dialog.remove();
    });
  });
}

//Chatbox, lấy tn

function ensureChatBox() {
  let box = document.getElementById("floating-chat-box");
  if (box) return box;

  box = document.createElement("div");
  box.id = "floating-chat-box";
  box.style.position = "fixed";
  box.style.right = "360px";
  box.style.bottom = "8px";
  box.style.width = "330px";
  box.style.maxHeight = "520px";
  box.style.display = "flex";
  box.style.flexDirection = "column";
  box.style.background = "var(--panel)";
  box.style.border = "2px solid var(--border)";
  box.style.borderRadius = "18px";
  box.style.boxShadow = "0 14px 30px rgba(0,0,0,0.5)";
  box.innerHTML = `
  <div style="padding:8px 10px;display:flex;align-items:center;justify-content:space-between;background:var(--panel-light);">
    <div style="font-weight:700;" id="chat-title">Chat</div>
    <div>
      <button id="chat-minimize" style="border:0;background:transparent;color:inherit;font-size:18px;cursor:pointer;margin-right:6px;">−</button>
      <button id="chat-close" style="border:0;background:transparent;color:inherit;font-size:18px;cursor:pointer;">✕</button>
    </div>
  </div>
  <div id="chat-body" style="
        height: 340px;
        overflow-y: auto;
        padding: 10px;
        background: transparent;
  "></div>
  <div id="chat-footer" style="display:flex;gap:8px;padding:8px;border-top:1px solid rgba(255,255,255,0.06);">
    <input id="chat-input" placeholder="Aa" style="flex:1;padding:8px;border-radius:10px;border:1px solid rgba(0,0,0,0.2);" />
    <button id="chat-send" style="padding:8px 14px;border-radius:10px;border:0;background:#ff7b1a;color:#fff;font-weight:600;cursor:pointer;">Gửi</button>
  </div>
`;

  document.body.appendChild(box);

  const bodyEl = box.querySelector("#chat-body");
  const footerEl = box.querySelector("#chat-footer");

  box.querySelector("#chat-minimize").addEventListener("click", () => {
    const hidden = bodyEl.style.display === "none";
    bodyEl.style.display = hidden ? "block" : "none";
    footerEl.style.display = hidden ? "flex" : "none";
  });

  box.querySelector("#chat-close").addEventListener("click", () => {
    box.remove();
  });

  box.querySelector("#chat-send").addEventListener("click", sendCurrentMessage);
  box
    .querySelector("#chat-input")
    .addEventListener(
      "keydown",
      (e) => e.key === "Enter" && sendCurrentMessage()
    );

  return box;
}

async function loadMessages(convId, loadMore = false) {
  try {
    const page = loadMore
      ? (messageCache[String(convId)]?.length || 0) / 50
      : 0;

    const res = await apiFetch(
      `/messages/conversation/${convId}?page=${page}&size=50`
    );

    const list = Array.isArray(res) ? res : res?.content || [];

    if (!loadMore) {
      messageCache[String(convId)] = [];
    }

    const body = document.getElementById("chat-body");
    if (!body) return;

    const reversedList = [...list].reverse();
    body.style.opacity = "0";
    if (!loadMore) {
      body.innerHTML = "";
    }

    reversedList.forEach((m) => {
      const isMe = String(m.senderId) === String(currentUserId);
      let senderName = m.senderName || "Người dùng";
      if (!m.senderName && m.senderId) {
        senderName = userCache[m.senderId] || "Người dùng";
      }

      const msgObj = {
        ...m,
        isMe,
        isOld: loadMore,
        senderName: isMe ? "Bạn" : senderName,
      };

      if (!messageCache[String(convId)]) messageCache[String(convId)] = [];
      messageCache[String(convId)].unshift(msgObj);

      appendMessageToChat(msgObj, msgObj.senderName, isMe, m.sentAt);
    });
    body.style.opacity = "1";
    body.scrollTop = body.scrollHeight;

    if (list.length === 50) {
      addLoadMoreButton(convId);
    }
  } catch (e) {
    console.error("loadMessages error", e);
  }
}

const userCache = {};

function appendMessageToChat(
  msg,
  senderName = "",
  isMe = false,
  timestamp = null
) {
  const body = document.getElementById("chat-body");
  if (!body) return;

  const wrap = document.createElement("div");
  wrap.className = "message-wrapper";
  wrap.style.margin = "6px 0";
  wrap.style.display = "flex";
  wrap.style.flexDirection = "column";
  wrap.style.alignItems = isMe ? "flex-end" : "flex-start";

  if (!isMe && senderName && currentConversation?.type === "GROUP") {
    const nameEl = document.createElement("div");
    nameEl.style.fontSize = "12px";
    nameEl.style.color = "var(--muted)";
    nameEl.style.marginLeft = "8px";
    nameEl.style.marginBottom = "2px";
    nameEl.style.fontWeight = "500";
    nameEl.textContent = senderName;
    wrap.appendChild(nameEl);
  }
  const bubbleContainer = document.createElement("div");
  bubbleContainer.style.display = "flex";
  bubbleContainer.style.flexDirection = isMe ? "row-reverse" : "row";
  bubbleContainer.style.alignItems = "flex-end";
  bubbleContainer.style.gap = "6px";
  bubbleContainer.style.maxWidth = "85%";

  const bubble = document.createElement("div");
  bubble.style.maxWidth = "100%";
  bubble.style.padding = "8px 12px";
  bubble.style.borderRadius = "16px";
  bubble.style.fontSize = "14px";
  bubble.style.whiteSpace = "pre-wrap";
  bubble.style.wordBreak = "break-word";
  bubble.style.lineHeight = "1.4";
  bubble.textContent = msg.content || "";

  if (isMe) {
    bubble.style.background = "#ff7b1a";
    bubble.style.color = "#fff";
    bubble.style.borderBottomRightRadius = "4px";
  } else {
    bubble.style.background = "rgba(255,255,255,0.07)";
    bubble.style.color = "var(--text)";
    bubble.style.borderBottomLeftRadius = "4px";
  }

  const timeEl = document.createElement("div");
  timeEl.style.fontSize = "10px";
  timeEl.style.color = "var(--muted)";
  timeEl.style.minWidth = "40px";
  timeEl.style.opacity = "0.7";

  if (timestamp) {
    const date = new Date(timestamp);
    timeEl.textContent = formatTime(date);
  } else {
    timeEl.textContent = "Vừa xong";
  }

  bubbleContainer.appendChild(bubble);
  bubbleContainer.appendChild(timeEl);
  wrap.appendChild(bubbleContainer);

  if (msg.isOld) {
    body.insertBefore(wrap, body.firstChild);
  } else {
    body.appendChild(wrap);
  }

  if (!msg.isOld) {
    body.scrollTop = body.scrollHeight;
  }
}

function formatTime(date) {
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);

  if (diffMins < 1) return "Vừa xong";
  if (diffMins < 60) return `${diffMins} phút`;

  return date.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function addLoadMoreButton(convId) {
  const body = document.getElementById("chat-body");
  if (!body) return;

  if (document.getElementById("load-more-btn")) return;

  const loadMoreBtn = document.createElement("button");
  loadMoreBtn.id = "load-more-btn";
  loadMoreBtn.textContent = "Tải thêm tin nhắn cũ";
  loadMoreBtn.style.margin = "10px auto";
  loadMoreBtn.style.padding = "6px 12px";
  loadMoreBtn.style.background = "var(--panel-light)";
  loadMoreBtn.style.border = "1px solid var(--border)";
  loadMoreBtn.style.borderRadius = "8px";
  loadMoreBtn.style.color = "var(--text)";
  loadMoreBtn.style.cursor = "pointer";
  loadMoreBtn.style.fontSize = "12px";
  loadMoreBtn.style.display = "block";

  loadMoreBtn.onclick = () => {
    loadMoreBtn.remove();
    loadMessages(convId, true);
  };

  body.insertBefore(loadMoreBtn, body.firstChild);
}

async function sendCurrentMessage() {
  if (!currentConversation) {
    alert("Hãy chọn 1 nhóm trước");
    return;
  }
  const input = document.getElementById("chat-input");
  if (!input) return;
  const text = input.value.trim();
  if (!text) return;

  const body = {
    conversationId: currentConversation.id,
    content: text,
    messageType: "TEXT",
    clientCid: generateCid(),
  };

  try {
    const resp = await apiFetch("/messages", {
      method: "POST",
      body: JSON.stringify(body),
    });
    if (resp.id) {
      seenMessageIds.add(resp.id);
    }

    const msgObj = {
      ...resp,
      isMe: true,
      senderName: "Bạn",
      createdAt: new Date().toISOString(),
    };

    const key = String(currentConversation.id);
    if (!messageCache[key]) messageCache[key] = [];
    messageCache[key].push(msgObj);

    appendMessageToChat(msgObj, "Bạn", true, msgObj.createdAt);
    input.value = "";
  } catch (e) {
    console.error("send message error", e);
  }
}
async function openDirectChat(friend) {
  try {
    // Tạo hoặc lấy conversation DIRECT
    const convResponse = await apiFetch(`/conversations/direct/${friend.id}`, {
      method: "POST",
    });

    currentConversation = {
      id: convResponse.id,
      name: friend.name,
      type: "DIRECT",
    };

    openChatBox(convResponse.id, friend.name);
    loadMessages(convResponse.id);
  } catch (e) {
    console.error("Lỗi mở chat trực tiếp:", e);
    alert("Không thể mở chat với " + friend.name);
  }
}

// Đăng ảnh, caption lỗi cần fix to đùng

function openPostDialog() {
  // nếu đã mở thì không tạo thêm
  if (document.querySelector(".post-backdrop")) return;

  let selectedFile = null;

  const backdrop = document.createElement("div");
  backdrop.className = "post-backdrop";

  const modal = document.createElement("div");
  modal.className = "post-modal";
  backdrop.appendChild(modal);

  // header
  modal.innerHTML = `
    <div class="post-modal-header">
      <button class="post-modal-close">✕</button>
      <div class="post-modal-title">Đăng ảnh chiu</div>
    </div>
    <div class="post-modal-body">
      <div class="post-modal-image-wrap">
        <span class="post-modal-placeholder">Nhấn để chọn ảnh...</span>
        <img id="post-modal-img" style="display:none;" alt="preview" />
      </div>
      <div>
        <div class="post-modal-caption-label">Cáp sừn</div>
        <textarea
          class="post-modal-caption-input"
          id="post-modal-caption"
          placeholder="Viết gì đó dễ thương..."
        ></textarea>
      </div>
    </div>
    <div class="post-modal-footer">
      <button class="post-modal-submit">Đăng tin</button>
    </div>
  `;

  document.body.appendChild(backdrop);

  const btnClose = modal.querySelector(".post-modal-close");
  const imageWrap = modal.querySelector(".post-modal-image-wrap");
  const imgEl = modal.querySelector("#post-modal-img");
  const placeholder = modal.querySelector(".post-modal-placeholder");
  const captionInput = modal.querySelector("#post-modal-caption");
  const btnSubmit = modal.querySelector(".post-modal-submit");

  const fileInput = document.createElement("input");
  fileInput.type = "file";
  fileInput.accept = "image/*";
  fileInput.style.display = "none";
  document.body.appendChild(fileInput);

  function closeModal() {
    backdrop.remove();
    fileInput.remove();
  }

  btnClose.addEventListener("click", closeModal);
  backdrop.addEventListener("click", (e) => {
    if (e.target === backdrop) closeModal();
  });

  imageWrap.addEventListener("click", () => fileInput.click());

  fileInput.addEventListener("change", () => {
    const file = fileInput.files[0];
    if (!file) return;
    selectedFile = file;

    const reader = new FileReader();
    reader.onload = (ev) => {
      imgEl.src = ev.target.result;
      imgEl.style.display = "block";
      placeholder.style.display = "none";
    };
    reader.readAsDataURL(file);
  });

  btnSubmit.addEventListener("click", async () => {
    if (!selectedFile) {
      alert("Hãy chọn 1 ảnh trước đã!");
      return;
    }

    const caption = captionInput.value.trim();

    try {
      const form = new FormData();
      form.append("file", selectedFile);
      form.append("mediaType", "PHOTO");

      const media = await apiFetch("/media/upload", {
        method: "POST",
        body: form,
      });

      if (media.fileUrl && mainImageEl) {
        mainImageEl.src = media.fileUrl;
      }
      if (captionEl) {
        captionEl.textContent = caption || "";
      }
      // Khả năng cần 1 api tạo post lại

      closeModal();
    } catch (e) {
      console.error("Upload ảnh / đăng tin lỗi", e);
      alert("Đăng ảnh thất bại: " + (e.body?.message || e.status || ""));
    }
  });
}
// 6.Tìm bạn theo id or tên
searchInput?.addEventListener("keydown", async (e) => {
  if (e.key !== "Enter") return;

  const val = searchInput.value.trim();
  if (!val) {
    searchResultList.innerHTML = "";
    searchResultPanel.style.display = "none";
    return;
  }

  try {
    let users;
    if (/^\d+$/.test(val)) {
      // search theo id
      console.log("Searching by ID:", val);
      const u = await apiFetch(`/users/${encodeURIComponent(val)}`);
      users = u ? [u] : [];
      console.log("User found by ID:", u);
    } else {
      // search theo tên
      console.log("Searching by name:", val);
      users = await apiFetch(
        `/users/search?keyword=${encodeURIComponent(val)}`
      );
      console.log("Users found by name:", users);
    }

    searchResultList.innerHTML = "";
    if (users && users.length) {
      users.forEach((u) => {
        console.log("User object for rendering:", u);
        if (u.userId) {
          searchResultList.appendChild(renderUserSearchItem(u));
        } else {
          console.warn("User object missing userId:", u);
        }
      });
      searchResultPanel.style.display = "block";
    } else {
      searchResultList.innerHTML = "<li>Không tìm thấy người dùng</li>";
      searchResultPanel.style.display = "block";
    }
  } catch (err) {
    console.error("Search error:", err);
    alert(
      "Search lỗi: " + (err.body?.message || err.status || "Không tìm thấy API")
    );
  }
});

function renderUserSearchItem(user) {
  console.log("Rendering user:", user);

  const li = document.createElement("li");
  li.style.display = "flex";
  li.style.alignItems = "center";
  li.style.justifyContent = "space-between";
  li.style.padding = "8px 10px";
  li.style.borderRadius = "12px";
  li.style.marginBottom = "6px";
  li.style.background = "var(--panel-light)";

  const left = document.createElement("div");
  left.style.display = "flex";
  left.style.alignItems = "center";
  left.style.gap = "10px";

  const avatar = document.createElement("div");
  avatar.style.width = "36px";
  avatar.style.height = "36px";
  avatar.style.borderRadius = "50%";
  avatar.style.backgroundSize = "cover";
  avatar.style.backgroundPosition = "center";
  avatar.style.border = "2px solid rgba(0,0,0,0.25)";

  const avatarUrl = user.avatarUrl || "";
  if (avatarUrl) {
    avatar.style.backgroundImage = `url('${avatarUrl}')`;
  }

  const info = document.createElement("div");

  // SỬA: Kiểm tra cả userId và id
  const userId = user.userId || user.id;
  const displayName = user.displayName || user.username || "Người dùng";
  const username = user.username || "";

  info.innerHTML = `
      <div style="font-weight:600;">${escapeHtml(displayName)}</div>
      <div style="font-size:12px;color:var(--muted);">
        ${username ? `${escapeHtml(username)} • ` : ""}ID: ${userId || "N/A"}
      </div>
  `;

  left.appendChild(avatar);
  left.appendChild(info);

  const btn = document.createElement("button");
  btn.textContent = "Kết bạn";
  btn.style.border = "0";
  btn.style.borderRadius = "16px";
  btn.style.padding = "6px 12px";
  btn.style.cursor = "pointer";
  btn.style.fontSize = "13px";
  btn.style.background = "var(--accent)";
  btn.style.color = "#fff";

  btn.addEventListener("click", async (e) => {
    e.stopPropagation();
    console.log("Friend request clicked for user:", user);

    // SỬA: Dùng biến userId đã xác định
    const targetUserId = user.userId || user.id;

    if (!targetUserId) {
      console.error("No user ID found in:", user);
      alert("Người dùng này không có ID hợp lệ");
      return;
    }

    const targetId = Number(targetUserId);
    if (isNaN(targetId) || targetId <= 0) {
      alert("ID người dùng không hợp lệ: " + targetUserId);
      return;
    }

    // SỬA: Thêm /api prefix
    try {
      const friendshipStatus = await apiFetch(
        `/friends/status?user1Id=${currentUserId}&user2Id=${targetId}` // THÊM /api
      );

      if (friendshipStatus && friendshipStatus.status === "ACCEPTED") {
        alert(`Bạn và ${displayName} đã là bạn bè`);
        btn.textContent = "Đã là bạn";
        btn.style.background = "#27ae60";
        btn.disabled = true;
        return;
      }

      if (friendshipStatus && friendshipStatus.status === "PENDING") {
        alert(`Đã gửi lời mời kết bạn tới ${displayName}`);
        btn.textContent = "Đã gửi lời mời";
        btn.style.background = "#f39c12";
        btn.disabled = true;
        return;
      }

      // THÊM: Xử lý các trạng thái khác
      if (friendshipStatus && friendshipStatus.status === "BLOCKED") {
        alert(`Không thể kết bạn: ${displayName} đã bị chặn`);
        btn.textContent = "Đã chặn";
        btn.style.background = "#e74c3c";
        btn.disabled = true;
        return;
      }
    } catch (statusError) {
      console.log("Could not check friendship status:", statusError);
      // Nếu lỗi 404 (không tìm thấy friendship), tiếp tục gửi request
      if (statusError.status !== 404) {
        console.log("Proceeding to send request anyway...");
      }
    }

    // Gửi friend request
    sendFriendRequest(targetId, btn, displayName);
  });

  left.addEventListener("click", () => {
    const userId = user.userId || user.id;
    if (userId) {
      window.location.href = `/api/public/html/profile.html?userId=${userId}`;
    } else {
      alert("Không thể xem profile: ID người dùng không hợp lệ");
    }
  });

  li.appendChild(left);
  li.appendChild(btn);

  return li;
}
async function sendFriendRequest(
  targetUserId,
  buttonElement = null,
  displayName = ""
) {
  if (!currentUserId) {
    alert("Không xác định được user hiện tại");
    return;
  }

  const targetId = Number(targetUserId);
  if (isNaN(targetId) || targetId <= 0) {
    alert("ID người dùng phải là số dương");
    return;
  }

  if (currentUserId === targetId) {
    alert("Không thể kết bạn với chính mình");
    return;
  }

  try {
    const requestBody = {
      targetUserId: targetId,
    };

    console.log("Sending friend request:", {
      currentUserId,
      targetUserId: targetId,
      requestBody,
    });

    // SỬA: Thêm /api prefix
    const result = await apiFetch(
      `/friends/requests?currentUserId=${currentUserId}`, // THÊM /api
      {
        method: "POST",
        body: JSON.stringify(requestBody),
        headers: {
          "Content-Type": "application/json",
        },
      }
    );

    console.log("Friend request successful:", result);

    // Cập nhật UI nếu có button element
    if (buttonElement) {
      buttonElement.textContent = "Đã gửi lời mời";
      buttonElement.style.background = "#f39c12";
      buttonElement.disabled = true;
    }

    alert("Đã gửi lời mời kết bạn thành công!");
  } catch (e) {
    console.error("Send friend request error:", e);

    let errorMsg = "Lỗi không xác định";
    if (e.body) {
      if (typeof e.body === "string") {
        try {
          const parsed = JSON.parse(e.body);
          errorMsg = parsed.message || parsed.error || e.body;
        } catch {
          errorMsg = e.body;
        }
      } else if (e.body.message) {
        errorMsg = e.body.message;
      }
    } else if (e.status) {
      if (e.status === 400) {
        errorMsg =
          "Yêu cầu không hợp lệ (có thể đã là bạn bè hoặc đã gửi lời mời)";
      } else if (e.status === 404) {
        errorMsg = "Người dùng không tồn tại";
      } else if (e.status === 403) {
        errorMsg = "Không có quyền thực hiện";
      } else {
        errorMsg = `Lỗi HTTP ${e.status}`;
      }
    }

    alert("Gửi lời mời lỗi: " + errorMsg);
  }
}

btnCreateGroup?.addEventListener("click", createGroup);
btnPost?.addEventListener("click", openPostDialog);

let feedPosts = [];
let currentPostIndex = 0;

const postAuthorNameEl = document.getElementById("postAuthorName");
const postPrevBtn = document.getElementById("postPrevBtn");
const postNextBtn = document.getElementById("postNextBtn");

// load danh sách tin (đoạn này sai sai)
async function loadFeed() {
  try {
    // Lấy feed từ API mới (bài đăng của bạn bè)
    feedPosts = await apiFetch("/feed?page=0&size=20");

    if (feedPosts && feedPosts.length > 0) {
      showPost(0);
    } else {
      const mediaList = await apiFetch("/media/my-media?mediaType=PHOTO");
      feedPosts =
        mediaList?.map((m) => ({
          id: m.id,
          fileUrl: m.fileUrl,
          caption: m.caption || m.fileName,
          userId: m.userId,
          authorName: "Bạn", // cần userinfor
        })) || [];

      if (feedPosts.length > 0) {
        showPost(0);
      } else {
        mainImageEl.src = "";
        captionEl.textContent = "Chưa có bài đăng nào.";
      }
    }
  } catch (e) {
    console.error("loadFeed error", e);
  }
}

function showPost(index) {
  if (!feedPosts || feedPosts.length === 0) return;

  if (index < 0) index = feedPosts.length - 1;
  if (index >= feedPosts.length) index = 0;
  currentPostIndex = index;

  const p = feedPosts[currentPostIndex];
  mainImageEl.src = p.fileUrl || "";
  captionEl.textContent = p.caption || "";

  if (p.authorName) {
    postAuthorNameEl.textContent = p.authorName;
  } else if (p.userId === currentUserId) {
    postAuthorNameEl.textContent = "Bạn"; //tra lại
  } else {
    postAuthorNameEl.textContent = "Người dùng"; //tra lại tiếp
  }
}

postPrevBtn?.addEventListener("click", () => showPost(currentPostIndex - 1));
postNextBtn?.addEventListener("click", () => showPost(currentPostIndex + 1));

// (light / dark)
const themeStylesheet = document.getElementById("themeStylesheet");
const themeToggleBtn = document.getElementById("themeToggle");
const themeIcon = document.getElementById("themeIcon");

function applyTheme(theme) {
  if (!themeStylesheet) return;

  if (theme === "dark") {
    themeStylesheet.href = "/api/public/css/style-dark.css";
    if (themeIcon) {
      themeIcon.classList.remove("fi-sr-moon");
      themeIcon.classList.add("fi-sr-sun");
    }
  } else {
    themeStylesheet.href = "/api/public/css/style.css";
    if (themeIcon) {
      themeIcon.classList.remove("fi-sr-sun");
      themeIcon.classList.add("fi-sr-moon");
    }
  }
  localStorage.setItem("zmnt_theme", theme);
}

// nút bấm đổi theme
themeToggleBtn?.addEventListener("click", () => {
  const current = localStorage.getItem("zmnt_theme") || "light";
  const next = current === "light" ? "dark" : "light";
  applyTheme(next);
});

// áp dụng theme lúc load trang
applyTheme(localStorage.getItem("zmnt_theme") || "light");

(async function init() {
  connectWebSocket();
  await loadProfile();
  await loadFriends();
  await loadGroups();
  await loadFeed();
})();
