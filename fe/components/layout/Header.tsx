import React from 'react'
import {} from 'lucide-react'
import Link from 'next/link'
import {Button} from '@/components/ui/button'

import { AccountDD } from '../dropdown/AccountDD'


export default function Header() {
  return (
    <div className = "flex items-center justify-between bg-black text-white h-20 w-full px-10">
      {/* LOGO */}
        <div className="">LOGO</div>
      {/* NAV + Account */}
        <div className="flex gap-6 items-center">
            <Link href="/">
                Home
            </Link>
            <Link href="/">
                Home
            </Link>
            <Link href="/">
                Home
            </Link>

            <AccountDD />
        </div>
    </div>
  )
}
