"use client";

import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";
import { Heart, MessageCircle, Send, Bookmark, MoreHorizontal, Plus } from "lucide-react";

export default function HomePage() {
  // Mock data for stories
  const stories = [
    { id: 1, username: "your_story", avatar: "👤", hasStory: true, isYou: true },
    { id: 2, username: "user_1", avatar: "🧑", hasStory: true },
    { id: 3, username: "user_2", avatar: "👩", hasStory: true },
    { id: 4, username: "user_3", avatar: "👨", hasStory: true },
    { id: 5, username: "user_4", avatar: "👧", hasStory: true },
    { id: 6, username: "user_5", avatar: "👦", hasStory: true },
  ];

  // Mock data for posts
  const posts = [
    {
      id: 1,
      username: "travel_lover",
      avatar: "🌍",
      location: "Paris, France",
      image: null,
      caption: "Amazing sunset at the Eiffel Tower! 🗼✨ #paris #travel",
      likes: 1234,
      comments: 89,
      timeAgo: "2 giờ trước",
    },
    {
      id: 2,
      username: "food_enthusiast",
      avatar: "🍕",
      location: "Hanoi, Vietnam",
      image: null,
      caption: "Best pho in town! 🍜 Can't get enough of Vietnamese cuisine",
      likes: 856,
      comments: 45,
      timeAgo: "5 giờ trước",
    },
    {
      id: 3,
      username: "tech_geek",
      avatar: "💻",
      location: "Silicon Valley",
      image: null,
      caption: "Just finished building my new setup! What do you think? 🖥️⌨️",
      likes: 2341,
      comments: 156,
      timeAgo: "1 ngày trước",
    },
  ];

  // Mock suggestions
  const suggestions = [
    { id: 1, username: "suggested_user1", avatar: "👤", mutualFriends: 5 },
    { id: 2, username: "suggested_user2", avatar: "🧑", mutualFriends: 12 },
    { id: 3, username: "suggested_user3", avatar: "👩", mutualFriends: 8 },
  ];

  return (
    <>
      <Header />
      <div className="pt-16 min-h-screen bg-gray-50">
        <div className="max-w-6xl mx-auto px-4 py-8">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Main Feed - Left/Center */}
            <div className="lg:col-span-2 space-y-6">
              {/* Stories */}
              <div className="bg-white border border-gray-200 rounded-lg p-4 overflow-hidden">
                <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-hide">
                  {stories.map((story) => (
                    <div key={story.id} className="flex flex-col items-center gap-1 flex-shrink-0">
                      <div className={`relative ${story.hasStory ? 'p-[2px] bg-gradient-to-tr from-yellow-400 via-pink-500 to-purple-600 rounded-full' : ''}`}>
                        <div className="bg-white p-[2px] rounded-full">
                          <div className="w-16 h-16 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-2xl">
                            {story.avatar}
                          </div>
                        </div>
                        {story.isYou && (
                          <div className="absolute bottom-0 right-0 bg-blue-500 rounded-full p-1">
                            <Plus className="h-3 w-3 text-white" strokeWidth={3} />
                          </div>
                        )}
                      </div>
                      <span className="text-xs text-gray-700 max-w-[70px] truncate">
                        {story.isYou ? "Story của bạn" : story.username}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Posts Feed */}
              {posts.map((post) => (
                <div key={post.id} className="bg-white border border-gray-200 rounded-lg overflow-hidden">
                  {/* Post Header */}
                  <div className="flex items-center justify-between p-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-xl">
                        {post.avatar}
                      </div>
                      <div>
                        <p className="font-semibold text-sm text-gray-900">{post.username}</p>
                        {post.location && (
                          <p className="text-xs text-gray-600">{post.location}</p>
                        )}
                      </div>
                    </div>
                    <button className="p-2 hover:bg-gray-100 rounded-full transition-colors">
                      <MoreHorizontal className="h-5 w-5 text-gray-900" />
                    </button>
                  </div>

                  {/* Post Image Placeholder */}
                  <div className="w-full aspect-square bg-gradient-to-br from-purple-100 via-pink-100 to-orange-100 flex items-center justify-center">
                    <div className="text-6xl">{post.avatar}</div>
                  </div>

                  {/* Post Actions */}
                  <div className="p-4">
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center gap-4">
                        <button className="hover:text-gray-500 transition-colors">
                          <Heart className="h-6 w-6" />
                        </button>
                        <button className="hover:text-gray-500 transition-colors">
                          <MessageCircle className="h-6 w-6" />
                        </button>
                        <button className="hover:text-gray-500 transition-colors">
                          <Send className="h-6 w-6" />
                        </button>
                      </div>
                      <button className="hover:text-gray-500 transition-colors">
                        <Bookmark className="h-6 w-6" />
                      </button>
                    </div>

                    {/* Likes */}
                    <p className="font-semibold text-sm text-gray-900 mb-2">
                      {post.likes.toLocaleString()} lượt thích
                    </p>

                    {/* Caption */}
                    <p className="text-sm text-gray-900 mb-2">
                      <span className="font-semibold mr-2">{post.username}</span>
                      {post.caption}
                    </p>

                    {/* Comments */}
                    <button className="text-sm text-gray-600 mb-2 hover:text-gray-900">
                      Xem tất cả {post.comments} bình luận
                    </button>

                    {/* Time */}
                    <p className="text-xs text-gray-400 uppercase">{post.timeAgo}</p>
                  </div>

                  {/* Add Comment */}
                  <div className="border-t border-gray-200 p-4">
                    <div className="flex items-center gap-3">
                      <input
                        type="text"
                        placeholder="Thêm bình luận..."
                        className="flex-1 outline-none text-sm text-gray-900 placeholder-gray-500"
                      />
                      <button className="text-blue-500 font-semibold text-sm hover:text-blue-700">
                        Đăng
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Right Sidebar - Suggestions */}
            <div className="hidden lg:block">
              <div className="sticky top-20">
                {/* User Profile */}
                <div className="flex items-center gap-3 mb-6">
                  <div className="w-14 h-14 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-2xl">
                    👤
                  </div>
                  <div className="flex-1">
                    <p className="font-semibold text-sm text-gray-900">your_username</p>
                    <p className="text-sm text-gray-600">Tên hiển thị</p>
                  </div>
                  <button className="text-blue-500 text-xs font-semibold hover:text-blue-700">
                    Chuyển
                  </button>
                </div>

                {/* Suggestions */}
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <p className="text-sm font-semibold text-gray-700">
                      Gợi ý cho bạn
                    </p>
                    <button className="text-xs font-semibold text-gray-900 hover:text-gray-600">Xem tất cả</button>
                  </div>

                  <div className="space-y-3">
                    {suggestions.map((user) => (
                      <div key={user.id} className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-400 to-pink-400 flex items-center justify-center text-xl">
                          {user.avatar}
                        </div>
                        <div className="flex-1">
                          <p className="font-semibold text-sm text-gray-900">{user.username}</p>
                          <p className="text-xs text-gray-600">
                            {user.mutualFriends} bạn chung
                          </p>
                        </div>
                        <button className="text-blue-500 text-xs font-semibold hover:text-blue-700">
                          Theo dõi
                        </button>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Footer Links */}
                <div className="mt-8 text-xs text-gray-400 space-y-2">
                  <div className="flex flex-wrap gap-2">
                    <a href="#" className="hover:underline">Giới thiệu</a>
                    <span>·</span>
                    <a href="#" className="hover:underline">Trợ giúp</a>
                    <span>·</span>
                    <a href="#" className="hover:underline">Điều khoản</a>
                  </div>
                  <p>© 2025 CHATROOM</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <Footer />
    </>
  );
}
