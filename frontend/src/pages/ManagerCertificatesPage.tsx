import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/Alert';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage } from '@/services/api';
import { getAcademicYears, getClasses, getStreamsByClass, getSubjects } from '@/services/academic';
import {
  downloadCertificate,
  generateCertificate,
  getCertificateCandidates,
  getCertificates,
  getEnrolledCertificateStudents,
  releaseCertificate,
  revokeCertificate,
} from '@/services/certificates';

const inputClass = 'h-10 w-full rounded-md border border-input bg-background px-3 text-sm text-foreground';

export function ManagerCertificatesPage() {
  const queryClient = useQueryClient();
  const [academicYearId, setAcademicYearId] = useState('');
  const [classId, setClassId] = useState('');
  const [streamId, setStreamId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [awardType, setAwardType] = useState('ACADEMIC_EXCELLENCE');
  const [studentId, setStudentId] = useState('');
  const [title, setTitle] = useState('Certificate of Academic Excellence');
  const [description, setDescription] = useState('');
  const [search, setSearch] = useState('');
  const [yearSearch, setYearSearch] = useState('');
  const [classSearch, setClassSearch] = useState('');
  const [streamSearch, setStreamSearch] = useState('');
  const [subjectSearch, setSubjectSearch] = useState('');
  const [error, setError] = useState('');

  const yearsQuery = useQuery({ queryKey: ['certificate-years', yearSearch], queryFn: () => getAcademicYears(0, 50, yearSearch) });
  const classesQuery = useQuery({ queryKey: ['certificate-classes', classSearch], queryFn: () => getClasses(0, 50, classSearch) });
  const subjectsQuery = useQuery({ queryKey: ['certificate-subjects', subjectSearch], queryFn: () => getSubjects(0, 50, subjectSearch) });
  const streamsQuery = useQuery({
    queryKey: ['certificate-streams', classId],
    queryFn: () => getStreamsByClass(Number(classId)),
    enabled: Boolean(classId),
  });
  const scopeReady = Boolean(academicYearId && classId);
  const academicCandidatesQuery = useQuery({
    queryKey: ['certificate-candidates', academicYearId, classId, streamId, subjectId],
    queryFn: () => getCertificateCandidates(
      Number(academicYearId), Number(classId), streamId ? Number(streamId) : null, Number(subjectId)
    ),
    enabled: scopeReady && Boolean(subjectId) && awardType === 'ACADEMIC_EXCELLENCE',
  });
  const studentsQuery = useQuery({
    queryKey: ['certificate-students', academicYearId, classId, streamId],
    queryFn: () => getEnrolledCertificateStudents(
      Number(academicYearId), Number(classId), streamId ? Number(streamId) : null
    ),
    enabled: scopeReady && awardType !== 'ACADEMIC_EXCELLENCE',
  });
  const certificatesQuery = useQuery({ queryKey: ['certificates'], queryFn: getCertificates });

  const candidates = awardType === 'ACADEMIC_EXCELLENCE'
    ? academicCandidatesQuery.data ?? []
    : studentsQuery.data ?? [];
  const certificates = useMemo(() => (certificatesQuery.data ?? []).filter((certificate) => {
    const needle = search.trim().toLocaleLowerCase();
    return !needle || [
      certificate.studentName, certificate.admissionNumber, certificate.className,
      certificate.streamName, certificate.subjectName, certificate.academicYear,
      certificate.awardType, certificate.status,
    ].some((field) => field?.toLocaleLowerCase().includes(needle));
  }), [certificatesQuery.data, search]);

  const refreshCertificates = () => queryClient.invalidateQueries({ queryKey: ['certificates'] });
  const generateMutation = useMutation({
    mutationFn: () => generateCertificate({
      studentId: Number(studentId),
      academicYearId: Number(academicYearId),
      classId: Number(classId),
      streamId: streamId ? Number(streamId) : null,
      subjectId: subjectId ? Number(subjectId) : null,
      awardType,
      title,
      description: description.trim() || undefined,
    }),
    onSuccess: async () => {
      setError('');
      setStudentId('');
      await refreshCertificates();
      await queryClient.invalidateQueries({ queryKey: ['certificate-candidates'] });
    },
    onError: (reason) => setError(getApiErrorMessage(reason, 'Unable to generate the certificate.')),
  });
  const releaseMutation = useMutation({
    mutationFn: releaseCertificate,
    onSuccess: refreshCertificates,
    onError: (reason) => setError(getApiErrorMessage(reason, 'Unable to release the certificate.')),
  });
  const revokeMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) => revokeCertificate(id, reason),
    onSuccess: refreshCertificates,
    onError: (reason) => setError(getApiErrorMessage(reason, 'Unable to revoke the certificate.')),
  });

  const selectorsLoading = yearsQuery.isLoading || classesQuery.isLoading || subjectsQuery.isLoading;
  const selectorError = yearsQuery.error || classesQuery.error || subjectsQuery.error || streamsQuery.error;

  const handleRevoke = (id: number) => {
    const reason = window.prompt('Enter the reason for revoking this certificate:')?.trim();
    if (reason) revokeMutation.mutate({ id, reason });
  };

  const handleDownload = async (id: number) => {
    try {
      await downloadCertificate(id);
    } catch (reason) {
      setError(getApiErrorMessage(reason, 'Unable to download the certificate.'));
    }
  };

  return (
    <main className="mx-auto max-w-7xl space-y-6">
      <header>
        <h1 className="text-3xl font-bold">Certificate Management</h1>
        <p className="mt-1 text-muted-foreground">Review verified academic results, prepare certificates, then release them to students.</p>
      </header>

      {selectorError && <Alert variant="destructive"><AlertTitle>Unable to load certificate options</AlertTitle><AlertDescription>{getApiErrorMessage(selectorError, 'Please refresh and try again.')}</AlertDescription></Alert>}
      {error && <Alert variant="destructive"><AlertTitle>Certificate action failed</AlertTitle><AlertDescription>{error}</AlertDescription></Alert>}

      <Card>
        <CardHeader><CardTitle>Prepare a certificate</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {selectorsLoading ? <p className="text-sm text-muted-foreground">Loading school options…</p> : (
            <>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                <label className="space-y-1 text-sm">Academic year
                  <input className={inputClass} value={yearSearch} onChange={(event) => setYearSearch(event.target.value)} placeholder="Search academic years" />
                  <select className={inputClass} value={academicYearId} onChange={(event) => { setAcademicYearId(event.target.value); setStudentId(''); }}>
                    <option value="">Select academic year</option>
                    {yearsQuery.data?.content.map((year) => <option key={year.id} value={year.id}>{year.name}</option>)}
                  </select>
                </label>
                <label className="space-y-1 text-sm">Class
                  <input className={inputClass} value={classSearch} onChange={(event) => setClassSearch(event.target.value)} placeholder="Search classes" />
                  <select className={inputClass} value={classId} onChange={(event) => { setClassId(event.target.value); setStreamId(''); setStreamSearch(''); setStudentId(''); }}>
                    <option value="">Select class</option>
                    {classesQuery.data?.content.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                  </select>
                </label>
                <label className="space-y-1 text-sm">Stream (optional)
                  <input className={inputClass} value={streamSearch} onChange={(event) => setStreamSearch(event.target.value)} placeholder="Search streams" disabled={!classId} />
                  <select className={inputClass} value={streamId} onChange={(event) => { setStreamId(event.target.value); setStudentId(''); }} disabled={!classId}>
                    <option value="">No stream</option>
                    {streamsQuery.data?.filter((stream) => stream.name.toLocaleLowerCase().includes(streamSearch.trim().toLocaleLowerCase())).map((stream) => <option key={stream.id} value={stream.id}>{stream.name}</option>)}
                  </select>
                </label>
                <label className="space-y-1 text-sm">Award type
                  <select className={inputClass} value={awardType} onChange={(event) => {
                    setAwardType(event.target.value);
                    setTitle(event.target.value === 'ACADEMIC_EXCELLENCE' ? 'Certificate of Academic Excellence' : '');
                    if (event.target.value !== 'ACADEMIC_EXCELLENCE') setSubjectId('');
                    setStudentId('');
                  }}>
                    <option value="ACADEMIC_EXCELLENCE">Academic Excellence</option>
                    <option value="PARTICIPATION">Participation</option>
                    <option value="ACHIEVEMENT">Achievement</option>
                    <option value="RECOGNITION">Recognition</option>
                  </select>
                </label>
                {awardType === 'ACADEMIC_EXCELLENCE' && (
                  <label className="space-y-1 text-sm">Subject
                    <input className={inputClass} value={subjectSearch} onChange={(event) => setSubjectSearch(event.target.value)} placeholder="Search subjects" />
                    <select className={inputClass} value={subjectId} onChange={(event) => { setSubjectId(event.target.value); setStudentId(''); }}>
                      <option value="">Select subject</option>
                      {subjectsQuery.data?.content.map((subject) => <option key={subject.id} value={subject.id}>{subject.name}</option>)}
                    </select>
                  </label>
                )}
                {awardType !== 'ACADEMIC_EXCELLENCE' && (
                  <>
                    <label className="space-y-1 text-sm">Enrolled student
                      <select className={inputClass} value={studentId} onChange={(event) => setStudentId(event.target.value)} disabled={!scopeReady}>
                        <option value="">Select student</option>
                        {studentsQuery.data?.map((student) => <option key={student.studentId} value={student.studentId}>{student.studentName} ({student.admissionNumber})</option>)}
                      </select>
                      {studentsQuery.isLoading && <span className="text-xs text-muted-foreground">Loading enrolled students…</span>}
                      {studentsQuery.error && <span className="text-xs text-destructive">{getApiErrorMessage(studentsQuery.error, 'Unable to load enrolled students.')}</span>}
                    </label>
                    <label className="space-y-1 text-sm">Certificate title
                      <input className={inputClass} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="e.g. Certificate of Participation" />
                    </label>
                    <label className="space-y-1 text-sm sm:col-span-2 lg:col-span-1">Evidence / achievement description
                      <input className={inputClass} value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Describe the verified achievement" />
                    </label>
                  </>
                )}
              </div>

              {awardType === 'ACADEMIC_EXCELLENCE' && (
                <div className="space-y-3">
                  <h3 className="font-medium">Published top results</h3>
                  {!subjectId || !scopeReady ? <p className="text-sm text-muted-foreground">Select the year, class, and subject to review eligible students.</p>
                    : academicCandidatesQuery.isLoading ? <p className="text-sm text-muted-foreground">Loading published results…</p>
                      : academicCandidatesQuery.error ? <p className="text-sm text-destructive">{getApiErrorMessage(academicCandidatesQuery.error, 'Could not load eligible results.')}</p>
                        : candidates.length === 0 ? <p className="text-sm text-muted-foreground">No valid published results are available in this scope.</p>
                          : <div className="overflow-x-auto rounded-md border"><table className="w-full text-left text-sm">
                            <thead className="bg-muted/50"><tr><th className="p-3">Student</th><th className="p-3">Admission No.</th><th className="p-3">Published marks</th><th className="p-3">Percent</th><th className="p-3">Grade</th><th className="p-3">Review</th></tr></thead>
                            <tbody>{candidates.map((student) => <tr key={`${student.studentId}-${student.resultId}`} className="border-t">
                              <td className="p-3">{student.studentName}</td><td className="p-3">{student.admissionNumber}</td>
                              <td className="p-3">{student.marks ?? '—'}</td><td className="p-3">{student.percentage ?? '—'}%</td><td className="p-3">{student.grade ?? '—'}</td>
                              <td className="p-3"><Button size="sm" variant={studentId === String(student.studentId) ? 'secondary' : 'outline'} onClick={() => setStudentId(String(student.studentId))}>{studentId === String(student.studentId) ? 'Selected' : 'Select'}</Button></td>
                            </tr>)}</tbody></table></div>}
                  {candidates.length > 1 && <p className="text-sm text-muted-foreground">The top result is tied. Review and select each qualifying student separately.</p>}
                </div>
              )}

              <Button
                disabled={!studentId || !academicYearId || !classId || (awardType === 'ACADEMIC_EXCELLENCE' && !subjectId)
                  || (awardType !== 'ACADEMIC_EXCELLENCE' && (!title.trim() || !description.trim())) || generateMutation.isPending}
                onClick={() => generateMutation.mutate()}
              >
                {generateMutation.isPending ? 'Generating…' : 'Generate draft certificate'}
              </Button>
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="gap-3 sm:flex-row sm:items-center sm:justify-between">
          <CardTitle>Certificates</CardTitle>
          <input className={`${inputClass} sm:max-w-sm`} value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search student, class, subject, year, type, or status" />
        </CardHeader>
        <CardContent>
          {certificatesQuery.isLoading ? <p className="text-sm text-muted-foreground">Loading certificates…</p>
            : certificatesQuery.error ? <p className="text-sm text-destructive">{getApiErrorMessage(certificatesQuery.error, 'Unable to load certificates.')}</p>
              : certificates.length === 0 ? <p className="text-sm text-muted-foreground">No certificates match this search.</p>
                : <div className="overflow-x-auto"><table className="w-full text-left text-sm">
                  <thead><tr className="border-b text-muted-foreground"><th className="p-3">Student</th><th className="p-3">Award</th><th className="p-3">Scope</th><th className="p-3">Number / version</th><th className="p-3">Status</th><th className="p-3">Status date</th><th className="p-3">Actions</th></tr></thead>
                  <tbody>{certificates.map((certificate) => <tr key={certificate.id} className="border-b last:border-0">
                    <td className="p-3"><div className="font-medium">{certificate.studentName}</div><div className="text-xs text-muted-foreground">{certificate.admissionNumber}</div></td>
                    <td className="p-3">{certificate.title}</td>
                    <td className="p-3">{[certificate.className, certificate.streamName, certificate.subjectName, certificate.academicYear].filter(Boolean).join(' · ')}</td>
                    <td className="p-3">{certificate.certificateNumber} · v{certificate.version}</td>
                    <td className="p-3">{certificate.status}</td>
                    <td className="p-3">{certificate.status === 'REVOKED' && certificate.revokedAt
                      ? `Revoked ${new Date(certificate.revokedAt).toLocaleDateString()}`
                      : certificate.issuedAt ? new Date(certificate.issuedAt).toLocaleDateString() : '—'}</td>
                    <td className="space-x-2 whitespace-nowrap p-3">
                      {certificate.status === 'GENERATED' && <Button size="sm" onClick={() => releaseMutation.mutate(certificate.id)} disabled={releaseMutation.isPending}>Release</Button>}
                      {certificate.status === 'RELEASED' && <>
                        <Button size="sm" variant="outline" onClick={() => void handleDownload(certificate.id)}>PDF</Button>
                        <Button size="sm" variant="destructive" onClick={() => handleRevoke(certificate.id)} disabled={revokeMutation.isPending}>Revoke</Button>
                      </>}
                      {certificate.status === 'REVOKED' && <span className="text-xs text-muted-foreground" title={certificate.revocationReason ?? ''}>Revoked</span>}
                    </td>
                  </tr>)}</tbody></table></div>}
        </CardContent>
      </Card>
    </main>
  );
}
