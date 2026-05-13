import { Link } from 'react-router-dom';
import { teamMembers } from './js/aboutUsLogic';
import '../index.css';
import './css/AboutUsPage.css';

export function AboutUsPage() {
  return (
    <div className="enterprise-home">
      <div className="background-shapes">
        <div className="shape shape-1"></div>
        <div className="shape shape-2"></div>
        <div className="shape shape-3"></div>
      </div>

      <header className="enterprise-nav">
        <div style={{ display: 'flex', alignItems: 'center', gap: '3rem' }}>
          <Link to="/" className="enterprise-logo-wrap" style={{ textDecoration: 'none' }}>
            <img src="/bankai-logo.png" alt="BANKΛI" style={{ width: '60px' }} />
            <div>
              <span className="enterprise-logo-text">BANKΛI</span>
              <span className="enterprise-logo-subtitle">Premium AI FinTech</span>
            </div>
          </Link>
          <nav style={{ display: 'flex', gap: '1rem' }}>
            <Link to="/" className="custom-nav-pill custom-nav-pill-blue-light">Home</Link>
            <Link to="/about" className="custom-nav-pill custom-nav-pill-blue-light" style={{ backgroundColor: 'rgba(0, 123, 255, 0.2)' }}>About Us</Link>
          </nav>
        </div>
        <Link to="/login" className="enterprise-btn enterprise-btn-secondary">Sign in</Link>
      </header>

      <main className="enterprise-main">
        <section className="enterprise-hero enterprise-section">
          <div className="enterprise-hero-content">
            <div className="brand-badge">Our Story</div>
            <h1 className="main-title enterprise-title">
              Banking at the speed
              <span className="gradient-text"> of thought</span>
            </h1>
            <p className="subtitle enterprise-subtitle" style={{ maxWidth: '700px', lineHeight: '1.6' }}>
              Founded in 2026, BANKΛI transformed from an ambitious AI research project into a global financial powerhouse. We are the first institution to eliminate bureaucracy through fully automated, intelligent core banking systems.
            </p>

            <div className="enterprise-metrics" aria-label="Business metrics" style={{ marginTop: '3rem' }}>
              <article className="enterprise-metric-card">
                <p className="enterprise-metric-value">1.2M+</p>
                <p className="enterprise-metric-label">Active Users</p>
              </article>
              <article className="enterprise-metric-card">
                <p className="enterprise-metric-value">0.01s</p>
                <p className="enterprise-metric-label">Risk Assessment</p>
              </article>
              <article className="enterprise-metric-card">
                <p className="enterprise-metric-value">€5B+</p>
                <p className="enterprise-metric-label">Processed Safely</p>
              </article>
            </div>
          </div>

          <aside className="enterprise-highlight-panel" aria-label="Our Journey">
            <h2><b>The Journey</b></h2>
            <p style={{ marginBottom: '1rem' }}>How we evolved from a concept to a global financial standard.</p>

            <div className="timeline-vertical">
              <div className="timeline-node-v">
                <div className="timeline-marker-v"></div>
                <div className="timeline-content-v">
                  <h3>2018</h3>
                  <p>Idea Emerged. Concept for a decentralized AI core was born.</p>
                </div>
              </div>

              <div className="timeline-node-v">
                <div className="timeline-marker-v"></div>
                <div className="timeline-content-v">
                  <h3>2020</h3>
                  <p>Implementation started. Building the production-grade engine.</p>
                </div>
              </div>

              <div className="timeline-node-v">
                <div className="timeline-marker-v"></div>
                <div className="timeline-content-v">
                  <h3>2026</h3>
                  <p>Established. BANKΛI officially launched for global banking.</p>
                </div>
              </div>
            </div>
          </aside>
        </section>

        <section className="enterprise-section enterprise-security" style={{ borderRadius: '32px', marginBottom: '0' }}>
          <div>
            <h2>The Minds Behind BANKΛI</h2>
            <p style={{ color: '#94a3b8' }}>
              A collective of security specialists and AI researchers dedicated to excellence.
            </p>
          </div>
          <div className="team-grid-2x2">
            {teamMembers.map((member, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '1.2rem' }}>
                <img src={member.image} alt={member.name} className="team-avatar" />
                <div>
                  <h4 style={{ margin: 0, fontSize: '1.2rem', color: 'white' }}>{member.name}</h4>
                  <p style={{ margin: 0, fontSize: '0.95rem', color: '#94a3b8' }}>{member.role}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="enterprise-section" style={{ paddingTop: '10px', paddingBottom: '60px', margin: '0 auto' }}>
          <h2 style={{ marginBottom: '1.5rem', fontSize: '2.6rem' }}>Our Core Pillars</h2>
          <div className="enterprise-card-grid" style={{ gap: '1.5rem' }}>
            <article className="enterprise-info-card">
              <h3>Secure DNA🧬</h3>
              <p>Mathematical certainty in every transaction with AES-256 and JWT protection.</p>
            </article>
            <article className="enterprise-info-card">
              <h3>Predictive Tech🚀</h3>
              <p>Our AI engine anticipates market shifts to protect and grow user capital.</p>
            </article>
            <article className="enterprise-info-card">
              <h3>Borderless Finance🌎</h3>
              <p>Removing geographic barriers to provide high-end banking to the modern world.</p>
            </article>
          </div>
        </section>

      </main>

      <footer className="enterprise-footer">
        <p>© 2026 BANKΛI Bank System. All rights reserved.</p>
      </footer>
    </div>
  );
}