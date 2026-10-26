#!/bin/bash
set -e

# Preflight checks for FEAT-004 CloudFormation deployment
# Validates all prerequisites before sam deploy

REPO_ROOT="/Users/jefferson/Documents/FIAP/fase-4"
INFRA_DIR="$REPO_ROOT/infra"
REGION="us-east-1"

echo "========================================"
echo "PREFLIGHT VALIDATION FOR FEAT-004"
echo "========================================"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_failed=0

# Check 1: AWS CLI installed
echo -n "✓ Checking AWS CLI..."
if ! command -v aws &> /dev/null; then
    echo -e "${RED} FAILED${NC}"
    echo "  AWS CLI is not installed. Install it from https://aws.amazon.com/cli/"
    check_failed=1
else
    echo -e "${GREEN} OK${NC}"
fi

# Check 2: SAM CLI installed
echo -n "✓ Checking SAM CLI..."
if ! command -v sam &> /dev/null; then
    echo -e "${RED} FAILED${NC}"
    echo "  SAM CLI is not installed. Install with: pip install aws-sam-cli"
    check_failed=1
else
    SAM_VERSION=$(sam --version 2>&1 | sed -n 's/.*SAM CLI, version \([^,]*\).*/\1/p')
    echo -e "${GREEN} OK (v${SAM_VERSION})${NC}"
fi

# Check 3: Maven built all modules
echo -n "✓ Checking Lambda function ZIPs..."
MODULES=("receive-feedback" "send-notification" "generate-report")
for module in "${MODULES[@]}"; do
    ZIP_FILE="$REPO_ROOT/$module/target/function.zip"
    if [ ! -f "$ZIP_FILE" ]; then
        echo -e "${RED} FAILED${NC}"
        echo "  Missing: $ZIP_FILE"
        echo "  Run: mvn clean package -DskipTests -Pnative"
        check_failed=1
    fi
done
if [ $check_failed -eq 0 ]; then
    echo -e "${GREEN} OK${NC}"
fi

# Check 4: packaged.yaml exists
echo -n "✓ Checking packaged.yaml..."
if [ ! -f "$INFRA_DIR/packaged.yaml" ]; then
    echo -e "${RED} FAILED${NC}"
    echo "  Missing: $INFRA_DIR/packaged.yaml"
    echo "  Run: sam build && sam package --s3-bucket <bucket> --output-template-file packaged.yaml"
    check_failed=1
else
    echo -e "${GREEN} OK${NC}"
fi

# Check 5: AWS credentials configured
echo -n "✓ Checking AWS credentials..."
if ! aws sts get-caller-identity &> /dev/null; then
    echo -e "${RED} FAILED${NC}"
    echo "  AWS credentials not configured. Run: aws configure"
    check_failed=1
else
    ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
    PRINCIPAL=$(aws sts get-caller-identity --query Arn --output text)
    echo -e "${GREEN} OK${NC}"
    echo "  Account: $ACCOUNT"
    echo "  Principal: $PRINCIPAL"
fi

# Check 6: SES identities verified (for prod)
echo -n "✓ Checking SES identities..."
ADMIN_EMAIL="admin@feedback-fase-4.com"
if ! aws ses verify-email-identity --email-address "$ADMIN_EMAIL" --region "$REGION" 2>/dev/null; then
    # Check if already verified
    VERIFIED=$(aws ses list-verified-email-addresses --region "$REGION" --query "VerifiedEmailAddresses[?contains(@, '$ADMIN_EMAIL')]" --output text)
    if [ -z "$VERIFIED" ]; then
        echo -e "${YELLOW} WARNING${NC}"
        echo "  Email $ADMIN_EMAIL is not verified in SES."
        echo "  Verify it in AWS Console: SES > Verified Identities > Add New"
        echo "  Or: aws ses verify-email-identity --email-address $ADMIN_EMAIL --region $REGION"
        check_failed=1
    else
        echo -e "${GREEN} OK${NC}"
    fi
else
    echo -e "${GREEN} OK${NC}"
fi

# Check 7: S3 bucket exists for SAM artifacts
echo -n "✓ Checking S3 artifact bucket..."
S3_BUCKET="sam-artifacts-us-east-1"
if ! aws s3 ls s3://$S3_BUCKET --region "$REGION" &> /dev/null; then
    echo -e "${YELLOW} WARNING${NC}"
    echo "  S3 bucket $S3_BUCKET may not exist or is not accessible."
    echo "  If packaged.yaml references different bucket, this is OK."
else
    echo -e "${GREEN} OK${NC}"
fi

# Check 8: Java 17 available (for Maven builds)
echo -n "✓ Checking Java version..."
if ! command -v java &> /dev/null; then
    echo -e "${RED} FAILED${NC}"
    echo "  Java is not installed. Required: Java 17+"
    check_failed=1
else
    JAVA_VERSION=$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        echo -e "${RED} FAILED${NC}"
        echo "  Java version is $JAVA_VERSION, required: 17+"
        check_failed=1
    else
        echo -e "${GREEN} OK (v${JAVA_VERSION})${NC}"
    fi
fi

# Check 9: Maven installed
echo -n "✓ Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo -e "${RED} FAILED${NC}"
    echo "  Maven is not installed. Required for building modules."
    check_failed=1
else
    MVN_VERSION=$(mvn --version 2>&1 | sed -n 's/Apache Maven \([0-9.]*\).*/\1/p' | head -1)
    echo -e "${GREEN} OK (v${MVN_VERSION})${NC}"
fi

# Check 10: template.yaml is valid
echo -n "✓ Validating template.yaml..."
if ! sam validate --template "$REPO_ROOT/infra/template.yaml" --region "$REGION" &> /dev/null; then
    echo -e "${RED} FAILED${NC}"
    echo "  SAM template validation failed."
    sam validate --template "$REPO_ROOT/infra/template.yaml" --region "$REGION"
    check_failed=1
else
    echo -e "${GREEN} OK${NC}"
fi

echo ""
echo "========================================"
if [ $check_failed -eq 0 ]; then
    echo -e "${GREEN}ALL CRITICAL CHECKS PASSED${NC}"
    echo "========================================"
    exit 0
else
    echo -e "${RED}SOME CRITICAL CHECKS FAILED${NC}"
    echo "Fix the issues above and re-run this script."
    echo "========================================"
    exit 1
fi
