# Fixtures biométricos de prueba

- `dotnet-interop-vector.json`: payload generado por `HybridCryptographyService` del middleware
  (.NET 10, `RSAEncryptionPadding.OaepSHA256` + AES-256-GCM con AAD `biometric-identify`) sobre
  `sourceafis-template-a.b64`. Incluye una clave RSA **solo de prueba** (no se usa fuera de tests).
- `sourceafis-template-a.b64` / `sourceafis-template-b.b64`: templates SourceAFIS 3.14 (.NET) de dos
  huellas sintéticas distintas; Java SourceAFIS 3.18 los deserializa con la misma puntuación
  (a~a = 86,9; a~b = 0).
