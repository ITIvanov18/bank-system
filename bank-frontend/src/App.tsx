import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import { SessionExpiryGuard } from './components/SessionExpiryGuard';
import { DashboardPage } from './pages/DashboardPage';
import { FirstLoginPage } from './pages/FirstLoginPage';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { AdminOnboardingPage } from './pages/AdminOnboardingPage';
import { PasswordResetPage } from './pages/PasswordResetPage';
import { AboutUsPage } from './pages/AboutUsPage';

function App() {
    return (
        <BrowserRouter>
            <SessionExpiryGuard />
            <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/reset-password" element={<PasswordResetPage />} />
                <Route
                    path="/first-login"
                    element={
                        <ProtectedRoute requireFirstLogin>
                            <FirstLoginPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/app"
                    element={
                        <ProtectedRoute denyFirstLogin allowedRoles={['CUSTOMER']}>
                            <DashboardPage />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/dashboard"
                    element={<Navigate to="/app" replace />}
                />
                <Route
                    path="/admin/secret-onboarding"
                    element={
                        <ProtectedRoute denyFirstLogin allowedRoles={['EMPLOYEE']}>
                            <AdminOnboardingPage />
                        </ProtectedRoute>
                    }
                />
                <Route path="/about" element={<AboutUsPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
