import React from 'react'
import Link from 'next/link'
import { Button } from '../ui/button'

import { AccountDD } from '../dropdown/AccountDD'

export default function Header() {
  return (
    <div className='flex w-full h-20 bg-white top-0 text-gray-500 justify-between items-center'>
        {/* LOGO */}
        <Link href = '/' className='flex items-center'> 
            <div className='ml-8 font-bold text-lg'>LOGO</div>
        </Link>

        {/* SEARCH */}
        <div className="flex bg-gray-100 h-10 w-1/4 rounded-lg items-center pl-4 px-2">
            <input type="text" placeholder='Search...' className='bg-gray-100 w-full outline-none'/>
            <div className="bg-gray-300 flex w-5 h-5 rounded-lg justify-center items-center">
                <Button variant='ghost' size='icon-sm' className='cursor-pointer'></Button>
            </div>
        </div>
        {/* NAV */}
        <div className="flex gap-10">
            <div className="flex items-center justify-between mr-8 space-x-6">
            <Link href = '/' className='hover:text-black'> Home</Link>
            <Link href = '/about' className='hover:text-black'> About Us</Link>
            <Link href = '/service' className='hover:text-black'> Service</Link>
            <Link href = '/blog' className='hover:text-black'> Blog</Link>
        </div>
        {/* ICON ACC */}
        <div className="flex items-center justify-end pr-10 bg-gray-100">
            <AccountDD />
        </div>
        </div>
    </div>
  )
}
