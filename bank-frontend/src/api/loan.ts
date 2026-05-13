import { httpClient } from './http';
import type {
  CustomerLoanApplicationStatusResponse,
  LoanApplicationRequest,
  LoanApplicationResponse,
  LoanReviewHistoryItem,
  LoanReviewRequest,
} from '../types/auth';

export async function submitLoanApplication(
  request: LoanApplicationRequest,
): Promise<CustomerLoanApplicationStatusResponse> {
  const response = await httpClient.post<CustomerLoanApplicationStatusResponse>('/api/customer/loans/applications', request);
  return response.data;
}

export async function getLatestCustomerLoanApplication(): Promise<CustomerLoanApplicationStatusResponse | null> {
  const response = await httpClient.get<CustomerLoanApplicationStatusResponse | ''>(
    '/api/customer/loans/applications/latest',
    { validateStatus: (status) => status === 200 || status === 204 },
  );
  return response.status === 204 ? null : response.data as CustomerLoanApplicationStatusResponse;
}

export async function getPendingLoanApplications(): Promise<LoanApplicationResponse[]> {
  const response = await httpClient.get<LoanApplicationResponse[]>('/api/employee/loans/applications/pending');
  return response.data;
}

export async function approveLoanApplication(
  loanId: number,
  request: LoanReviewRequest,
): Promise<LoanApplicationResponse> {
  const response = await httpClient.post<LoanApplicationResponse>(
    `/api/employee/loans/applications/${loanId}/approve`,
    request,
  );
  return response.data;
}

export async function rejectLoanApplication(
  loanId: number,
  request: LoanReviewRequest,
): Promise<LoanApplicationResponse> {
  const response = await httpClient.post<LoanApplicationResponse>(
    `/api/employee/loans/applications/${loanId}/reject`,
    request,
  );
  return response.data;
}

export async function getLoanReviewHistory(): Promise<LoanReviewHistoryItem[]> {
  const response = await httpClient.get<LoanReviewHistoryItem[]>('/api/employee/loans/applications/history');
  return response.data;
}
