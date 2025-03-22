const API_BASE_URL = "http://localhost:8080/api"; // Backend URL

document.getElementById("investmentForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const ticker = document.getElementById("ticker").value;
    const amount = document.getElementById("amount").value;
    const pastReturns = document.getElementById("pastReturns").value.split(",");

    try {
        await fetch(`${API_BASE_URL}/add?ticker=${ticker}&amount=${amount}&pastReturns=${pastReturns}`, {
            method: "POST",
        });

        alert("Investment added successfully!");
        document.getElementById("investmentForm").reset();
        loadRiskData();  // Refresh risk analysis table
    } catch (error) {
        console.error("Error adding investment:", error);
    }
});

// Function to fetch and display risk analysis
async function loadRiskData() {
    try {
        const response = await fetch(`${API_BASE_URL}/risk`);
        const data = await response.json();

        const tableBody = document.querySelector("#riskTable tbody");
        tableBody.innerHTML = "";  // Clear previous data

        data.forEach((item) => {
            const row = `<tr>
                <td>${item.Investment}</td>
                <td>${item["Risk (Std Dev)"].toFixed(4)}</td>
            </tr>`;
            tableBody.innerHTML += row;
        });

    } catch (error) {
        console.error("Error fetching risk data:", error);
    }
}

// Load risk data when page opens
loadRiskData();
