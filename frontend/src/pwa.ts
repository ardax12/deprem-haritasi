// PWA Service Worker & Install Management

export function registerServiceWorker() {
  if (typeof window !== 'undefined' && 'serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker
        .register('/sw.js')
        .then((registration) => {
          console.log('Service Worker kayıt başarılı:', registration.scope);
        })
        .catch((error) => {
          console.warn('Service Worker kaydı başarısız:', error);
        });
    });
  }
}
