import { useEffect, useMemo, useRef, useState, type MouseEvent, type ReactNode } from "react";

/*
 * Orrery Comic Edition — the Orrery launcher laid out as a comic page.
 * Performance rules: no animation, no transitions, no shadows, no blur, no
 * translucent layers. Everything is solid fills and thin static SVG line art.
 * Navigation is spatial (nearest focusable element in the pressed direction).
 * Accents: classic suit red/blue over black-suit charcoal; web linework in corners only.
 */

const C = {
  bg: "#111216",
  surface: "#1A1C22",
  suit: "#0B0C0F",
  navy: "#16203A",
  web: "#2E3442",
  webOnNavy: "#2C3A5C",
  text: "#F2F3F5",
  silver: "#B8BDC8",
  red: "#E0353F",
  blue: "#4A7BE0",
  rule: "#262932",
  cream: "#F1E7CF",
  // newsprint caption boxes
  ink: "#08090B",
  hood: "#7E1820",
  hoodShade: "#4E0C12"
};

/* ---------- Web linework (static, generated once) ---------- */
function webPaths(cx: number, cy: number, startDeg: number, endDeg: number, spokes: number, radii: number[]) {
  const angles = Array.from({
    length: spokes
  }, (_, i) => (startDeg + (endDeg - startDeg) * i / (spokes - 1)) * Math.PI / 180);
  const pt = (a: number, r: number) => [cx + Math.cos(a) * r, cy + Math.sin(a) * r];
  const d: string[] = [];
  const maxR = radii[radii.length - 1];
  for (const a of angles) {
    const [x, y] = pt(a, maxR * 1.08);
    d.push(`M${cx} ${cy}L${x.toFixed(1)} ${y.toFixed(1)}`);
  }
  for (const r of radii) {
    for (let i = 0; i < angles.length - 1; i++) {
      const [x1, y1] = pt(angles[i], r);
      const [x2, y2] = pt(angles[i + 1], r);
      const [mx, my] = pt((angles[i] + angles[i + 1]) / 2, r * 0.86); // threads sag toward the hub
      d.push(`M${x1.toFixed(1)} ${y1.toFixed(1)}Q${mx.toFixed(1)} ${my.toFixed(1)} ${x2.toFixed(1)} ${y2.toFixed(1)}`);
    }
  }
  return d.join("");
}
const CORNER_WEB = webPaths(100, 0, 90, 180, 5, [26, 50, 74, 98]);
const FULL_WEB = webPaths(50, 50, 0, 360, 9, [14, 28, 42]);
const WebCorner = ({
  color,
  className = ""
}: {
  color: string;
  className?: string;
}) => <svg viewBox="0 0 100 100" fill="none" className={className} aria-hidden>
    <path d={CORNER_WEB} stroke={color} strokeWidth="1.1" vectorEffect="non-scaling-stroke" />
  </svg>;
const WebGlyph = ({
  className = ""
}: {
  className?: string;
}) => <svg viewBox="0 0 100 100" fill="none" className={className} aria-hidden>
    <circle cx="50" cy="50" r="49" fill={C.suit} />
    <path d={FULL_WEB} stroke={C.silver} strokeWidth="2.2" />
    <circle cx="50" cy="50" r="5" fill={C.red} />
  </svg>;

/* ---------- Comic print elements (static) ---------- */
// Halftone dots that fade out toward the opposite corner. Rendered once; no animation.
const Halftone = ({
  id,
  color,
  className = "",
  toward = "bottom-right",
  gap = 9,
  r = 2
}: {
  id: string;
  color: string;
  className?: string;
  toward?: "bottom-right" | "bottom-left";
  gap?: number;
  r?: number;
}) => <svg className={className} aria-hidden>
    <defs>
      <pattern id={`${id}-p`} width={gap} height={gap} patternUnits="userSpaceOnUse">
        <circle cx={gap / 2} cy={gap / 2} r={r} fill={color} />
      </pattern>
      <linearGradient id={`${id}-g`} x1={toward === "bottom-right" ? 1 : 0} y1="1" x2={toward === "bottom-right" ? 0.15 : 0.85} y2="0.1">
        <stop offset="0" stopColor="#fff" />
        <stop offset="1" stopColor="#000" />
      </linearGradient>
      <mask id={`${id}-m`}>
        <rect width="100%" height="100%" fill={`url(#${id}-g)`} />
      </mask>
    </defs>
    <rect width="100%" height="100%" fill={`url(#${id}-p)`} mask={`url(#${id}-m)`} />
  </svg>;
const Caption = ({
  children,
  className = ""
}: {
  children: ReactNode;
  className?: string;
}) => <span className={`inline-block -skew-x-6 border-2 px-2 py-[2px] font-extrabold uppercase tracking-[0.08em] ${className}`} style={{
  background: C.cream,
  color: C.ink,
  borderColor: C.ink
}}>
    <span className="inline-block skew-x-6">{children}</span>
  </span>;
const BURST = Array.from({
  length: 24
}, (_, i) => {
  const a = i / 24 * Math.PI * 2;
  const r = i % 2 ? 37 : 50;
  return `${(50 + Math.cos(a) * r).toFixed(1)},${(50 + Math.sin(a) * r).toFixed(1)}`;
}).join(" ");
const Burst = ({
  children,
  className = ""
}: {
  children: ReactNode;
  className?: string;
}) => <span className={`inline-grid aspect-square flex-none place-items-center ${className}`}>
    <svg viewBox="0 0 100 100" className="absolute inset-0 h-full w-full" aria-hidden>
      <polygon points={BURST} fill={C.red} stroke={C.cream} strokeWidth="3" strokeLinejoin="round" />
    </svg>
    <span className="relative -rotate-6 px-[16%] text-center leading-[0.9]" style={{
    fontFamily: "Bangers, Impact, sans-serif",
    letterSpacing: "0.04em",
    color: C.cream
  }}>
      {children}
    </span>
  </span>;

/* ---------- Apps & icon marks ---------- */
type IconStyle = "custom" | "original";
type AppDef = {
  id: string;
  name: string;
  tag: string;
  bg: string; // original tile background
  label: string; // label colour on the original background
  ink: string; // mark colour on the original background
  customInk: string; // mark colour on the unified dark tile
  accent: string; // small brand dot / web tint
  mark: (ink: string) => ReactNode; // drawn in a 160×90 box
};
const word = (t: string, ink: string, size = 44, opts: {
  y?: number;
  x?: number;
  weight?: number;
  serif?: boolean;
  italic?: boolean;
} = {}) => <text x={opts.x ?? 80} y={opts.y ?? 45 + size * 0.36} textAnchor={opts.x ? "start" : "middle"} fontFamily={opts.serif ? "Marcellus, Georgia, serif" : "Manrope, Arial, sans-serif"} fontWeight={opts.weight ?? 800} fontStyle={opts.italic ? "italic" : undefined} fontSize={size} letterSpacing={opts.serif ? 0 : -1.2} fill={ink}>
  
    {t}
  </text>;
