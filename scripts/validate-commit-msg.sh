#!/bin/bash
# Validate commit message follows conventional commits format

set -e

COMMIT_MSG_FILE="$1"

if [ -z "$COMMIT_MSG_FILE" ]; then
    echo "Usage: $0 <commit-message-file>"
    exit 1
fi

COMMIT_MSG=$(cat "$COMMIT_MSG_FILE")

# Conventional commits regex
# Format: <type>(<scope>): <description>
# Types: feat, fix, docs, refactor, test, chore, build, ci, perf, security
REGEX='^(feat|fix|docs|refactor|test|chore|build|ci|perf|security)(\(.+\))?: .{1,50}'

if ! echo "$COMMIT_MSG" | grep -qE "$REGEX"; then
    echo "ERROR: Commit message does not follow conventional commits format!"
    echo ""
    echo "Format: <type>(<scope>): <description>"
    echo ""
    echo "Types:"
    echo "  feat     - A new feature"
    echo "  fix      - A bug fix"
    echo "  docs     - Documentation only changes"
    echo "  refactor - Code change that neither fixes a bug nor adds a feature"
    echo "  test     - Adding missing tests or correcting existing tests"
    echo "  chore    - Changes to the build process or auxiliary tools"
    echo "  build    - Changes that affect the build system or external dependencies"
    echo "  ci       - Changes to CI configuration files and scripts"
    echo "  perf     - A code change that improves performance"
    echo "  security - Security improvements"
    echo ""
    echo "Examples:"
    echo "  feat(auth): add JWT refresh token rotation"
    echo "  fix(cart): resolve stock validation race condition"
    echo "  docs(api): update Swagger documentation for order endpoints"
    echo "  refactor(auth): extract payment service interface"
    echo "  test(payment): add unit tests for M-Pesa callback handling"
    echo ""
    echo "Your commit message:"
    echo "$COMMIT_MSG"
    exit 1
fi

# Check description length
DESCRIPTION=$(echo "$COMMIT_MSG" | sed -E 's/^[^:]+: //')
if [ ${#DESCRIPTION} -gt 50 ]; then
    echo "WARNING: Commit description is longer than 50 characters (${#DESCRIPTION} chars)"
    echo "Consider shortening for better readability in git log"
fi

exit 0