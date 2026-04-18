import { useState } from "react";

const COLORS = {
  primary: "#6C5CE7",
  primaryLight: "#A29BFE",
  primaryDark: "#5A4BD1",
  accent: "#FDCB6E",
  accentDark: "#F0B429",
  bg: "#F8F7FC",
  card: "#FFFFFF",
  text: "#1A1A2E",
  textSecondary: "#6B7280",
  textTertiary: "#9CA3AF",
  border: "#F0EDF6",
  chipBg: "#F3F1FA",
  chipActive: "#6C5CE7",
  badge: "#FF6B6B",
  save: "#FF6B81",
  success: "#00B894",
  shadow: "0 2px 12px rgba(108, 92, 231, 0.08)",
  cardShadow: "0 4px 20px rgba(108, 92, 231, 0.10)",
  topCardShadow: "0 8px 32px rgba(108, 92, 231, 0.18)",
};

const filters = [
  { label: "데이트", emoji: "💑" },
  { label: "조용한", emoji: "🤫" },
  { label: "디저트", emoji: "🍰" },
  { label: "가성비", emoji: "💰" },
  { label: "뷰 좋은", emoji: "🌇" },
  { label: "혼밥 가능", emoji: "🍜" },
];

const places = [
  {
    rank: 1,
    name: "어반플랜트 한강대교점",
    category: "브런치카페",
    rating: 4.8,
    reviews: 1243,
    distance: "350m",
    tags: ["데이트", "뷰 좋은", "브런치"],
    address: "서울 용산구 양녕로 496",
    image: "🏞️",
    saved: false,
  },
  {
    rank: 2,
    name: "오설록 티하우스 용산점",
    category: "카페 · 차",
    rating: 4.6,
    reviews: 892,
    distance: "500m",
    tags: ["조용한", "디저트"],
    address: "서울 용산구 한강대로 100",
    image: "🍵",
    saved: true,
  },
  {
    rank: 3,
    name: "모센트 용산점",
    category: "카페 · 디저트",
    rating: 4.5,
    reviews: 567,
    distance: "420m",
    tags: ["디저트", "데이트"],
    address: "서울 용산구 한강대로52길 25-14",
    image: "☕",
    saved: false,
  },
  {
    rank: 4,
    name: "아이엠베이글 용산점",
    category: "브런치카페",
    rating: 4.4,
    reviews: 445,
    distance: "600m",
    tags: ["가성비", "브런치"],
    address: "서울 용산구 서빙고로 17",
    image: "🥯",
    saved: false,
  },
  {
    rank: 5,
    name: "이코복스커피 이태원점",
    category: "카페 · 디저트",
    rating: 4.3,
    reviews: 320,
    distance: "750m",
    tags: ["조용한", "가성비"],
    address: "서울 용산구 우사단로14길 4",
    image: "🫘",
    saved: false,
  },
];

function StarIcon({ size = 14 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={COLORS.accent}>
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  );
}

function HeartIcon({ filled, size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={filled ? COLORS.save : "none"} stroke={filled ? COLORS.save : COLORS.textTertiary} strokeWidth="2">
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
    </svg>
  );
}

function NavigateIcon({ size = 16 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="3 11 22 2 13 21 11 13 3 11" />
    </svg>
  );
}

function CloseIcon({ size = 22 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={COLORS.textSecondary} strokeWidth="2" strokeLinecap="round">
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

function MapPinIcon({ size = 16, color = COLORS.primary }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
      <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" />
    </svg>
  );
}

function FilterChip({ label, emoji, active, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        padding: "8px 16px",
        borderRadius: 100,
        border: active ? `2px solid ${COLORS.primary}` : `1.5px solid ${COLORS.border}`,
        background: active ? COLORS.primary : COLORS.card,
        color: active ? "#fff" : COLORS.text,
        fontSize: 13,
        fontWeight: active ? 600 : 500,
        cursor: "pointer",
        whiteSpace: "nowrap",
        transition: "all 0.2s ease",
        boxShadow: active ? `0 2px 8px rgba(108, 92, 231, 0.3)` : "none",
      }}
    >
      <span>{emoji}</span>
      <span>{label}</span>
    </button>
  );
}

