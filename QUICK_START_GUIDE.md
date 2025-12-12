# 🏥 HPMS2 System - Quick Start Guide

## ✅ System Status: RUNNING AND CONNECTED

Your HPMS2 Hospital Patient Management System is **NOW RUNNING** with full database connectivity!

---

## 📱 Access the Application

### Login Screen
The application is displaying a login window on your screen.

### Demo Credentials

| Role | Username | Password | Access Level |
|------|----------|----------|--------------|
| 👨‍💼 Admin | `admin` | `admin123` | Full system control |
| 👨‍⚕️ Doctor | `doctor1` | `doctor123` | Patient records, appointments |
| 👩‍⚕️ Staff | `staff1` | `staff123` | Intake, scheduling |
| 🧑‍🤝 Patient | `patient1` | `patient123` | Personal health info |

---

## 📊 Database Status

```
Connected Database: hpms2_db
Server: localhost:3306
Connection: ✅ ACTIVE
Tables: 25 (All created)
```

### Current Data
- ✅ 4 Demo Users Created
- ✅ 3 Departments Initialized
- ✅ 1 Demo Patient
- ✅ All tables ready for data entry

---

## 🎯 Key Features Available

### For Patients
- 📋 View health records
- 📅 Book appointments
- 💰 Check billing
- 🏥 View hospital info

### For Doctors
- 👥 View assigned patients
- 📝 Update medical records
- 📅 Manage schedule
- 📊 View statistics

### For Staff
- 📝 Register patients
- 📅 Schedule appointments
- 💼 Manage resources
- 📋 Process admissions

### For Admin
- 👥 Manage all users
- 🏢 Control departments
- 📊 View reports
- ⚙️ System configuration

---

## 🚀 Quick Test

1. **Try Login**
   - Click on login field
   - Enter: `admin` / `admin123`
   - Click "Log In"

2. **Explore Dashboard**
   - You'll see the admin dashboard
   - Use tabs to navigate features
   - Create a new patient record

3. **Book Appointment**
   - Go to Appointments
   - Select patient and doctor
   - Choose date/time
   - Confirm booking

4. **Generate Bill**
   - Go to Billing
   - Create billing record
   - Mark as paid/pending

---

## 🔧 Technical Status

### Java Runtime
- Version: Java 17.0.16 (Eclipse Adoptium)
- Status: ✅ Running

### MySQL Database
- Service: ✅ Running
- Database: `hpms2_db`
- Tables: 25
- Records: ~5

### Application
- Framework: Swing (Java GUI)
- Connection: JDBC with MySQL Connector/J
- Status: ✅ Running and connected

---

## 📱 System Components Status

| Component | Status | Details |
|-----------|--------|---------|
| LoginUI | ✅ Active | Authentication interface |
| DashboardUI | ✅ Ready | Role-based dashboards |
| Database Layer | ✅ Connected | All repositories working |
| Service Layer | ✅ Active | Business logic loaded |
| MySQL Server | ✅ Running | 24/7 available |
| Schema | ✅ Initialized | All tables created |

---

## 💻 Command Reference

### To Stop Application
```powershell
Get-Process java | Stop-Process -Force
```

### To Restart
```powershell
cd c:\xampp\htdocs\HPMS2
&"C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\java.exe" -cp "lib/mysql-connector-j-9.5.0.jar;bin" UI.LoginUI
```

### To View Database
```powershell
mysql -u root hpms2_db
SHOW TABLES;
SELECT * FROM users;
```

### To Check System Health
```powershell
# Check MySQL running
Get-Process mysqld

# Connect to database
mysql -u root hpms2_db -e "SELECT 'Connected' as Status;"
```

---

## 🎓 What's Working Right Now

✅ Database is connected  
✅ All 25 tables created  
✅ User authentication system ready  
✅ Patient management module active  
✅ Doctor scheduling active  
✅ Appointment system ready  
✅ Billing system ready  
✅ Admin panel ready  
✅ Department management ready  
✅ Medical records system active  

---

## 🚨 Common Issues & Solutions

### "Application doesn't appear"
- MySQL might not be running
- Check: `Get-Process mysqld`
- Start XAMPP and enable MySQL

### "Login fails"
- Try demo account: `admin` / `admin123`
- Check MySQL is running
- Verify database connection: `mysql -u root hpms2_db`

### "Can't create patient"
- Database might be full
- Check available space
- Try restarting application

### "Slow performance"
- Close other applications
- Restart MySQL service
- Check database logs

---

## 📝 What to Try Next

1. **Create a Test Patient**
   - Login as `admin`
   - Go to Patient Management
   - Click "Add New Patient"
   - Fill in information
   - Click Save

2. **Book an Appointment**
   - Select the patient you created
   - Choose a doctor
   - Pick available date/time
   - Confirm booking

3. **Check Database Records**
   - Open terminal/command prompt
   - Run: `mysql -u root hpms2_db`
   - Query: `SELECT * FROM patients;`
   - Query: `SELECT * FROM appointments;`

4. **Try Different Roles**
   - Logout (Menu → Logout)
   - Login as `doctor1` (doctor view)
   - Login as `patient1` (patient view)
   - Compare different dashboards

---

## 📞 Support

### Check System Logs
- Console output shows all database operations
- Watch for errors in terminal
- Database errors help troubleshoot

### Database Connection Details
```
Hostname: localhost
Port: 3306
Database: hpms2_db
Username: root
Password: (empty)
Driver: MySQL Connector/J 9.5.0
```

### phpMyAdmin Access
```
URL: http://localhost/phpmyadmin
Username: root
Password: (leave blank)
Select Database: hpms2_db
```

---

## 🎉 You're All Set!

Your HPMS2 system is fully operational. The application is running, the database is connected, and demo data is ready for testing.

**Start by logging in with the admin account to explore all the features!**

---

**Status**: ✅ FULLY OPERATIONAL  
**Database**: ✅ CONNECTED  
**Ready to Use**: ✅ YES  

Enjoy managing the hospital! 🏥
