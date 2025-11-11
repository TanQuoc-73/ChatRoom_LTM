import React from 'react'
import Link from 'next/link'

export default function Header() {
  return (
    <div className='flex w-full h-20 bg-white top-0 text-gray-500 justify-between items-center'>
        {/* LOGO */}
        <Link href = '/' className='flex items-center'> 
            <div className='ml-8 font-bold text-lg'>LOGO</div>
        </Link>
        {/* NAV */}
        <div className="flex items-center justify-between mr-8 space-x-6">
            <Link href = '/' className='hover:text-black'> Home</Link>
            <Link href = '/' className='hover:text-black'> Home</Link>
            <Link href = '/' className='hover:text-black'> Home</Link>
            <Link href = '/' className='hover:text-black'> Home</Link>
        </div>
    </div>
  )
}
