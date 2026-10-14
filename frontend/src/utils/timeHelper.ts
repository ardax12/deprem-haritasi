/**
 * Formats an ISO date string into a localized relative time string in Turkish.
 * e.g., 'Az önce', '4 dk önce', '2 sa önce', 'Dün', '3 gün önce'.
 */
export function formatRelativeTime(isoStr: string | null | undefined): string {
  if (!isoStr) return '';
  try {
    const occurred = new Date(isoStr).getTime();
    if (isNaN(occurred)) return '';
    const now = Date.now();
    const diffMs = now - occurred;

    if (diffMs < 0) return 'Az önce';
    const diffMins = Math.floor(diffMs / (60 * 1000));
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Az önce';
    if (diffMins < 60) return `${diffMins} dk önce`;
    if (diffHours < 24) return `${diffHours} sa önce`;
    if (diffDays === 1) return 'Dün';
    if (diffDays < 30) return `${diffDays} gün önce`;
    return `${Math.floor(diffDays / 30)} ay önce`;
  } catch {
    return '';
  }
}
