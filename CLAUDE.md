# MMA Project AI Context

## 📌 Project Overview

This workspace contains two main backend systems for the **fightweek MMA platform**.

* Spring Boot API Server
* Python Flask Crawling Server

The system provides:

* fighter and match information
* real-time chat
* betting features
* ranking system
* notification system
* admin event control

---

## 📁 Project Structure

### ✅ Spring Boot Main Server

**Path**

```
E:\spring\jht_spring\mma
```

**Tech Stack**

* Java 21
* Spring Boot
* Spring Security (JWT + Refresh Token)
* WebSocket (real-time chat)
* Redis (pub/sub + cache)
* MySQL
* Docker
* Firebase Push Notification

**Main Responsibilities**

* User authentication / authorization
* Betting system logic
* Chat room management
* Fighter / event API
* Admin control APIs
* Notification scheduling
* Tier & point system

---

### ✅ Python Crawling Server

**Path**

```
/e/dest/crawling_py/ufc
```

**Tech Stack**

* Python
* Flask
* Web crawling scripts
* Scheduled data collection

**Main Responsibilities**

* UFC fighter data crawling
* Match schedule crawling
* Event result crawling
* Data preprocessing
* Providing REST endpoints for Spring server

---

## 🔗 System Interaction

Flow:

1. Flask server crawls fight data
2. Spring server requests crawler API
3. Spring saves processed data into MySQL
4. Flutter client consumes Spring API
5. WebSocket used for live fight chat and also stream fight data

Features:
- fight events
- athlete information
- user accounts
- application data storage

---

## 🧠 Important Domain Concepts

* User Tier System
  White → Blue → Purple → Brown → Black

* Betting Rewards

* Real-time Global Chat
  One chat room activated during fight time

* Admin Stream Event Control
  Admin triggers fight stream lifecycle

---

## ⚙️ Development Goals

* Improve backend architecture stability
* Add automated test coverage
* Optimize Redis pub/sub usage
* Enhance real-time chat scalability
* Improve crawler reliability
* Introduce better domain separation

## API Principles
- RESTful design
- consistent response structure
- backward compatibility for mobile clients

## Development Rules
- Controllers must remain thin
- Business logic belongs in service layer
- Entities must not be exposed directly
---

## 🤖 AI Agent Instructions

When working on this project:

* Always treat Spring project as **main system**
* Flask project is **data provider service**
* Avoid breaking WebSocket chat flow
* Preserve betting business logic integrity
* Follow layered architecture (controller → service → repository)
* Prefer refactoring over rewriting

---

## 🚀 Future Improvements

* MSA migration possibility
* Kafka event streaming
* Chat room sharding
* Prediction AI model integration
* Betting risk management module