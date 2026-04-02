# Splendor Web Game 💎

A real-time multiplayer web implementation of the popular board game **Splendor**. Players collect gem tokens, buy development cards, and compete to attract wealthy nobles to reach 15 prestige points first. 

This project features a custom game engine, real-time multiplayer support via WebSockets, AI opponents, and a responsive web interface.

## 🚀 Features
* **Real-Time Multiplayer:** Play with friends in synchronized game rooms powered by WebSockets.
* **AI Opponents:** Play solo or fill empty lobby slots with computer-controlled players.
* **Lobby Management:** Create private rooms with custom player counts (2-4 players) and server capacity limits.
* **Game Logic Engine:** Fully automated enforcement of Splendor rules, including turn validation, discount calculations, noble visits, and coin limits.
* **Responsive UI:** Clean, modern interface styled with Tailwind CSS.

## 🛠️ Tech Stack
* **Backend:** Java, Spring Boot (MVC, WebSockets)
* **Frontend:** HTML5, Thymeleaf, Tailwind CSS, JavaScript
* **Build Tool:** Maven, npm (for Tailwind)
* **Deployment:** GitHub Actions

## 📋 Prerequisites
Before you begin, ensure you have the following installed on your machine:
* [Java Development Kit (JDK) 17+](https://adoptium.net/)
* [Node.js and npm](https://nodejs.org/) (required to build the Tailwind CSS files)

## ⚙️ How to Run Locally

### 1. Clone the Repository
```bash
git clone https://github.com/Russell-Codes/CS102
cd ./CS102
```

### 2. Build the Frontend (Tailwind CSS)
#### [IF YOU HAVE NODE.JS, AND NPM INSTALLED ALREADY, ELSE SKIP TO STEP 3]
Navigate to the frontend directory to install dependencies and compile the CSS.
```bash
cd src/main/frontend
npm install
npm run build   # Or the specific build script defined in your package.json
cd ../../../
```

### 3. Run the Spring Boot Application
You can run the application directly using the included Maven wrapper. There is no need to install Maven globally.

**On Windows:**
```cmd
mvnw.cmd spring-boot:run
```

**On macOS/Linux:**
First, ensure the wrapper script is executable:
```bash
chmod +x mvnw
```
Then, start the application:
```bash
./mvnw spring-boot:run
```

### 4. Play the Game
Once the application has started, open your web browser and navigate to:
```text
http://localhost:8080
```

## 📂 Project Structure
* `src/main/java/com/g1t7/splendor/`: Contains all Java source code.
    * `/controller`: Spring MVC controllers handling HTTP requests and WebSocket routing.
    * `/service`: Core business logic, including `GameEngineService`, `PlayerActionService`, and `AIPlayer`.
    * `/model`: Data representations of Cards, Nobles, Gems, Players, and the Game state.
* `src/main/resources/`: Configuration files and static assets.
    * `/templates`: Thymeleaf HTML views (`login.html`, `lobby.html`, `game.html`).
    * `/static/images`: Image assets for cards, nobles, and gems.
    * `/game/config`: CSV data files initializing the deck and nobles.
* `src/main/frontend/`: Tailwind CSS configuration and uncompiled stylesheets.