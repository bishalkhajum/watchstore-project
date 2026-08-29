#!/bin/bash

# PostgreSQL
export DATABASE_URL="jdbc:postgresql://localhost:5432/watchstore"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD="your_postgres_password"

# JWT
export JWT_SECRET="your_long_random_jwt_secret"
export JWT_EXPIRATION_MS="86400000"

# eSewa Sandbox
export ESEWA_MERCHANT_CODE="EPAYTEST"
export ESEWA_SECRET_KEY="your_esewa_secret_key"
export ESEWA_PAYMENT_URL="https://rc-epay.esewa.com.np/api/epay/main/v2/form"
export ESEWA_STATUS_CHECK_URL="https://rc.esewa.com.np/api/epay/transaction/status/"

# Frontend
export FRONTEND_URL="http://localhost:5173"

# eSewa redirect URLs
export ESEWA_SUCCESS_URL="http://localhost:5173/payment/result?status=success"
export ESEWA_FAILURE_URL="http://localhost:5173/payment/result?status=failure"
