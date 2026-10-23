import { publicKeySha256 } from './public-key-fingerprint';

describe('publicKeySha256', () => {
  // Clave pública del vector de interoperabilidad del backend (test-only). Valor esperado calculado aparte como
  // SHA-256 del SubjectPublicKeyInfo DER: lo mismo que compara el agente al fijar la clave (-PinnedPublicKeySha256).
  const PEM = `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6kF8CaGA8o0IKiaosXoZ
nmTkqjjDqA0QbBxw7gw0ddmbTyMuIQdyWFO/sUbq/Ao164w+zYL+mqyB7PuaDW4Q
bJdTAcVyoEHmiVf31ykpnmZRMsvyfQgWzAOl9yJ90gbA/HoVjS06S9/rPBBzTnwH
1E6KpOsoiNkyvUCaMX+oa2o3y3EL8O+Gl92GN0KE7a/aBTHH1HpFgkgSDREwhMCl
ICRjDJsLmFt2LkUX5ZDbCN96EV53uIQK568RGdYArn8JsOBdUYA78hiltrpFXZZj
vI7hsCAzT6io0EiWicop0ovDhZzK9KppYQfnzGw6SZ+576UCrlNq8bKD8SYGN9vZ
fwIDAQAB
-----END PUBLIC KEY-----`;

  it('calcula la huella que el agente compara al fijar la clave', async () => {
    expect(await publicKeySha256(PEM)).toBe('ede7b18e96401468935f0bac78512e5f84acd549fb54142aee897fc47119556c');
  });

  it('no depende de los saltos de línea del PEM', async () => {
    expect(await publicKeySha256(PEM.replace(/\n/g, '\r\n'))).toBe(await publicKeySha256(PEM));
  });

  it('devuelve null si el PEM no es Base64 válido', async () => {
    expect(await publicKeySha256('-----BEGIN PUBLIC KEY-----\nno-es-base64-*\n-----END PUBLIC KEY-----')).toBeNull();
  });
});
