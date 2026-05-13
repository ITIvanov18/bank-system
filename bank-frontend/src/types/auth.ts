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
  outstandingDebtAmount: number | null;
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

export type LoanType = 'CONSUMER' | 'MORTGAGE';
export type LoanStatus = 'PENDING' | 'ACTIVE' | 'REJECTED' | 'CLOSED';
export type LoanReviewDecision = 'APPROVED' | 'REJECTED';

export interface LoanApplicationRequest {
  loanType: LoanType;
  principalAmount: number;
  repaymentTermMonths: number;
}

export interface InstallmentResponse {
  installmentId: number;
  installmentNumber: number;
  dueDate: string;
  monthlyInstallmentAmount: number;
  principalPart: number;
  interestPart: number;
  remainingBalance: number;
  status: 'PENDING' | 'PAID' | 'OVERDUE';
}

export interface LoanApplicationResponse {
  loanId: number;
  customerId: number;
  customerEmail: string | null;
  customerDisplayName: string | null;
  customerType: CustomerType | null;
  loanType: LoanType;
  principalAmount: number;
  annualInterestRate: number;
  repaymentTermMonths: number;
  status: LoanStatus;
  startDate: string | null;
  reviewedAt: string | null;
  monthlyInstallmentAmount: number;
  repaymentSchedule: InstallmentResponse[];
  message: string;
}

export interface LoanReviewRequest {
  decisionNote?: string;
}

export interface LoanReviewHistoryItem {
  logId: number;
  loanId: number;
  customerId: number;
  customerEmail: string;
  employeeEmail: string;
  decision: LoanReviewDecision;
  loanType: LoanType;
  principalAmount: number;
  annualInterestRate: number;
  repaymentTermMonths: number;
  decisionNote: string | null;
  decidedAt: string;
}

