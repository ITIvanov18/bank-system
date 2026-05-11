import type { AuthResponse, AuthSession } from '../types/auth';

const AUTH_STORAGE_KEY = 'bank_auth_session';

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const tokenParts = token.split('.');
  if (tokenParts.length !== 3) {
    return null;
  }

  const base64UrlPayload = tokenParts[1];
  const base64Payload = base64UrlPayload.replace(/-/g, '+').replace(/_/g, '/');
  const paddedPayload = base64Payload.padEnd(Math.ceil(base64Payload.length / 4) * 4, '=');

  try {
    const jsonPayload = atob(paddedPayload);
    return JSON.parse(jsonPayload) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function isTokenExpired(token: string): boolean {
  const payload = decodeJwtPayload(token);
  const expClaim = payload?.exp;

  if (typeof expClaim !== 'number') {
    return false;
  }

  const expiryTimestampMs = expClaim * 1000;
  return Date.now() >= expiryTimestampMs;
}

function isValidSessionShape(value: unknown): value is AuthSession {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const maybeSession = value as Partial<AuthSession>;
  const customerTypeValue = (value as Record<string, unknown>).customerType;
  const displayNameValue = (value as Record<string, unknown>).displayName;
  return (
    typeof maybeSession.token === 'string' &&
    typeof maybeSession.customerId === 'number' &&
    typeof maybeSession.email === 'string' &&
    (displayNameValue === null || typeof displayNameValue === 'string') &&
    (maybeSession.role === 'CUSTOMER' || maybeSession.role === 'EMPLOYEE') &&
    (customerTypeValue === undefined || customerTypeValue === null || customerTypeValue === 'INDIVIDUAL' || customerTypeValue === 'CORPORATE') &&
    typeof maybeSession.firstLogin === 'boolean'
  );
}

export function saveSession(auth: AuthResponse): void {
  const session = {
    token: auth.token,
    customerId: auth.customerId,
    email: auth.email,
    displayName: ('displayName' in auth) ? auth.displayName ?? null : null,
    role: auth.role,
    firstLogin: auth.firstLogin,
    ...(('customerType' in auth)
      ? { customerType: (auth as AuthResponse & { customerType?: 'INDIVIDUAL' | 'CORPORATE' | null }).customerType ?? null }
      : {}),
  } as AuthSession;
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function getSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!isValidSessionShape(parsed)) {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }

    if (isTokenExpired(parsed.token)) {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }

    return parsed;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function logoutIfSessionExpired(): boolean {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return false;
  }

  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!isValidSessionShape(parsed)) {
      clearSession();
      return true;
    }

    if (isTokenExpired(parsed.token)) {
      clearSession();
      return true;
    }
  } catch {
    clearSession();
    return true;
  }

  return false;
}

export function clearSession(): void {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function markFirstLoginDone(): void {
  const session = getSession();
  if (!session) {
    return;
  }

  saveSession({ ...session, firstLogin: false });
}