const APPS: AppDef[] = [{
  id: "netflix",
  name: "Netflix",
  tag: "Series & films",
  bg: "#000000",
  label: "#F2F3F5",
  ink: "#E50914",
  customInk: "#E50914",
  accent: "#E50914",
  mark: ink => <path d="M64 16h13l19 38V16h13v58H96L77 36v38H64Z" fill={ink} />
}, {
  id: "youtube",
  name: "YouTube",
  tag: "Subscriptions",
  bg: "#FFFFFF",
  label: "#0F0F0F",
  ink: "#FF0000",
  customInk: "#FF0000",
  accent: "#FF0000",
  mark: ink => <><rect x="42" y="20" width="76" height="52" rx="14" fill={ink} /><path d="M72 33v26l22-13Z" fill="#fff" /></>
}, {
  id: "prime",
  name: "Prime Video",
  tag: "Movies & sport",
  bg: "#0F171E",
  label: "#F2F3F5",
  ink: "#1A98FF",
  customInk: "#1A98FF",
  accent: "#1A98FF",
  mark: ink => <>{word("prime", ink, 34, {
      y: 44
    })}<path d="M50 52q30 13 60 0" stroke={ink} strokeWidth="4" strokeLinecap="round" fill="none" /><path d="M104 47l7 5-8 3" stroke={ink} strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round" fill="none" />{word("video", ink, 20, {
      y: 76,
      weight: 700
    })}</>
}, {
  id: "disney",
  name: "Disney+",
  tag: "Family",
  bg: "#0B1A55",
  label: "#F2F3F5",
  ink: "#FFFFFF",
  customInk: "#E6ECFA",
  accent: "#5C8DFF",
  mark: ink => <><path d="M30 62q50-46 100-10" stroke={ink} strokeWidth="2" fill="none" />{word("Disney+", ink, 36, {
      y: 60,
      serif: true,
      italic: true
    })}</>
}, {
  id: "max",
  name: "Max",
  tag: "HBO & more",
  bg: "#002BE7",
  label: "#FFFFFF",
  ink: "#FFFFFF",
  customInk: "#6C8BFF",
  accent: "#3D63FF",
  mark: ink => word("max", ink, 50)
}, {
  id: "crunchyroll",
  name: "Crunchyroll",
  tag: "Anime",
  bg: "#F47521",
  label: "#1A0E05",
  ink: "#FFFFFF",
  customInk: "#F47521",
  accent: "#F47521",
  mark: ink => <><circle cx="80" cy="45" r="27" fill="none" stroke={ink} strokeWidth="8" /><circle cx="90" cy="38" r="12" fill={ink} /></>
}, {
  id: "spotify",
  name: "Spotify",
  tag: "Music",
  bg: "#121212",
  label: "#F2F3F5",
  ink: "#1DB954",
  customInk: "#1DB954",
  accent: "#1DB954",
  mark: ink => <><circle cx="80" cy="45" r="30" fill={ink} /><g stroke="#121212" strokeLinecap="round" fill="none"><path d="M62 36q19-7 37 3" strokeWidth="5.5" /><path d="M64 46q15-5 31 2" strokeWidth="4.5" /><path d="M66 55q12-4 25 2" strokeWidth="3.8" /></g></>
}, {
  id: "ytmusic",
  name: "YouTube Music",
  tag: "Music",
  bg: "#0F0F0F",
  label: "#F2F3F5",
  ink: "#FF0000",
  customInk: "#FF0000",
  accent: "#FF0000",
  mark: ink => <><circle cx="80" cy="45" r="30" fill={ink} /><circle cx="80" cy="45" r="16" fill="none" stroke="#fff" strokeWidth="3" /><path d="M75 37v16l13-8Z" fill="#fff" /></>
}, {
  id: "hulu",
  name: "Hulu",
  tag: "Next-day TV",
  bg: "#0B0C0F",
  label: "#F2F3F5",
  ink: "#1CE783",
  customInk: "#1CE783",
  accent: "#1CE783",
  mark: ink => word("hulu", ink, 50)
}, {
  id: "appletv",
  name: "Apple TV",
  tag: "Originals",
  bg: "#000000",
  label: "#F2F3F5",
  ink: "#FFFFFF",
  customInk: "#E8E9ED",
  accent: "#B8BDC8",
  mark: ink => <>{word("apple", ink, 22, {
      y: 38,
      weight: 600
    })}{word("tv", ink, 44, {
      y: 74
    })}</>
}, {
  id: "peacock",
  name: "Peacock",
  tag: "Live & on demand",
  bg: "#000000",
  label: "#F2F3F5",
  ink: "#FFFFFF",
  customInk: "#FFFFFF",
  accent: "#6E55DC",
  mark: ink => <>{["#FFC700", "#FF7112", "#EF1541", "#6E55DC", "#069DE0", "#05AC3F"].map((c, i) => <circle key={c} cx={55 + i * 10} cy={[38, 30, 26, 26, 30, 38][i]} r="4.6" fill={c} />)}{word("peacock", ink, 26, {
      y: 70
    })}</>
}, {
  id: "googletv",
  name: "Google TV",
  tag: "Live channels",
  bg: "#202124",
  label: "#F2F3F5",
  ink: "#E8EAED",
  customInk: "#E8EAED",
  accent: "#4285F4",
  mark: ink => <text x="80" y="57" textAnchor="middle" fontFamily="Manrope, Arial, sans-serif" fontWeight={700} fontSize={30} letterSpacing={-0.8}>
        {[["G", "#4285F4"], ["o", "#EA4335"], ["o", "#FBBC05"], ["g", "#4285F4"], ["l", "#34A853"], ["e", "#EA4335"]].map(([l, c], i) => <tspan key={i} fill={c}>{l}</tspan>)}
        <tspan fill={ink}> TV</tspan>
      </text>
}, {
  id: "tubi",
  name: "Tubi",
  tag: "Free movies",
  bg: "#7408FF",
  label: "#FFFFFF",
  ink: "#FFFF13",
  customInk: "#FFFF13",
  accent: "#7408FF",
  mark: ink => word("tubi", ink, 52)
}, {
  id: "pluto",
  name: "Pluto TV",
  tag: "Free channels",
  bg: "#000000",
  label: "#F2F3F5",
  ink: "#FFFFFF",
  customInk: "#FFFFFF",
  accent: "#FFF200",
  mark: ink => <><circle cx="42" cy="45" r="15" fill="#FFF200" /><path d="M22 50q20-14 40-10" stroke="#000" strokeWidth="3" fill="none" />{word("pluto", ink, 34, {
      x: 64,
      y: 48
    })}{word("tv", ink, 20, {
      x: 66,
      y: 70,
      weight: 700
    })}</>
}, {
  id: "roku",
  name: "The Roku Channel",
  tag: "Free TV",
  bg: "#6C3C97",
  label: "#FFFFFF",
  ink: "#FFFFFF",
  customInk: "#B48BE6",
  accent: "#6C3C97",
  mark: ink => <>{word("The Roku", ink, 28, {
      y: 42
    })}{word("Channel", ink, 28, {
      y: 72,
      weight: 600
    })}</>
}, {
  id: "instagram",
  name: "Instagram",
  tag: "Reels",
  bg: "linear-gradient(45deg,#F58529 0%,#DD2A7B 50%,#8134AF 100%)",
  label: "#FFFFFF",
  ink: "#FFFFFF",
  customInk: "#F06292",
  accent: "#DD2A7B",
  mark: ink => <><rect x="56" y="21" width="48" height="48" rx="14" fill="none" stroke={ink} strokeWidth="6" /><circle cx="80" cy="45" r="11" fill="none" stroke={ink} strokeWidth="6" /><circle cx="95" cy="30" r="3.6" fill={ink} /></>
}, {
  id: "applemusic",
  name: "Apple Music",
  tag: "Music",
  bg: "#FA243C",
  label: "#FFFFFF",
  ink: "#FFFFFF",
  customInk: "#FF5A6B",
  accent: "#FA243C",
  mark: ink => <path d="M95 18v38a10 10 0 1 1-6-9.2V31l-24 6v28a10 10 0 1 1-6-9.2V27Z" fill={ink} />
}, {
  id: "aircast",
  name: "AirCast",
  tag: "Cast from phone",
  bg: "#1565C0",
  label: "#FFFFFF",
  ink: "#FFFFFF",
  customInk: "#5AA6F5",
  accent: "#1E88E5",
  mark: ink => <><path d="M52 32v-6a4 4 0 0 1 4-4h50a4 4 0 0 1 4 4v40a4 4 0 0 1-4 4H84" stroke={ink} strokeWidth="5" fill="none" strokeLinecap="round" /><path d="M52 46a22 22 0 0 1 22 22M52 56a12 12 0 0 1 12 12" stroke={ink} strokeWidth="5" fill="none" strokeLinecap="round" /><circle cx="54" cy="67" r="4" fill={ink} /></>
}];
const appById = (id: string) => APPS.find(a => a.id === id)!;
const DEFAULT_ORDER = APPS.map(a => a.id);
const DEFAULT_FAVS = ["netflix", "youtube", "prime", "disney"];

/* One reusable icon renderer: same geometry in both styles, only colours change. */
const AppIcon = ({
  app,
  style,
  markClass = "h-[46%]",
  web = true
}: {
  app: AppDef;
  style: IconStyle;
  markClass?: string;
  web?: boolean;
}) => <>
    {style === "custom" && web && <WebCorner color="#252932" className="pointer-events-none absolute right-0 top-0 h-[30%] max-h-[84px] w-auto" />}
    <span className="flex min-h-0 flex-1 items-center justify-center">
      <svg viewBox="0 0 160 90" className={`${markClass} w-auto max-w-full`} aria-hidden>
        {app.mark(style === "custom" ? app.customInk : app.ink)}
      </svg>
    </span>
  </>;
