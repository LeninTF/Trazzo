/**
 * SHA-256 (hex en minúsculas) del SubjectPublicKeyInfo DER de una clave pública en PEM: la misma huella que el
 * agente compara al fijar la clave del backend (Configure-Agent.ps1 -PinnedPublicKeySha256).
 * Devuelve null si el PEM no contiene Base64 válido.
 */
export async function publicKeySha256(pem: string): Promise<string | null> {
  const body = pem.replace(/-----(BEGIN|END) [A-Z ]+-----/g, '').replace(/\s+/g, '');
  let binary: string;
  try {
    binary = atob(body);
  } catch {
    return null;
  }
  const der = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    der[i] = binary.charCodeAt(i);
  }
  const digest = await crypto.subtle.digest('SHA-256', der);
  return Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, '0')).join('');
}
