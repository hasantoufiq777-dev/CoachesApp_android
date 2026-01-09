# ⚽ Coaches App - Android Version

A complete Android application for managing soccer/football coaches, players, clubs, and player transfers. This is the Android port of the desktop JavaFX Coaches App.

## 🎯 Features

### Core Functionality
- **User Authentication** - Login system with role-based access (Admin, Club Manager, Player)
- **Player Management** - Add, edit, delete, and view players with positions and injury status
- **Club Management** - Manage football clubs and their players
- **Manager Management** - Handle club managers and their associations
- **Transfer System** - Submit transfer requests, browse transfer market, approve transfers
- **Registration System** - New users can register and await admin approval

### Role-Based Access
- **System Admin** - Full access to all features
- **Club Manager** - Manage their club's players and transfers
- **Player** - View profile, submit transfer requests, browse transfer market

## 📱 Technical Stack

- **Language:** Java
- **Database:** To be implemented (Firebase recommended)
- **UI:** Material Design Components
- **Architecture:** Repository Pattern, Singleton Pattern
- **Min SDK:** Android 7.0 (API 24)
- **Target SDK:** Android 14 (API 36)

## 🏗️ Project Structure

```
app/src/main/java/com/example/coachesapp_android/
├── model/                      # Data models
│   ├── Player.java
│   ├── Club.java
│   ├── Manager.java
│   ├── User.java
│   ├── Position.java (enum)
│   ├── Role.java (enum)
│   ├── TransferRequest.java
│   └── RegistrationRequest.java
│
├── database/                   # Database layer (interfaces only)
│   ├── IPlayerRepository.java
│   ├── IClubRepository.java
│   └── IUserRepository.java
│
├── util/                       # Utilities
│   ├── SessionManager.java
│   └── AppState.java
│
├── LoginActivity.java          # Authentication
├── MainActivity.java           # Main navigation menu
├── PlayerListActivity.java     # Player list with RecyclerView
├── PlayerProfileActivity.java  # Player profile and injury toggle
└── [Other Activities]          # Additional features

app/src/main/res/
├── layout/                     # XML layouts
│   ├── activity_login.xml
│   ├── activity_main.xml
│   ├── activity_player_list.xml
│   ├── item_player.xml         # RecyclerView item
│   └── [Other layouts]
└── values/
    └── strings.xml
```

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- Android device or emulator with API 24+

### Installation
1. Clone or open this project in Android Studio
2. Sync Gradle files
3. Run the app on an emulator or physical device

### Default Login Credentials

The app comes with pre-populated test data:

**System Admin:**
- Username: `admin`
- Password: `admin123`

**Club Manager:**
- Username: `manager1`
- Password: `pass123`

**Player:**
- Username: `player1`
- Password: `pass123`

## 📊 Database Schema

Database implementation has been removed. The app is ready for Firebase integration.

Planned data structures:
- **clubs** - Football clubs
- **players** - Player information with club associations
- **managers** - Club managers
- **users** - User accounts with roles
- **transfer_requests** - Player transfer requests
- **registration_requests** - Pending user registrations

## 🎮 Usage

### As Admin:
1. Login with admin credentials
2. Access all features from the main menu
3. Manage clubs, players, managers
4. Approve registration requests
5. Monitor transfer activities

### As Club Manager:
1. Login with manager credentials
2. View and manage players in your club
3. Approve/reject transfer requests
4. Browse transfer market

### As Player:
1. Login with player credentials
2. View your profile
3. Toggle injury status
4. Submit transfer requests
5. Browse available players in transfer market

## 🔧 Implementation Status

### ✅ Completed
- Model classes (Player, Club, Manager, User, etc.)
- Repository interfaces (IPlayerRepository, IClubRepository, IUserRepository)
- User authentication framework and session management
- Login activity with role-based navigation
- Main navigation menu
- Player list with RecyclerView
- Player profile with injury toggle
- Role-based UI visibility

### 🚧 To Be Implemented
The following features need implementation:
- **Database layer**: Implement Firebase repositories (SQLite has been removed)
- Add/Edit Player form
- Club list and management
- Manager list and management
- Transfer request submission
- Transfer market browse/purchase
- Registration form
- Registration approval interface

## 📝 Development Notes

### Key Design Patterns
- **Repository Pattern** - Separates data access logic
- **Singleton Pattern** - Used for AppState and database helpers
- **Session Management** - SharedPreferences for user sessions

### Database Features
- Sample data pre-populated on first run
- Foreign key constraints
- Proper indexing for queries

### UI/UX
- Material Design components
- Role-based navigation
- Card-based layouts
- Intuitive user flow

## 🔗 Desktop Version

This Android app is based on the desktop JavaFX version available at:
https://github.com/hasantoufiq777-dev/coachApp

## 📄 License

This project is provided as educational material. Feel free to use, modify, and distribute as needed.

## 🤝 Contributing

To contribute or extend features:
1. Choose a stub activity to implement
2. Create the UI layout
3. Implement business logic
4. Test with different user roles
5. Update this README

## 📞 Support

For questions or issues:
- Check the desktop version documentation
- Review the code comments
- Examine the database schema

---

**Status:** ✅ Core Features Implemented - Ready for Extension
**Version:** 1.0
**Last Updated:** December 2025
