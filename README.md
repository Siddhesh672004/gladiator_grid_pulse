# GridPulse

Motorsport track booking with live race control. Drivers book sessions on real circuits and pay online, admins run the sessions and competitions, and everyone can follow the live timing board lap by lap.

## Features
- Sign up with an email code, JWT login, roles: driver, admin, super admin
- Race tracks with photos (Cloudinary), sessions and competitions (qualifying + race)
- Booking with Razorpay payment, approve / reject, refunds
- Live race control and timing board
- Feedback with AI sentiment labels (Groq)
- Chatbot that answers questions and can book a session (Spring AI + Gemini)
- Admin dashboard, user management, booking emails

## Tech
- Backend: Spring Boot 3.5, Java 17, Spring Security + JWT, JPA, MySQL, Spring AI
- Frontend: Angular 16, Angular Material (dialogs)

## Run locally
Full steps (Docker infra, tests, Swagger): see `HOW-TO-RUN.txt`.

1. MySQL on port 3306, login via `DB_USERNAME` / `DB_PASSWORD` (database `appdb` is created on first start).
2. Backend: put your keys in `springapp/secrets.properties` (`MAIL_USERNAME`, `MAIL_PASSWORD`, `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_PASSWORD`, `GEMINI_API_KEY`, `GROQ_API_KEY`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`), then
   ```
   cd springapp
   ./mvnw spring-boot:run
   ```
   API on http://localhost:8080
3. Frontend (Node 20):
   ```
   cd angularapp
   npm install
   npx ng serve --port 8081
   ```
   Open http://localhost:8081
