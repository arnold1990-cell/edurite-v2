type Props = { kind: "academic" | "transcript" | "ai" };

/** Decorative, self-contained artwork. No student results are encoded in it. */
export function SummaryCardIllustration({ kind }: Props) {
  return (
    <svg
      className="cg-summary-illustration"
      viewBox="0 0 240 220"
      width="200"
      height="184"
      fill="none"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <ellipse cx="122" cy="119" rx="100" ry="86" fill="var(--cg-brand-wash)" />
      <circle cx="208" cy="59" r="4" fill="var(--cg-logo-teal)" opacity=".35" />
      <path d="m24 132 4-5 5 5-5 5-4-5Z" fill="var(--ed-orange)" opacity=".4" />
      {kind === "academic" && (
        <>
          <ellipse
            cx="123"
            cy="193"
            rx="96"
            ry="7"
            fill="var(--cg-logo-teal)"
            opacity=".09"
          />
          <path d="m57 46 37-15 37 15-37 15-37-15Z" fill="#253b4e" />
          <path d="M71 53v17c14 8 32 8 46 0V53L94 62 71 53Z" fill="#314e62" />
          <path d="M126 48v22" stroke="var(--ed-orange)" />
          <path d="m123 77 3-8 3 8" stroke="var(--ed-orange)" />
          <rect
            x="170"
            y="36"
            width="45"
            height="43"
            rx="7"
            fill="white"
            stroke="var(--student-border)"
          />
          <path
            d="M180 69v-8M189 69V56M198 69V50"
            stroke="var(--cg-logo-teal)"
            strokeWidth="5"
          />
          <path
            d="m178 54 12-8 10 1 6-7M201 40h5v5"
            stroke="var(--ed-orange)"
          />
          <path
            d="M75 184v-27c0-22 15-35 40-35s41 13 41 35v27"
            fill="var(--ed-orange)"
          />
          <path d="m101 124 13 18 15-18" fill="#ff9b4d" />
          <path d="M108 113v15c5 5 11 5 16 0v-15" fill="#edb68f" />
          <path
            d="M96 94c0-19 38-20 38 0v15c0 13-8 21-19 21s-19-8-19-21V94Z"
            fill="#f5c6a3"
          />
          <path
            d="M96 102c-8-24 5-34 21-31 15-6 24 6 20 24l-5 9-3-16c-8 5-17 6-26 2l-2 14-5-2Z"
            fill="#253b4e"
          />
          <path d="M97 101c-6-5-8 8 0 10M134 101c6-5 8 8 0 10" fill="#f5c6a3" />
          <path d="M107 103h1M122 103h1" stroke="#253b4e" strokeWidth="3" />
          <path d="m112 113 5 1 4-3" stroke="#bc795b" />
          <path d="M88 149v29M102 145v13M125 146v13" stroke="#f4a15c" />
          <path d="m85 171 29 8 12-2 6 9H97l-16-5" fill="#f5c6a3" />
          <path
            d="m149 143-10 43h63l10-43a4 4 0 0 0-4-5h-54a5 5 0 0 0-5 5Z"
            fill="#bdd8df"
          />
          <path d="m139 186-16 6h79l7-6" fill="#7ba9b6" />
          <ellipse cx="178" cy="162" rx="4" ry="5" fill="var(--cg-logo-teal)" />
          <rect
            x="29"
            y="179"
            width="42"
            height="11"
            rx="3"
            fill="var(--cg-logo-teal)"
          />
          <path d="M34 179v11" stroke="white" opacity=".65" />
          <path
            d="M32 168h37a5 5 0 0 1 0 10H32"
            fill="white"
            stroke="#b4c8d0"
          />
          <rect
            x="27"
            y="156"
            width="43"
            height="11"
            rx="3"
            fill="var(--ed-orange)"
          />
          <path d="M35 161h25" stroke="white" opacity=".7" />
          <path d="M27 193h188" stroke="#7895a7" />
        </>
      )}
      {kind === "transcript" && (
        <>
          <path
            d="M65 29h73l30 30v125a7 7 0 0 1-7 7H65a7 7 0 0 1-7-7V36a7 7 0 0 1 7-7Z"
            fill="#d4e5e9"
            transform="translate(4 5)"
            opacity=".55"
          />
          <path
            d="M65 29h73l30 30v125a7 7 0 0 1-7 7H65a7 7 0 0 1-7-7V36a7 7 0 0 1 7-7Z"
            fill="white"
            stroke="#a9bdca"
          />
          <path
            d="M138 29v25a5 5 0 0 0 5 5h25"
            fill="#f0f7f8"
            stroke="#a9bdca"
          />
          <path d="M77 55h43" stroke="#253b4e" strokeWidth="4" />
          <path d="M77 65h55" stroke="#bdced6" />
          <rect
            x="77"
            y="84"
            width="13"
            height="13"
            rx="3"
            stroke="var(--ed-orange)"
          />
          <path d="m80 90 3 3 5-6" stroke="var(--ed-orange)" />
          <path
            d="M102 87h43M102 95h31M102 117h37M102 125h31M102 147h43M102 155h35M77 175h60"
            stroke="#a9bdca"
            strokeWidth="3"
          />
          <path
            d="m78 119 4 4 7-8M78 149l4 4 7-8"
            stroke="var(--cg-logo-teal)"
            strokeWidth="2.5"
          />
          <path d="m178 158 26 33" stroke="#253b4e" strokeWidth="12" />
          <path
            d="m175 154 10 13"
            stroke="var(--cg-logo-teal)"
            strokeWidth="9"
          />
          <circle
            cx="156"
            cy="130"
            r="32"
            fill="white"
            fillOpacity=".6"
            stroke="var(--ed-orange)"
            strokeWidth="7"
          />
          <path
            d="M139 132h28M139 141h20"
            stroke="var(--cg-logo-teal)"
            strokeWidth="3"
          />
          <path
            d="m192 38 4-5 5 5-5 5-4-5Z"
            fill="var(--ed-orange)"
            opacity=".4"
          />
        </>
      )}
      {kind === "ai" && (
        <>
          <path
            d="M120 55V35M120 165v24M74 108H42M167 108h31"
            stroke="var(--cg-logo-teal)"
            strokeDasharray="4 5"
            opacity=".5"
          />
          <path
            d="M120 78c-11-13-31-7-32 9-17-1-25 17-17 29-8 14 0 29 16 30 1 16 20 21 33 9 13 12 32 7 33-9 16-1 24-16 16-30 8-12 0-30-17-29-1-16-21-22-32-9Z"
            fill="white"
            stroke="var(--ed-orange)"
            strokeWidth="3"
          />
          <path
            d="M120 79v75M102 90v18l-10 8M139 91v17l10 8M95 134h12v-12M139 136v-12h11"
            stroke="var(--ed-orange)"
            strokeWidth="2.5"
          />
          <g stroke="var(--cg-logo-teal)" strokeWidth="2.5" fill="white">
            <circle cx="102" cy="90" r="4" />
            <circle cx="139" cy="91" r="4" />
            <circle cx="92" cy="116" r="4" />
            <circle cx="149" cy="116" r="4" />
            <circle cx="107" cy="122" r="4" />
            <circle cx="150" cy="124" r="4" />
          </g>
          <g fill="white" stroke="#b8dce4">
            <circle cx="120" cy="25" r="21" />
            <circle cx="25" cy="108" r="21" />
            <circle cx="215" cy="108" r="21" />
            <circle cx="120" cy="194" r="21" />
          </g>
          <path
            d="m107 22 13-6 13 6-13 6-13-6ZM112 25v8c5 3 11 3 16 0v-8"
            fill="var(--ed-orange)"
          />
          <path d="M132 23v9" stroke="var(--ed-orange)" />
          <path
            d="M16 119v-11h5v11M24 119V99h5v20M32 119v-15h4v15"
            stroke="var(--cg-logo-teal)"
            strokeWidth="3"
          />
          <rect
            x="203"
            y="101"
            width="24"
            height="17"
            rx="3"
            stroke="var(--ed-orange)"
          />
          <path
            d="M211 101v-5h8v5M203 108h24M213 108v4h4v-4"
            stroke="var(--ed-orange)"
          />
          <circle cx="120" cy="194" r="12" stroke="var(--cg-logo-teal)" />
          <path d="m115 199 3-8 8-3-3 8-8 3Z" stroke="var(--ed-orange)" />
          <path
            d="m63 62 3-4 4 4-4 4-3-4Z"
            fill="var(--ed-orange)"
            opacity=".55"
          />
        </>
      )}
    </svg>
  );
}
