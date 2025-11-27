'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { AccountDD } from '../dropdown/AccountDD';
import { 
  Home, 
  MessageCircle, 
  Users, 
  Search,
  PlusSquare,
  Heart
} from 'lucide-react';

export default function Header() {
  const pathname = usePathname();
  const [searchQuery, setSearchQuery] = useState('');

  const isActive = (path: string) => pathname === path;

  const navItems = [
    { href: '/home', icon: Home, label: 'Trang chủ' },
    { href: '/messages', icon: MessageCircle, label: 'Tin nhắn' },
    { href: '/conversations', icon: Users, label: 'Nhóm chat' },
    { href: '/users/search', icon: Search, label: 'Tìm kiếm' },
  ];

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-white border-b border-gray-200">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        {/* LOGO - Instagram Style */}
        <Link href="/home" className="flex items-center">
          <div className="font-bold text-2xl bg-gradient-to-r from-purple-600 via-pink-600 to-orange-500 bg-clip-text text-transparent">
            Chát Room
          </div>
        </Link>

        {/* SEARCH BAR - Instagram Style */}
        <div className="hidden md:flex items-center flex-1 max-w-xs mx-8">
          <div className="relative w-full">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-4 w-4 text-gray-400" />
            </div>
            <input
              type="text"
              placeholder="Tìm kiếm..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-gray-50 border-0 rounded-lg text-sm placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-gray-200 transition-all"
            />
          </div>
        </div>

        {/* NAVIGATION ICONS - Instagram Style */}
        <nav className="flex items-center gap-5">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.href);
            
            return (
              <Link
                key={item.href}
                href={item.href}
                className="group relative"
                title={item.label}
              >
                <Icon
                  className={`h-6 w-6 transition-all ${
                    active 
                      ? 'text-black scale-110' 
                      : 'text-gray-700 hover:text-black hover:scale-110'
                  }`}
                  strokeWidth={active ? 2.5 : 2}
                />
                {active && (
                  <div className="absolute -bottom-[21px] left-1/2 -translate-x-1/2 w-full h-[2px] bg-black" />
                )}
              </Link>
            );
          })}

          {/* CREATE POST ICON */}
          <button
            className="group relative"
            title="Tạo bài viết"
          >
            <PlusSquare
              className="h-6 w-6 text-gray-700 hover:text-black hover:scale-110 transition-all"
              strokeWidth={2}
            />
          </button>

          {/* NOTIFICATIONS */}
          <button
            className="group relative"
            title="Thông báo"
          >
            <Heart
              className="h-6 w-6 text-gray-700 hover:text-black hover:scale-110 transition-all"
              strokeWidth={2}
            />
            {/* Notification badge */}
            <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full h-4 w-4 flex items-center justify-center font-semibold">
              3
            </span>
          </button>

          {/* ACCOUNT DROPDOWN */}
          <div className="ml-2">
            <AccountDD />
          </div>
        </nav>
      </div>
    </header>
  );
}