type Profile = {
  id: string;
  name: string;
  color: string;
  feature: {
    kicker: string;
    title: string;
    meta: string;
    app: string;
    progress?: number;
  };
  favs: Fav[];
};
type Fav = {
  id: string;
  app: string;
  title: string;
  meta: string;
};
const PROFILES: Profile[] = [{
  id: "mahin",
  name: "Mahin",
  color: C.red,
  feature: {
    kicker: "Resume tonight",
    title: "The Quiet Meridian",
    meta: "Netflix · 47 min left",
    app: "netflix",
    progress: 0.62
  },
  favs: [{
    id: "f1",
    app: "spotify",
    title: "Late Night Jazz",
    meta: "Playlist · 64 songs"
  }, {
    id: "f2",
    app: "crunchyroll",
    title: "Simulcast queue",
    meta: "3 new"
  }, {
    id: "f3",
    app: "youtube",
    title: "Speedrun Weekly",
    meta: "Live now"
  }, {
    id: "f4",
    app: "ytmusic",
    title: "Liked songs",
    meta: "412 songs"
  }]
}, {
  id: "aria",
  name: "Aria",
  color: C.blue,
  feature: {
    kicker: "Next episode",
    title: "Low Tide Club",
    meta: "Prime Video · S2 · E5",
    app: "prime",
    progress: 0.05
  },
  favs: [{
    id: "f1",
    app: "hulu",
    title: "Ninth Harbor",
    meta: "S1 · E3"
  }, {
    id: "f2",
    app: "max",
    title: "Velvet Circuit",
    meta: "S1 · E2"
  }, {
    id: "f3",
    app: "instagram",
    title: "Saved reels",
    meta: "18 new"
  }, {
    id: "f4",
    app: "applemusic",
    title: "Focus Flow",
    meta: "Playlist · 40 songs"
  }]
}, {
  id: "kids",
  name: "Kids",
  color: "#E9C46A",
  feature: {
    kicker: "Favourite film",
    title: "Paper Moons",
    meta: "Disney+ · 1 h 27 min",
    app: "disney"
  },
  favs: [{
    id: "f1",
    app: "disney",
    title: "Bedtime stories",
    meta: "Collection"
  }, {
    id: "f2",
    app: "pluto",
    title: "Cartoon channel",
    meta: "Live now"
  }, {
    id: "f3",
    app: "tubi",
    title: "Animal films",
    meta: "12 titles"
  }, {
    id: "f4",
    app: "spotify",
    title: "Car singalong",
    meta: "Playlist"
  }]
}, {
  id: "guest",
  name: "Guest",
  color: C.silver,
  feature: {
    kicker: "Welcome",
    title: "Pick anything to watch",
    meta: "Free apps need no sign-in",
    app: "tubi"
  },
  favs: [{
    id: "f1",
    app: "youtube",
    title: "Trending",
    meta: "YouTube"
  }, {
    id: "f2",
    app: "aircast",
    title: "Cast from phone",
    meta: "Same Wi-Fi"
  }, {
    id: "f3",
    app: "roku",
    title: "Free live TV",
    meta: "Live now"
  }, {
    id: "f4",
    app: "peacock",
    title: "Movie night",
    meta: "Collection"
  }]
}];

/* Comic page layout: a tall cover panel, then panel groups. Explicit placement keeps remote navigation predictable. */
const PLACE = {
  feature: "col-span-2 min-h-[360px] order-1 lg:order-none lg:min-h-0 lg:col-start-1 lg:col-end-5 lg:row-start-1 lg:row-end-7",
  fav0: "col-span-2 order-3 lg:order-none lg:col-start-5 lg:col-end-7 lg:row-start-4 lg:row-end-6",
  fav1: "col-span-2 order-3 lg:order-none lg:col-start-7 lg:col-end-10 lg:row-start-4 lg:row-end-5",
  fav2: "col-span-2 order-3 lg:order-none lg:col-start-7 lg:col-end-10 lg:row-start-5 lg:row-end-6",
  fav3: "col-span-2 order-3 lg:order-none lg:col-start-5 lg:col-end-10 lg:row-start-6 lg:row-end-7",
  network: "col-span-2 order-4 lg:order-none lg:col-start-10 lg:col-end-13 lg:row-start-1 lg:row-end-4",
  settings: "order-4 lg:order-none lg:col-start-10 lg:col-end-12 lg:row-start-4 lg:row-end-6",
  inputs: "order-4 lg:order-none lg:col-start-12 lg:col-end-13 lg:row-start-4 lg:row-end-6",
  allapps: "col-span-2 order-4 lg:order-none lg:col-start-10 lg:col-end-13 lg:row-start-6 lg:row-end-7"
};
// The six Watch panels, filled by the user's app order (favourites first).
const WATCH_SLOTS = ["order-2 lg:order-none lg:col-start-5 lg:col-end-7 lg:row-start-1 lg:row-end-3", "col-span-2 order-2 lg:order-none lg:col-start-7 lg:col-end-10 lg:row-start-1 lg:row-end-2", "order-2 lg:order-none lg:col-start-7 lg:col-end-8 lg:row-start-2 lg:row-end-4", "order-2 lg:order-none lg:col-start-8 lg:col-end-10 lg:row-start-2 lg:row-end-3", "order-2 lg:order-none lg:col-start-8 lg:col-end-10 lg:row-start-3 lg:row-end-4", "order-2 lg:order-none lg:col-start-5 lg:col-end-7 lg:row-start-3 lg:row-end-4"];
const SHORT_SLOTS = [1, 3, 4, 5];
const FAV_PLACES = [PLACE.fav0, PLACE.fav1, PLACE.fav2, PLACE.fav3];

// Comic-yellow focus ring: the one thing on screen that is never red, blue or cream.
const FOCUS = "outline-none focus:outline-solid focus:outline-[5px] focus:outline-offset-[3px] focus:outline-[#FFD23F]";
const TILE = `relative min-h-[120px] overflow-hidden rounded-[4px] border-[3px] border-[#F1E7CF] text-left lg:min-h-0 ${FOCUS}`;
const COMIC = {
  fontFamily: "Bangers, Impact, sans-serif",
  letterSpacing: "0.04em",
  fontWeight: 400
};
const Glyph = {
  gear: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className="h-full w-full" aria-hidden>
      <circle cx="12" cy="12" r="3.2" />
      <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z" />
    </svg>,
  input: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" className="h-full w-full" aria-hidden>
      <rect x="3" y="5" width="18" height="12" rx="1.5" />
      <path d="M8 21h8M12 17v4" />
    </svg>,
  grid: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" className="h-full w-full" aria-hidden>
      <rect x="3.5" y="3.5" width="7" height="7" rx="1" />
      <rect x="13.5" y="3.5" width="7" height="7" rx="1" />
      <rect x="3.5" y="13.5" width="7" height="7" rx="1" />
      <rect x="13.5" y="13.5" width="7" height="7" rx="1" />
    </svg>,
  shield: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" className="h-full w-full" aria-hidden>
      <path d="M12 3 4.5 6v5.5c0 4.6 3.2 8.3 7.5 9.5 4.3-1.2 7.5-4.9 7.5-9.5V6Z" />
      <path d="m8.8 12.2 2.3 2.3 4.2-4.6" strokeLinecap="round" />
    </svg>,
  star: (filled: boolean) => <svg viewBox="0 0 24 24" fill={filled ? "currentColor" : "none"} stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" className="h-full w-full" aria-hidden>
      <path d="m12 3.5 2.6 5.4 5.9.8-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.8Z" />
    </svg>
};

/* ---------- Mock network data (replace with NextDNS analytics API) ---------- */
const NET = {
  status: "Protected",
  profile: "Living room",
  queries: 18402,
  blocked: 2318,
  latencyMs: 14,
  devices: 6,
  hourly: [32, 41, 28, 22, 18, 26, 44, 61, 72, 66, 58, 80],
  topBlocked: [{
    domain: "telemetry.tv-vendor.example",
    hits: 812
  }, {
    domain: "ads.streaming-cdn.example",
    hits: 534
  }, {
    domain: "tracker.analytics.example",
    hits: 297
  }, {
    domain: "metrics.smart-home.example",
    hits: 188
  }]
};
const nf = new Intl.NumberFormat();

