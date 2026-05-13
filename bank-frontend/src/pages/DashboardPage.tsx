import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { getCustomerAccountStatus, openCustomerAccount } from '../api/account';
import { extractApiErrorMessage } from '../api/http';
import { getLatestCustomerLoanApplication, submitLoanApplication } from '../api/loan';
import type { AccountStatusResponse, CustomerLoanApplicationStatusResponse } from '../types/auth';
import { clearSession, getSession } from '../utils/authStorage';
import {
  loanProductLimits,
  formatMoney,
  formatDebt,
  useCountUp,
  formatOptionalMoney,
  formatAmountLimits,
  formatTermLimits,
  getLoanApplicationValidationMessage,
  formatCustomerReference,
  formatLoanType,
  formatLoanStatus,
  formatDateTime,
  calculateEstimatedAnnualInterestRate,
  calculateMonthlyPayment,
  calculateApr,
  type LoanApplicationDraft
} from './js/dashboardLogic';
import './css/DashboardPage.css';
import '../index.css';

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
  const [latestLoanApplication, setLatestLoanApplication] = useState<CustomerLoanApplicationStatusResponse | null>(null);
  const [isLoadingLoanApplication, setIsLoadingLoanApplication] = useState(session?.role === 'CUSTOMER');
  const [isSubmittingLoanApplication, setIsSubmittingLoanApplication] = useState(false);
  const [isLogoAvailable, setIsLogoAvailable] = useState(true);

  const isAccountPanelLoading = !hasLoadedStatus || isLoadingStatus;
  const hasActiveAccount = accountStatus?.hasAccount && accountStatus.status === 'ACTIVE';
  const hasPendingLoanApplication = latestLoanApplication?.status === 'PENDING';
  const selectedLoanProduct = loanProductLimits[loanApplicationDraft.loanType];
  const animatedBalance = useCountUp(accountStatus?.balance);
  const animatedOutstandingDebt = useCountUp(accountStatus?.outstandingDebtAmount);

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
      setIsLoadingLoanApplication(true);
      setAccountError(null);

      try {
        const [response, latestLoanApplicationResponse] = await Promise.all([
          getCustomerAccountStatus(),
          getLatestCustomerLoanApplication(),
        ]);
        setAccountStatus(response);
        setLatestLoanApplication(latestLoanApplicationResponse);
      } catch (error) {
        setAccountError(extractApiErrorMessage(error, 'Failed to load account status.'));
      } finally {
        setIsLoadingStatus(false);
        setIsLoadingLoanApplication(false);
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
        outstandingDebtAmount: 0,
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

  async function handleLoanApplicationSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (loanApplicationValidationMessage) {
      setLoanApplicationMessage(loanApplicationValidationMessage);
      return;
    }

    if (hasPendingLoanApplication) {
      setLoanApplicationMessage('You already have a loan application waiting for employee review.');
      return;
    }

    setIsSubmittingLoanApplication(true);
    setLoanApplicationMessage(null);

    try {
      const response = await submitLoanApplication({
        loanType: loanApplicationDraft.loanType,
        principalAmount: Number(loanApplicationDraft.principalAmount),
        repaymentTermMonths: Number(loanApplicationDraft.repaymentTermMonths),
      });
      setLatestLoanApplication(response);
      setIsLoanApplicationOpen(false);
      setLoanApplicationMessage('Your loan application was submitted for employee review.');
    } catch (submitError) {
      setLoanApplicationMessage(extractApiErrorMessage(submitError, 'Loan application could not be submitted.'));
    } finally {
      setIsSubmittingLoanApplication(false);
    }
  }

  return (
    <div className="bank-dashboard-shell">
      <header className="bank-dashboard-topbar">

        <div style={{ display: 'flex', alignItems: 'center', gap: '3rem' }}>
          <div className="bank-dashboard-brand" aria-label="BANKAI brand">
            {isLogoAvailable ? (
              <span className="bank-brand-logo-frame">
                <img
                  className="bank-brand-logo"
                  src="/bankai-logo.png"
                  alt="BANKΛI"
                  onError={() => setIsLogoAvailable(false)}
                />
              </span>
            ) : (
              <span className="bank-brand-mark">BΛ</span>
            )}
            <div>
              <p className="bank-brand-name">BANKΛI</p>
              <p className="bank-brand-subtitle">Premium AI FinTech</p>
            </div>
          </div>

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
                  <strong>{formatMoney(animatedBalance)}</strong>
                  <small>{formatCustomerReference(session?.customerId)}</small>
                </div>

                <div className={`bank-account-debt-card ${(accountStatus.outstandingDebtAmount ?? 0) > 0 ? 'bank-account-debt-card-active' : ''}`}>
                  <span>Outstanding debt</span>
                  <strong>{formatDebt(animatedOutstandingDebt)}</strong>
                  <small>Active loan principal</small>
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

              {loanApplicationMessage && !isLoanApplicationOpen && (
                <p className="bank-form-message">{loanApplicationMessage}</p>
              )}

              {isLoadingLoanApplication ? (
                <div className="bank-application-status bank-application-status-muted">
                  <span>Application status</span>
                  <strong>Loading...</strong>
                </div>
              ) : latestLoanApplication && (
                <div className={`bank-application-status bank-application-status-${latestLoanApplication.status.toLowerCase()}`}>
                  <div className="bank-application-status-head">
                    <span>Loan application</span>
                    <strong>{formatLoanStatus(latestLoanApplication.status)}</strong>
                  </div>
                  <div className="bank-application-status-grid">
                    <div>
                      <span>Product</span>
                      <strong>{formatLoanType(latestLoanApplication.loanType)}</strong>
                    </div>
                    <div>
                      <span>Amount</span>
                      <strong>{formatMoney(latestLoanApplication.principalAmount)}</strong>
                    </div>
                    <div>
                      <span>Submitted</span>
                      <strong>{formatDateTime(latestLoanApplication.submittedAt)}</strong>
                    </div>
                    <div>
                      <span>Indicative rate</span>
                      <strong>{latestLoanApplication.annualInterestRate.toFixed(2)}%</strong>
                    </div>
                  </div>
                  <p>{latestLoanApplication.message}</p>
                </div>
              )}

              <div className="bank-rate-row">
                <span>Consumer 5.20% - 6.70%</span>
                <span>Mortgage 2.65% - 3.45%</span>
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
                disabled={!hasActiveAccount || hasPendingLoanApplication}
              >
                {hasPendingLoanApplication ? 'Application pending' : 'Request loan'}
              </button>

              {!hasActiveAccount && !isAccountPanelLoading && (
                <p className="bank-panel-note">Open an account before submitting a credit request.</p>
              )}
              {hasPendingLoanApplication && (
                <p className="bank-panel-note">Wait for employee review before submitting another request.</p>
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
                <button
                  type="submit"
                  className="bank-primary-button"
                  disabled={Boolean(loanApplicationValidationMessage) || hasPendingLoanApplication || isSubmittingLoanApplication}
                >
                  {isSubmittingLoanApplication ? 'Submitting...' : 'Submit request'}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}