# Pre-commit Hooks

This directory contains git hooks for maintaining code quality.

## Installation

```bash
# Install hooks
cp .githooks/* .git/hooks/
chmod +x .git/hooks/*

# Or use the setup script
./scripts/setup-git-hooks.sh
```

## Hooks

### pre-commit
Runs before each commit:
- Runs unit tests
- Checks code formatting
- Validates commit message format

### pre-push
Runs before each push:
- Runs full test suite
- Checks for security issues
- Validates no secrets in code

## Scripts

### setup-git-hooks.sh
Installs all git hooks automatically.

### validate-commit-msg.sh
Validates commit message follows conventional commits format.

### check-secrets.sh
Scans for potential secrets in staged files.