/* ---------- Spatial navigation ---------- */
type Dir = "left" | "right" | "up" | "down";
function nextInDirection(scope: HTMLElement, current: HTMLElement, dir: Dir): HTMLElement | null {
  const items = Array.from(scope.querySelectorAll<HTMLElement>("[data-nav]"));
  const r = current.getBoundingClientRect();
  let best: HTMLElement | null = null;
  let bestScore = Infinity;
  for (const el of items) {
    if (el === current || (el as HTMLButtonElement).disabled) continue;
    const b = el.getBoundingClientRect();
    if (b.width === 0 && b.height === 0) continue;
    let primary: number;
    let secondary: number;
    if (dir === "right" || dir === "left") {
      primary = dir === "right" ? b.left - r.right : r.left - b.right;
      secondary = b.bottom < r.top ? r.top - b.bottom : b.top > r.bottom ? b.top - r.bottom : 0;
      secondary += Math.abs(b.top + b.height / 2 - (r.top + r.height / 2)) * 0.1;
    } else {
      primary = dir === "down" ? b.top - r.bottom : r.top - b.bottom;
      secondary = b.right < r.left ? r.left - b.right : b.left > r.right ? b.left - r.right : 0;
      secondary += Math.abs(b.left - r.left) * 0.1;
    }
    if (primary < -2) continue;
    const score = primary + secondary * 3;
    if (score < bestScore) {
      bestScore = score;
      best = el;
    }
  }
  return best;
}

