import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { getApiErrorMessage } from '@/services/api';
import { downloadCertificate, getMyCertificates } from '@/services/certificates';
import { useState } from 'react';

export function StudentCertificatesPage() {
  const [downloadError, setDownloadError] = useState('');
  const certificatesQuery = useQuery({
    queryKey: ['my-certificates'],
    queryFn: getMyCertificates,
  });

  const handleDownload = async (id: number, print: boolean) => {
    try {
      setDownloadError('');
      await downloadCertificate(id, print);
    } catch (error) {
      setDownloadError(getApiErrorMessage(error, 'Unable to open this certificate.'));
    }
  };

  return (
    <main className="mx-auto max-w-5xl space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold">My certificates</h1>
          <p className="mt-1 text-muted-foreground">Certificates released by your school.</p>
        </div>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground" to="/school/student/dashboard">Back to dashboard</Link>
      </header>
      {downloadError && <p role="alert" className="text-sm text-destructive">{downloadError}</p>}
      <Card>
        <CardHeader><CardTitle>Issued certificates</CardTitle></CardHeader>
        <CardContent>
          {certificatesQuery.isLoading ? <p className="text-sm text-muted-foreground">Loading certificates…</p>
            : certificatesQuery.error ? <p role="alert" className="text-sm text-destructive">{getApiErrorMessage(certificatesQuery.error, 'Unable to load your certificates.')}</p>
              : !certificatesQuery.data?.length ? <p className="text-sm text-muted-foreground">No certificates have been released to you yet.</p>
                : <div className="space-y-3">
                  {certificatesQuery.data.map((certificate) => (
                    <article key={certificate.id} className="flex flex-col gap-4 rounded-lg border p-4 sm:flex-row sm:items-center sm:justify-between">
                      <div className="space-y-1">
                        <h2 className="font-semibold">{certificate.title}</h2>
                        <p className="text-sm text-muted-foreground">
                          {[certificate.awardType.replace(/_/g, ' '), certificate.subjectName, certificate.className, certificate.streamName, certificate.academicYear]
                            .filter(Boolean).join(' · ')}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {certificate.certificateNumber} · Issued {certificate.issuedAt ? new Date(certificate.issuedAt).toLocaleDateString() : '—'}
                        </p>
                        <p className={`text-sm font-medium ${certificate.status === 'REVOKED' ? 'text-destructive' : 'text-emerald-700 dark:text-emerald-400'}`}>
                          {certificate.status === 'REVOKED' ? `Revoked${certificate.revokedAt ? ` on ${new Date(certificate.revokedAt).toLocaleDateString()}` : ''}` : 'Released and valid'}
                        </p>
                        {certificate.status === 'REVOKED' && certificate.revocationReason && <p className="text-sm text-muted-foreground">{certificate.revocationReason}</p>}
                      </div>
                      {certificate.status === 'RELEASED' && (
                        <div className="flex gap-2">
                          <Button variant="outline" onClick={() => void handleDownload(certificate.id, true)}>Preview / print</Button>
                          <Button onClick={() => void handleDownload(certificate.id, false)}>Download PDF</Button>
                        </div>
                      )}
                    </article>
                  ))}
                </div>}
        </CardContent>
      </Card>
    </main>
  );
}
