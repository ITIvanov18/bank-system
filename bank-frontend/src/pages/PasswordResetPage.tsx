import React, { useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { requestPasswordReset, resetPassword } from '../api/auth';
import '../index.css';

export function PasswordResetPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = useMemo(() => searchParams.get('token')?.trim() || '', [searchParams]);
  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleRequestReset = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setIsLoading(true);

    try {
      const response = await requestPasswordReset({ email });
      setMessage(response.message);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Could not send a reset link.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setIsLoading(true);

    try {
      await resetPassword({ token, newPassword });
      setMessage('Password reset successfully. Redirecting to sign in...');
      setTimeout(() => navigate('/login', { replace: true }), 1200);
    } catch (err: any) {
      setError(err.response?.data?.message || 'This reset link is invalid or expired.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="home-container">
      <div className="background-shapes">
        <div className="shape shape-1"></div>
        <div className="shape shape-2"></div>
      </div>

      <div className="glass-card auth-card auth-card-login">
        <div className="brand-badge">Account recovery</div>

        <h1 className="main-title page-title">{token ? 'Reset password' : 'Forgot password'}</h1>
        <p className="subtitle page-subtitle">
          {token ? 'Choose a new password for your account' : 'Enter your email to receive a secure reset link'}
        </p>

        {error && (
          <div className="status-banner status-banner-error status-banner-inline">
            {error}
          </div>
        )}

        {message && (
          <div className="status-banner status-banner-success status-banner-inline">
            {message}
          </div>
        )}

        {token ? (
          <form onSubmit={handleResetPassword} className="auth-form-stack">
            <div className="form-field">
              <label className="form-label">New password</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={8}
                className="glass-input"
                placeholder="At least 8 characters"
              />
            </div>

            <div className="form-field">
              <label className="form-label">Confirm password</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={8}
                className="glass-input"
                placeholder="Repeat new password"
              />
            </div>

            <div className="auth-submit-wrap">
              <button type="submit" className="btn-primary auth-submit-btn" disabled={isLoading}>
                {isLoading ? 'Resetting...' : 'Reset password'}
              </button>
            </div>
          </form>
        ) : (
          <form onSubmit={handleRequestReset} className="auth-form-stack">
            <div className="form-field">
              <label className="form-label">Email address</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="glass-input"
                placeholder="name@company.com"
              />
            </div>

            <div className="auth-submit-wrap">
              <button type="submit" className="btn-primary auth-submit-btn" disabled={isLoading}>
                {isLoading ? 'Sending...' : 'Send reset link'}
              </button>
            </div>
          </form>
        )}

        <div className="auth-back-link">
          <Link to="/login" className="link-muted">← Back to sign in</Link>
        </div>
      </div>
    </div>
  );
}