function PlaceCard({ place, isTop }) {
  const [saved, setSaved] = useState(place.saved);

  return (
    <div
      style={{
        background: COLORS.card,
        borderRadius: isTop ? 20 : 16,
        padding: isTop ? "20px 18px" : "16px 16px",
        boxShadow: isTop ? COLORS.topCardShadow : COLORS.cardShadow,
        border: isTop ? `2px solid ${COLORS.primaryLight}` : `1px solid ${COLORS.border}`,
        position: "relative",
        transition: "all 0.2s ease",
        overflow: "hidden",
      }}
    >
      {isTop && (
        <div
          style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            height: 3,
            background: `linear-gradient(90deg, ${COLORS.primary}, ${COLORS.accent})`,
          }}
        />
      )}

      <div style={{ display: "flex", gap: 14 }}>
        {/* Thumbnail */}
        <div
          style={{
            width: isTop ? 80 : 64,
            height: isTop ? 80 : 64,
            borderRadius: 14,
            background: isTop
              ? `linear-gradient(135deg, ${COLORS.primaryLight}30, ${COLORS.accent}30)`
              : `${COLORS.chipBg}`,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: isTop ? 32 : 26,
            flexShrink: 0,
          }}
        >
          {place.image}
        </div>

        {/* Info */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
            {isTop && (
              <span
                style={{
                  background: `linear-gradient(135deg, ${COLORS.badge}, #FF8E53)`,
                  color: "#fff",
                  fontSize: 11,
                  fontWeight: 700,
                  padding: "3px 8px",
                  borderRadius: 6,
                  letterSpacing: 0.3,
                }}
              >
                🔥 추천 1위
              </span>
            )}
            <span
              style={{
                color: COLORS.textTertiary,
                fontSize: 12,
                fontWeight: 500,
              }}
            >
              {place.category}
            </span>
          </div>

          <div
            style={{
              fontSize: isTop ? 17 : 15,
              fontWeight: 700,
              color: COLORS.text,
              marginBottom: 6,
              lineHeight: 1.3,
            }}
          >
            {place.name}
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 8 }}>
            <StarIcon size={14} />
            <span style={{ fontSize: 13, fontWeight: 600, color: COLORS.text }}>
              {place.rating}
            </span>
            <span style={{ fontSize: 12, color: COLORS.textTertiary }}>
              ({place.reviews.toLocaleString()})
            </span>
            <span style={{ color: COLORS.textTertiary, fontSize: 12 }}>·</span>
            <span style={{ fontSize: 12, color: COLORS.primary, fontWeight: 600 }}>
              {place.distance}
            </span>
          </div>

          {/* Tags */}
          <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
            {place.tags.map((tag) => (
              <span
                key={tag}
                style={{
                  fontSize: 11,
                  fontWeight: 500,
                  color: COLORS.primary,
                  background: COLORS.chipBg,
                  padding: "3px 8px",
                  borderRadius: 6,
                }}
              >
                {tag}
              </span>
            ))}
          </div>
        </div>

        {/* Save Button */}
        <button
          onClick={() => setSaved(!saved)}
          style={{
            background: "none",
            border: "none",
            cursor: "pointer",
            padding: 4,
            alignSelf: "flex-start",
            transition: "transform 0.2s",
          }}
        >
          <HeartIcon filled={saved} />
        </button>
      </div>

      {/* Action Buttons */}
      <div style={{ display: "flex", gap: 8, marginTop: 14 }}>
        <button
          style={{
            flex: 1,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: 6,
            padding: "10px 0",
            borderRadius: 12,
            border: "none",
            background: COLORS.primary,
            color: "#fff",
            fontSize: 13,
            fontWeight: 600,
            cursor: "pointer",
            boxShadow: `0 2px 8px rgba(108, 92, 231, 0.3)`,
          }}
        >
          <NavigateIcon size={14} />
          길찾기
        </button>
        <button
          style={{
            flex: 1,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: 6,
            padding: "10px 0",
            borderRadius: 12,
            border: `1.5px solid ${COLORS.border}`,
            background: COLORS.card,
            color: COLORS.text,
            fontSize: 13,
            fontWeight: 600,
            cursor: "pointer",
          }}
        >
          <MapPinIcon size={14} />
          상세보기
        </button>
      </div>
    </div>
  );
}

