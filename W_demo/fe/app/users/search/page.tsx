"use client";

import { useUserSearch } from "@/hooks/useUserSearch";
import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";
import { Search, UserPlus, MessageCircle, MoreHorizontal, Loader2, AlertCircle } from "lucide-react";

export default function UserSearchPage() {
  const { searchQuery, setSearchQuery, users, isLoading, error } = useUserSearch(300);

  // Format date for display
  const formatDate = (dateString?: string) => {
    if (!dateString) return null;
    try {
      return new Date(dateString).toLocaleDateString('vi-VN');
    } catch {
      return null;
    }
  };

  // Get avatar display
  const getAvatarDisplay = (user: typeof users) => {
    if (user?.avatarUrl) {
      return (
        <img 
          src={user.avatarUrl} 
          alt={user.displayName || user.username}
          className="w-16 h-16 rounded-full object-cover"
        />
      );
    }
    return <div className="w-16 h-16 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-white text-2xl font-bold">
      {(user?.displayName || user?.username || '?').charAt(0).toUpperCase()}
    </div>;
  };

  return (
    <>
      <Header />
      <div className="pt-16 min-h-screen bg-gray-50">
        <div className="max-w-4xl mx-auto p-4">
          {/* Search Header */}
          <div className="bg-white rounded-lg border border-gray-200 p-6 mb-6">
            <h1 className="text-2xl font-bold text-gray-900 mb-4">Tìm kiếm người dùng</h1>
            
            {/* Search Bar */}
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <Search className="h-5 w-5 text-gray-400" />
              </div>
              <input
                type="text"
                placeholder="Tìm kiếm theo username..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-12 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-lg text-base placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
              />
              {isLoading && (
                <div className="absolute inset-y-0 right-0 pr-4 flex items-center">
                  <Loader2 className="h-5 w-5 text-purple-600 animate-spin" />
                </div>
              )}
            </div>

            {searchQuery && !isLoading && (
              <p className="mt-3 text-sm text-gray-600">
                {users ? (
                  <>Tìm thấy kết quả cho "<span className="font-semibold">{searchQuery}</span>"</>
                ) : error ? (
                  <span className="text-red-600">Lỗi: {error}</span>
                ) : (
                  <>Không tìm thấy kết quả cho "<span className="font-semibold">{searchQuery}</span>"</>
                )}
              </p>
            )}
          </div>

          {/* Error State */}
          {error && !isLoading && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-6 mb-6">
              <div className="flex items-start gap-3">
                <AlertCircle className="h-6 w-6 text-red-600 flex-shrink-0 mt-0.5" />
                <div>
                  <h3 className="font-semibold text-red-900 mb-1">Có lỗi xảy ra</h3>
                  <p className="text-red-700">{error}</p>
                </div>
              </div>
            </div>
          )}

          {/* Loading Skeleton */}
          {isLoading && (
            <div className="bg-white rounded-lg border border-gray-200 p-5 animate-pulse">
              <div className="flex items-start gap-4">
                <div className="w-16 h-16 bg-gray-200 rounded-full"></div>
                <div className="flex-1">
                  <div className="h-6 bg-gray-200 rounded w-1/3 mb-2"></div>
                  <div className="h-4 bg-gray-200 rounded w-1/4 mb-3"></div>
                  <div className="h-4 bg-gray-200 rounded w-2/3"></div>
                </div>
              </div>
            </div>
          )}

          {/* User Result */}
          {users && !isLoading && (
            <div className="bg-white rounded-lg border border-gray-200 p-5 hover:shadow-md transition-all">
              <div className="flex items-start gap-4">
                {/* Avatar */}
                {getAvatarDisplay(users)}

                {/* User Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <h3 className="font-semibold text-lg text-gray-900 hover:text-purple-600 cursor-pointer transition-colors flex items-center gap-2">
                        {users.displayName || users.username}
                        {users.verified && (
                          <span className="text-blue-500" title="Đã xác minh">✓</span>
                        )}
                      </h3>
                      <p className="text-sm text-gray-500">@{users.username}</p>
                    </div>
                    <button className="p-2 hover:bg-gray-100 rounded-full transition-colors">
                      <MoreHorizontal className="h-5 w-5 text-gray-600" />
                    </button>
                  </div>

                  {users.bio && (
                    <p className="text-sm text-gray-700 mb-3">{users.bio}</p>
                  )}

                  <div className="flex flex-wrap gap-3 text-sm text-gray-600 mb-3">
                    {users.email && (
                      <span>📧 {users.email}</span>
                    )}
                    {users.website && (
                      <a href={users.website} target="_blank" rel="noopener noreferrer" className="text-purple-600 hover:underline">
                        🔗 {users.website}
                      </a>
                    )}
                    {users.dateOfBirth && (
                      <span>🎂 {formatDate(users.dateOfBirth)}</span>
                    )}
                    {users.gender && (
                      <span>👤 {users.gender}</span>
                    )}
                  </div>

                  <div className="flex items-center justify-between">
                    <p className="text-sm text-gray-600">
                      {users.lastActive && (
                        <span>Hoạt động lần cuối: {formatDate(users.lastActive)}</span>
                      )}
                    </p>

                    {/* Action Buttons */}
                    <div className="flex items-center gap-2">
                      <button className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-lg hover:from-purple-700 hover:to-pink-700 transition-all text-sm font-medium">
                        <UserPlus className="h-4 w-4" />
                        Theo dõi
                      </button>
                      <button className="p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
                        <MessageCircle className="h-4 w-4 text-gray-700" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Empty State - No Search Query */}
          {!searchQuery && !isLoading && (
            <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
              <Search className="h-16 w-16 text-gray-300 mx-auto mb-4" strokeWidth={1} />
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Tìm kiếm người dùng</h3>
              <p className="text-gray-600">
                Nhập username để tìm kiếm người dùng trong hệ thống
              </p>
            </div>
          )}

          {/* Empty State - No Results */}
          {searchQuery && !users && !isLoading && !error && (
            <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
              <Search className="h-16 w-16 text-gray-300 mx-auto mb-4" strokeWidth={1} />
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Không tìm thấy kết quả</h3>
              <p className="text-gray-600">
                Không có người dùng nào với username "{searchQuery}". Thử tìm kiếm với từ khóa khác.
              </p>
            </div>
          )}
        </div>
      </div>
      <Footer />
    </>
  );
}
