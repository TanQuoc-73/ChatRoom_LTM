import React from 'react';
import Link from 'next/link';
import { Button } from '../ui/button';
import { AccountDD } from '../dropdown/AccountDD';

export default function Header() {
  return (
    <div className="flex w-full h-20 bg-white top-0 text-blue-600 justify-between items-center px-4 shadow-sm">
      {/* LOGO */}
      <Link href="/" className="flex items-center">
        <div className="ml-2 font-bold text-2xl text-blue-800">LOGO</div>
      </Link>

      {/* SEARCH */}
      <div className="flex bg-blue-50 h-10 w-1/4 rounded-lg items-center pl-4 px-2">
        <input
          type="text"
          placeholder="Search..."
          className="bg-blue-50 w-full outline-none text-blue-700"
        />
        <div className="bg-blue-200 flex w-5 h-5 rounded-lg justify-center items-center ml-2">
          <Button variant="ghost" size="icon-sm" className="cursor-pointer text-blue-600">
            {/* icon could be placed here */}
          </Button>
        </div>
      </div>

      {/* NAV */}
      <div className="flex gap-6">
        <Link href="/home" className="hover:text-green-600 transition-colors">
          Trang Chủ
        </Link>
        <Link href="/chats" className="hover:text-green-600 transition- colors">
          Tin Nhắn
        </Link>
        <Link href="/groups" className="hover:text-green-600 transition-colors">
          Nhóm
        </Link>
        <Link href="/settings" className="hover:text-green-600 transition-colors">
          Cài Đặt
        </Link>
      </div>

      {/* ICON ACC */}
      <div className="flex items-center justify-end pr-4 bg-blue-50 h-full">
        <AccountDD />
      </div>
    </div>
  );
}
