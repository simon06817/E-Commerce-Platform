const SESSION_KEY = 'ecommerce_session'
const AVATAR_PREFIX = 'ecommerce_avatar_'

export function getSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null')
  } catch {
    return null
  }
}

export function saveSession(data) {
  const session = {
    token: data.token,
    refreshToken: data.refreshToken,
    userId: data.userId,
    username: data.username,
    role: data.role,
    displayName: data.displayName
  }
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY)
}

export function getAccessToken() {
  return getSession()?.token || ''
}

export function getRefreshToken() {
  return getSession()?.refreshToken || ''
}

export function getAvatarUrl(userId) {
  return userId ? localStorage.getItem(`${AVATAR_PREFIX}${userId}`) || '' : ''
}

export function setAvatarUrl(userId, url) {
  if (!userId) return
  if (url) {
    localStorage.setItem(`${AVATAR_PREFIX}${userId}`, url)
  } else {
    localStorage.removeItem(`${AVATAR_PREFIX}${userId}`)
  }
}

export function roleHome(role) {
  if (role === 'SELLER') return '/seller'
  if (role === 'ADMIN') return '/admin'
  return '/home'
}
