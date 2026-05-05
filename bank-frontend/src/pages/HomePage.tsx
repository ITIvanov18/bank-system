import { Link } from 'react-router-dom';
import '../index.css';

export function HomePage() {
  return (
    <div className="enterprise-home">
      <div className="background-shapes">
        <div className="shape shape-1"></div>
        <div className="shape shape-2"></div>
        <div className="shape shape-3"></div>
      </div>

      <header className="enterprise-nav">
        <div className="enterprise-logo-wrap">
          <div className="enterprise-logo-dot"></div>
          <span className="enterprise-logo-text">BANKΛI | Unleash Your Assets</span>
        </div>

        <nav className="enterprise-nav-links" aria-label="Primary">
          <a href="#products">Products</a>
          <a href="#security">Security</a>
          <a href="#support">Support</a>
        </nav>

        <Link to="/login" className="enterprise-btn enterprise-btn-secondary">
          Sign in
        </Link>
      </header>

      <main className="enterprise-main">
        <section className="enterprise-hero enterprise-section">
          <div className="enterprise-hero-content">
            <div className="brand-badge">Trusted digital banking</div>

            <h1 className="main-title enterprise-title">
              Enterprise-grade banking
              <span className="gradient-text"> for modern customers</span>
            </h1>

            <p className="subtitle enterprise-subtitle">
              Secure onboarding, reliable account access, and production-ready controls for customer and employee workflows.
            </p>

            <div className="enterprise-hero-actions">
              <Link to="/login" className="enterprise-btn enterprise-btn-primary">
                Access online banking
              </Link>
              <a href="#security" className="enterprise-btn enterprise-btn-ghost">
                Explore security
              </a>
            </div>

            <div className="enterprise-metrics" aria-label="Business metrics">
              <article className="enterprise-metric-card">
                <p className="enterprise-metric-value">97.95%</p>
                <p className="enterprise-metric-label">Platform uptime</p>
              </article>
              <article className="enterprise-metric-card">
                <p className="enterprise-metric-value">24/7</p>
                <p className="enterprise-metric-label">Fraud monitoring</p>
              </article>
              <article className="enterprise-metric-card">
                <p className="enterprise-metric-value">AES-256</p>
                <p className="enterprise-metric-label">Data encryption</p>
              </article>
            </div>
          </div>

          <aside className="enterprise-highlight-panel" aria-label="Core capabilities">
            <h2>Banking workspace</h2>
            <p>One secure environment for onboarding, first login password reset, and day-to-day banking operations.</p>
            <ul>
              <li>JWT-based authentication and protected routes</li>
              <li>First-login password renewal policy</li>
              <li>Role-aware access for employees and customers</li>
              <li>Email-based temporary password onboarding</li>
            </ul>
          </aside>
        </section>

        <section id="products" className="enterprise-section">
          <h2>Products and operational modules</h2>
          <div className="enterprise-card-grid">
            <article className="enterprise-info-card">
              <h3>Customer onboarding</h3>
              <p>Create individual and corporate customers with controlled access and validation rules.</p>
            </article>
            <article className="enterprise-info-card">
              <h3>Online account access</h3>
              <p>Customers sign in with email and password, then continue to secure dashboard flows.</p>
            </article>
            <article className="enterprise-infoard">
              <h3>Operational controls</h3>
              <p>Dedicated employee area for secure customer provisioning and lifecycle administration.</p>
            </article>
          </div>
        </section>

        <section id="security" className="enterprise-section enterprise-security">
          <div>
            <h2>Security and compliance by design</h2>
            <p>
              The system follows enterprise patterns with token validation, forced password rotation on first login,
              and transport-level protection for backend API communication.
            </p>
          </div>
          <div className="enterprise-security-items">
            <p>Protected API routes for authenticated sessions</p>
            <p>Session cleanup on token expiry and unauthorized responses</p>
            <p>Temporary password delivery through configured SMTP channels</p>
          </div>
        </section>

        <section id="support" className="enterprise-cta-banner">
          <h2>Ready to continue?</h2>
          <p>Employees and customers can access their secure workspace from the sign-in page.</p>
          <Link to="/login" className="enterprise-btn enterprise-btn-primary">
            Go to sign in
          </Link>
        </section>
      </main>

      <footer className="enterprise-footer">
        <p>© 2026 BANKAI Bank System. All rights reserved.</p>
        <p>Built for secure banking operations and trusted digital customer access.</p>
      </footer>
    </div>
  );
}