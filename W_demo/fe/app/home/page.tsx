"use client";

import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";

export default function HomePage() {
  return (
    <>
      <Header />
      <div className="pt-16 min-h-screen flex items-center justify-center bg-gray-50 text-gray-900">
        <h1 className="text-3xl font-bold">Chat Room Page</h1>
      </div>
      <Footer />
    </>
  );
}
