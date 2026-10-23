// BiometricSecurityGates.AllowPlaintextTemplateFallback es estado estático global: varias clases
// lo abren con PlaintextGateScope y las fábricas de FingerprintCapture/Enroll/IdentifyResult lo leen.
// Con clases en paralelo, un test que espera el gate cerrado fallaba de forma intermitente mientras
// otra clase lo tenía abierto. Se ejecutan en serie.
[assembly: CollectionBehavior(DisableTestParallelization = true)]
