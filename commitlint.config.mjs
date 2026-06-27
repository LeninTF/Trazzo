export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      ['feat', 'fix', 'docs', 'style', 'refactor', 'perf', 'test', 'chore', 'revert', 'ci', 'build']
    ],
    'subject-case': [0], // importante para dependabot
  },
  ignores: [
    (commit) => commit.includes('Bump')
  ]
};