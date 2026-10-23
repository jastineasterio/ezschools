import { api, unwrapApiResponse } from './api';
import type { ApiResponse } from '@/types/api';

export interface Certificate {
  id: number;
  studentId: number;
  studentName: string;
  admissionNumber: string;
  academicYearId: number;
  academicYear: string;
  classId: number;
  className: string;
  streamId: number | null;
  streamName: string | null;
  subjectId: number | null;
  subjectName: string | null;
  resultId: number | null;
  percentage: number | null;
  awardType: string;
  title: string;
  description: string | null;
  certificateNumber: string;
  verificationCode: string;
  status: 'GENERATED' | 'RELEASED' | 'REVOKED';
  version: number;
  issuedAt: string | null;
  revokedAt: string | null;
  revocationReason: string | null;
}

export interface EligibleStudent {
  studentId: number;
  studentName: string;
  admissionNumber: string;
  resultId: number | null;
  marks: number | null;
  percentage: number | null;
  grade: string | null;
  gradingSchemeId: number | null;
}

export interface CreateCertificateRequest {
  studentId: number;
  academicYearId: number;
  classId: number;
  streamId: number | null;
  subjectId: number | null;
  awardType: string;
  title: string;
  description?: string;
}

export async function getCertificates() {
  return unwrapApiResponse(api.get<ApiResponse<Certificate[]>>('/school/certificates'));
}

export async function getMyCertificates() {
  return unwrapApiResponse(api.get<ApiResponse<Certificate[]>>('/school/certificates/mine'));
}

export async function getCertificateCandidates(
  academicYearId: number,
  classId: number,
  streamId: number | null,
  subjectId: number
) {
  return unwrapApiResponse(api.get<ApiResponse<EligibleStudent[]>>('/school/certificates/eligible', {
    params: { academicYearId, classId, streamId, subjectId },
  }));
}

export async function getEnrolledCertificateStudents(
  academicYearId: number,
  classId: number,
  streamId: number | null
) {
  return unwrapApiResponse(api.get<ApiResponse<EligibleStudent[]>>('/school/certificates/students', {
    params: { academicYearId, classId, streamId },
  }));
}

export async function generateCertificate(request: CreateCertificateRequest) {
  return unwrapApiResponse(api.post<ApiResponse<Certificate>>('/school/certificates', request));
}

export async function releaseCertificate(id: number) {
  return unwrapApiResponse(api.post<ApiResponse<Certificate>>(`/school/certificates/${id}/release`));
}

export async function revokeCertificate(id: number, reason: string) {
  return unwrapApiResponse(api.post<ApiResponse<Certificate>>(`/school/certificates/${id}/revoke`, undefined, {
    params: { reason },
  }));
}

export async function downloadCertificate(id: number, print = false) {
  const response = await api.get<Blob>(`/school/certificates/${id}/document`, { responseType: 'blob' });
  const url = URL.createObjectURL(response.data);
  if (print) {
    const printWindow = window.open(url, '_blank');
    if (!printWindow) {
      URL.revokeObjectURL(url);
      throw new Error('Allow pop-ups to preview and print this certificate.');
    }
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  } else {
    const link = document.createElement('a');
    link.href = url;
    link.download = `certificate-${id}.pdf`;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }
}
