#!/bin/bash
# Check for potential secrets in files

set -e

# Patterns to detect secrets
SECRET_PATTERNS=(
    # AWS
    "AKIA[0-9A-Z]{16}"
    "aws_access_key_id\s*=\s*[\"'][A-Z0-9]{20}[\"']"
    "aws_secret_access_key\s*=\s*[\"'][A-Za-z0-9/+=]{40}[\"']"
    
    # Generic API keys
    "api[_-]?key\s*=\s*[\"'][A-Za-z0-9_\-]{20,}[\"']"
    "secret[_-]?key\s*=\s*[\"'][A-Za-z0-9_\-]{20,}[\"']"
    "access[_-]?token\s*=\s*[\"'][A-Za-z0-9_\-]{20,}[\"']"
    
    # Database
    "jdbc:postgresql://[^:]+:[^@]+@"
    "DATABASE_PASSWORD\s*=\s*[\"'][^\"']+[\"']"
    
    # JWT
    "JWT_SECRET\s*=\s*[\"'][A-Za-z0-9_\-]{32,}[\"']"
    "JWT_ACCESS_SECRET\s*=\s*[\"'][A-Za-z0-9_\-]{32,}[\"']"
    "JWT_REFRESH_SECRET\s*=\s*[\"'][A-Za-z0-9_\-]{32,}[\"']"
    
    # M-Pesa
    "MPESA_CONSUMER_SECRET\s*=\s*[\"'][A-Za-z0-9_\-]{20,}[\"']"
    "MPESA_PASSKEY\s*=\s*[\"'][A-Za-z0-9+/=]{32,}[\"']"
    
    # Generic
    "password\s*=\s*[\"'][^\"']{8,}[\"']"
    "secret\s*=\s*[\"'][^\"']{16,}[\"']"
)

check_file() {
    local file="$1"
    local found=0
    
    # Skip binary files
    if file "$file" | grep -q "binary"; then
        return 0
    fi
    
    for pattern in "${SECRET_PATTERNS[@]}"; do
        if grep -qiE "$pattern" "$file" 2>/dev/null; then
            echo "  Potential secret found in $file (pattern: $pattern)"
            found=1
        fi
    done
    
    return $found
}

main() {
    local exit_code=0
    
    if [ $# -eq 0 ]; then
        # Check all tracked files
        FILES=$(git ls-files)
    else
        FILES="$@"
    fi
    
    echo "Scanning for potential secrets..."
    
    for file in $FILES; do
        if [ -f "$file" ]; then
            if check_file "$file"; then
                exit_code=1
            fi
        fi
    done
    
    if [ $exit_code -eq 0 ]; then
        echo "No secrets found."
    else
        echo ""
        echo "WARNING: Potential secrets detected!"
        echo "Please review the flagged files and remove any secrets."
        echo "Use environment variables or secret management instead."
    fi
    
    exit $exit_code
}

main "$@"