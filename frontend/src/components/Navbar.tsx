import React from "react";
import logo from "../assets/logo.svg";

function Navbar() {
  return (
    <div className="flex w-full font-['Manrope'] justify-between px-4 py-4">
      <a href="/" className="group flex items-center gap-2 no-underline">
        <img src={logo} className="h-7" />
        <span className="text-3xl text-emerald-500 font-bold">Mint</span>
      </a>
    </div>
  );
}

export default Navbar;
