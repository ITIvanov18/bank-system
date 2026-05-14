import { useEffect, useRef, useState } from 'react';
import type { LoanStatus, LoanType } from '../../types/auth';

export type LoanApplicationDraft = {
  loanType: LoanType;
  principalAmount: string;
  repaymentTermMonths: string;
};

export const loanProductLimits = {
  CONSUMER: {
    label: 'Consumer loan',
    baseRate: 5.95,
    minimumRate: 5.2,
    maximumRate: 6.7,
    minimumPrincipalAmount: 1_000,
    maximumPrincipalAmount: 40_000,
    principalStepAmount: 5,
    minimumRepaymentTermMonths: 6,
    maximumRepaymentTermMonths: 120,
    monthlyServiceFee: 2.5,
    upfrontFees: {
      analysis: 0,
      collateralAssessment: 0,
    },
  },
  MORTGAGE: {
    label: 'Mortgage loan',
    baseRate: 3.05,
    minimumRate: 2.65,
    maximumRate: 3.45,
    minimumPrincipalAmount: 3_000,
    maximumPrincipalAmount: 500_000,
    principalStepAmount: 500,
    minimumRepaymentTermMonths: 12,
    maximumRepaymentTermMonths: 360,
    monthlyServiceFee: 10,
    upfrontFees: {
      analysis: 200,
      collateralAssessment: 100,
    },
  },
} as const;

export const monthsInYear = 12;
export const shortMortgageTermThresholdMonths = 36;
export type LoanProductLimits = typeof loanProductLimits[keyof typeof loanProductLimits];

export function formatMoney(value: number | null | undefined) {
  return `${(value ?? 0).toLocaleString('bg-BG', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })} EUR`;
}

export function formatDebt(value: number | null | undefined) {
  const debtAmount = Math.max(value ?? 0, 0);
  return debtAmount === 0 ? formatMoney(0) : `-${formatMoney(debtAmount)}`;
}

export function useCountUp(value: number | null | undefined, durationMs = 1500) {
  const targetValue = value ?? 0;
  const [displayValue, setDisplayValue] = useState(targetValue);
  const previousValueRef = useRef(targetValue);

  useEffect(() => {
    if (typeof window === 'undefined') {
      setDisplayValue(targetValue);
      return;
    }
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setDisplayValue(targetValue);
      return;
    }
    const startValue = previousValueRef.current;
    const difference = targetValue - startValue;
    const startTime = window.performance.now();
    let animationFrameId = 0;
    previousValueRef.current = targetValue;
    function animate(currentTime: number) {
      const progress = Math.min((currentTime - startTime) / durationMs, 1);
      const easedProgress = 1 - ((1 - progress) ** 3);
      setDisplayValue(startValue + (difference * easedProgress));
      if (progress < 1) {
        animationFrameId = window.requestAnimationFrame(animate);
      }
    }
    animationFrameId = window.requestAnimationFrame(animate);
    return () => window.cancelAnimationFrame(animationFrameId);
  }, [targetValue, durationMs]);

  return displayValue;
}

export function formatOptionalMoney(value: number | null) {
  return value === null ? '-' : formatMoney(value);
}

export function formatAmountLimits(product: LoanProductLimits) {
  return `${formatMoney(product.minimumPrincipalAmount)} - ${formatMoney(product.maximumPrincipalAmount)}`;
}

export function formatTermLimits(product: LoanProductLimits) {
  return `${product.minimumRepaymentTermMonths} - ${product.maximumRepaymentTermMonths} months`;
}

export function getLoanApplicationValidationMessage(loanApplicationDraft: LoanApplicationDraft) {
  const product = loanProductLimits[loanApplicationDraft.loanType];
  const principalAmount = Number(loanApplicationDraft.principalAmount);
  const repaymentTermMonths = Number(loanApplicationDraft.repaymentTermMonths);

  if (!Number.isFinite(principalAmount) || principalAmount <= 0) {
    return 'Enter a valid loan amount.';
  }
  if (principalAmount < product.minimumPrincipalAmount) {
    return `Minimum amount for ${product.label.toLowerCase()} is ${formatMoney(product.minimumPrincipalAmount)}.`;
  }
  if (principalAmount > product.maximumPrincipalAmount) {
    return `Maximum amount for ${product.label.toLowerCase()} is ${formatMoney(product.maximumPrincipalAmount)}.`;
  }
  const stepRemainder = (principalAmount - product.minimumPrincipalAmount) % product.principalStepAmount;
  if (Math.abs(stepRemainder) > 0.000001 && Math.abs(stepRemainder - product.principalStepAmount) > 0.000001) {
    return `Amount must increase in steps of ${formatMoney(product.principalStepAmount)} for ${product.label.toLowerCase()}.`;
  }
  if (!Number.isInteger(repaymentTermMonths) || repaymentTermMonths <= 0) {
    return 'Enter a valid repayment term in months.';
  }
  if (product.minimumRepaymentTermMonths !== null && repaymentTermMonths < product.minimumRepaymentTermMonths) {
    return `Minimum term for ${product.label.toLowerCase()} is ${product.minimumRepaymentTermMonths} months.`;
  }
  if (repaymentTermMonths > product.maximumRepaymentTermMonths) {
    return `Maximum term for ${product.label.toLowerCase()} is ${product.maximumRepaymentTermMonths} months.`;
  }
  return null;
}

