#!/bin/bash
# Setup git hooks

set -e

HOOKS_DIR=".githooks"
GIT_HOOKS_DIR=".git/hooks"

echo "Setting up git hooks..."

# Create git hooks directory if it doesn't exist
mkdir -p "$GIT_HOOKS_DIR"

# Copy hooks
cp "$HOOKS_DIR/pre-commit" "$GIT_HOOKS_DIR/pre-commit"
cp "$HOOKS_DIR/pre-push" "$GIT_HOOKS_DIR/pre-push"

# Make executable
chmod +x "$GIT_HOOKS_DIR/pre-commit"
chmod +x "$GIT_HOOKS_DIR/pre-push"

echo "Git hooks installed successfully!"
echo "Hooks installed:"
echo "  - pre-commit: Runs before each commit"
echo "  - pre-push: Runs before each push"