/* ---------- Main ---------- */
type Dialog = null | "network" | "settings" | "inputs" | "apps";
export const OrreryComicEdition = () => {
  const [profileId, setProfileId] = useState("mahin");
  const [dialog, setDialog] = useState<Dialog>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [selectedLabel, setSelectedLabel] = useState("");
  const [now, setNow] = useState(() => new Date());
  const [settings, setSettings] = useState({
    showNetwork: true,
    sleepAfterIdle: true,
    kidsPin: false,
    largeText: false
  });
  const [input, setInput] = useState("home");
  const [iconStyle, setIconStyle] = useState<IconStyle>("custom");
  const [order, setOrder] = useState<string[]>(DEFAULT_ORDER);
  const [favApps, setFavApps] = useState<string[]>(DEFAULT_FAVS);
  const [editApps, setEditApps] = useState(false);
  const [sheetApp, setSheetApp] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const dialogRef = useRef<HTMLDivElement>(null);
  const sheetRef = useRef<HTMLDivElement>(null);
  const returnFocus = useRef<HTMLElement | null>(null);
  const profile = PROFILES.find(p => p.id === profileId)!;
  const featureApp = appById(profile.feature.app);

  // Favourites always lead; within each group the user's order applies.
  const sortedApps = useMemo(() => {
    const favs = order.filter(id => favApps.includes(id));
    const rest = order.filter(id => !favApps.includes(id));
    return [...favs, ...rest].map(appById);
  }, [order, favApps]);
  useEffect(() => {
    const id = "orrery-comic-fonts";
    if (document.getElementById(id)) return;
    const link = document.createElement("link");
    link.id = id;
    link.rel = "stylesheet";
    link.href = "https://fonts.googleapis.com/css2?family=Bangers&family=Manrope:wght@500;600;700;800&display=swap";
    document.head.appendChild(link);
  }, []);

  // Minimal background work: one clock tick every 30 s.
  useEffect(() => {
    const t = window.setInterval(() => setNow(new Date()), 30000);
    return () => window.clearInterval(t);
  }, []);
  useEffect(() => {
    if (!status) return;
    const t = window.setTimeout(() => setStatus(null), 3200);
    return () => window.clearTimeout(t);
  }, [status]);
  useEffect(() => {
    rootRef.current?.querySelector<HTMLElement>("[data-nav-default]")?.focus();
  }, []);
  useEffect(() => {
    if (dialog) {
      dialogRef.current?.querySelector<HTMLElement>("[data-nav]")?.focus();
    } else if (returnFocus.current) {
      returnFocus.current.focus();
      returnFocus.current = null;
    }
  }, [dialog]);
  useEffect(() => {
    if (sheetApp) sheetRef.current?.querySelector<HTMLElement>("[data-nav]")?.focus();
  }, [sheetApp]);
  const openDialog = (d: Exclude<Dialog, null>) => {
    returnFocus.current = document.activeElement as HTMLElement;
    setDialog(d);
  };
  const closeSheet = () => {
    const id = sheetApp;
    setSheetApp(null);
    if (id) window.setTimeout(() => dialogRef.current?.querySelector<HTMLElement>(`[data-app="${id}"]`)?.focus(), 0);
  };
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const map: Record<string, Dir> = {
        ArrowLeft: "left",
        ArrowRight: "right",
        ArrowUp: "up",
        ArrowDown: "down"
      };
      if (map[e.key]) {
        const scope = sheetApp ? sheetRef.current : dialog ? dialogRef.current : rootRef.current;
        if (!scope) return;
        e.preventDefault();
        const cur = document.activeElement as HTMLElement | null;
        if (!cur || !scope.contains(cur) || !cur.hasAttribute("data-nav")) {
          scope.querySelector<HTMLElement>("[data-nav]")?.focus();
          return;
        }
        nextInDirection(scope, cur, map[e.key])?.focus();
      } else if (e.key === "Escape" || e.key === "Backspace") {
        if (sheetApp) {
          e.preventDefault();
          closeSheet();
        } else if (dialog) {
          e.preventDefault();
          setDialog(null);
          setEditApps(false);
        }
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  // Shared props for anything the remote can land on.
  const nav = (label: string, onSelect: () => void) => ({
    "data-nav": "",
    onClick: onSelect,
    onFocus: () => setSelectedLabel(label),
    onMouseEnter: (e: MouseEvent<HTMLElement>) => e.currentTarget.focus()
  });
  const launch = (a: AppDef, what?: string) => setStatus(`Opening ${what ? `${what} in ` : ""}${a.name}…`);
  const toggleFav = (id: string) => {
    const a = appById(id);
    const on = favApps.includes(id);
    setFavApps(f => on ? f.filter(x => x !== id) : [...f, id]);
    setStatus(on ? `${a.name} removed from favorites` : `${a.name} added to favorites`);
  };

  // Moves within the app's own group (favorites or the rest), then stores the full order.
  const moveApp = (id: string, how: "earlier" | "later" | "front") => {
    const ids = sortedApps.map(a => a.id);
    const isFav = favApps.includes(id);
    const group = ids.filter(x => favApps.includes(x) === isFav);
    const i = group.indexOf(id);
    const j = how === "front" ? 0 : how === "earlier" ? i - 1 : i + 1;
    if (j < 0 || j >= group.length || i === j) return;
    group.splice(i, 1);
    group.splice(j, 0, id);
    const other = ids.filter(x => favApps.includes(x) !== isFav);
    setOrder(isFav ? [...group, ...other] : [...other, ...group]);
  };
  const time = now.toLocaleTimeString([], {
    hour: "numeric",
    minute: "2-digit"
  });
  const date = now.toLocaleDateString([], {
    weekday: "long",
    month: "long",
    day: "numeric"
  });
  const hour = now.getHours();
  const greeting = hour < 5 ? "Good night" : hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
  const blockedPct = (NET.blocked / NET.queries * 100).toFixed(1);
  const maxBar = Math.max(...NET.hourly);

  // Pick just enough wide tiles (spread evenly) for the 8-column board to end on a full row.
  const wideSet = useMemo(() => {
    const rest = APPS.length - favApps.length;
    const w = Math.min(rest, (8 - (favApps.length * 4 + rest) % 8) % 8);
    return new Set(Array.from({
      length: w
    }, (_, j) => Math.floor((j + 0.5) * rest / w)));
  }, [favApps]);
  const tileBg = (a: AppDef) => iconStyle === "custom" ? C.suit : a.bg;
  const tileLabel = (a: AppDef) => iconStyle === "custom" ? C.text : a.label;
  const sheet = sheetApp ? appById(sheetApp) : null;
  const sheetGroup = sheet ? sortedApps.filter(a => favApps.includes(a.id) === favApps.includes(sheet.id)) : [];
  const sheetIndex = sheet ? sheetGroup.findIndex(a => a.id === sheet.id) : -1;
  return <div ref={rootRef} className="relative flex min-h-dvh w-full flex-col px-[3vw] pb-[2vh] pt-[3vh] lg:h-dvh lg:min-h-[600px] lg:overflow-hidden" style={{
    background: C.bg,
    color: C.text,
    fontFamily: "Manrope, ui-sans-serif, system-ui, sans-serif",
    fontSize: settings.largeText ? "112%" : undefined
  }}>
      
      {/* Header */}
      <Halftone id="ht-page" color="#1D2028" className="pointer-events-none absolute right-0 top-0 h-[34%] w-[50%]" gap={11} r={2.4} />
      <header className="relative flex flex-none flex-wrap items-center gap-x-[2.4vw] gap-y-4 pb-[2.4vh]">
        <div className="flex items-center gap-3">
          <WebGlyph className="h-[clamp(42px,3vw,70px)] w-[clamp(42px,3vw,70px)]" />
          <span className="flex flex-col items-start">
            <span className="text-[clamp(2.2rem,2.9vw,4.6rem)] leading-[0.85]" style={{
            ...COMIC,
            color: C.red
          }}>
              Orrery
            </span>
            <span className="mt-1 -skew-x-6 px-2 py-[1px] text-[clamp(0.6rem,0.62vw,0.95rem)] font-extrabold uppercase tracking-[0.14em]" style={{
            background: C.blue,
            color: C.text
          }}>
              Living room · Vol. 1
            </span>
          </span>
        </div>

        <div role="radiogroup" aria-label="Who's watching" className="flex flex-wrap items-center gap-2">
          {PROFILES.map(p => {
          const active = p.id === profileId;
          return <button key={p.id} role="radio" aria-checked={active} {...nav(`Switch to ${p.name}'s profile`, () => {
            setProfileId(p.id);
            setStatus(`Switched to ${p.name}`);
          })} className={`flex items-center gap-2.5 rounded-full py-[clamp(5px,0.4vw,10px)] pl-[clamp(5px,0.4vw,10px)] pr-[clamp(14px,1vw,24px)] text-[clamp(0.95rem,1vw,1.6rem)] font-semibold ${FOCUS}`} style={{
            background: active ? C.text : C.surface,
            color: active ? C.bg : C.silver
          }}>
                
                <span className="grid h-[clamp(28px,1.9vw,46px)] w-[clamp(28px,1.9vw,46px)] place-items-center rounded-full text-[0.85em] font-bold" style={{
              background: p.color,
              color: C.suit
            }}>
                  {p.name[0]}
                </span>
                {p.name}
              </button>;
        })}
        </div>

        <div className="ml-auto text-right">
          <div className="text-[clamp(2rem,2.5vw,4rem)] leading-none tabular-nums" style={COMIC}>{time}</div>
          <div className="mt-1.5 text-[clamp(0.85rem,0.85vw,1.3rem)]" style={{
          color: C.silver
        }}>
            {greeting}, {profile.name} · {date}
          </div>
        </div>
      </header>

      {/* Board */}
      <main className="relative grid min-h-0 flex-1 grid-cols-2 gap-[clamp(10px,0.8vw,20px)] lg:grid-cols-12 lg:grid-rows-[repeat(6,minmax(0,1fr))]">

        {/* Cover panel: your collage, with tonight's pick lettered over it */}
        <button data-nav-default {...nav(`${profile.feature.title} on ${featureApp.name} — press OK to play`, () => launch(featureApp, profile.feature.title))} className={`${TILE} ${PLACE.feature} flex flex-col justify-end p-[clamp(16px,1.5vw,36px)]`} style={{
        background: C.ink
      }}>
          
          <img src="https://storage.googleapis.com/storage.magicpath.ai/component-assets/468214371865219072/468215455874699264/bc3be34fa1fe12828de6cd70ceed772bc8ba0620fb8e98ce3977660ac079cbaa.jpg" alt="" decoding="async" className="pointer-events-none absolute inset-0 h-full w-full object-cover object-[50%_18%]" />
          <span className="pointer-events-none absolute inset-x-0 bottom-0 h-[52%]" style={{
          background: `linear-gradient(0deg, ${C.ink} 0%, ${C.ink} 34%, rgba(8,9,11,0) 100%)`
        }} />
          <span className="absolute left-[clamp(10px,0.8vw,18px)] top-[clamp(10px,0.8vw,18px)]">
            <Caption className="text-[clamp(0.75rem,0.8vw,1.2rem)]">Tonight&apos;s issue</Caption>
          </span>
          <Burst className="absolute right-[clamp(8px,0.6vw,14px)] top-[clamp(8px,0.6vw,14px)] w-[clamp(72px,5.6vw,130px)] text-[clamp(0.85rem,1vw,1.6rem)]">
            {profile.feature.progress !== undefined ? "Resume!" : "New!"}
          </Burst>
          <span className="relative">
            <Caption className="text-[clamp(0.7rem,0.75vw,1.1rem)]">{profile.feature.kicker}</Caption>
          </span>
          <span className="relative mt-2 text-[clamp(2.6rem,3.6vw,6rem)] leading-[0.92]" style={COMIC}>
            {profile.feature.title}
          </span>
          <span className="relative mt-2 text-[clamp(1rem,1.05vw,1.6rem)] font-semibold" style={{
          color: C.silver
        }}>
            {profile.feature.meta}
          </span>
          {profile.feature.progress !== undefined && <span className="relative mt-3 block h-[6px] w-full border border-[#F1E7CF]" style={{
          background: C.ink
        }}>
              <span className="block h-full" style={{
            width: `${profile.feature.progress * 100}%`,
            background: C.red
          }} />
            </span>}
        </button>

        {/* Watch slots: first six apps in the user's order */}
        {sortedApps.slice(0, 6).map((a, i) => <button key={a.id} {...nav(`${a.name} — press OK to open`, () => launch(a))} className={`${TILE} ${WATCH_SLOTS[i]} flex flex-col p-[clamp(14px,1.2vw,28px)]`} style={{
        background: tileBg(a),
        color: tileLabel(a)
      }}>
          
            {i === 0 && <span className="absolute left-0 top-0 z-[1]">
                <Caption className="text-[clamp(0.7rem,0.75vw,1.1rem)]">Watch</Caption>
              </span>}
            {SHORT_SLOTS.includes(i) ?
        // Short slot: label left, mark right, so the logo keeps a usable size.
        <span className="flex h-full items-center gap-3">
                <span className="min-w-0 flex-1 truncate text-[clamp(1.1rem,1.3vw,2.1rem)] font-bold leading-none">{a.name}</span>
                <svg viewBox="0 0 160 90" className="h-[90%] max-w-[50%] w-auto flex-none" aria-hidden>
                  {a.mark(iconStyle === "custom" ? a.customInk : a.ink)}
                </svg>
              </span> : <AppIcon app={a} style={iconStyle} markClass={i === 2 ? "h-[40%] scale-150" : "h-[52%]"} />}
            <span className={`relative flex items-end justify-between gap-2 ${SHORT_SLOTS.includes(i) ? "hidden" : ""}`}>
              <span className={`font-bold leading-none ${i === 2 ? "text-[clamp(0.9rem,0.95vw,1.5rem)]" : "truncate text-[clamp(1.1rem,1.3vw,2.1rem)]"}`}>{a.name}</span>
              {i === 0 && <span className="hidden flex-none text-[clamp(0.7rem,0.7vw,1.05rem)] font-semibold uppercase tracking-[0.12em] xl:block" style={{
            color: iconStyle === "custom" ? C.silver : a.label
          }}>
                  {a.tag}
                </span>}
            </span>
          </button>)}

        {/* Favorites (content pins per profile) */}
        {profile.favs.map((f, i) => {
        const a = appById(f.app);
        const live = f.meta === "Live now";
        const chip = <span className="relative flex aspect-[16/10] h-full max-h-[clamp(34px,3vw,72px)] flex-none flex-col overflow-hidden rounded-md" style={{
          background: tileBg(a)
        }}>
              <AppIcon app={a} style={iconStyle} markClass="h-[70%]" web={false} />
            </span>;
        if (i !== 0) {
          return <button key={`${profile.id}-${f.id}`} {...nav(`${f.title} · ${a.name} — press OK to open`, () => launch(a, f.title))} className={`${TILE} ${FAV_PLACES[i]} flex items-center gap-[clamp(10px,1vw,24px)] px-[clamp(12px,1vw,24px)] py-[clamp(8px,1vh,16px)]`} style={{
            background: C.surface
          }}>
                {chip}
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-[clamp(1.1rem,1.25vw,2rem)] leading-tight" style={COMIC}>
                    {f.title}
                  </span>
                  <span className="mt-0.5 block truncate text-[clamp(0.8rem,0.8vw,1.2rem)]" style={{
                color: C.silver
              }}>
                    {a.name}
                  </span>
                </span>
                {live ? <Burst className="relative w-[clamp(52px,3.8vw,90px)] text-[clamp(0.75rem,0.85vw,1.3rem)]">Live!</Burst> : <span className="flex-none text-[clamp(0.85rem,0.85vw,1.3rem)] font-semibold" style={{
              color: C.silver
            }}>
                    {f.meta}
                  </span>}
              </button>;
        }
        return <button key={`${profile.id}-${f.id}`} {...nav(`${f.title} · ${a.name} — press OK to open`, () => launch(a, f.title))} className={`${TILE} ${FAV_PLACES[i]} flex flex-col justify-between p-[clamp(14px,1.2vw,28px)]`} style={{
          background: C.surface
        }}>
              <Halftone id={`ht-fav-${i}`} color="#2A2E38" className="pointer-events-none absolute bottom-0 right-0 h-[85%] w-[65%]" />
              <span className="absolute left-0 top-0 z-[1]">
                <Caption className="text-[clamp(0.7rem,0.75vw,1.1rem)]">{profile.name}&apos;s favorites</Caption>
              </span>
              <span className="relative mt-[clamp(18px,1.8vw,36px)] flex h-[clamp(34px,3vw,72px)] items-center gap-3">
                {chip}
                <span className="text-[clamp(0.8rem,0.8vw,1.2rem)] font-semibold" style={{
              color: C.silver
            }}>
                  {a.name}
                </span>
              </span>
              <span className="relative">
                <span className="block text-[clamp(1.35rem,1.6vw,2.6rem)] leading-[1.05]" style={COMIC}>
                  {f.title}
                </span>
                <span className="mt-1.5 block text-[clamp(0.85rem,0.85vw,1.3rem)]" style={{
              color: live ? C.red : C.silver
            }}>
                  {f.meta}
                </span>
              </span>
            </button>;
      })}

        {/* Network widget */}
        {settings.showNetwork ? <button {...nav("Network — press OK for details", () => openDialog("network"))} className={`${TILE} ${PLACE.network} flex flex-col p-[clamp(14px,1.2vw,28px)] pt-[clamp(34px,2.6vw,56px)]`} style={{
        background: C.surface
      }}>
            <span className="absolute left-0 top-0 z-[1]">
              <Caption className="text-[clamp(0.7rem,0.75vw,1.1rem)]">Home base</Caption>
            </span>
            <span className="flex items-center gap-2.5">
              <span className="h-[clamp(20px,1.4vw,34px)] w-[clamp(20px,1.4vw,34px)]" style={{
            color: C.blue
          }}>
                {Glyph.shield}
              </span>
              <span className="text-[clamp(1.05rem,1.1vw,1.7rem)] font-bold">{NET.status}</span>
              <span className="ml-auto rounded-full px-2.5 py-0.5 text-[clamp(0.7rem,0.65vw,1rem)] font-semibold uppercase tracking-wider" style={{
            background: C.navy,
            color: C.silver
          }}>
                Sample data
              </span>
            </span>
            <span className="mt-[clamp(10px,1.2vh,22px)] grid grid-cols-3 gap-2">
              <Stat label="Queries" value={nf.format(NET.queries)} />
              <Stat label="Blocked" value={`${blockedPct}%`} accent />
              <Stat label="Latency" value={`${NET.latencyMs} ms`} />
            </span>
            <span className="mt-auto flex h-[clamp(36px,6vh,90px)] items-end gap-[3px] pt-3" aria-label="Queries per hour, last 12 hours">
              {NET.hourly.map((v, i) => <span key={i} className="flex-1 rounded-[2px]" style={{
            height: `${v / maxBar * 100}%`,
            background: i === NET.hourly.length - 1 ? C.red : C.blue
          }} />)}
            </span>
            <span className="mt-2 flex justify-between text-[clamp(0.7rem,0.65vw,1rem)]" style={{
          color: C.silver
        }}>
              <span>12 h ago</span>
              <span>{NET.devices} devices · NextDNS</span>
              <span>Now</span>
            </span>
          </button> : <div className={`${PLACE.network} flex min-h-[120px] items-center justify-center rounded-[clamp(10px,0.7vw,18px)] border-2 border-dashed p-4 text-center text-[clamp(0.85rem,0.85vw,1.3rem)] lg:min-h-0`} style={{
        borderColor: C.rule,
        color: C.silver
      }}>
            Network widget hidden — turn it on in Settings
          </div>}

        <button {...nav("Settings — press OK to open", () => openDialog("settings"))} className={`${TILE} ${PLACE.settings} flex flex-col justify-between p-[clamp(14px,1.2vw,28px)]`} style={{
        background: C.surface
      }}>
          <span className="h-[clamp(26px,2vw,48px)] w-[clamp(26px,2vw,48px)]" style={{
          color: C.silver
        }}>
            {Glyph.gear}
          </span>
          <span>
            <span className="block text-[clamp(1.3rem,1.5vw,2.4rem)] font-bold leading-none">Settings</span>
            <span className="mt-1.5 block text-[clamp(0.85rem,0.85vw,1.3rem)]" style={{
            color: C.silver
          }}>
              Display, icons, profiles
            </span>
          </span>
        </button>

        <button {...nav("Inputs — press OK to switch source", () => openDialog("inputs"))} className={`${TILE} ${PLACE.inputs} flex flex-col justify-between p-[clamp(14px,1.2vw,28px)]`} style={{
        background: C.surface
      }}>
          <span className="h-[clamp(26px,2vw,48px)] w-[clamp(26px,2vw,48px)]" style={{
          color: C.silver
        }}>
            {Glyph.input}
          </span>
          <span>
            <span className="block text-[clamp(1.1rem,1.2vw,2rem)] font-bold leading-none">Inputs</span>
            <span className="mt-1.5 block text-[clamp(0.8rem,0.8vw,1.2rem)]" style={{
            color: C.silver
          }}>
              {INPUTS.find(x => x.id === input)?.short}
            </span>
          </span>
        </button>

        <button {...nav("All apps — press OK to browse, reorder and pick favorites", () => openDialog("apps"))} className={`${TILE} ${PLACE.allapps} flex items-center gap-4 px-[clamp(14px,1.2vw,28px)] py-4`} style={{
        background: C.surface
      }}>
          <span className="h-[clamp(24px,1.8vw,44px)] w-[clamp(24px,1.8vw,44px)] flex-none" style={{
          color: C.silver
        }}>
            {Glyph.grid}
          </span>
          <span className="text-[clamp(1.15rem,1.3vw,2.1rem)] font-bold">All apps</span>
          <span className="ml-auto text-[clamp(0.85rem,0.85vw,1.3rem)]" style={{
          color: C.silver
        }}>
            {APPS.length} installed
          </span>
        </button>
      </main>

      {/* Status line — repeats the selected item in plain words */}
      <footer className="relative mt-[2vh] flex flex-none flex-wrap items-center gap-x-6 gap-y-2 border-t pt-[1.6vh] text-[clamp(0.9rem,0.9vw,1.4rem)]" style={{
      borderColor: C.rule
    }}>
        <span role="status" className="flex min-w-0 flex-1 items-center">
          {status ? <span className="relative ml-[14px] max-w-full truncate rounded-[999px] border-2 px-4 py-1 font-extrabold" style={{
          background: C.cream,
          color: C.ink,
          borderColor: C.ink
        }}>
              <svg viewBox="0 0 16 14" className="absolute -left-[14px] top-1/2 h-[14px] w-[16px] -translate-y-1/2" aria-hidden>
                <path d="M16 2 1 7l15 5" fill={C.cream} stroke={C.ink} strokeWidth="2" strokeLinejoin="round" />
              </svg>
              {status}
            </span> : <span className="truncate font-semibold">{selectedLabel}</span>}
        </span>
        <span className="hidden items-center gap-3 md:flex" style={{
        color: C.silver
      }}>
          <Key>◀ ▲ ▼ ▶</Key> Move <Key>OK</Key> Open <Key>Back</Key> Close
        </span>
      </footer>

      {/* Dialogs — solid, opaque, no blur */}
      {dialog && <div className="fixed inset-0 z-20 flex items-center justify-center p-[3vw]" style={{
      background: C.bg
    }}>
          <div ref={dialogRef} role="dialog" aria-modal="true" aria-label={DIALOG_TITLES[dialog]} className={`flex max-h-full w-full flex-col overflow-auto rounded-[clamp(12px,1vw,22px)] border-2 p-[clamp(20px,2.2vw,52px)] ${dialog === "apps" ? "max-w-[min(110rem,96vw)]" : "max-w-[min(72rem,92vw)]"}`} style={{
        background: C.surface,
        borderColor: C.rule
      }}>
          
            <div className="mb-[clamp(16px,2vh,36px)] flex flex-wrap items-center gap-4">
              <h2 className="text-[clamp(1.8rem,2.4vw,3.8rem)] leading-none" style={COMIC}>
                {DIALOG_TITLES[dialog]}
              </h2>
              {dialog === "apps" && <>
                  <span className="text-[clamp(0.9rem,0.9vw,1.4rem)]" style={{
              color: C.silver
            }}>
                    {editApps ? "Edit mode — press OK on an app to favorite or move it" : "★ Favorites lead the board and fill your Watch section"}
                  </span>
                  <div className="ml-auto flex flex-wrap items-center gap-2">
                    <div role="radiogroup" aria-label="Icon style" className="flex rounded-full p-1" style={{
                background: C.bg
              }}>
                      {(["custom", "original"] as const).map(s => <button key={s} role="radio" aria-checked={iconStyle === s} data-nav onClick={() => setIconStyle(s)} onMouseEnter={e => e.currentTarget.focus()} className={`rounded-full px-4 py-2 text-[clamp(0.9rem,0.9vw,1.4rem)] font-semibold ${FOCUS}`} style={{
                  background: iconStyle === s ? C.text : "transparent",
                  color: iconStyle === s ? C.bg : C.silver
                }}>
                    
                          {s === "custom" ? "Custom icons" : "Original icons"}
                        </button>)}
                    </div>
                    <button data-nav aria-pressed={editApps} onClick={() => setEditApps(v => !v)} onMouseEnter={e => e.currentTarget.focus()} className={`rounded-full px-5 py-2.5 text-[clamp(0.9rem,0.9vw,1.4rem)] font-semibold ${FOCUS}`} style={{
                background: editApps ? C.red : C.rule,
                color: C.text
              }}>
                  
                      {editApps ? "Done editing" : "Edit apps"}
                    </button>
                  </div>
                </>}
              <button data-nav onClick={() => {
            setDialog(null);
            setEditApps(false);
          }} onMouseEnter={e => e.currentTarget.focus()} className={`${dialog === "apps" ? "" : "ml-auto"} rounded-full px-5 py-2.5 text-[clamp(0.95rem,0.95vw,1.5rem)] font-semibold ${FOCUS}`} style={{
            background: C.rule,
            color: C.text
          }}>
              
                Back
              </button>
            </div>

            {dialog === "apps" && <ul className="grid grid-flow-dense auto-rows-[clamp(104px,8.6vw,190px)] grid-cols-2 gap-[clamp(10px,0.9vw,20px)] sm:grid-cols-4 lg:grid-cols-8">
                {sortedApps.map((a, i) => {
            const fav = favApps.includes(a.id);
            const restIndex = i - favApps.length;
            const size = fav ? "col-span-2 row-span-2" : wideSet.has(restIndex) ? "col-span-2" : "";
            return <li key={a.id} className={size}>
                      <button data-nav data-app={a.id} onClick={() => editApps ? setSheetApp(a.id) : (setDialog(null), launch(a))} onMouseEnter={e => e.currentTarget.focus()} aria-label={`${a.name}${fav ? ", favorite" : ""}${editApps ? ", press OK for options" : ""}`} className={`relative flex h-full w-full flex-col overflow-hidden rounded-[clamp(10px,0.7vw,16px)] p-[clamp(10px,0.9vw,22px)] text-left ${FOCUS}`} style={{
                background: tileBg(a),
                color: tileLabel(a),
                outlineColor: editApps ? C.red : undefined
              }}>
                    
                        <AppIcon app={a} style={iconStyle} markClass={fav ? "h-[48%]" : "h-[62%]"} />
                        <span className="relative flex items-center gap-2">
                          {fav && <span className="h-[1em] w-[1em] flex-none" style={{
                    color: iconStyle === "custom" ? C.red : tileLabel(a)
                  }} aria-hidden>
                              {Glyph.star(true)}
                            </span>}
                          <span className={`truncate font-bold leading-tight ${fav ? "text-[clamp(1.1rem,1.3vw,2rem)]" : "text-[clamp(0.85rem,0.9vw,1.4rem)]"}`}>{a.name}</span>
                        </span>
                      </button>
                    </li>;
          })}
              </ul>}

            {dialog === "network" && <div className="grid gap-[clamp(16px,1.6vw,40px)] md:grid-cols-2">
                <div>
                  <p className="text-[clamp(1rem,1vw,1.6rem)]" style={{
              color: C.silver
            }}>
                    Profile <span style={{
                color: C.text
              }}>{NET.profile}</span> · {NET.devices} devices on this network. These numbers are sample data until NextDNS is linked.
                  </p>
                  <dl className="mt-6 grid grid-cols-2 gap-x-6 gap-y-5">
                    <Stat label="Queries today" value={nf.format(NET.queries)} large />
                    <Stat label="Blocked" value={`${nf.format(NET.blocked)} · ${blockedPct}%`} accent large />
                    <Stat label="Average latency" value={`${NET.latencyMs} ms`} large />
                    <Stat label="Encrypted DNS" value="DNS-over-TLS" large />
                  </dl>
                  <button data-nav onClick={() => setStatus("NextDNS linking will be available in a future update")} onMouseEnter={e => e.currentTarget.focus()} className={`mt-8 rounded-full px-6 py-3 text-[clamp(1rem,1vw,1.6rem)] font-bold ${FOCUS}`} style={{
              background: C.text,
              color: C.bg
            }}>
                    Link NextDNS account
                  </button>
                </div>
                <div>
                  <h3 className="text-[clamp(0.85rem,0.85vw,1.3rem)] font-semibold uppercase tracking-[0.14em]" style={{
              color: C.silver
            }}>
                    Most blocked today
                  </h3>
                  <ul className="mt-3">
                    {NET.topBlocked.map(d => <li key={d.domain} className="grid grid-cols-[minmax(0,1fr)_auto] gap-4 border-b py-3 text-[clamp(0.95rem,0.95vw,1.5rem)]" style={{
                borderColor: C.rule
              }}>
                        <span className="truncate">{d.domain}</span>
                        <span className="tabular-nums" style={{
                  color: C.silver
                }}>
                          {nf.format(d.hits)}
                        </span>
                      </li>)}
                  </ul>
                </div>
              </div>}

            {dialog === "settings" && <ul className="grid gap-3">
                <li>
                  <SwitchRow label="Custom app icons" hint="Unified dark tiles with web details. Off shows each app's original colors." on={iconStyle === "custom"} onToggle={() => setIconStyle(s => s === "custom" ? "original" : "custom")} />
              
                </li>
                {([["showNetwork", "Show network widget on home", "NextDNS stats in the Home section"], ["largeText", "Larger text", "For viewing from farther away"], ["sleepAfterIdle", "Turn screen off after 2 hours idle", "Saves power when nobody is watching"], ["kidsPin", "Require PIN to leave Kids profile", "Applies to profile switching"]] as const).map(([key, label, hint]) => <li key={key}>
                    <SwitchRow label={label} hint={hint} on={settings[key]} onToggle={() => setSettings(s => ({
              ...s,
              [key]: !s[key]
            }))} />
                  </li>)}
              </ul>}

            {dialog === "inputs" && <ul className="grid gap-3 sm:grid-cols-2">
                {INPUTS.map(x => {
            const active = x.id === input;
            return <li key={x.id}>
                      <button data-nav disabled={x.empty} aria-pressed={active} onClick={() => {
                setInput(x.id);
                setDialog(null);
                setStatus(x.id === "home" ? "Back to home screen" : `Switching to ${x.short} · ${x.device}`);
              }} onMouseEnter={e => e.currentTarget.focus()} className={`flex w-full items-center justify-between gap-4 rounded-[clamp(10px,0.7vw,16px)] px-[clamp(16px,1.4vw,32px)] py-[clamp(14px,1.8vh,30px)] text-left disabled:cursor-not-allowed ${FOCUS}`} style={{
                background: active ? C.navy : C.bg,
                color: x.empty ? "#7D8291" : C.text
              }}>
                    
                        <span>
                          <span className="block text-[clamp(1.1rem,1.2vw,1.9rem)] font-bold">{x.short}</span>
                          <span className="block text-[clamp(0.85rem,0.85vw,1.3rem)]" style={{
                    color: x.empty ? "#7D8291" : C.silver
                  }}>
                            {x.device}
                          </span>
                        </span>
                        {active && <span className="text-[clamp(0.8rem,0.8vw,1.2rem)] font-semibold uppercase tracking-wider" style={{
                  color: C.red
                }}>
                            Current
                          </span>}
                      </button>
                    </li>;
          })}
              </ul>}
          </div>

          {/* App options sheet (edit mode) */}
          {sheet && <div className="fixed inset-y-0 right-0 z-30 flex w-[min(34rem,100vw)] items-center border-l-2 p-[clamp(16px,1.6vw,36px)]" style={{
        background: C.bg,
        borderColor: C.rule
      }}>
              <div ref={sheetRef} role="dialog" aria-modal="true" aria-label={`${sheet.name} options`} className="w-full rounded-[clamp(12px,1vw,22px)] border-2 p-[clamp(20px,2vw,40px)]" style={{
          background: C.surface,
          borderColor: C.rule
        }}>
            
                <div className="flex items-center gap-[clamp(14px,1.2vw,28px)]">
                  <span className="relative flex aspect-[16/10] w-[clamp(120px,9vw,200px)] flex-none flex-col overflow-hidden rounded-xl p-2" style={{
              background: tileBg(sheet)
            }}>
                    <AppIcon app={sheet} style={iconStyle} markClass="h-[70%]" />
                  </span>
                  <div className="min-w-0">
                    <h3 className="truncate text-[clamp(1.6rem,2vw,3.2rem)] leading-none" style={COMIC}>
                      {sheet.name}
                    </h3>
                    <p className="mt-2 text-[clamp(0.95rem,0.95vw,1.5rem)]" style={{
                color: C.silver
              }}>
                      {favApps.includes(sheet.id) ? "Favorite" : "App"} · position {sheetIndex + 1} of {sheetGroup.length}
                      {favApps.includes(sheet.id) ? " favorites" : " others"}
                    </p>
                  </div>
                </div>
                <div className="mt-[clamp(18px,2.4vh,40px)] grid grid-cols-2 gap-3">
                  <SheetButton onClick={() => toggleFav(sheet.id)} wide accent={favApps.includes(sheet.id)}>
                    <span className="h-[1.1em] w-[1.1em]">{Glyph.star(favApps.includes(sheet.id))}</span>
                    {favApps.includes(sheet.id) ? "Remove from favorites" : "Add to favorites"}
                  </SheetButton>
                  <SheetButton onClick={() => moveApp(sheet.id, "earlier")} disabled={sheetIndex <= 0}>
                    ◀ Move earlier
                  </SheetButton>
                  <SheetButton onClick={() => moveApp(sheet.id, "later")} disabled={sheetIndex >= sheetGroup.length - 1}>
                    Move later ▶
                  </SheetButton>
                  <SheetButton onClick={() => moveApp(sheet.id, "front")} disabled={sheetIndex <= 0}>
                    Move to front
                  </SheetButton>
                  <SheetButton onClick={closeSheet}>Done</SheetButton>
                </div>
              </div>
            </div>}
        </div>}
    </div>;
};
const DIALOG_TITLES: Record<Exclude<Dialog, null>, string> = {
  network: "Network",
  settings: "Settings",
  inputs: "Inputs",
  apps: "Apps"
};
const INPUTS = [{
  id: "home",
  short: "Home",
  device: "Orrery launcher",
  empty: false
}, {
  id: "hdmi1",
  short: "HDMI 1",
  device: "PlayStation 5",
  empty: false
}, {
  id: "hdmi2",
  short: "HDMI 2 · eARC",
  device: "Soundbar",
  empty: false
}, {
  id: "hdmi3",
  short: "HDMI 3",
  device: "Nothing connected",
  empty: true
}];
const Stat = ({
  label,
  value,
  accent,
  large
}: {
  label: string;
  value: string;
  accent?: boolean;
  large?: boolean;
}) => <span className="block min-w-0">
    <span className="block truncate text-[clamp(0.7rem,0.7vw,1.05rem)] font-semibold uppercase tracking-[0.1em]" style={{
    color: C.silver
  }}>
      {label}
    </span>
    <span className={`mt-1 block truncate font-bold tabular-nums ${large ? "text-[clamp(1.3rem,1.6vw,2.6rem)]" : "text-[clamp(1.05rem,1.15vw,1.8rem)]"}`} style={{
    color: accent ? C.red : C.text
  }}>
      {value}
    </span>
  </span>;
const SwitchRow = ({
  label,
  hint,
  on,
  onToggle
}: {
  label: string;
  hint: string;
  on: boolean;
  onToggle: () => void;
}) => <button data-nav role="switch" aria-checked={on} onClick={onToggle} onMouseEnter={e => e.currentTarget.focus()} className={`flex w-full items-center gap-6 rounded-[clamp(10px,0.7vw,16px)] px-[clamp(16px,1.4vw,32px)] py-[clamp(12px,1.4vh,24px)] text-left ${FOCUS}`} style={{
  background: C.bg
}}>
  
    <span className="min-w-0 flex-1">
      <span className="block text-[clamp(1.05rem,1.1vw,1.7rem)] font-semibold">{label}</span>
      <span className="mt-0.5 block text-[clamp(0.85rem,0.85vw,1.3rem)]" style={{
      color: C.silver
    }}>
        {hint}
      </span>
    </span>
    <span className="flex h-[clamp(28px,1.9vw,44px)] w-[clamp(52px,3.4vw,80px)] flex-none items-center rounded-full p-1" style={{
    background: on ? C.red : C.rule,
    justifyContent: on ? "flex-end" : "flex-start"
  }}>
      <span className="aspect-square h-full rounded-full" style={{
      background: C.text
    }} />
    </span>
    <span className="w-[3ch] flex-none text-right text-[clamp(0.9rem,0.9vw,1.4rem)] font-semibold" style={{
    color: C.silver
  }}>
      {on ? "On" : "Off"}
    </span>
  </button>;
const SheetButton = ({
  children,
  onClick,
  disabled,
  wide,
  accent
}: {
  children: ReactNode;
  onClick: () => void;
  disabled?: boolean;
  wide?: boolean;
  accent?: boolean;
}) => <button data-nav disabled={disabled} onClick={onClick} onMouseEnter={e => e.currentTarget.focus()} className={`flex items-center justify-center gap-2.5 rounded-full px-5 py-[clamp(10px,1.2vh,18px)] text-[clamp(1rem,1vw,1.6rem)] font-semibold disabled:cursor-not-allowed ${wide ? "col-span-2" : ""} ${FOCUS}`} style={{
  background: accent ? C.red : C.bg,
  color: disabled ? "#6B7080" : C.text
}}>
  
    {children}
  </button>;
const Key = ({
  children
}: {
  children: ReactNode;
}) => <kbd className="rounded-md border px-2 py-0.5 font-sans text-[0.9em]" style={{
  borderColor: C.rule,
  background: C.surface,
  color: C.text
}}>
    {children}
  </kbd>;