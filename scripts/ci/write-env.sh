#!/usr/bin/env bash
# ci only: writes .env with throwaway random secrets for the compose stack and hands the smoke test what it needs
# (GITHUB_ENV, masked). nothing is printed: the values only exist in .env and the job's environment
set -euo pipefail
cd "$(dirname "$0")/../.."

mysql_password=$(openssl rand -hex 16)
jwt_secret=$(openssl rand -hex 48)          # 96 characters (64+ needed)
internal_key=$(openssl rand -hex 24)        # 48 characters (32+ needed)
ticket_secret=$(openssl rand -hex 32)
admin_email=admin@gridpulse.test
# upper, lower, digit and special character, no spaces (the strong password rule)
admin_password="Gp!$(openssl rand -hex 8)Zz9"
razorpay_secret=$(openssl rand -hex 20)

umask 077
cat > .env <<EOF
MYSQL_ROOT_PASSWORD=${mysql_password}
MYSQL_PORT=3306
JWT_SECRET=${jwt_secret}
TICKET_SECRET=${ticket_secret}
INTERNAL_API_KEY=${internal_key}
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_TO_FILE=true
SUPER_ADMIN_EMAIL=${admin_email}
SUPER_ADMIN_PASSWORD=${admin_password}
RAZORPAY_KEY_ID=rzp_test_stub
RAZORPAY_KEY_SECRET=${razorpay_secret}
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
GEMINI_API_KEY=
GEMINI_API_KEY_2=
GEMINI_API_KEY_3=
GROQ_API_KEY=
EOF

# outside github actions only the .env is written
if [ -n "${GITHUB_ENV:-}" ]; then
  for secret in "$mysql_password" "$jwt_secret" "$internal_key" "$ticket_secret" "$admin_password" "$razorpay_secret"; do
    echo "::add-mask::${secret}"
  done
  {
    echo "SUPER_ADMIN_EMAIL=${admin_email}"
    echo "SUPER_ADMIN_PASSWORD=${admin_password}"
    echo "RAZORPAY_KEY_SECRET=${razorpay_secret}"
  } >> "$GITHUB_ENV"
fi
