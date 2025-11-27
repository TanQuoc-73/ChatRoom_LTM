"use client";

import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";
import { MessageCircle, Search, MoreVertical } from "lucide-react";

export default function MessagesPage() {
  // Mock data for demonstration
  const conversations = [
    { id: 1, name: "Nguyễn Văn A", lastMessage: "Chào bạn!", time: "10:30", unread: 2, avatar: "🧑" },
    { id: 2, name: "Trần Thị B", lastMessage: "Hẹn gặp lại nhé", time: "09:15", unread: 0, avatar: "👩" },
    { id: 3, name: "Lê Văn C", lastMessage: "Cảm ơn bạn nhiều!", time: "Hôm qua", unread: 1, avatar: "👨" },
    { id: 4, name: "Phạm Thị D", lastMessage: "OK, tôi hiểu rồi", time: "Hôm qua", unread: 0, avatar: "👧" },
  ];

  return (
    <>
      <Header />
      <div className="pt-16 min-h-screen bg-gray-50">
        <div className="max-w-6xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-0 bg-white border border-gray-200 mt-4 mx-4 rounded-lg overflow-hidden">
            {/* Left Sidebar - Conversation List */}
            <div className="md:col-span-1 border-r border-gray-200">
              {/* Header */}
              <div className="p-4 border-b border-gray-200">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-xl font-semibold">Tin nhắn</h2>
                  <button className="p-2 hover:bg-gray-100 rounded-full transition-colors">
                    <MoreVertical className="h-5 w-5" />
                  </button>
                </div>
                
                {/* Search */}
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Search className="h-4 w-4 text-gray-400" />
                  </div>
                  <input
                    type="text"
                    placeholder="Tìm kiếm tin nhắn..."
                    className="w-full pl-10 pr-4 py-2 bg-gray-100 border-0 rounded-lg text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-all"
                  />
                </div>
              </div>

              {/* Conversation List */}
              <div className="overflow-y-auto" style={{ maxHeight: 'calc(100vh - 240px)' }}>
                {conversations.map((conv) => (
                  <div
                    key={conv.id}
                    className="flex items-center gap-3 p-4 hover:bg-gray-50 cursor-pointer transition-colors border-b border-gray-100"
                  >
                    <div className="text-3xl">{conv.avatar}</div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between mb-1">
                        <h3 className="font-semibold text-sm truncate">{conv.name}</h3>
                        <span className="text-xs text-gray-500">{conv.time}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <p className="text-sm text-gray-600 truncate">{conv.lastMessage}</p>
                        {conv.unread > 0 && (
                          <span className="bg-blue-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center font-semibold ml-2">
                            {conv.unread}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Right Side - Chat Area */}
            <div className="md:col-span-2 flex flex-col items-center justify-center p-8 bg-gray-50">
              <MessageCircle className="h-24 w-24 text-gray-300 mb-4" strokeWidth={1} />
              <h3 className="text-2xl font-semibold text-gray-800 mb-2">Tin nhắn của bạn</h3>
              <p className="text-gray-500 text-center max-w-md">
                Gửi tin nhắn riêng tư cho bạn bè hoặc nhóm. Chọn một cuộc trò chuyện để bắt đầu.
              </p>
            </div>
          </div>
        </div>
      </div>
      <Footer />
    </>
  );
}