export default function PlaceRecommendationScreen() {
  const [activeFilters, setActiveFilters] = useState(new Set());

  const toggleFilter = (label) => {
    setActiveFilters((prev) => {
      const next = new Set(prev);
      if (next.has(label)) next.delete(label);
      else next.add(label);
      return next;
    });
  };

  return (
    <div
      style={{
        width: 393,
        height: 852,
        background: COLORS.bg,
        fontFamily: '-apple-system, BlinkMacSystemFont, "Pretendard", sans-serif',
        overflow: "hidden",
        display: "flex",
        flexDirection: "column",
        margin: "0 auto",
        borderRadius: 20,
        boxShadow: "0 8px 40px rgba(0,0,0,0.12)",
        position: "relative",
      }}
    >
      {/* ─── Header ─── */}
      <div
        style={{
          padding: "16px 20px 12px",
          background: COLORS.card,
          borderBottom: `1px solid ${COLORS.border}`,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div>
            <h1
              style={{
                fontSize: 22,
                fontWeight: 800,
                color: COLORS.text,
                margin: 0,
                letterSpacing: -0.5,
              }}
            >
              만나기 좋은 장소
            </h1>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 6,
                marginTop: 6,
              }}
            >
              <MapPinIcon size={14} color={COLORS.primary} />
              <span style={{ fontSize: 13, color: COLORS.textSecondary, fontWeight: 500 }}>
                용산구 · 카페 · 3명
              </span>
            </div>
          </div>
          <button
            style={{
              background: COLORS.chipBg,
              border: "none",
              borderRadius: 12,
              width: 40,
              height: 40,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              cursor: "pointer",
            }}
          >
            <CloseIcon />
          </button>
        </div>
      </div>

      {/* ─── Filter Chips ─── */}
      <div
        style={{
          padding: "14px 20px",
          background: COLORS.card,
          display: "flex",
          gap: 8,
          overflowX: "auto",
          borderBottom: `1px solid ${COLORS.border}`,
        }}
      >
        {filters.map((f) => (
          <FilterChip
            key={f.label}
            label={f.label}
            emoji={f.emoji}
            active={activeFilters.has(f.label)}
            onClick={() => toggleFilter(f.label)}
          />
        ))}
      </div>

      {/* ─── Scrollable Content ─── */}
      <div
        style={{
          flex: 1,
          overflowY: "auto",
          padding: "16px 20px 100px",
          display: "flex",
          flexDirection: "column",
          gap: 14,
        }}
      >
        {/* Map Preview Card */}
        <div
          style={{
            background: `linear-gradient(135deg, ${COLORS.primaryLight}20, ${COLORS.primary}10)`,
            borderRadius: 16,
            padding: 16,
            border: `1px solid ${COLORS.primaryLight}30`,
          }}
        >
          <div
            style={{
              height: 120,
              borderRadius: 12,
              background: `linear-gradient(135deg, #e8e4f3, #d5d0e8)`,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              position: "relative",
              overflow: "hidden",
              marginBottom: 12,
            }}
          >
            {/* Fake map pins */}
            <div style={{ position: "absolute", top: 25, left: "30%", fontSize: 20 }}>📍</div>
            <div style={{ position: "absolute", top: 45, left: "55%", fontSize: 20 }}>📍</div>
            <div style={{ position: "absolute", top: 35, left: "42%", fontSize: 24 }}>📌</div>
            <div style={{ position: "absolute", top: 60, left: "65%", fontSize: 20 }}>📍</div>
            <div style={{ position: "absolute", top: 55, left: "25%", fontSize: 20 }}>📍</div>
            <span style={{ fontSize: 12, color: COLORS.textTertiary, marginTop: 80 }}>
              추천 장소 5곳이 표시됩니다
            </span>
          </div>
          <button
            style={{
              width: "100%",
              padding: "10px 0",
              borderRadius: 10,
              border: `1.5px solid ${COLORS.primary}`,
              background: "transparent",
              color: COLORS.primary,
              fontSize: 13,
              fontWeight: 600,
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: 6,
            }}
          >
            <MapPinIcon size={14} />
            지도에서 보기
          </button>
        </div>

        {/* Section Title */}
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "4px 0" }}>
          <span style={{ fontSize: 16, fontWeight: 700, color: COLORS.text }}>
            추천 장소
          </span>
          <span style={{ fontSize: 12, color: COLORS.textTertiary }}>
            중간 지점 기준
          </span>
        </div>

        {/* Place Cards */}
        {places.map((place) => (
          <PlaceCard key={place.rank} place={place} isTop={place.rank === 1} />
        ))}
      </div>

      {/* ─── Bottom Button ─── */}
      <div
        style={{
          position: "absolute",
          bottom: 0,
          left: 0,
          right: 0,
          padding: "12px 20px 24px",
          background: `linear-gradient(transparent, ${COLORS.bg} 30%)`,
        }}
      >
        <button
          style={{
            width: "100%",
            padding: "16px 0",
            borderRadius: 16,
            border: "none",
            background: `linear-gradient(135deg, ${COLORS.primary}, ${COLORS.primaryDark})`,
            color: "#fff",
            fontSize: 15,
            fontWeight: 700,
            cursor: "pointer",
            boxShadow: `0 4px 16px rgba(108, 92, 231, 0.4)`,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: 8,
            letterSpacing: 0.3,
          }}
        >
          🔄 다시 추천받기
        </button>
      </div>
    </div>
  );
}
