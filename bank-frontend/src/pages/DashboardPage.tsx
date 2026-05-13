import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { getCustomerAccountStatus, openCustomerAccount } from '../api/account';
import { extractApiErrorMessage } from '../api/http';
import type { AccountStatusResponse } from '../types/auth';
import { clearSession, getSession } from '../utils/authStorage';
import '../index.css';

type LoanApplicationDraft = {
  loanType: 'CONSUMER' | 'MORTGAGE';
  principalAmount: string;
  repaymentTermMonths: string;
};

const loanProductLimits = {
  CONSUMER: {
    label: 'Consumer loan',
    baseRate: 5.95,
    minimumRate: 5.2,
    maximumRate: 6.7,
    minimumPrincipalAmount: 1_000,
    maximumPrincipalAmount: 40_000,
    principalStepAmount: 5,
    minimumRepaymentTermMonths: 12,
    maximumRepaymentTermMonths: 120,
    monthlyServiceFee: 2.5,
    upfrontFees: {
      analysis: 0,
      collateralAssessment: 0,
    },
  },
  MORTGAGE: {
    label: 'Mortgage loan',
    baseRate: 2.85,
    minimumRate: 2.25,
    maximumRate: 3.45,
    minimumPrincipalAmount: 3_000,
    maximumPrincipalAmount: 500_000,
    principalStepAmount: 500,
    minimumRepaymentTermMonths: null,
    maximumRepaymentTermMonths: 360,
    monthlyServiceFee: 10,
    upfrontFees: {
      analysis: 200,
      collateralAssessment: 100,
    },
  },
} as const;

const monthsInYear = 12;
type LoanProductLimits = typeof loanProductLimits[keyof typeof loanProductLimits];

function formatMoney(value: number | null | undefined) {
  return `${(value ?? 0).toLocaleString('bg-BG', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })} EUR`;
}

function formatOptionalMoney(value: number | null) {
  return value === null ? '-' : formatMoney(value);
}

function formatAmountLimits(product: LoanProductLimits) {
  return `${formatMoney(product.minimumPrincipalAmount)} - ${formatMoney(product.maximumPrincipalAmount)}`;
}

function formatTermLimits(product: LoanProductLimits) {
  if (product.minimumRepaymentTermMonths === null) {
    return `No product minimum, up to ${product.maximumRepaymentTermMonths} months`;
  }

  return `${product.minimumRepaymentTermMonths} - ${product.maximumRepaymentTermMonths} months`;
}

function getLoanApplicationValidationMessage(loanApplicationDraft: LoanApplicationDraft) {
  const product = loanProductLimits[loanApplicationDraft.loanType];
  const principalAmount = Number(loanApplicationDraft.principalAmount);
  const repaymentTermMonths = Number(loanApplicationDraft.repaymentTermMonths);

  if (!Number.isFinite(principalAmount) || principalAmount <= 0) {
    return 'Enter a valid loan amount.';
  }

  if (principalAmount < product.minimumPrincipalAmount) {
    return `Minimum amount for ${product.label.toLowerCase()} is ${formatMoney(product.minimumPrincipalAmount)}.`;
  }

  if (principalAmount > product.maximumPrincipalAmount) {
    return `Maximum amount for ${product.label.toLowerCase()} is ${formatMoney(product.maximumPrincipalAmount)}.`;
  }

  const stepRemainder = (principalAmount - product.minimumPrincipalAmount) % product.principalStepAmount;
  if (Math.abs(stepRemainder) > 0.000001 && Math.abs(stepRemainder - product.principalStepAmount) > 0.000001) {
    return `Amount must increase in steps of ${formatMoney(product.principalStepAmount)} for ${product.label.toLowerCase()}.`;
  }

  if (!Number.isInteger(repaymentTermMonths) || repaymentTermMonths <= 0) {
    return 'Enter a valid repayment term in months.';
  }

  if (
    product.minimumRepaymentTermMonths !== null
    && repaymentTermMonths < product.minimumRepaymentTermMonths
  ) {
    return `Minimum term for ${product.label.toLowerCase()} is ${product.minimumRepaymentTermMonths} months.`;
  }

  if (repaymentTermMonths > product.maximumRepaymentTermMonths) {
    return `Maximum term for ${product.label.toLowerCase()} is ${product.maximumRepaymentTermMonths} months.`;
  }

  return null;
}

