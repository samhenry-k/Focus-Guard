# Focus Guard

> An Android productivity application designed to reduce digital distractions and help users maintain focused work sessions.

## 📱 Overview

**Focus Guard** helps users manage distracting applications and build better focus habits through app blocking, structured focus sessions, and accountability-based access control.

The project is developed as a native Android application using **Kotlin** and **Jetpack Compose**.

## ✨ Features

* ⏱️ **Focus Timer** — Structured focus sessions for productive work.
* 🚫 **App Blocking** — Select and restrict distracting applications.
* 👥 **Accountability** — Trusted-contact based access control.
* 🔐 **Accessibility-Based Blocking** — Detects and restricts selected applications.
* 📊 **Focus Tracking** — Supports tracking of focus activity and blocked applications.
* 🔑 **User Authentication** — Authentication and user data management.

## 🛠️ Tech Stack

| Technology            | Purpose                             |
| --------------------- | ----------------------------------- |
| Kotlin                | Android development                 |
| Jetpack Compose       | User interface                      |
| Android SDK           | Platform functionality              |
| Gradle Kotlin DSL     | Build system                        |
| Firebase              | Authentication and backend services |
| Accessibility Service | Application blocking                |

## 📂 Project Structure

```text
Focus-Guard/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/example/focusguard_v20/
│   │       │       ├── auth/
│   │       │       ├── blocking/
│   │       │       ├── data/
│   │       │       ├── focus/
│   │       │       ├── nav/
│   │       │       ├── ui/
│   │       │       └── MainActivity.kt
│   │       │
│   │       └── AndroidManifest.xml
│   │
│   ├── build.gradle.kts
│   └── proguard-rules.pro
│
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

## 🎯 Target Users

Focus Guard is primarily designed for:

* Students
* Exam preparation
* Project and assignment work
* Users reducing social-media distractions
* Anyone who wants structured screen-time control

## 🔮 Future Improvements

* Advanced productivity analytics
* Recurring focus schedules
* Improved accountability notifications
* Stronger anti-bypass mechanisms
* Daily and weekly productivity reports
* Cloud synchronization
* Productivity streaks and achievements

## 🔒 Security

Sensitive configuration files such as:

```text
google-services.json
local.properties
.idea/
```

should not be committed to the repository.

API keys and other credentials should be properly restricted and managed through the appropriate development environment.

## 📌 Project Status

**Status: 🚧 Active Development**

Focus Guard is currently under development as an Android productivity and distraction-management application.

## 👨‍💻 Author

**Sam Henry K**

GitHub: [@samhenry-k](https://github.com/samhenry-k)

---

<p align="center">
  <b>Focus better. Stay accountable. Get more done. 🚀</b>
</p>
