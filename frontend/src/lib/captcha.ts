import { config } from './config';

let scriptPromise: Promise<void> | null = null;

function loadRecaptchaScript(): Promise<void> {
  if (typeof window === 'undefined') return Promise.resolve();
  if ((window as any).grecaptcha) return Promise.resolve();
  if (scriptPromise) return scriptPromise;
  scriptPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://www.google.com/recaptcha/api.js?render=${config.recaptcha.siteKey}`;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Could not load reCAPTCHA'));
    document.head.appendChild(script);
  });
  return scriptPromise;
}

/**
 * Returns a reCAPTCHA v3 token for registration. When reCAPTCHA is disabled
 * (or has no site key), returns the dev-bypass token the backend accepts
 * when its own secret is unset — production must set both sides.
 */
export async function getRegisterCaptchaToken(): Promise<string> {
  if (!config.recaptcha.enabled || !config.recaptcha.siteKey) {
    return 'dev-bypass-token';
  }
  await loadRecaptchaScript();
  const grecaptcha = (window as any).grecaptcha;
  return grecaptcha.execute(config.recaptcha.siteKey, { action: 'register' });
}
