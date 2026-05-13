import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createCorporateCustomer, createIndividualCustomer } from '../api/adminOnboarding';
import { extractApiErrorMessage } from '../api/http';
import {
  approveLoanApplication,
  getLoanReviewHistory,
  getPendingLoanApplications,
  rejectLoanApplication,
} from '../api/loan';
import type { LoanApplicationResponse, LoanReviewHistoryItem, OnboardingResponse } from '../types/auth';
import { clearSession } from '../utils/authStorage';
import '../index.css';

type OnboardingMode = 'INDIVIDUAL' | 'CORPORATE';
type EmployeeWorkspaceTab = 'ONBOARDING' | 'LOANS';

function formatMoney(value: number | null | undefined) {
  return `${(value ?? 0).toLocaleString('bg-BG', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })} EUR`;
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return 'Not recorded';
  }

  return new Intl.DateTimeFormat('bg-BG', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatCustomer(application: Pick<LoanApplicationResponse, 'customerDisplayName' | 'customerEmail' | 'customerId'>) {
  return application.customerDisplayName?.trim()
    || application.customerEmail?.trim()
    || `Customer #${application.customerId}`;
}

export function AdminOnboardingPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<EmployeeWorkspaceTab>('ONBOARDING');
  const [mode, setMode] = useState<OnboardingMode>('INDIVIDUAL');
  const [adminSecret, setAdminSecret] = useState('');

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [egn, setEgn] = useState('');

  const [companyName, setCompanyName] = useState('');
  const [eik, setEik] = useState('');
  const [representativeFirstName, setRepresentativeFirstName] = useState('');
  const [representativeLastName, setRepresentativeLastName] = useState('');

  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<OnboardingResponse | null>(null);

  const [pendingApplications, setPendingApplications] = useState<LoanApplicationResponse[]>([]);
  const [reviewHistory, setReviewHistory] = useState<LoanReviewHistoryItem[]>([]);
  const [loanDeskError, setLoanDeskError] = useState<string | null>(null);
  const [loanDeskMessage, setLoanDeskMessage] = useState<string | null>(null);
  const [isLoadingLoanDesk, setIsLoadingLoanDesk] = useState(false);
  const [reviewingLoanId, setReviewingLoanId] = useState<number | null>(null);
  const [decisionNotes, setDecisionNotes] = useState<Record<number, string>>({});

  const pendingTotalAmount = useMemo(
    () => pendingApplications.reduce((total, application) => total + application.principalAmount, 0),
    [pendingApplications]
  );

  async function loadLoanDesk() {
    setIsLoadingLoanDesk(true);
    setLoanDeskError(null);

    try {
      const [pendingResponse, historyResponse] = await Promise.all([
        getPendingLoanApplications(),
        getLoanReviewHistory(),
      ]);
      setPendingApplications(pendingResponse);
      setReviewHistory(historyResponse);
    } catch (loadError) {
      setLoanDeskError(extractApiErrorMessage(loadError, 'Loan applications could not be loaded.'));
    } finally {
      setIsLoadingLoanDesk(false);
    }
  }

  useEffect(() => {
    if (activeTab === 'LOANS') {
      void loadLoanDesk();
    }
  }, [activeTab]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    setResult(null);

    try {
      const response =
        mode === 'INDIVIDUAL'
          ? await createIndividualCustomer(adminSecret, {
              firstName,
              lastName,
              egn,
              email,
            })
          : await createCorporateCustomer(adminSecret, {
              companyName,
              eik,
              representativeFirstName,
              representativeLastName,
              email,
            });

      setResult(response);
    } catch (submitError) {
      setError(extractApiErrorMessage(submitError, 'Failed to create customer.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleReview(loanId: number, decision: 'APPROVE' | 'REJECT') {
    setReviewingLoanId(loanId);
    setLoanDeskError(null);
    setLoanDeskMessage(null);

    try {
      const request = { decisionNote: decisionNotes[loanId]?.trim() || undefined };
      const response = decision === 'APPROVE'
        ? await approveLoanApplication(loanId, request)
        : await rejectLoanApplication(loanId, request);

      setLoanDeskMessage(`Application #${response.loanId} was ${response.status.toLowerCase()}.`);
      setDecisionNotes((currentNotes) => {
        const nextNotes = { ...currentNotes };
        delete nextNotes[loanId];
        return nextNotes;
      });
      await loadLoanDesk();
    } catch (reviewError) {
      setLoanDeskError(extractApiErrorMessage(reviewError, 'Loan application could not be reviewed.'));
    } finally {
      setReviewingLoanId(null);
    }
  }

  function handleLogout() {
    clearSession();
    navigate('/login', { replace: true });
  }

  return (
    <div className="home-container home-container-top">
      <div className="background-shapes">
        <div className="shape shape-1"></div>
        <div className="shape shape-2"></div>
      </div>

      <section className="glass-card auth-card employee-workspace-card page-content-left">
        <div className="panel-topbar">
          <div className="brand-badge">Employee area</div>
          <button type="button" className="btn-secondary btn-compact" onClick={handleLogout}>
            Logout
          </button>
        </div>

        <h1 className="main-title page-title">Employee workspace</h1>
        <p className="subtitle page-subtitle page-subtitle-tight">
          Create customers and review submitted credit applications from one operational view.
        </p>

        <div className="segmented-control-wrap employee-workspace-tabs">
          <div className="segmented-control" role="tablist" aria-label="Employee workspace">
            <button
              type="button"
              className={`segment-btn ${activeTab === 'ONBOARDING' ? 'segment-btn-active' : ''}`}
              onClick={() => setActiveTab('ONBOARDING')}
            >
              Customer onboarding
            </button>
            <button
              type="button"
              className={`segment-btn ${activeTab === 'LOANS' ? 'segment-btn-active' : ''}`}
              onClick={() => setActiveTab('LOANS')}
            >
              Loan applications
            </button>
          </div>
        </div>

        {activeTab === 'ONBOARDING' ? (
          <>
            <div className="segmented-control-wrap">
              <div className="segmented-control" role="tablist" aria-label="Customer type">
                <button
                  type="button"
                  className={`segment-btn ${mode === 'INDIVIDUAL' ? 'segment-btn-active' : ''}`}
                  onClick={() => setMode('INDIVIDUAL')}
                >
                  Individual
                </button>
                <button
                  type="button"
                  className={`segment-btn ${mode === 'CORPORATE' ? 'segment-btn-active' : ''}`}
                  onClick={() => setMode('CORPORATE')}
                >
                  Corporate
                </button>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="onboarding-form-grid">
              <label className="form-field">
                <span className="form-label">Admin secret</span>
                <input
                  type="password"
                  value={adminSecret}
                  onChange={(event) => setAdminSecret(event.target.value)}
                  required
                  autoComplete="off"
                  className="glass-input"
                />
              </label>

              <label className="form-field">
                <span className="form-label">Customer email</span>
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                  autoComplete="off"
                  className="glass-input"
                />
              </label>

              {mode === 'INDIVIDUAL' ? (
                <>
                  <label className="form-field">
                    <span className="form-label">First name</span>
                    <input
                      type="text"
                      value={firstName}
                      onChange={(event) => setFirstName(event.target.value)}
                      required
                      maxLength={100}
                      className="glass-input"
                    />
                  </label>

                  <label className="form-field">
                    <span className="form-label">Last name</span>
                    <input
                      type="text"
                      value={lastName}
                      onChange={(event) => setLastName(event.target.value)}
                      required
                      maxLength={100}
                      className="glass-input"
                    />
                  </label>

                  <label className="form-field">
                    <span className="form-label">EGN (10 digits)</span>
                    <input
                      type="text"
                      value={egn}
                      onChange={(event) => setEgn(event.target.value)}
                      required
                      pattern="[0-9]{10}"
                      maxLength={10}
                      className="glass-input"
                    />
                  </label>
                </>
              ) : (
                <>
                  <label className="form-field">
                    <span className="form-label">Company name</span>
                    <input
                      type="text"
                      value={companyName}
                      onChange={(event) => setCompanyName(event.target.value)}
                      required
                      maxLength={200}
                      className="glass-input"
                    />
                  </label>

                  <label className="form-field">
                    <span className="form-label">EIK (9 to 13 digits)</span>
                    <input
                      type="text"
                      value={eik}
                      onChange={(event) => setEik(event.target.value)}
                      required
                      pattern="[0-9]{9,13}"
                      maxLength={13}
                      className="glass-input"
                    />
                  </label>

                  <label className="form-field">
                    <span className="form-label">Representative first name</span>
                    <input
                      type="text"
                      value={representativeFirstName}
                      onChange={(event) => setRepresentativeFirstName(event.target.value)}
                      required
                      maxLength={100}
                      className="glass-input"
                    />
                  </label>

                  <label className="form-field">
                    <span className="form-label">Representative last name</span>
                    <input
                      type="text"
                      value={representativeLastName}
                      onChange={(event) => setRepresentativeLastName(event.target.value)}
                      required
                      maxLength={100}
                      className="glass-input"
                    />
                  </label>
                </>
              )}

              {error && <div className="status-banner status-banner-error">{error}</div>}

              <div className="form-actions">
                <button className="btn-primary submit-btn-fixed" type="submit" disabled={isSubmitting}>
                  {isSubmitting ? 'Creating customer...' : 'Create customer'}
                </button>
              </div>
            </form>

            {result && (
              <section className="result-panel" aria-live="polite">
                <h2>Customer created</h2>
                <p>
                  <strong>ID:</strong> {result.customerId}
                </p>
                <p>
                  <strong>Email:</strong> {result.email}
                </p>
                <p>
                  <strong>Type:</strong> {result.customerType}
                </p>
                <p>
                  <strong>Temporary password email:</strong>{' '}
                  {result.temporaryPasswordSent ? 'Sent successfully' : 'Not sent'}
                </p>
                <p>
                  <strong>Delivery channel:</strong> {result.emailDeliveryChannel}
                </p>
                <p>
                  <strong>SMTP relay:</strong> {result.emailRelay}
                </p>
                {result.emailDeliveryChannel === 'LOCAL_MAILHOG' ? (
                  <p>Email was captured locally. Open http://localhost:8025 to read it.</p>
                ) : (
                  <p>Please ask the customer to check inbox and spam folder.</p>
                )}
              </section>
            )}
          </>
        ) : (
          <section className="employee-loan-desk">
            <div className="employee-loan-toolbar">
              <div>
                <p className="bank-section-kicker">Credit operations</p>
                <h2>Loan application queue</h2>
              </div>
              <button type="button" className="btn-secondary btn-compact" onClick={loadLoanDesk} disabled={isLoadingLoanDesk}>
                {isLoadingLoanDesk ? 'Refreshing...' : 'Refresh'}
              </button>
            </div>

            <div className="employee-loan-stats">
              <div>
                <span>Pending applications</span>
                <strong>{pendingApplications.length}</strong>
              </div>
              <div>
                <span>Pending principal</span>
                <strong>{formatMoney(pendingTotalAmount)}</strong>
              </div>
              <div>
                <span>Reviewed decisions</span>
                <strong>{reviewHistory.length}</strong>
              </div>
            </div>

            {loanDeskError && <div className="status-banner status-banner-error">{loanDeskError}</div>}
            {loanDeskMessage && <div className="status-banner status-banner-success">{loanDeskMessage}</div>}

            <div className="employee-loan-section">
              <h3>Pending review</h3>
              {pendingApplications.length === 0 ? (
                <div className="employee-empty-state">
                  {isLoadingLoanDesk ? 'Loading applications...' : 'There are no pending loan applications.'}
                </div>
              ) : (
                <div className="employee-loan-list">
                  {pendingApplications.map((application) => (
                    <article className="employee-loan-card" key={application.loanId}>
                      <div className="employee-loan-card-head">
                        <div>
                          <span>Application #{application.loanId}</span>
                          <strong>{formatCustomer(application)}</strong>
                          <small>{application.customerEmail} · {application.customerType ?? 'CUSTOMER'}</small>
                        </div>
                        <span className="employee-status-pill employee-status-pending">{application.status}</span>
                      </div>

                      <div className="employee-loan-facts">
                        <div>
                          <span>Loan type</span>
                          <strong>{application.loanType}</strong>
                        </div>
                        <div>
                          <span>Amount</span>
                          <strong>{formatMoney(application.principalAmount)}</strong>
                        </div>
                        <div>
                          <span>Term</span>
                          <strong>{application.repaymentTermMonths} mo.</strong>
                        </div>
                        <div>
                          <span>Interest</span>
                          <strong>{application.annualInterestRate.toFixed(2)}%</strong>
                        </div>
                      </div>

                      <label className="form-field employee-decision-note">
                        <span className="form-label">Decision note</span>
                        <textarea
                          className="glass-input"
                          value={decisionNotes[application.loanId] ?? ''}
                          maxLength={500}
                          onChange={(event) => setDecisionNotes((currentNotes) => ({
                            ...currentNotes,
                            [application.loanId]: event.target.value,
                          }))}
                          placeholder="Optional note for the review history"
                        />
                      </label>

                      <div className="employee-review-actions">
                        <button
                          type="button"
                          className="employee-approve-button"
                          disabled={reviewingLoanId === application.loanId}
                          onClick={() => handleReview(application.loanId, 'APPROVE')}
                        >
                          Approve
                        </button>
                        <button
                          type="button"
                          className="employee-reject-button"
                          disabled={reviewingLoanId === application.loanId}
                          onClick={() => handleReview(application.loanId, 'REJECT')}
                        >
                          Reject
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>

            <div className="employee-loan-section">
              <h3>Review history</h3>
              {reviewHistory.length === 0 ? (
                <div className="employee-empty-state">No reviewed applications yet.</div>
              ) : (
                <div className="employee-history-list">
                  {reviewHistory.map((historyItem) => (
                    <article className="employee-history-row" key={historyItem.logId}>
                      <div>
                        <span>{formatDateTime(historyItem.decidedAt)}</span>
                        <strong>
                          {historyItem.decision} application #{historyItem.loanId}
                        </strong>
                        <small>{historyItem.customerEmail} · reviewed by {historyItem.employeeEmail}</small>
                      </div>
                      <div>
                        <span>{historyItem.loanType}</span>
                        <strong>{formatMoney(historyItem.principalAmount)}</strong>
                        <small>{historyItem.repaymentTermMonths} mo. · {historyItem.annualInterestRate.toFixed(2)}%</small>
                      </div>
                      {historyItem.decisionNote && <p>{historyItem.decisionNote}</p>}
                    </article>
                  ))}
                </div>
              )}
            </div>
          </section>
        )}
      </section>
    </div>
  );
}
