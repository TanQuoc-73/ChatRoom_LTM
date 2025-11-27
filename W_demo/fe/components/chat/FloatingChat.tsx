"use client";

import { useState } from "react";
import { MessageCircle, X, Send, Minimize2, Search } from "lucide-react";

export default function FloatingChat() {
  const [isOpen, setIsOpen] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);
  const [message, setMessage] = useState("");
  const [selectedChat, setSelectedChat] = useState<number | null>(null);

  // Mock conversations
  const conversations = [
    { id: 1, name: "Nguyễn Văn A", avatar: "👤", lastMessage: "Chào bạn!", unread: 2, online: true },
    { id: 2, name: "Trần Thị B", avatar: "👩", lastMessage: "Hẹn gặp lại!", unread: 0, online: false },
    { id: 3, name: "Lê Văn C", avatar: "👨", lastMessage: "OK nhé", unread: 1, online: true },
  ];

  // Mock messages
  const messages = [
    { id: 1, sender: "other", text: "Chào bạn!", time: "10:30" },
    { id: 2, sender: "me", text: "Chào! Bạn khỏe không?", time: "10:31" },
    { id: 3, sender: "other", text: "Mình khỏe, cảm ơn bạn!", time: "10:32" },
  ];

  const handleSend = () => {
    if (message.trim()) {
      console.log("Sending:", message);
      setMessage("");
    }
  };

  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-6 right-6 z-50 group animate-in fade-in zoom-in duration-300"
        aria-label="Open chat"
      >
        <div className="relative">
          {/* Notification Badge */}
          <div className="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center animate-pulse">
            3
          </div>
          
          {/* Chat Button */}
          <div className="bg-gradient-to-br from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 text-white rounded-full p-4 shadow-lg hover:shadow-xl transition-all duration-300 group-hover:scale-110 active:scale-95">
            <MessageCircle className="h-6 w-6" strokeWidth={2} />
          </div>
          
          {/* Tooltip */}
          <div className="absolute bottom-full right-0 mb-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
            <div className="bg-gray-900 text-white text-sm px-3 py-2 rounded-lg whitespace-nowrap">
              Tin nhắn
              <div className="absolute top-full right-4 w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-900"></div>
            </div>
          </div>
        </div>
      </button>
    );
  }

  return (
    <div className={`fixed bottom-6 right-6 z-50 transition-all duration-300 ease-out ${
      isMinimized ? 'w-80 h-14' : 'w-96 h-[600px]'
    } ${
      isOpen ? 'scale-100 opacity-100' : 'scale-0 opacity-0'
    }`}
    style={{ transformOrigin: 'bottom right' }}
    >
      <div className="bg-white rounded-lg shadow-2xl border border-gray-200 h-full">
        {/* Header */}
        <div className="bg-gradient-to-r from-purple-600 to-pink-600 text-white p-4 rounded-t-lg flex items-center justify-between">
          <div className="flex items-center gap-2">
            <MessageCircle className="h-5 w-5" />
            <h3 className="font-semibold">
              {selectedChat ? conversations.find(c => c.id === selectedChat)?.name : "Tin nhắn"}
            </h3>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsMinimized(!isMinimized)}
              className="p-1 hover:bg-white/20 rounded transition-colors"
            >
              <Minimize2 className="h-4 w-4" />
            </button>
            <button
              onClick={() => {
                setIsOpen(false);
                setTimeout(() => setSelectedChat(null), 300);
              }}
              className="p-1 hover:bg-white/20 rounded transition-colors"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

      {/* Content */}
      {!isMinimized && (
        <>
          {selectedChat === null ? (
            // Conversation List
            <div className="h-[calc(100%-56px)] flex flex-col">
              {/* Search */}
              <div className="p-3 border-b border-gray-200">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Tìm kiếm..."
                    className="w-full pl-10 pr-4 py-2 bg-gray-100 rounded-full text-sm outline-none focus:ring-2 focus:ring-purple-500"
                  />
                </div>
              </div>

              {/* Conversations */}
              <div className="flex-1 overflow-y-auto">
                {conversations.map((conv) => (
                  <button
                    key={conv.id}
                    onClick={() => setSelectedChat(conv.id)}
                    className="w-full p-3 hover:bg-gray-50 transition-colors flex items-center gap-3 border-b border-gray-100"
                  >
                    <div className="relative">
                      <div className="w-12 h-12 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-2xl">
                        {conv.avatar}
                      </div>
                      {conv.online && (
                        <div className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-white"></div>
                      )}
                    </div>
                    <div className="flex-1 text-left">
                      <div className="flex items-center justify-between mb-1">
                        <p className="font-semibold text-sm text-gray-900">{conv.name}</p>
                        {conv.unread > 0 && (
                          <span className="bg-red-500 text-white text-xs rounded-full px-2 py-0.5">
                            {conv.unread}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-gray-600 truncate">{conv.lastMessage}</p>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            // Chat Window
            <div className="h-[calc(100%-56px)] flex flex-col">
              {/* Back Button */}
              <div className="p-3 border-b border-gray-200">
                <button
                  onClick={() => setSelectedChat(null)}
                  className="text-sm text-purple-600 hover:text-purple-700 font-medium"
                >
                  ← Quay lại
                </button>
              </div>

              {/* Messages */}
              <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`flex ${msg.sender === "me" ? "justify-end" : "justify-start"}`}
                  >
                    <div className={`max-w-[70%] ${
                      msg.sender === "me"
                        ? "bg-gradient-to-r from-purple-600 to-pink-600 text-white"
                        : "bg-gray-200 text-gray-900"
                    } rounded-2xl px-4 py-2`}>
                      <p className="text-sm">{msg.text}</p>
                      <p className={`text-xs mt-1 ${
                        msg.sender === "me" ? "text-purple-100" : "text-gray-500"
                      }`}>
                        {msg.time}
                      </p>
                    </div>
                  </div>
                ))}
              </div>

              {/* Input */}
              <div className="p-3 border-t border-gray-200">
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    onKeyPress={(e) => e.key === "Enter" && handleSend()}
                    placeholder="Aa"
                    className="flex-1 px-4 py-2 bg-gray-100 rounded-full text-sm outline-none focus:ring-2 focus:ring-purple-500"
                  />
                  <button
                    onClick={handleSend}
                    className="p-2 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-full hover:from-purple-700 hover:to-pink-700 transition-colors"
                  >
                    <Send className="h-4 w-4" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}
      </div>
    </div>
  );
}