function formatCustomerReference(customerId: number | null | undefined) {
  if (!customerId) {
    return 'Not assigned';
  }

  return `BK-${customerId.toString().padStart(6, '0')}`;
}

function calculateEstimatedAnnualInterestRate(loanApplicationDraft: LoanApplicationDraft) {
  const product = loanProductLimits[loanApplicationDraft.loanType];
  const principalAmount = Number(loanApplicationDraft.principalAmount);
  const repaymentTermMonths = Number(loanApplicationDraft.repaymentTermMonths);

  if (!Number.isFinite(principalAmount) || !Number.isFinite(repaymentTermMonths)) {
    return product.baseRate;
  }

  const amountRange = product.maximumPrincipalAmount - product.minimumPrincipalAmount;
  const amountUtilization = Math.min(Math.max((principalAmount - product.minimumPrincipalAmount) / amountRange, 0), 1);
  const minimumTerm = product.minimumRepaymentTermMonths ?? 1;
  const termRange = product.maximumRepaymentTermMonths - minimumTerm;
  const termUtilization = Math.min(Math.max((repaymentTermMonths - minimumTerm) / termRange, 0), 1);

  if (loanApplicationDraft.loanType === 'MORTGAGE') {
    const mortgageRiskScore = Math.min(termUtilization * 0.8 + (1 - amountUtilization) * 0.2, 1);
    return product.minimumRate + (product.maximumRate - product.minimumRate) * mortgageRiskScore;
  }

  const consumerRiskScore = Math.min((1 - amountUtilization) * 0.65 + (1 - termUtilization) * 0.35, 1);
  return product.minimumRate + (product.maximumRate - product.minimumRate) * consumerRiskScore;
}

function calculateMonthlyPayment(principalAmount: number, annualInterestRate: number, repaymentTermMonths: number) {
  const monthlyInterestRate = annualInterestRate / 100 / monthsInYear;

  if (monthlyInterestRate === 0) {
    return principalAmount / repaymentTermMonths;
  }

  const compoundFactor = (1 + monthlyInterestRate) ** repaymentTermMonths;
  return principalAmount * monthlyInterestRate * compoundFactor / (compoundFactor - 1);
}

function calculateApr(
  principalAmount: number,
  monthlyPaymentWithFee: number,
  repaymentTermMonths: number,
  upfrontFees: number
) {
  const netReceivedAmount = principalAmount - upfrontFees;

  if (netReceivedAmount <= 0) {
    return 0;
  }

  let low = 0;
  let high = 1;

  for (let iteration = 0; iteration < 80; iteration += 1) {
    const middle = (low + high) / 2;
    let presentValue = 0;

    for (let month = 1; month <= repaymentTermMonths; month += 1) {
      presentValue += monthlyPaymentWithFee / ((1 + middle) ** month);
    }

    if (presentValue > netReceivedAmount) {
      low = middle;
    } else {
      high = middle;
    }
  }

  return (((1 + (low + high) / 2) ** monthsInYear) - 1) * 100;
}

