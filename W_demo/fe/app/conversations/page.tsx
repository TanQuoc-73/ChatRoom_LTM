"use client";

import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";
import { Users, Search, Plus, Lock, Globe } from "lucide-react";

export default function ConversationsPage() {
  // Mock data for group conversations
  const groups = [
    { id: 1, name: "Nhóm Học Tập", members: 15, lastActive: "5 phút trước", isPublic: true, avatar: "📚" },
    { id: 2, name: "Dự Án Web", members: 8, lastActive: "1 giờ trước", isPublic: false, avatar: "💻" },
    { id: 3, name: "Gia Đình", members: 6, lastActive: "2 giờ trước", isPublic: false, avatar: "👨‍👩‍👧‍👦" },
    { id: 4, name: "Bạn Thân", members: 12, lastActive: "Hôm qua", isPublic: false, avatar: "🎉" },
  ];

  return (
    <>
      <Header />
      <div className="pt-16 min-h-screen bg-gray-50">
        <div className="max-w-6xl mx-auto p-4">
          {/* Page Header */}
          <div className="bg-white rounded-lg border border-gray-200 p-6 mb-4">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h1 className="text-2xl font-bold text-gray-900 mb-1">Nhóm Chat</h1>
                <p className="text-gray-600">Quản lý các cuộc trò chuyện nhóm của bạn</p>
              </div>
              <button className="flex items-center gap-2 bg-gradient-to-r from-purple-600 to-pink-600 text-white px-4 py-2 rounded-lg hover:from-purple-700 hover:to-pink-700 transition-all">
                <Plus className="h-5 w-5" />
                Tạo nhóm mới
              </button>
            </div>

            {/* Search */}
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Search className="h-4 w-4 text-gray-400" />
              </div>
              <input
                type="text"
                placeholder="Tìm kiếm nhóm..."
                className="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 transition-all"
              />
            </div>
          </div>

          {/* Groups Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {groups.map((group) => (
              <div
                key={group.id}
                className="bg-white rounded-lg border border-gray-200 p-6 hover:shadow-lg transition-all cursor-pointer group"
              >
                {/* Group Avatar */}
                <div className="flex items-start justify-between mb-4">
                  <div className="text-5xl">{group.avatar}</div>
                  <div className="flex items-center gap-1 text-xs text-gray-500">
                    {group.isPublic ? (
                      <>
                        <Globe className="h-3 w-3" />
                        <span>Công khai</span>
                      </>
                    ) : (
                      <>
                        <Lock className="h-3 w-3" />
                        <span>Riêng tư</span>
                      </>
                    )}
                  </div>
                </div>

                {/* Group Info */}
                <h3 className="font-semibold text-lg text-gray-900 mb-2 group-hover:text-purple-600 transition-colors">
                  {group.name}
                </h3>
                
                <div className="flex items-center gap-4 text-sm text-gray-600 mb-3">
                  <div className="flex items-center gap-1">
                    <Users className="h-4 w-4" />
                    <span>{group.members} thành viên</span>
                  </div>
                </div>

                <p className="text-xs text-gray-500">
                  Hoạt động {group.lastActive}
                </p>

                {/* Action Button */}
                <button className="w-full mt-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors">
                  Xem chi tiết
                </button>
              </div>
            ))}

            {/* Create New Group Card */}
            <div className="bg-gradient-to-br from-purple-50 to-pink-50 rounded-lg border-2 border-dashed border-purple-300 p-6 flex flex-col items-center justify-center cursor-pointer hover:border-purple-500 transition-all group">
              <div className="bg-white rounded-full p-4 mb-3 group-hover:scale-110 transition-transform">
                <Plus className="h-8 w-8 text-purple-600" />
              </div>
              <h3 className="font-semibold text-gray-900 mb-1">Tạo nhóm mới</h3>
              <p className="text-sm text-gray-600 text-center">
                Bắt đầu cuộc trò chuyện nhóm với bạn bè
              </p>
            </div>
          </div>
        </div>
      </div>
      <Footer />
    </>
  );
}
