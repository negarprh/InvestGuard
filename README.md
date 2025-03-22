# **InvestGuard – Smart Investment Risk Analysis Tool**  
📊 **Spring Boot (Java) | Portfolio Risk Calculation | Financial Technology**  

> **"Analyze investment risk and make informed financial decisions with real-time risk assessment."**  

![InvestGuard Banner](https://source.unsplash.com/1600x400/?finance,stocks)  

---

## **🔗 Live Demo (If Hosted)**
🌍 **[Live Version](https://yourdeploymentlink.com)**  

---

## **📖 About InvestGuard**
**InvestGuard** is a **Spring Boot-powered investment risk analysis tool** that helps investors calculate **portfolio volatility**. Users enter stock details, and the system **computes risk metrics like standard deviation** to assess investment stability.  

### **✨ Features**
✅ **Add Investment Data** – Enter stock tickers, investment amounts, and past returns.  
✅ **Risk Calculation** – Uses **standard deviation** to measure investment volatility.  
✅ **Sharpe Ratio Analysis** – (Future Feature) Evaluates **risk-adjusted returns**.  
✅ **Simple UI** – Built using **HTML, CSS, and JavaScript**, served by Spring Boot.  
✅ **REST API** – Scalable backend for risk computation.  

---

## **🛠️ Tech Stack**
### **🚀 Backend (Full Stack in Spring Boot)**
- **Java 17**, **Spring Boot** (REST API & Static Frontend)
- **Maven** (Dependency Management)
- **H2 Database** *(or SQLite for persistence)*
- **JUnit** *(For Testing)*

### **💻 Frontend (Served by Spring Boot)**
- **HTML, CSS, JavaScript**
- **Fetch API** *(for REST API calls)*
- **Static Resources in `src/main/resources/static/`*

---

## **📸 Screenshots**
### **📊 Dashboard & Risk Analysis**
<img src="https://source.unsplash.com/800x400/?charts,finance" width="80%">

---

## **🛠️ Installation & Setup**
### **1️⃣ Clone the Repository**
```sh
git clone https://github.com/negarprh/InvestGuard.git
cd RiskWise
```

### **2️⃣ Run the Application (Spring Boot)**
```sh
mvn clean install
mvn spring-boot:run
```
✅ **Backend & frontend will both run at:**  
📌 **`http://localhost:8080`**  

### **3️⃣ Open the Frontend**
📌 Go to:  
```sh
http://localhost:8080/index.html
```
Since the frontend is inside **Spring Boot's `static/` folder**, it's **automatically served by the backend**.

---

## **📡 API Endpoints**
### **➕ Add Investment**
```http
POST /api/add?ticker=AAPL&amount=5000&pastReturns=0.05,0.02,-0.01,0.04,0.03
```
✅ **Response**
```json
"Investment added: AAPL"
```

### **📊 Get Risk Analysis**
```http
GET /api/risk
```
✅ **Response**
```json
[
    {
        "Investment": "AAPL",
        "Risk (Std Dev)": 0.025
    }
]
```

---

## **📝 Future Improvements**
🔹 Fetch **real stock data** via an API (Yahoo Finance, Alpha Vantage)  
🔹 Store investment data in **MySQL/PostgreSQL**  
🔹 Add a **Sharpe Ratio calculator** for risk-adjusted returns  

---

## **👨‍💻 Author**
**Negar** – *Backend & Frontend Developer*  
🔗 **GitHub:** [YourGitHubProfile](https://github.com/YOUR_GITHUB)  
📧 **Email:** your.email@example.com  

---

## **📜 License**
This project is **MIT Licensed** – Feel free to use and modify!  

📌 **Next Steps:**  
✅ **Commit this `README.md` to GitHub**  
✅ **Upload screenshots (`/assets/` folder)**  
✅ **Share your project on LinkedIn for visibility!**  

🚀 **Let me know if you need any modifications!** 🔥