export function formatCustomerReference(customerId: number | null | undefined) {
  if (!customerId) return 'Not assigned';
  return `BK-${customerId.toString().padStart(6, '0')}`;
}

export function formatLoanType(value: LoanType) {
  return value === 'MORTGAGE' ? 'Mortgage loan' : 'Consumer loan';
}

export function formatLoanStatus(value: LoanStatus) {
  return value === 'ACTIVE' ? 'APPROVED' : value;
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

export function calculateEstimatedAnnualInterestRate(loanApplicationDraft: LoanApplicationDraft) {
  const product = loanProductLimits[loanApplicationDraft.loanType];
  const principalAmount = Number(loanApplicationDraft.principalAmount);
  const repaymentTermMonths = Number(loanApplicationDraft.repaymentTermMonths);

  if (!Number.isFinite(principalAmount) || !Number.isFinite(repaymentTermMonths)) {
    return product.baseRate;
  }

  const amountRange = product.maximumPrincipalAmount - product.minimumPrincipalAmount;
  const amountUtilization = Math.min(Math.max((principalAmount - product.minimumPrincipalAmount) / amountRange, 0), 1);
  const minimumTerm = product.minimumRepaymentTermMonths ?? 1;
  const termRange = product.maximumRepaymentTermMonths - minimumTerm;
  const termUtilization = Math.min(Math.max((repaymentTermMonths - minimumTerm) / termRange, 0), 1);

  if (loanApplicationDraft.loanType === 'MORTGAGE') {
    const shortTermRisk = repaymentTermMonths >= shortMortgageTermThresholdMonths
      ? 0
      : (shortMortgageTermThresholdMonths - repaymentTermMonths) / (shortMortgageTermThresholdMonths - 1);
    const termRisk = Math.max(termUtilization, shortTermRisk);
    const mortgageRiskScore = Math.min(termRisk * 0.65 + (1 - amountUtilization) * 0.35, 1);
    return product.minimumRate + (product.maximumRate - product.minimumRate) * mortgageRiskScore;
  }

  const consumerRiskScore = Math.min((1 - amountUtilization) * 0.65 + (1 - termUtilization) * 0.35, 1);
  return product.minimumRate + (product.maximumRate - product.minimumRate) * consumerRiskScore;
}

export function calculateMonthlyPayment(principalAmount: number, annualInterestRate: number, repaymentTermMonths: number) {
  const monthlyInterestRate = annualInterestRate / 100 / monthsInYear;
  if (monthlyInterestRate === 0) return principalAmount / repaymentTermMonths;
  const compoundFactor = (1 + monthlyInterestRate) ** repaymentTermMonths;
  return principalAmount * monthlyInterestRate * compoundFactor / (compoundFactor - 1);
}

export function calculateApr(principalAmount: number, monthlyPaymentWithFee: number, repaymentTermMonths: number, upfrontFees: number) {
  const netReceivedAmount = principalAmount - upfrontFees;
  if (netReceivedAmount <= 0) return 0;
  let low = 0;
  let high = 1;
  for (let i = 0; i < 80; i++) {
    const middle = (low + high) / 2;
    let presentValue = 0;
    for (let month = 1; month <= repaymentTermMonths; month++) {
      presentValue += monthlyPaymentWithFee / ((1 + middle) ** month);
    }
    if (presentValue > netReceivedAmount) low = middle; else high = middle;
  }
  return (((1 + (low + high) / 2) ** monthsInYear) - 1) * 100;
}

export function calculateFullLoanDetails(
  loanApplicationDraft: LoanApplicationDraft,
  estimatedAnnualInterestRate: number | null,
  selectedLoanProduct: LoanProductLimits,
  validationMessage: string | null
) {
  const principalAmount = Number(loanApplicationDraft.principalAmount);
  const repaymentTermMonths = Number(loanApplicationDraft.repaymentTermMonths);

  if (
    validationMessage ||
    estimatedAnnualInterestRate === null ||
    !Number.isFinite(principalAmount) ||
    !Number.isFinite(repaymentTermMonths) ||
    repaymentTermMonths <= 0
  ) {
    return { monthlyPayment: null, monthlyPaymentWithFee: null, totalInterest: null, totalDueAmount: null, apr: null };
  }

  const monthlyPayment = calculateMonthlyPayment(principalAmount, estimatedAnnualInterestRate, repaymentTermMonths);
  const monthlyPaymentWithFee = monthlyPayment + selectedLoanProduct.monthlyServiceFee;
  const totalInterest = monthlyPayment * repaymentTermMonths - principalAmount;
  const upfrontFees = selectedLoanProduct.upfrontFees.analysis + selectedLoanProduct.upfrontFees.collateralAssessment;
  const totalDueAmount = monthlyPaymentWithFee * repaymentTermMonths + upfrontFees;
  const apr = calculateApr(principalAmount, monthlyPaymentWithFee, repaymentTermMonths, upfrontFees);

  return { monthlyPayment, monthlyPaymentWithFee, totalInterest, totalDueAmount, apr };
}