export function DashboardPage() {
  const navigate = useNavigate();
  const session = getSession();
  const customerType = (session as (Record<string, unknown> | null))?.customerType;
  const customerDisplayName = session?.displayName?.trim() || session?.email || 'BankAI client';
  const roleLabel = session?.role === 'EMPLOYEE'
    ? 'EMPLOYEE'
    : (customerType === 'INDIVIDUAL' || customerType === 'CORPORATE' ? customerType : 'CUSTOMER');

  const [accountStatus, setAccountStatus] = useState<AccountStatusResponse | null>(null);
  const [isLoadingStatus, setIsLoadingStatus] = useState(session?.role === 'CUSTOMER');
  const [hasLoadedStatus, setHasLoadedStatus] = useState(session?.role !== 'CUSTOMER');
  const [isOpeningAccount, setIsOpeningAccount] = useState(false);
  const [accountError, setAccountError] = useState<string | null>(null);
  const [accountSuccess, setAccountSuccess] = useState<string | null>(null);
  const [isLoanApplicationOpen, setIsLoanApplicationOpen] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const [loanApplicationDraft, setLoanApplicationDraft] = useState<LoanApplicationDraft>({
    loanType: 'CONSUMER',
    principalAmount: '12000',
    repaymentTermMonths: '24',
  });

  const [loanApplicationMessage, setLoanApplicationMessage] = useState<string | null>(null);
  const [isLogoAvailable, setIsLogoAvailable] = useState(true);

  const isAccountPanelLoading = !hasLoadedStatus || isLoadingStatus;
  const hasActiveAccount = accountStatus?.hasAccount && accountStatus.status === 'ACTIVE';
  const selectedLoanProduct = loanProductLimits[loanApplicationDraft.loanType];

  const loanApplicationValidationMessage = useMemo(
    () => getLoanApplicationValidationMessage(loanApplicationDraft),
    [loanApplicationDraft]
  );

  const estimatedAnnualInterestRate = useMemo(
    () => loanApplicationValidationMessage ? null : calculateEstimatedAnnualInterestRate(loanApplicationDraft),
    [loanApplicationDraft, loanApplicationValidationMessage]
  );

  const loanCalculation = useMemo(() => {
    const principalAmount = Number(loanApplicationDraft.principalAmount);
    const repaymentTermMonths = Number(loanApplicationDraft.repaymentTermMonths);

    if (
      loanApplicationValidationMessage
      || estimatedAnnualInterestRate === null
      || !Number.isFinite(principalAmount)
      || !Number.isFinite(repaymentTermMonths)
      || repaymentTermMonths <= 0
    ) {
      return {
        monthlyPayment: null,
        monthlyPaymentWithFee: null,
        totalInterest: null,
        totalDueAmount: null,
        apr: null,
      };
    }

    const monthlyPayment = calculateMonthlyPayment(
      principalAmount,
      estimatedAnnualInterestRate,
      repaymentTermMonths
    );
    const monthlyPaymentWithFee = monthlyPayment + selectedLoanProduct.monthlyServiceFee;
    const totalInterest = monthlyPayment * repaymentTermMonths - principalAmount;
    const upfrontFees = selectedLoanProduct.upfrontFees.analysis + selectedLoanProduct.upfrontFees.collateralAssessment;
    const totalDueAmount = monthlyPaymentWithFee * repaymentTermMonths + upfrontFees;
    const apr = calculateApr(principalAmount, monthlyPaymentWithFee, repaymentTermMonths, upfrontFees);

    return {
      monthlyPayment,
      monthlyPaymentWithFee,
      totalInterest,
      totalDueAmount,
      apr,
    };
  }, [estimatedAnnualInterestRate, loanApplicationDraft.principalAmount, loanApplicationDraft.repaymentTermMonths, loanApplicationValidationMessage, selectedLoanProduct]);

  useEffect(() => {
    if (!session || session.role !== 'CUSTOMER') {
      return;
    }

    async function loadAccountStatus() {
      setIsLoadingStatus(true);
      setAccountError(null);

      try {
        const response = await getCustomerAccountStatus();
        setAccountStatus(response);
      } catch (error) {
        setAccountError(extractApiErrorMessage(error, 'Failed to load account status.'));
      } finally {
        setIsLoadingStatus(false);
        setHasLoadedStatus(true);
      }
    }

    void loadAccountStatus();
  }, [session?.customerId, session?.role]);

  function handleLogout() {
    clearSession();
    navigate('/login', { replace: true });
  }

  async function handleOpenAccount() {
    setIsOpeningAccount(true);
    setAccountError(null);
    setAccountSuccess(null);

    try {
      const response = await openCustomerAccount();
      setAccountStatus({
        hasAccount: true,
        accountId: response.accountId,
        iban: response.iban,
        balance: response.balance,
        status: response.status,
      });
      setAccountSuccess(response.message);
    } catch (error) {
      setAccountError(extractApiErrorMessage(error, 'Bank account could not be created.'));
    } finally {
      setIsOpeningAccount(false);
    }
  }

  function updateLoanApplicationDraft(field: keyof LoanApplicationDraft, value: string) {
    setLoanApplicationDraft((currentDraft) => ({
      ...currentDraft,
      [field]: value,
    }));
    setLoanApplicationMessage(null);
  }

  function handleAmountChange(delta: number) {
    const current = Number(loanApplicationDraft.principalAmount) || 0;
    const step = selectedLoanProduct.principalStepAmount;
    let next = current + (delta * step);
    if (next < selectedLoanProduct.minimumPrincipalAmount) next = selectedLoanProduct.minimumPrincipalAmount;
    if (next > selectedLoanProduct.maximumPrincipalAmount) next = selectedLoanProduct.maximumPrincipalAmount;
    updateLoanApplicationDraft('principalAmount', next.toString());
  }

  function handleTermChange(delta: number) {
    const current = Number(loanApplicationDraft.repaymentTermMonths) || 0;
    const min = selectedLoanProduct.minimumRepaymentTermMonths ?? 1;
    const max = selectedLoanProduct.maximumRepaymentTermMonths;
    let next = current + delta;
    if (next < min) next = min;
    if (next > max) next = max;
    updateLoanApplicationDraft('repaymentTermMonths', next.toString());
  }

  function handleLoanApplicationSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (loanApplicationValidationMessage) {
      setLoanApplicationMessage(loanApplicationValidationMessage);
      return;
    }

    setLoanApplicationMessage('Loan request prepared for employee review. The approval workflow will be connected next.');
  }

  return (
    <div className="bank-dashboard-shell">
      <style>
        {`
          .custom-nav-pill {
            padding: 0.5rem 1.2rem;
            border-radius: 20px;
            text-decoration: none;
            color: #f1f5f9;
            font-weight: 500;
            transition: all 0.3s ease;
            display: inline-block;
          }
          .custom-nav-pill:hover {
            transform: translateY(-2px);
            color: white;
          }
          .custom-nav-pill-blue-light {
            background-color: rgba(59, 130, 246, 0.1);
            color: #60a5fa;
          }
          .custom-nav-pill-blue-light:hover {
            background-color: rgba(59, 130, 246, 0.2);
            color: #93c5fd;
          }
          .custom-dropdown-container {
            position: relative;
            width: 100%;
          }
          .custom-dropdown-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 8px;
            padding: 12px 16px;
            color: white;
            cursor: pointer;
            font-size: 1rem;
            transition: border-color 0.2s;
          }
          .custom-dropdown-header:hover {
            border-color: rgba(255, 255, 255, 0.3);
          }
          .custom-dropdown-list {
            position: absolute;
            top: calc(100% + 4px);
            left: 0;
            width: 100%;
            background: #1e293b;
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 8px;
            z-index: 50;
            overflow: hidden;
            box-shadow: 0 10px 25px rgba(0,0,0,0.5);
          }
          .custom-dropdown-item {
            padding: 12px 16px;
            color: white;
            cursor: pointer;
            transition: background 0.2s;
          }
          .custom-dropdown-item:hover {
            background: rgba(255, 255, 255, 0.1);
          }
          .custom-number-input-wrapper {
            display: flex;
            align-items: center;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 8px;
            transition: border-color 0.2s;
            overflow: hidden;
          }
          .custom-number-input-wrapper:focus-within {
            border-color: #3b82f6;
          }
          .custom-number-input-wrapper input {
            flex: 1;
            background: transparent;
            border: none;
            color: white;
            padding: 12px 16px;
            font-size: 1rem;
            outline: none;
            -moz-appearance: textfield;
          }
          .custom-number-input-wrapper input::-webkit-inner-spin-button,
          .custom-number-input-wrapper input::-webkit-outer-spin-button {
            -webkit-appearance: none;
            margin: 0;
          }
          .custom-suffix {
            color: #94a3b8;
            padding-right: 12px;
            font-size: 0.9rem;
          }
          .custom-spinners {
            display: flex;
            flex-direction: column;
            border-left: 1px solid rgba(255, 255, 255, 0.1);
          }
          .custom-spinner-btn {
            background: transparent;
            border: none;
            color: #94a3b8;
            padding: 4px 12px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background 0.2s, color 0.2s;
          }
          .custom-spinner-btn:hover {
            background: rgba(255, 255, 255, 0.1);
            color: white;
          }
          .custom-spinner-btn:first-child {
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
          }
        `}
      </style>

      <header className="bank-dashboard-topbar" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>

        <div style={{ display: 'flex', alignItems: 'center', gap: '3rem' }}>
          <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '1rem', textDecoration: 'none' }}>
            {isLogoAvailable ? (
              <img
                src="/bankai-logo.png"
                alt="BANKλI"
                style={{ width: '60px', height: 'auto', background: 'transparent' }}
                onError={() => setIsLogoAvailable(false)}
              />
            ) : (
              <span style={{ fontSize: '2rem', fontWeight: 'bold', color: '#3b82f6' }}>Bλ</span>
            )}
            <div>
              <span style={{ fontSize: '1.8rem', fontWeight: 'bold', color: 'white' }}>BANKλI</span>
            </div>
          </Link>


        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <div style={{ textAlign: 'right' }}>
            <span style={{ display: 'block', color: 'white', fontSize: '0.95rem', fontWeight: '500' }}>
              {customerDisplayName}
            </span>
            <strong style={{ display: 'block', color: '#3b82f6', fontSize: '0.75rem', letterSpacing: '1px', textTransform: 'uppercase' }}>
              {roleLabel} CLIENT
            </strong>
          </div>
          <button
            type="button"
            className="bank-ghost-button"
            onClick={handleLogout}
            style={{ padding: '0.6rem 1.5rem', fontSize: '0.95rem' }}
          >
            Logout
          </button>
        </div>

      </header>

      <main className="bank-dashboard-main">
        <section className="bank-hero-panel">
          <div>
            <p className="bank-section-kicker">Private banking dashboard</p>
            <h1>Your accounts and credit services in one place</h1>
            <p className="bank-hero-copy">
              Monitor your current account, prepare financing requests, and follow the next banking operations from a focused workspace.
            </p>
          </div>
        </section>

        <section className="bank-content-grid">
          <article className={`bank-panel bank-account-panel ${isAccountPanelLoading ? 'account-status-panel-loading' : ''}`.trim()}>
            <div className="bank-panel-heading">
              <div>
                <p className="bank-section-kicker">Primary account</p>
                <h2>Current account</h2>
              </div>
            </div>

            {isAccountPanelLoading ? (
              <div className="account-status-skeleton" aria-label="Loading account status">
                <div className="skeleton-line skeleton-line-short"></div>
                <div className="skeleton-line"></div>
                <div className="skeleton-line skeleton-line-medium"></div>
              </div>
            ) : accountStatus?.hasAccount ? (
              <div className="bank-account-overview">
                <div className="bank-account-balance-card">
                  <span>Available balance</span>
                  <strong>{formatMoney(accountStatus.balance)}</strong>
                  <small>{formatCustomerReference(session?.customerId)}</small>
                </div>

                <div className="bank-account-meta-grid">
                  <div>
                    <span>IBAN</span>
                    <strong>{accountStatus.iban}</strong>
                  </div>
                  <div>
                    <span>Currency</span>
                    <strong>EUR</strong>
                  </div>
                  <div>
                    <span>Account package</span>
                    <strong>Standard digital</strong>
                  </div>
                </div>
              </div>
            ) : (
              <div className="bank-empty-state">
                <p>You do not have an opened bank account yet.</p>
                <button
                  type="button"
                  className="bank-primary-button"
                  onClick={handleOpenAccount}
                  disabled={isOpeningAccount}
                >
                  {isOpeningAccount ? 'Opening account...' : 'Open bank account'}
                </button>
              </div>
            )}

            {accountSuccess && <p className="status-success-text">{accountSuccess}</p>}
            {accountError && <p className="status-error-text">{accountError}</p>}
          </article>

          <aside className="bank-side-stack">
            <article className="bank-panel bank-profile-panel">
              <p className="bank-section-kicker">Customer profile</p>
              <div className="bank-profile-list">
                <div>
                  <span>Email</span>
                  <strong>{session?.email}</strong>
                </div>
                <div>
                  <span>Type</span>
                  <strong>{roleLabel}</strong>
                </div>
              </div>
            </article>

            <article className="bank-panel bank-credit-panel">
              <div className="bank-panel-heading">
                <div>
                  <p className="bank-section-kicker">Credit services</p>
                  <h2>Request financing</h2>
                </div>
              </div>

              <p className="bank-panel-copy">
                Choose loan parameters and prepare a request for employee review.
              </p>

              <div className="bank-rate-row">
                <span>Consumer 5.20% - 6.70%</span>
                <span>Mortgage 2.25% - 3.45%</span>
              </div>

              <div className="bank-product-limit-grid">
                <div>
                  <span>Consumer amount</span>
                  <strong>{formatAmountLimits(loanProductLimits.CONSUMER)}</strong>
                  <small>{formatTermLimits(loanProductLimits.CONSUMER)}</small>
                </div>
                <div>
                  <span>Mortgage amount</span>
                  <strong>{formatAmountLimits(loanProductLimits.MORTGAGE)}</strong>
                  <small>{formatTermLimits(loanProductLimits.MORTGAGE)}</small>
                </div>
              </div>

              <button
                type="button"
                className="bank-primary-button bank-full-width-button"
                onClick={() => setIsLoanApplicationOpen(true)}
                disabled={!hasActiveAccount}
              >
                Request loan
              </button>

              {!hasActiveAccount && !isAccountPanelLoading && (
                <p className="bank-panel-note">Open an account before submitting a credit request.</p>
              )}
            </article>

            <article className="bank-panel bank-next-panel">
              <p className="bank-section-kicker">Coming next</p>
              <ul className="bank-next-list">
                <li>Repayment schedule</li>
                <li>Mark installment paid</li>
                <li>Loan status check</li>
              </ul>
            </article>
          </aside>
        </section>
      </main>

      {isLoanApplicationOpen && (
        <div className="bank-modal-backdrop" role="presentation">
          <section className="bank-loan-modal" role="dialog" aria-modal="true" aria-labelledby="loan-request-title">
            <div className="bank-panel-heading">
              <div>
                <p className="bank-section-kicker">Credit request</p>
                <h2 id="loan-request-title">Request a loan</h2>
              </div>
              <button
                type="button"
                className="bank-icon-button"
                aria-label="Close loan request"
                onClick={() => setIsLoanApplicationOpen(false)}
              >
                x
              </button>
            </div>

            <form className="bank-loan-form" onSubmit={handleLoanApplicationSubmit} noValidate>
              <label className="form-field">
                <span className="form-label">Loan type</span>
                <div className="custom-dropdown-container">
                  <div className="custom-dropdown-header" onClick={() => setIsDropdownOpen(!isDropdownOpen)}>
                    {selectedLoanProduct.label}
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="6 9 12 15 18 9"></polyline>
                    </svg>
                  </div>
                  {isDropdownOpen && (
                    <div className="custom-dropdown-list">
                      <div className="custom-dropdown-item" onClick={() => { updateLoanApplicationDraft('loanType', 'CONSUMER'); setIsDropdownOpen(false); }}>
                        Consumer loan
                      </div>
                      <div className="custom-dropdown-item" onClick={() => { updateLoanApplicationDraft('loanType', 'MORTGAGE'); setIsDropdownOpen(false); }}>
                        Mortgage loan
                      </div>
                    </div>
                  )}
                </div>
              </label>

              <div className="bank-selected-product-terms">
                <div>
                  <span>Allowed amount</span>
                  <strong>{formatAmountLimits(selectedLoanProduct)}</strong>
                </div>
                <div>
                  <span>Allowed term</span>
                  <strong>{formatTermLimits(selectedLoanProduct)}</strong>
                </div>
              </div>

              <label className="form-field">
                <span className="form-label">Amount</span>
                <div className="custom-number-input-wrapper">
                  <input
                    type="number"
                    value={loanApplicationDraft.principalAmount}
                    onChange={(event) => updateLoanApplicationDraft('principalAmount', event.target.value)}
                  />
                  <span className="custom-suffix">EUR</span>
                  <div className="custom-spinners">
                    <button type="button" className="custom-spinner-btn" onClick={() => handleAmountChange(1)}>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="18 15 12 9 6 15"></polyline></svg>
                    </button>
                    <button type="button" className="custom-spinner-btn" onClick={() => handleAmountChange(-1)}>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
                    </button>
                  </div>
                </div>
                <p className="bank-field-hint">Allowed: {formatAmountLimits(selectedLoanProduct)}. Step: {formatMoney(selectedLoanProduct.principalStepAmount)}.</p>
              </label>

              <label className="form-field">
                <span className="form-label">Term in months</span>
                <div className="custom-number-input-wrapper">
                  <input
                    type="number"
                    value={loanApplicationDraft.repaymentTermMonths}
                    onChange={(event) => updateLoanApplicationDraft('repaymentTermMonths', event.target.value)}
                  />
                  <span className="custom-suffix">mo.</span>
                  <div className="custom-spinners">
                    <button type="button" className="custom-spinner-btn" onClick={() => handleTermChange(1)}>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="18 15 12 9 6 15"></polyline></svg>
                    </button>
                    <button type="button" className="custom-spinner-btn" onClick={() => handleTermChange(-1)}>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
                    </button>
                  </div>
                </div>
                <p className="bank-field-hint">Allowed: {formatTermLimits(selectedLoanProduct)}.</p>
              </label>

              <label className="form-field">
                <span className="form-label form-label-accent">Interest rate</span>
                <div className="bank-calculator-input bank-calculator-input-accent">
                  <input
                    type="number"
                    value={estimatedAnnualInterestRate === null ? '' : estimatedAnnualInterestRate.toFixed(2)}
                    placeholder="-"
                    readOnly
                  />
                  <span>%</span>
                </div>
              </label>

              <div className="bank-calculator-result">
                <div className="bank-calculator-result-head">
                  <div>
                    <span>Monthly installment</span>
                    <strong>{formatOptionalMoney(loanCalculation.monthlyPayment)}</strong>
                  </div>
                  <div>
                    <span>APR (ГПР)</span>
                    <strong>{loanCalculation.apr === null ? '-' : `${loanCalculation.apr.toFixed(2)}%`}</strong>
                  </div>
                </div>

                <dl className="bank-calculator-breakdown">
                  <div>
                    <dt>Credit term</dt>
                    <dd>{loanCalculation.apr === null ? '-' : `${Number(loanApplicationDraft.repaymentTermMonths)} mo.`}</dd>
                  </div>
                  <div>
                    <dt>Total due amount</dt>
                    <dd>{formatOptionalMoney(loanCalculation.totalDueAmount)}</dd>
                  </div>
                  <div>
                    <dt>Total interest</dt>
                    <dd>{formatOptionalMoney(loanCalculation.totalInterest)}</dd>
                  </div>
                  <div>
                    <dt>Interest rate</dt>
                    <dd>{estimatedAnnualInterestRate === null ? '-' : `${estimatedAnnualInterestRate.toFixed(2)}%`}</dd>
                  </div>
                  <div>
                    <dt>Monthly service fee</dt>
                    <dd>{formatMoney(selectedLoanProduct.monthlyServiceFee)}</dd>
                  </div>
                  {selectedLoanProduct.upfrontFees.analysis > 0 && (
                    <div>
                      <dt>Check and analysis fee</dt>
                      <dd>{formatMoney(selectedLoanProduct.upfrontFees.analysis)}</dd>
                    </div>
                  )}
                  {selectedLoanProduct.upfrontFees.collateralAssessment > 0 && (
                    <div>
                      <dt>Collateral assessment fee</dt>
                      <dd>{formatMoney(selectedLoanProduct.upfrontFees.collateralAssessment)}</dd>
                    </div>
                  )}
                </dl>
              </div>

              {(loanApplicationMessage || loanApplicationValidationMessage) && (
                <p className="bank-form-message">{loanApplicationMessage || loanApplicationValidationMessage}</p>
              )}

              <div className="bank-form-actions">
                <button type="button" className="bank-ghost-button" onClick={() => setIsLoanApplicationOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="bank-primary-button" disabled={Boolean(loanApplicationValidationMessage)}>
                  Submit request
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}