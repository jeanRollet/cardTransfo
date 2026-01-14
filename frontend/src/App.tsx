import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import CardList from './components/CardList';
import CardDetail from './components/CardDetail';
import AccountList from './components/AccountList';
import TransactionList from './components/TransactionList';
import TransactionAdd from './components/TransactionAdd';
import BillPayment from './components/BillPayment';
import PendingAuthorizations from './components/PendingAuthorizations';
import ReportList from './components/ReportList';

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />;
};

const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return isAuthenticated ? <Navigate to="/dashboard" /> : <>{children}</>;
};

const App: React.FC = () => {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicRoute>
            <Login />
          </PublicRoute>
        }
      />
      <Route
        path="/dashboard"
        element={
          <PrivateRoute>
            <Dashboard />
          </PrivateRoute>
        }
      />
      <Route
        path="/cards"
        element={
          <PrivateRoute>
            <CardList />
          </PrivateRoute>
        }
      />
      <Route
        path="/cards/:cardNumber"
        element={
          <PrivateRoute>
            <CardDetail />
          </PrivateRoute>
        }
      />
      <Route
        path="/accounts"
        element={
          <PrivateRoute>
            <AccountList />
          </PrivateRoute>
        }
      />
      <Route
        path="/transactions"
        element={
          <PrivateRoute>
            <TransactionList />
          </PrivateRoute>
        }
      />
      <Route
        path="/transactions/add"
        element={
          <PrivateRoute>
            <TransactionAdd />
          </PrivateRoute>
        }
      />
      <Route
        path="/bill-payment"
        element={
          <PrivateRoute>
            <BillPayment />
          </PrivateRoute>
        }
      />
      <Route
        path="/authorizations"
        element={
          <PrivateRoute>
            <PendingAuthorizations />
          </PrivateRoute>
        }
      />
      <Route
        path="/reports"
        element={
          <PrivateRoute>
            <ReportList />
          </PrivateRoute>
        }
      />
      <Route path="/" element={<Navigate to="/dashboard" />} />
      <Route path="*" element={<Navigate to="/dashboard" />} />
    </Routes>
  );
};

export default App;
