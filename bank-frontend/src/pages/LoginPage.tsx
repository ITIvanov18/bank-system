import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../api/auth';
import { getSession, saveSession } from '../utils/authStorage';
import '../index.css';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();
  const session = getSession();

  useEffect(() => {
    if (!session) {
      return;
    }

    if (session.role === 'EMPLOYEE') {
      navigate('/admin/secret-onboarding', { replace: true });
      return;
    }

    navigate(session.firstLogin ? '/first-login' : '/app', { replace: true });
  }, [navigate, session]);

  if (session) {
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const auth = await login({ email, password });
      saveSession(auth);

      if (auth.role === 'EMPLOYEE') {
        navigate('/admin/secret-onboarding', { replace: true });
        return;
      }

      navigate(auth.firstLogin ? '/first-login' : '/app', { replace: true });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid email or password.');
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
        <div className="brand-badge">Secure access</div>

        <h1 className="main-title page-title">Sign in</h1>
        <p className="subtitle page-subtitle">Welcome back to NBU Bank System</p>

        {error && (
          <div className="status-banner status-banner-error status-banner-inline">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form-stack">
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

          <div className="form-field">
            <label className="form-label">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="glass-input"
              placeholder="••••••••"
            />
          </div>

          <div className="auth-submit-wrap">
            <button
              type="submit"
              className="btn-primary auth-submit-btn"
              disabled={isLoading}
            >
              {isLoading ? 'Signing in...' : 'Sign in'}
            </button>
          </div>
        </form>

        <div className="auth-back-link">
          <Link to="/reset-password" className="link-muted">Forgot password?</Link>
        </div>

        <div className="auth-back-link">
          <Link to="/" className="link-muted">← Back to home</Link>
        </div>
      </div>
    </div>
  );
}
