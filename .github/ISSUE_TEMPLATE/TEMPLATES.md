# Issue Templates

## Bug Report Template

**File**: `.github/ISSUE_TEMPLATE/bug_report.md`

```markdown
---
name: Bug Report
about: Create a report to help us improve
title: '[BUG] '
labels: bug
assignees: ''
---

## Describe the Bug
A clear and concise description of what the bug is.

## To Reproduce
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

## Expected Behavior
A clear and concise description of what you expected to happen.

## Actual Behavior
What actually happened (include error messages, stack traces).

## Environment
- OS: [e.g. Ubuntu 22.04, macOS 14, Windows 11]
- Java Version: [e.g. 21.0.1]
- Maven Version: [e.g. 3.9.6]
- Docker Version: [e.g. 24.0.5]
- Browser: [e.g. Chrome 120, Firefox 121]

## Additional Context
Add any other context about the problem here (screenshots, logs, etc.).

## Possible Solution
If you have ideas on how to fix the issue, please describe them here.
```

## Feature Request Template

**File**: `.github/ISSUE_TEMPLATE/feature_request.md`

```markdown
---
name: Feature Request
about: Suggest an idea for this project
title: '[FEATURE] '
labels: enhancement
assignees: ''
---

## Problem Statement
A clear and concise description of what the problem is. Ex. I'm always frustrated when [...]

## Proposed Solution
A clear and concise description of what you want to happen.

## Alternatives Considered
A clear and concise description of any alternative solutions or features you've considered.

## Additional Context
Add any other context or screenshots about the feature request here.

## Acceptance Criteria
- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3
```

## Security Issue Template

**File**: `.github/ISSUE_TEMPLATE/security_issue.md`

```markdown
---
name: Security Issue
about: Report a security vulnerability privately
title: '[SECURITY] '
labels: security
assignees: ''
---

## Description
A clear and concise description of the security vulnerability.

## Impact
Describe the potential impact of this vulnerability.

## Reproduction Steps
1. Step 1
2. Step 2
3. Step 3

## Affected Versions
- Version 1.x
- Version 2.x

## Suggested Fix
If you have a suggested fix, please describe it here.

## Disclosure
- [ ] I agree to responsible disclosure
- [ ] I have not disclosed this publicly
```

## Pull Request Template

**File**: `.github/PULL_REQUEST_TEMPLATE.md`

```markdown
---
name: Pull Request
about: Submit a pull request
title: '[TYPE] '
labels: ''
assignees: ''
---

## Description
A clear and concise description of what this PR does.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] Refactoring (no functional changes)
- [ ] Performance improvement
- [ ] Test addition/update

## Related Issues
Closes #(issue number)

## Testing
Describe the tests you ran to verify your changes:
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing performed

## Screenshots (if applicable)
Add screenshots for UI changes.

## Checklist
- [ ] My code follows the project's style guidelines
- [ ] I have performed a self-review of my code
- [ ] I have commented my code where necessary
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes
- [ ] Any dependent changes have been merged and published

## Breaking Changes
If this introduces breaking changes, describe them and the migration path.

## Additional Notes
Any additional information that reviewers should know.
```