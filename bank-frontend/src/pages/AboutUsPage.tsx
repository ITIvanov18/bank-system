import { Link } from 'react-router-dom';
import '../index.css';

const teamMembers = [
  {
    name: 'Bank AI',
    role: 'CEO & Founder',
    image: 'https://i.pravatar.cc/150?u=elena'
  },
  {
    name: 'Plamen Kostov',
    role: 'Full-Stack Developer',
    image: 'https://avatars.githubusercontent.com/u/56884945?v=4'
  },
  {
    name: 'Ivan Ivanov',
    role: 'Chief Security Officer',
    image: 'https://avatars.githubusercontent.com/u/59703243?v=4'
  },
  {
    name: 'Ilian Yanev',
    role: 'Head of UI Design',
    image: 'https://scontent.fsof11-1.fna.fbcdn.net/v/t39.30808-1/370427029_1902288020157684_5149162200906890611_n.jpg?stp=dst-jpg_s200x200_tt6&_nc_cat=107&ccb=1-7&_nc_sid=e99d92&_nc_ohc=NCYj9qZwOEMQ7kNvwFxy7Th&_nc_oc=AdpGsiYn0mj62A-Gjm9by4rVEcIbvL-S6iII2XsNnxcRq77eovEUPsiAJKDrUjABINc&_nc_zt=24&_nc_ht=scontent.fsof11-1.fna&_nc_gid=DvOL5tgG5GOMI8e76mzAqw&_nc_ss=7b2a8&oh=00_Af4KsHYuOMUXYpydjX-im4Dr9eWyFJ0Hp_VifAiuqNZkdA&oe=6A0A6561'
  }
];

export function AboutUsPage() {
  return (
    <div className="enterprise-home">
      <style>
        {`
          .custom-nav-pill {
            padding: 0.5rem 1.2rem;
            border-radius: 20px;
            text-decoration: none;
            color: #333;
            font-weight: 500;
            transition: all 0.3s ease;
            display: inline-block;
          }
          .custom-nav-pill:hover { transform: translateY(-2px); }
          .custom-nav-pill-blue-light { background-color: rgba(0, 123, 255, 0.1); color: #007bff; }

          .team-avatar {
            width: 65px;
            height: 65px;
            border-radius: 50%;
            border: 2px solid #007bff;
            padding: 2px;
            background: white;
            object-fit: cover;
          }

          .team-grid-2x2 {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 2.5rem;
          }

          .timeline-vertical {
            position: relative;
            padding-left: 2rem;
            margin-top: 1.5rem;
          }
          .timeline-vertical::before {
            content: "";
            position: absolute;
            top: 8px;
            bottom: 8px;
            left: 7px;
            width: 2px;
            background: #e2e8f0;
            z-index: 0;
          }
          .timeline-node-v {
            position: relative;
            z-index: 1;
            margin-bottom: 2rem;
          }
          .timeline-node-v:last-child { margin-bottom: 0; }
          .timeline-marker-v {
            position: absolute;
            left: -2rem;
            width: 16px;
            height: 16px;
            background: white;
            border: 3px solid #007bff;
            border-radius: 50%;
            top: 4px;
            box-shadow: 0 0 0 4px rgba(0, 123, 255, 0.1);
          }
          .timeline-content-v h3 {
            font-size: 1.25rem;
            font-weight: 800;
            color: white;
            margin-bottom: 0.25rem;
            line-height: 1;
          }
          .timeline-content-v p {
            font-size: 0.95rem;
            color: #64748b;
            line-height: 1.4;
            margin: 0;
          }
        `}
      </style>

      <div className="background-shapes">
        <div className="shape shape-1"></div>
        <div className="shape shape-2"></div>
        <div className="shape shape-3"></div>
      </div>

      <header className="enterprise-nav">
        <div style={{ display: 'flex', alignItems: 'center', gap: '3rem' }}>
          <Link to="/" className="enterprise-logo-wrap" style={{ textDecoration: 'none' }}>
            <img src="/bankai-logo.png" alt="BANKλI" style={{ width: '60px' }} />
            <div>
              <span className="enterprise-logo-text">BANKλI</span>
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
              Founded in 2026, BANKλI transformed from an ambitious AI research project into a global financial powerhouse. We are the first institution to eliminate bureaucracy through fully automated, intelligent core banking systems.
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
                  <p>Established. BANKλI officially launched for global banking.</p>
                </div>
              </div>
            </div>
          </aside>
        </section>

        <section className="enterprise-section enterprise-security" style={{ borderRadius: '32px', marginBottom: '0' }}>
          <div>
            <h2>The Minds Behind BANKλI</h2>
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
        <p>© 2026 BANKλI Bank System. All rights reserved.</p>
      </footer>
    </div>
  );
}