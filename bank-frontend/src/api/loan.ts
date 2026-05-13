import { httpClient } from './http';
import type {
  LoanApplicationRequest,
  LoanApplicationResponse,
  LoanReviewHistoryItem,
  LoanReviewRequest,
} from '../types/auth';

export async function submitLoanApplication(
  request: LoanApplicationRequest,
): Promise<LoanApplicationResponse> {
  const response = await httpClient.post<LoanApplicationResponse>('/api/customer/loans/applications', request);
  return response.data;
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
