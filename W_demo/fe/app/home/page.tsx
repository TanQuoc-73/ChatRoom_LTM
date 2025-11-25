"use client";

import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

export default function HomePage() {
  return (
    <>
      <Header />
      <div className="min-h-screen flex items-center justify-center bg-gray-900 text-white">
        <h1 className="text-3xl font-bold">Chat Room Page</h1>
      </div>
      <Footer />
    </>
  );
}
