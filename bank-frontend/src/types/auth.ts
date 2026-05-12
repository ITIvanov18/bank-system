export type UserRole = 'CUSTOMER' | 'EMPLOYEE';
export type CustomerType = 'INDIVIDUAL' | 'CORPORATE';

export interface AuthResponse {
  token: string;
  customerId: number;
  email: string;
  displayName: string | null;
  role: UserRole;
  customerType: CustomerType | null;
  firstLogin: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ApiMessage {
  message: string;
}

export interface AuthSession {
  token: string;
  customerId: number;
  email: string;
  displayName: string | null;
  role: UserRole;
  customerType: CustomerType | null;
  firstLogin: boolean;
}

export interface IndividualOnboardingRequest {
  firstName: string;
  lastName: string;
  egn: string;
  email: string;
}

export interface CorporateOnboardingRequest {
  companyName: string;
  eik: string;
  representativeFirstName: string;
  representativeLastName: string;
  email: string;
}

export interface OnboardingResponse {
  customerId: number;
  email: string;
  customerType: 'INDIVIDUAL' | 'CORPORATE';
  temporaryPasswordSent: boolean;
  emailDeliveryChannel: 'LOCAL_MAILHOG' | 'EXTERNAL_SMTP';
  emailRelay: string;
}

export type BankAccountStatus = 'ACTIVE' | 'CLOSED';

export interface AccountStatusResponse {
  hasAccount: boolean;
  accountId: number | null;
  iban: string | null;
  balance: number | null;
  status: BankAccountStatus | null;
}

export interface AccountOpeningResponse {
  created: boolean;
  accountId: number;
  iban: string;
  balance: number;
  status: BankAccountStatus;
  message: string;
}

