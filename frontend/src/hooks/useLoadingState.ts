import { useState, useCallback } from 'react';

/**
 * Asenkron işlemler için yüklenme (loading) ve hata (error) durumlarını yöneten hook.
 */
export function useLoadingState() {
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Verilen asenkron fonksiyonu çalıştırır, 
   * işlem sırasında `loading` durumunu true yapar, 
   * işlem bitince false yapar ve olası hataları yakalayıp `error` state'ine set eder.
   * @param asyncFn Çalıştırılacak asenkron fonksiyon.
   * @param errorMessage Opsiyonel özel hata mesajı.
   */
  const wrapAsync = useCallback(async <T>(asyncFn: () => Promise<T>, errorMessage?: string): Promise<T | undefined> => {
    setLoading(true);
    setError(null);
    try {
      const result = await asyncFn();
      return result;
    } catch (err: unknown) {
      const defaultMessage = 'İşlem sırasında bir hata oluştu.';
      console.error(errorMessage || defaultMessage, err);
      
      let displayError = errorMessage || defaultMessage;
      // Hata nesnesinin yapısını kontrol ederek özelliklere güvenli erişim
      if (typeof err === 'object' && err !== null && 'response' in err) {
        const response = err.response; // Daha güvenli erişim için tip ataması (veya daha spesifik tip guard)
        if (response && typeof response === 'object' && 'data' in response && typeof response.data === 'string') {
          displayError = response.data; // Backend'den gelen hata mesajı?
        }
      } else if (err instanceof Error) { // Standart Error nesnesi mi kontrol et
        displayError = err.message; // Genel JS hatası
      }
      
      setError(displayError);
      return undefined; // Hata durumunda undefined döndür
    } finally {
      setLoading(false);
    }
  }, [setLoading, setError]); // Bağımlılıklar doğru ayarlandı

  return { loading, error, setLoading, setError, wrapAsync };
} 