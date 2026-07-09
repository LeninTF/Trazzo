export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    // Tipos de commit permitidos
    'type-enum': [
      2,
      'always',
      ['feat', 'fix', 'docs', 'style', 'refactor', 'perf', 'test', 'chore', 'revert', 'ci', 'build']
    ],
    
    'subject-case': [0], 
    
    'body-max-line-length': [0, 'always'],
    
    'footer-max-line-length': [0, 'always']
  },
  
  //Ignorar commits automáticos de versiones (ej. Dependabot) a nivel de linter
  ignores: [
    (commit) => /^Bump .* from .* to .*$/m.test(commit)
  ]
};