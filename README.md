# React + Spring Boot Login System

A simple and clean login system built with React frontend and Java Spring Boot backend.

## Features

- ✅ React frontend with modern UI
- ✅ Java Spring Boot backend
- ✅ MySQL database integration
- ✅ JWT-based authentication
- ✅ Responsive design
- ✅ CORS enabled

## Tech Stack

**Frontend:**
- React 18
- Axios for API calls
- CSS with gradient styling

**Backend:**
- Java 17
- Spring Boot 3.5.5
- Spring Data JPA
- Spring Security
- MySQL Database

## Project Structure
login_page/
├── frontend/ # React application
│ ├── src/
│ ├── public/
│ └── package.json
├── login-system/ # Spring Boot backend
│ ├── src/main/java/
│ ├── src/main/resources/
│ └── pom.xml
└── README.md

## Setup and Installation

### Prerequisites
- Node.js (v16+)
- Java 17+
- Maven
- MySQL Server

### Backend Setup
1. Navigate to backend folder:
cd login-system


2. Configure database in `application.properties`:
spring.datasource.url=jdbc:mysql://localhost:3306/logindb
spring.datasource.username=root
spring.datasource.password=yourpassword

3. Create database:
CREATE DATABASE logindb;
USE logindb;
INSERT INTO users (username, password) VALUES ('admin', 'admin123');

4. Run Spring Boot application:
mvn spring-boot:run

### Frontend Setup
1. Navigate to frontend folder:
cd frontend

2. Install dependencies:
npm install

3. Start React application:
npm start

## Usage

1. **Backend**: Runs on `http://localhost:8080`
2. **Frontend**: Runs on `http://localhost:3000`
3. **Login Credentials**:
- Username: `admin`
- Password: `admin123`


