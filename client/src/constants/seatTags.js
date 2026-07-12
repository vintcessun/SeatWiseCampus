// 座位属性标签（与后端 com.seatwise.common.SeatTags 对齐）
// key 与后端一致；label 为完整中文名；short 为座位徽标上的单字
export const SEAT_TAGS = [
  { key: 'window', label: '靠窗', short: '窗' },
  { key: 'power', label: '有插座', short: '插' },
  { key: 'quiet', label: '安静区', short: '静' },
  { key: 'discuss', label: '讨论区', short: '讨' },
  { key: 'near_door', label: '靠门', short: '门' },
]

export const tagCn = (t) => SEAT_TAGS.find((x) => x.key === t)?.label || t
export const tagShort = (t) => SEAT_TAGS.find((x) => x.key === t)?.short || t

// 兼容 CSV 字符串或数组，统一转成合法 key 数组
export function toTagKeys(tags) {
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string' && tags) return tags.split(',').filter(Boolean)
  return []
}
