import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCustomerAccountStatus, openCustomerAccount } from '../api/account';
import { extractApiErrorMessage } from '../api/http';
import { getLatestCustomerLoanApplication, submitLoanApplication, getAllCustomerLoans } from '../api/loan';
import type { AccountStatusResponse, CustomerLoanApplicationStatusResponse, LoanApplicationResponse, InstallmentPaymentLogResponse } from '../types/auth';
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
  calculateFullLoanDetails,
  type LoanApplicationDraft
} from './js/dashboardLogic';
import './css/DashboardPage.css';
import './css/HomePage.css';
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
  const [customerLoans, setCustomerLoans] = useState<LoanApplicationResponse[]>([]);
  const [paymentLogs, setPaymentLogs] = useState<InstallmentPaymentLogResponse[]>([]);
  const [expandedLoanId, setExpandedLoanId] = useState<number | null>(null);
  const [isLoadingLoanApplication, setIsLoadingLoanApplication] = useState(session?.role === 'CUSTOMER');
  const [isSubmittingLoanApplication, setIsSubmittingLoanApplication] = useState(false);
  const [isRepayingMap, setIsRepayingMap] = useState<Record<number, boolean>>({});

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

  const loanCalculation = useMemo(
    () => calculateFullLoanDetails(loanApplicationDraft, estimatedAnnualInterestRate, selectedLoanProduct, loanApplicationValidationMessage),
    [loanApplicationDraft, estimatedAnnualInterestRate, selectedLoanProduct, loanApplicationValidationMessage]
  );

  useEffect(() => {
    if (!session || session.role !== 'CUSTOMER') return;
    async function loadAccountStatus() {
      setIsLoadingStatus(true);
      setIsLoadingLoanApplication(true);
      setAccountError(null);
      try {
        const [response, latestLoanApplicationResponse, allLoansResponse] = await Promise.all([
          getCustomerAccountStatus(),
          getLatestCustomerLoanApplication(),
          import('../api/loan').then(m => m.getAllCustomerLoans()),
        ]);
        setAccountStatus(response);
        setLatestLoanApplication(latestLoanApplicationResponse);
        setCustomerLoans(allLoansResponse);
        const logs = await import('../api/loan').then(m => m.getCustomerPaymentLogs());
        setPaymentLogs(logs);
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

  async function handleRepay(loanId: number) {
    setIsRepayingMap(prev => ({ ...prev, [loanId]: true }));
    setAccountError(null);
    try {
      const updatedLoan = await import('../api/loan').then(m => m.repayInstallment(loanId));
      setCustomerLoans(currentLoans => currentLoans.map(l => l.loanId === loanId ? updatedLoan : l));
      const newStatus = await getCustomerAccountStatus();
      setAccountStatus(newStatus);
      const newLogs = await import('../api/loan').then(m => m.getCustomerPaymentLogs());
      setPaymentLogs(newLogs);
    } catch (err) {
      setAccountError(extractApiErrorMessage(err, 'Could not process repayment at this time.'));
    } finally {
      setIsRepayingMap(prev => ({ ...prev, [loanId]: false }));
    }
  }

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
    setLoanApplicationDraft((currentDraft) => ({ ...currentDraft, [field]: value }));
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
      const allLoansResponse = await getAllCustomerLoans();
      setCustomerLoans(allLoansResponse);
      setIsLoanApplicationOpen(false);
      setLoanApplicationMessage('Your loan application was submitted for employee review.');
    } catch (submitError) {
      setLoanApplicationMessage(extractApiErrorMessage(submitError, 'Loan application could not be submitted.'));
    } finally {
      setIsSubmittingLoanApplication(false);
    }
  }

  const hasAnyLoansVisually = customerLoans.length > 0;

  const paymentLogsPanel = (
    <article className="bank-panel bank-logs-panel">
      <div className="bank-panel-heading">
        <div>
          <p className="bank-section-kicker">Account Logs</p>
          <h2>Payment History</h2>
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', maxHeight: '400px', overflowY: 'auto', paddingRight: '0.5rem' }}>
        {paymentLogs.length === 0 ? (
          <p className="bank-panel-copy">No payments fully processed yet.</p>
        ) : (
          paymentLogs.map(log => (
            <div key={log.logId} style={{ display: 'grid', gap: '0.25rem', padding: '0.85rem', background: 'rgba(2, 6, 23, 0.34)', border: '1px solid rgba(148, 163, 184, 0.18)', borderRadius: '8px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: '#9fb0c6', fontSize: '0.85rem' }}>Loan #{log.loanId} · Inst #{log.installmentNumber}</span>
                <span style={{ color: '#4ade80', fontWeight: 'bold' }}>+{formatMoney(log.amountPaid)}</span>
              </div>
              <span style={{ color: '#9fb0c6', fontSize: '0.8rem' }}>{formatDateTime(log.paidAt)}</span>
            </div>
          ))
        )}
      </div>
    </article>
  );

  return (
    <div className="bank-dashboard-shell">
      <header className="enterprise-nav">
        <div className="enterprise-nav-left">
          <div className="enterprise-logo-wrap">
            <img className="enterprise-logo-image" src="/bankai-logo.png" alt="BANKΛI" />
            <div>
              <span className="enterprise-logo-text">BANKΛI</span>
              <span className="enterprise-logo-subtitle">Premium AI FinTech</span>
            </div>
          </div>
        </div>

        <div className="enterprise-nav-actions">
          <div className="enterprise-nav-user">
            <span>{customerDisplayName}</span>
            <strong>{roleLabel} CLIENT</strong>
          </div>
          <button type="button" className="enterprise-btn enterprise-btn-secondary" onClick={handleLogout}>Logout</button>
        </div>
      </header>

      <main className="bank-dashboard-main">
        <section className="bank-hero-panel">
          <div>
            <p className="bank-section-kicker">Private banking dashboard</p>
            <h1>Your accounts and credit services in one place</h1>
            <p className="bank-hero-copy">Monitor your current account, prepare financing requests, and follow the next banking operations from a focused workspace.</p>
          </div>
        </section>

        <section className="bank-content-grid">
          <div style={{ display: 'grid', gap: '1rem', alignContent: 'start' }}>
            <article className={`bank-panel bank-account-panel ${isAccountPanelLoading ? 'account-status-panel-loading' : ''}`.trim()}>
              <div className="bank-panel-heading">
                <div>
                  <p className="bank-section-kicker">Primary account</p>
                  <h2>Current account</h2>
                </div>
              </div>

              {isAccountPanelLoading ? (
                <div className="account-status-skeleton">
                  <div className="skeleton-line skeleton-line-short"></div>
                  <div className="skeleton-line"></div>
                  <div className="skeleton-line skeleton-line-medium"></div>
                </div>
              ) : accountStatus?.hasAccount ? (
                <div className="bank-account-overview" style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '2rem', alignItems: 'stretch' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', alignItems: 'stretch' }}>
                    <div className="account-balances-stack">
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
                  </div>
                  </div>

                  <div className="bank-account-meta-grid">
                    <div><span>IBAN</span><strong>{accountStatus.iban}</strong></div>
                    <div><span>Currency</span><strong>EUR</strong></div>
                    <div><span>Account package</span><strong>Standard digital</strong></div>
                  </div>

                  <div style={{ gridColumn: '1 / -1', marginTop: '1rem' }}>
                    {isLoadingLoanApplication ? (
                      <div className="bank-application-status bank-application-status-muted">
                        <div className="bank-application-status-head">
                          <span>Application status</span>
                          <span className="bank-application-status-pill bank-application-status-pill-muted">Loading...</span>
                        </div>
                      </div>
                    ) : latestLoanApplication && latestLoanApplication.status === 'PENDING' && (
                      <div className={`bank-application-status bank-application-status-${latestLoanApplication.status.toLowerCase()}`}>
                        <div className="bank-application-status-head">
                          <span>Latest loan application</span>
                          <span className={`bank-application-status-pill bank-application-status-pill-${latestLoanApplication.status.toLowerCase()}`}>
                            {formatLoanStatus(latestLoanApplication.status)}
                          </span>
                        </div>
                        <div className="bank-application-status-grid">
                          <div><span>Product</span><strong>{formatLoanType(latestLoanApplication.loanType)}</strong></div>
                          <div><span>Amount</span><strong>{formatMoney(latestLoanApplication.principalAmount)}</strong></div>
                          <div><span>Submitted</span><strong>{formatDateTime(latestLoanApplication.submittedAt)}</strong></div>
                          <div><span>Indicative rate</span><strong>{latestLoanApplication.annualInterestRate.toFixed(2)}%</strong></div>
                        </div>
                        <p>{latestLoanApplication.message}</p>
                      </div>
                    )}

                    {customerLoans.filter(l => l.status === 'ACTIVE' || l.status === 'CLOSED').map(loan => (
                      <div key={loan.loanId} className={`bank-application-status bank-application-status-${loan.status.toLowerCase()}`} style={{ marginTop: '1rem' }}>
                        <div className="bank-application-status-head">
                          <span>{formatLoanType(loan.loanType)}</span>
                          <span className={`bank-application-status-pill bank-application-status-pill-${loan.status.toLowerCase()}`}>
                            {loan.status}
                          </span>
                        </div>
                        <div className="bank-application-status-grid">
                          <div><span>Amount</span><strong>{formatMoney(loan.principalAmount)}</strong></div>
                          <div><span>Monthly installment</span><strong>{formatMoney(loan.monthlyInstallmentAmount)}</strong></div>
                          <div><span>Term</span><strong>{loan.repaymentTermMonths} months</strong></div>
                          <div><span>Interest rate</span><strong>{loan.annualInterestRate.toFixed(2)}%</strong></div>
                        </div>
                        
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem', flexWrap: 'wrap', gap: '1rem' }}>
                          {loan.repaymentSchedule && loan.repaymentSchedule.length > 0 && (
                            <div className="bank-repayment-schedule-container" style={{ marginTop: 0, paddingTop: 0, border: 'none' }}>
                              <button type="button" className="bank-toggle-schedule-btn" onClick={() => setExpandedLoanId(expandedLoanId === loan.loanId ? null : loan.loanId)}>
                                {expandedLoanId === loan.loanId ? 'Hide Repayment Schedule' : 'View Repayment Schedule'}
                                <span className="bank-toggle-icon" aria-hidden="true">{expandedLoanId === loan.loanId ? '▲' : '▼'}</span>
                              </button>
                            </div>
                          )}
                          
                          {loan.status === 'ACTIVE' && (() => {
                            const lastPaidInstallment = loan.repaymentSchedule.slice().reverse().find(i => i.status === 'PAID');
                            const hasPaidThisMonth = lastPaidInstallment && lastPaidInstallment.paidAt && 
                              new Date(lastPaidInstallment.paidAt).getMonth() === new Date().getMonth() && 
                              new Date(lastPaidInstallment.paidAt).getFullYear() === new Date().getFullYear();
                            const isFullyPaid = loan.repaymentSchedule.every(i => i.status === 'PAID');
                            return (
                              <button 
                                type="button" 
                                className="bank-primary-button" 
                                style={{ minHeight: '36px', padding: '0.4rem 1.2rem', fontSize: '0.85rem', marginLeft: 'auto' }}
                                onClick={() => handleRepay(loan.loanId)}
                                disabled={isRepayingMap[loan.loanId] || isFullyPaid || Boolean(hasPaidThisMonth)}
                                title={hasPaidThisMonth ? "You have already paid an installment this calendar month" : "Pay next installment"}
                              >
                                {isRepayingMap[loan.loanId] ? 'Processing...' : 'Repay'}
                              </button>
                            );
                          })()}
                        </div>

                        {expandedLoanId === loan.loanId && loan.repaymentSchedule && loan.repaymentSchedule.length > 0 && (
                          <div className="bank-repayment-schedule-table-wrapper" style={{ marginTop: '0.5rem' }}>
                            <table className="bank-repayment-schedule-table">
                              <thead>
                                <tr>
                                  <th>Month</th>
                                  <th>Status</th>
                                  <th>Installment</th>
                                  <th>Principal</th>
                                  <th>Interest</th>
                                  <th>Remaining</th>
                                </tr>
                              </thead>
                              <tbody>
                                {loan.repaymentSchedule.map((row) => (
                                  <tr key={row.installmentId} style={{ opacity: row.status === 'PAID' ? 0.6 : 1, background: row.status === 'PAID' ? 'rgba(34, 197, 94, 0.05)' : '' }}>
                                    <td>{row.installmentNumber}</td>
                                    <td style={{ color: row.status === 'PAID' ? '#4ade80' : 'inherit' }}>{row.status}</td>
                                    <td>{formatMoney(row.monthlyInstallmentAmount)}</td>
                                    <td>{formatMoney(row.principalPart)}</td>
                                    <td>{formatMoney(row.interestPart)}</td>
                                    <td>{formatMoney(row.remainingBalance)}</td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="bank-empty-state">
                  <p>You do not have an opened bank account yet.</p>
                  <button type="button" className="bank-primary-button" onClick={handleOpenAccount} disabled={isOpeningAccount}>
                    {isOpeningAccount ? 'Opening account...' : 'Open bank account'}
                  </button>
                </div>
              )}
              {accountSuccess && <p className="status-success-text">{accountSuccess}</p>}
              {accountError && <p className="status-error-text">{accountError}</p>}
            </article>
            
            {!hasAnyLoansVisually && paymentLogsPanel}
          </div>

          <aside className="bank-side-stack">
            <article className="bank-panel bank-profile-panel">
              <p className="bank-section-kicker">Customer profile</p>
              <div className="bank-profile-list">
                <div><span>Email</span><strong>{session?.email}</strong></div>
                <div><span>Type</span><strong>{roleLabel}</strong></div>
              </div>
            </article>

            <article className="bank-panel bank-credit-panel">
              <div className="bank-panel-heading">
                <div>
                  <p className="bank-section-kicker">Credit services</p>
                  <h2>Request financing</h2>
                </div>
              </div>
              <p className="bank-panel-copy">Choose loan parameters and prepare a request for employee review.</p>
              {loanApplicationMessage && !isLoanApplicationOpen && <p className="bank-form-message">{loanApplicationMessage}</p>}
              <div className="bank-rate-row">
                <span>Consumer 5.20% - 6.70%</span>
                <span>Mortgage 2.65% - 3.45%</span>
              </div>
              <div className="bank-product-limit-grid">
                <div><span>Consumer amount</span><strong>{formatAmountLimits(loanProductLimits.CONSUMER)}</strong><small>{formatTermLimits(loanProductLimits.CONSUMER)}</small></div>
                <div><span>Mortgage amount</span><strong>{formatAmountLimits(loanProductLimits.MORTGAGE)}</strong><small>{formatTermLimits(loanProductLimits.MORTGAGE)}</small></div>
              </div>
              <button type="button" className="bank-primary-button bank-full-width-button" onClick={() => setIsLoanApplicationOpen(true)} disabled={!hasActiveAccount || hasPendingLoanApplication}>
                {hasPendingLoanApplication ? 'Application pending' : 'Request loan'}
              </button>
              {!hasActiveAccount && !isAccountPanelLoading && <p className="bank-panel-note">Open an account before submitting a credit request.</p>}
              {hasPendingLoanApplication && <p className="bank-panel-note">Wait for employee review before submitting another request.</p>}
            </article>

            {hasAnyLoansVisually && paymentLogsPanel}

          </aside>
        </section>
      </main>

      {isLoanApplicationOpen && (
        <div className="bank-modal-backdrop" role="presentation">
          <section className="bank-loan-modal" role="dialog" aria-modal="true" aria-labelledby="loan-request-title">
            <div className="bank-panel-heading">
              <div><p className="bank-section-kicker">Credit request</p><h2 id="loan-request-title">Request a loan</h2></div>
              <button type="button" className="bank-icon-button" aria-label="Close loan request" onClick={() => setIsLoanApplicationOpen(false)}>x</button>
            </div>
            <form className="bank-loan-form" onSubmit={handleLoanApplicationSubmit} noValidate>
              <label className="form-field">
                <span className="form-label">Loan type</span>
                <div className="custom-dropdown-container">
                  <div className="custom-dropdown-header" onClick={() => setIsDropdownOpen(!isDropdownOpen)}>
                    {selectedLoanProduct.label}
                    <span className="custom-dropdown-chevron" aria-hidden="true">⌄</span>
                  </div>
                  {isDropdownOpen && (
                    <div className="custom-dropdown-list">
                      <div className="custom-dropdown-item" onClick={() => { updateLoanApplicationDraft('loanType', 'CONSUMER'); setIsDropdownOpen(false); }}>Consumer loan</div>
                      <div className="custom-dropdown-item" onClick={() => { updateLoanApplicationDraft('loanType', 'MORTGAGE'); setIsDropdownOpen(false); }}>Mortgage loan</div>
                    </div>
                  )}
                </div>
              </label>

              <div className="bank-selected-product-terms">
                <div><span>Allowed amount</span><strong>{formatAmountLimits(selectedLoanProduct)}</strong></div>
                <div><span>Allowed term</span><strong>{formatTermLimits(selectedLoanProduct)}</strong></div>
              </div>

              <label className="form-field">
                <span className="form-label">Amount</span>
                <div className="custom-number-input-wrapper">
                  <input type="number" value={loanApplicationDraft.principalAmount} onChange={(e) => updateLoanApplicationDraft('principalAmount', e.target.value)} />
                  <span className="custom-suffix">EUR</span>
                  <div className="custom-spinners">
                    <button type="button" className="custom-spinner-btn" onClick={() => handleAmountChange(1)}>+</button>
                    <button type="button" className="custom-spinner-btn" onClick={() => handleAmountChange(-1)}>-</button>
                  </div>
                </div>
              </label>

              <label className="form-field">
                <span className="form-label">Term in months</span>
                <div className="custom-number-input-wrapper">
                  <input type="number" value={loanApplicationDraft.repaymentTermMonths} onChange={(e) => updateLoanApplicationDraft('repaymentTermMonths', e.target.value)} />
                  <span className="custom-suffix">mo.</span>
                  <div className="custom-spinners">
                    <button type="button" className="custom-spinner-btn" onClick={() => handleTermChange(1)}>+</button>
                    <button type="button" className="custom-spinner-btn" onClick={() => handleTermChange(-1)}>-</button>
                  </div>
                </div>
              </label>

              <label className="form-field">
                <span className="form-label form-label-accent">Interest rate</span>
                <div className="bank-calculator-input bank-calculator-input-accent">
                  <input type="number" value={estimatedAnnualInterestRate === null ? '' : estimatedAnnualInterestRate.toFixed(2)} readOnly />
                  <span>%</span>
                </div>
              </label>

              <div className="bank-calculator-result">
                <div className="bank-calculator-result-head">
                  <div><span>Monthly installment</span><strong>{formatOptionalMoney(loanCalculation.monthlyPayment)}</strong></div>
                  <div><span>APR (ГПР)</span><strong>{loanCalculation.apr === null ? '-' : `${loanCalculation.apr.toFixed(2)}%`}</strong></div>
                </div>
                <dl className="bank-calculator-breakdown">
                  <div><dt>Credit term</dt><dd>{loanCalculation.apr === null ? '-' : `${Number(loanApplicationDraft.repaymentTermMonths)} mo.`}</dd></div>
                  <div><dt>Total due amount</dt><dd>{formatOptionalMoney(loanCalculation.totalDueAmount)}</dd></div>
                  <div><dt>Total interest</dt><dd>{formatOptionalMoney(loanCalculation.totalInterest)}</dd></div>
                  <div><dt>Interest rate</dt><dd>{estimatedAnnualInterestRate === null ? '-' : `${estimatedAnnualInterestRate.toFixed(2)}%`}</dd></div>
                  <div><dt>Monthly service fee</dt><dd>{formatMoney(selectedLoanProduct.monthlyServiceFee)}</dd></div>
                  {selectedLoanProduct.upfrontFees.analysis > 0 && (
                    <div><dt>Check and analysis fee</dt><dd>{formatMoney(selectedLoanProduct.upfrontFees.analysis)}</dd></div>
                  )}
                  {selectedLoanProduct.upfrontFees.collateralAssessment > 0 && (
                    <div><dt>Collateral assessment fee</dt><dd>{formatMoney(selectedLoanProduct.upfrontFees.collateralAssessment)}</dd></div>
                  )}
                </dl>
              </div>

              {(loanApplicationMessage || loanApplicationValidationMessage) && (
                <p className="bank-form-message">{loanApplicationMessage || loanApplicationValidationMessage}</p>
              )}

              <div className="bank-form-actions">
                <button type="button" className="bank-ghost-button" onClick={() => setIsLoanApplicationOpen(false)}>Cancel</button>
                <button type="submit" className="bank-primary-button" disabled={Boolean(loanApplicationValidationMessage) || hasPendingLoanApplication || isSubmittingLoanApplication}>
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
