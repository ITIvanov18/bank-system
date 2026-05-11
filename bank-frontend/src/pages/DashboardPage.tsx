import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
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
    minimumRepaymentTermMonths: 18,
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

function formatMoney(value: number | null | undefined) {
  return `${(value ?? 0).toLocaleString('bg-BG', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })} EUR`;
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
  const estimatedAnnualInterestRate = useMemo(
    () => calculateEstimatedAnnualInterestRate(loanApplicationDraft),
    [loanApplicationDraft]
  );
  const loanCalculation = useMemo(() => {
    const principalAmount = Number(loanApplicationDraft.principalAmount);
    const repaymentTermMonths = Number(loanApplicationDraft.repaymentTermMonths);

    if (!Number.isFinite(principalAmount) || !Number.isFinite(repaymentTermMonths) || repaymentTermMonths <= 0) {
      return {
        monthlyPayment: 0,
        monthlyPaymentWithFee: selectedLoanProduct.monthlyServiceFee,
        totalInterest: 0,
        totalDueAmount: selectedLoanProduct.upfrontFees.analysis + selectedLoanProduct.upfrontFees.collateralAssessment,
        apr: 0,
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
  }, [estimatedAnnualInterestRate, loanApplicationDraft.principalAmount, loanApplicationDraft.repaymentTermMonths, selectedLoanProduct]);

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

  function handleLoanApplicationSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const principalAmount = Number(loanApplicationDraft.principalAmount);
    const repaymentTermMonths = Number(loanApplicationDraft.repaymentTermMonths);

    if (!Number.isFinite(principalAmount) || principalAmount <= 0) {
      setLoanApplicationMessage('Enter a valid loan amount.');
      return;
    }

    if (principalAmount < selectedLoanProduct.minimumPrincipalAmount) {
      setLoanApplicationMessage(`Minimum amount for ${selectedLoanProduct.label.toLowerCase()} is ${formatMoney(selectedLoanProduct.minimumPrincipalAmount)}.`);
      return;
    }

    if (!Number.isInteger(repaymentTermMonths) || repaymentTermMonths <= 0) {
      setLoanApplicationMessage('Enter a valid repayment term in months.');
      return;
    }

    if (
      selectedLoanProduct.minimumRepaymentTermMonths !== null
      && repaymentTermMonths < selectedLoanProduct.minimumRepaymentTermMonths
    ) {
      setLoanApplicationMessage(`Minimum term for ${selectedLoanProduct.label.toLowerCase()} is ${selectedLoanProduct.minimumRepaymentTermMonths} months.`);
      return;
    }

    if (principalAmount > selectedLoanProduct.maximumPrincipalAmount) {
      setLoanApplicationMessage(`Maximum amount for ${selectedLoanProduct.label.toLowerCase()} is ${formatMoney(selectedLoanProduct.maximumPrincipalAmount)}.`);
      return;
    }

    const stepRemainder = (principalAmount - selectedLoanProduct.minimumPrincipalAmount) % selectedLoanProduct.principalStepAmount;
    if (Math.abs(stepRemainder) > 0.000001 && Math.abs(stepRemainder - selectedLoanProduct.principalStepAmount) > 0.000001) {
      setLoanApplicationMessage(`Amount must increase in steps of ${formatMoney(selectedLoanProduct.principalStepAmount)} for ${selectedLoanProduct.label.toLowerCase()}.`);
      return;
    }

    if (repaymentTermMonths > selectedLoanProduct.maximumRepaymentTermMonths) {
      setLoanApplicationMessage(`Maximum term for ${selectedLoanProduct.label.toLowerCase()} is ${selectedLoanProduct.maximumRepaymentTermMonths} months.`);
      return;
    }

    setLoanApplicationMessage('Loan request prepared for employee review. The approval workflow will be connected next.');
  }

  return (
    <div className="bank-dashboard-shell">
      <header className="bank-dashboard-topbar">
        <div className="bank-dashboard-brand">
          <span className="bank-brand-logo-frame">
            {isLogoAvailable ? (
              <img
                className="bank-brand-logo"
                src="/bankai-logo.png"
                alt="BankAI"
                onError={() => setIsLogoAvailable(false)}
              />
            ) : (
              <span className="bank-brand-mark">B</span>
            )}
          </span>
          <div>
            <p className="bank-brand-name">BankAI Online Banking</p>
            <p className="bank-brand-subtitle">Private digital banking workspace</p>
          </div>
        </div>

        <div className="bank-dashboard-user">
          <div className="bank-user-copy">
            <span>{customerDisplayName}</span>
            <strong>{roleLabel} CLIENT</strong>
          </div>
          <button type="button" className="bank-ghost-button" onClick={handleLogout}>
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

            <form className="bank-loan-form" onSubmit={handleLoanApplicationSubmit}>
              <label className="form-field">
                <span className="form-label">Loan type</span>
                <select
                  className="glass-input bank-calculator-select"
                  value={loanApplicationDraft.loanType}
                  onChange={(event) => updateLoanApplicationDraft('loanType', event.target.value as LoanApplicationDraft['loanType'])}
                >
                  <option value="CONSUMER">Consumer loan</option>
                  <option value="MORTGAGE">Mortgage loan</option>
                </select>
              </label>

              <label className="form-field">
                <span className="form-label">Amount</span>
                <div className="bank-calculator-input">
                  <input
                    type="number"
                    min={selectedLoanProduct.minimumPrincipalAmount}
                    max={selectedLoanProduct.maximumPrincipalAmount}
                    step={selectedLoanProduct.principalStepAmount}
                    value={loanApplicationDraft.principalAmount}
                    onChange={(event) => updateLoanApplicationDraft('principalAmount', event.target.value)}
                  />
                  <span>EUR</span>
                </div>
              </label>

              <label className="form-field">
                <span className="form-label">Term in months</span>
                <div className="bank-calculator-input">
                  <input
                    type="number"
                    min={selectedLoanProduct.minimumRepaymentTermMonths ?? 1}
                    max={selectedLoanProduct.maximumRepaymentTermMonths}
                    step="1"
                    value={loanApplicationDraft.repaymentTermMonths}
                    onChange={(event) => updateLoanApplicationDraft('repaymentTermMonths', event.target.value)}
                  />
                  <span>mo.</span>
                </div>
              </label>

              <label className="form-field">
                <span className="form-label form-label-accent">Interest rate</span>
                <div className="bank-calculator-input bank-calculator-input-accent">
                  <input
                    type="number"
                    value={estimatedAnnualInterestRate.toFixed(2)}
                    readOnly
                  />
                  <span>%</span>
                </div>
              </label>

              <div className="bank-calculator-result">
                <div className="bank-calculator-result-head">
                  <div>
                    <span>Monthly installment</span>
                    <strong>{formatMoney(loanCalculation.monthlyPayment)}</strong>
                  </div>
                  <div>
                    <span>APR (ГПР)</span>
                    <strong>{loanCalculation.apr.toFixed(2)}%</strong>
                  </div>
                </div>

                <dl className="bank-calculator-breakdown">
                  <div>
                    <dt>Credit term</dt>
                    <dd>{Number(loanApplicationDraft.repaymentTermMonths) || 0} mo.</dd>
                  </div>
                  <div>
                    <dt>Total due amount</dt>
                    <dd>{formatMoney(loanCalculation.totalDueAmount)}</dd>
                  </div>
                  <div>
                    <dt>Total interest</dt>
                    <dd>{formatMoney(loanCalculation.totalInterest)}</dd>
                  </div>
                  <div>
                    <dt>Interest rate</dt>
                    <dd>{estimatedAnnualInterestRate.toFixed(2)}%</dd>
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

              {loanApplicationMessage && (
                <p className="bank-form-message">{loanApplicationMessage}</p>
              )}

              <div className="bank-form-actions">
                <button type="button" className="bank-ghost-button" onClick={() => setIsLoanApplicationOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="bank-primary-button">
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
