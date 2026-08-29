# WatchStore — E-commerce with eSewa Payment Integration

A full-stack watch e-commerce site: Spring Boot REST API + React frontend +
PostgreSQL, with real eSewa **sandbox** payment integration. Built as a
working project for a BCA fifth-semester submission 

## Stack
- Backend: Spring Boot 3.3, Java 21, Spring Security + JWT, Spring Data JPA
- Frontend: React 18 + Vite, React Router, Axios
- Database: PostgreSQL
- Payments: eSewa ePay v2 sandbox (RC environment)

## Project layout
ecommerce-backend/    Spring Boot API (port 8080)
ecommerce-frontend/   React app (port 5173)

## Prerequisites
- Java 21+, Maven (or use your IDE's built-in Maven)
- Node.js 18+
- PostgreSQL running locally

## 1. Database
```sql
CREATE DATABASE watchstore;
```

## 2. Backend
```bash
cd ecommerce-backend
mvn spring-boot:run
```
API runs at `http://localhost:8080`. JPA `ddl-auto=update` auto-creates tables 

## 3. Frontend
```bash
cd ecommerce-frontend
npm install
npm run dev
```
Runs at `http://localhost:5173`.

## 4. Testing a payment
Register a customer account, add a watch to cart, check out. You'll be
redirected to eSewa's sandbox. Use eSewa's published test credentials:
- eSewa ID: `9806800001` (or `...002` through `...005`)
- Password: `Nepal@123`
- MPIN: `1122`
- Token/OTP: `123456`

## What's implemented
- JWT auth, role-based access (CUSTOMER / ADMIN)
- Product catalog with search + category filter
- Cart, checkout, order history
- eSewa sandbox payment: signed form generation, redirect-based flow,
  independent server-side verification, retry-payment for failed/abandoned
  orders
- Scheduled reconciliation job that catches payments where the user never
  came back from eSewa
- Admin dashboard: manage products, view all orders
