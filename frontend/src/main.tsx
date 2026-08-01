import React from "react";
import{createRoot}from"react-dom/client";
import{App}from"./App";
import{YoutubeMagazineApp}from"./YoutubeMagazineApp";
import"./styles.css";
const magazine=window.location.pathname.startsWith("/youtube-magazine");
createRoot(document.getElementById("root")!).render(<React.StrictMode>{magazine?<YoutubeMagazineApp/>:<><App/><a href="/youtube-magazine" style={{position:"fixed",right:20,bottom:20,zIndex:50,padding:"12px 16px",borderRadius:12,background:"#ff4d2e",color:"white",fontWeight:800,textDecoration:"none",boxShadow:"0 8px 30px #0008"}}>YouTube Hot 6</a></>}</React.StrictMode>);
