# Manual test script

A click-through you can run in about 15 minutes to see the whole app working, using the demo data that is created
automatically on a local run.

## 1. Start the app

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=h2" "-Dspring-boot.run.arguments=--server.port=8081"
```

Open http://localhost:8081 (8081 avoids a clash with Docker Desktop, which may hold 8080).
The `h2` profile uses an in-memory database that is **reset on every start** and seeds the data below.
(`docker compose up --build` uses PostgreSQL and does not create demo outings.)

## 2. Demo data

All passwords are `password123`.

| User | Login | Role | Notes |
|---|---|---|---|
| Rafsan | `rafsan10` | USER | Friends with Tanvir, Nabila, Samiha |
| Tanvir | `tanvir_h` | USER | |
| Nabila | `nabila_r` | USER | |
| Samiha | `samiha` | USER | |
| Administrator | `admin` | ADMIN | Admin dashboard |

| Outing | By | Status | What happened |
|---|---|---|---|
| Burger night | Rafsan | Active | Rafsan, Tanvir, Nabila + guest Rahim. Bill 1550. Rafsan paid 1050, Tanvir 500 |
| Cox's Bazar trip | Tanvir | Active | Tanvir, Rafsan, Samiha. Bill 6000 split equally, Tanvir paid it all |
| Movie night | Rafsan | Draft | Only Rafsan can see it |

Expected balances: Rafsan is owed 550 by Rahim (guest) and owes Tanvir 2000. Nabila owes Tanvir 200. Samiha owes
Tanvir 2000.

## 3. Checks

Tick each one off.

**Login and home**
- [ ] Log in as `rafsan10`. Home says "Hi, Rafsan", net balance is `−৳1450.00` (550 − 2000), You'll get `৳550.00`, You owe `৳2000.00`.
- [ ] Balances list shows Tanvir (you owe ৳2000.00) and Rahim with a Guest tag (owes you ৳550.00). Tanvir opens his ledger page; Rahim is not a link.
- [ ] Active outings shows Burger night and Cox's Bazar trip; a note says you have 1 unfinished draft.
- [ ] Log out. The home page now shows the welcome page with Create account / Log in.
- [ ] A wrong password shows an error and does not log in.

**Outings**
- [ ] Outings page groups Active (2) and Drafts (1: Movie night).
- [ ] Open Burger night: Rafsan ate 500 / paid 1050; Tanvir ate 300 / paid 500; Nabila ate 200 / paid 0; Rahim (Guest of Rafsan) ate 550.
- [ ] As `tanvir_h`: Burger night is visible, but there is no Edit/Cancel button; Movie night is **not** visible.
- [ ] As `samiha`: only Cox's Bazar trip is visible.

**New outing wizard** (as `rafsan10`)
- [ ] New outing → title "Test dinner", add Tanvir, add a guest "Karim".
- [ ] Add items: "Biryani" 600 split equally between everyone; "Dessert" 200 personal to Karim.
- [ ] Who paid: Rafsan 800. The split card shows difference ৳0.00 and a net per person.
- [ ] Change Rafsan's payment to 700: the split shows a "still to do" message about the difference. Set it back.
- [ ] Save and split → opens the Active outing. Home now includes it.

**Rules**
- [ ] Cancel an active outing you created: its debts disappear from the ledger.
- [ ] As Tanvir, opening `/outings/<Burger night id>/edit` redirects to the list with a message.
- [ ] As `admin`, the admin dashboard opens; as a normal user, `/admin` is refused.

**Security**
- [ ] Logged out, open `/outings` → redirected to login.
- [ ] Logged in as Nabila, open `/?userId=1` → still shows Nabila's own dashboard.

## 4. Automated checks

```powershell
./mvnw verify
```

Runs every unit test (Surefire), every integration test (Failsafe) and the coverage gate on the split engine.
