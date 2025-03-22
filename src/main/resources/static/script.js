const API_BASE_URL = "http://localhost:8080/api"; // Backend URL

document.getElementById("investmentForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const ticker = document.getElementById("ticker").value;
    const amount = document.getElementById("amount").value;

    console.log("Submitting Investment:", { ticker, amount }); // ✅ Debugging

    try {
        const response = await fetch(`${API_BASE_URL}/add-stock?ticker=${ticker}&amount=${amount}`, {
            method: "POST"
        });

        if (response.ok) {
            alert("✅ Investment added successfully!");
            document.getElementById("investmentForm").reset(); // ✅ Clears form
            loadRiskData();  // ✅ Refresh risk analysis table
        } else {
            alert("❌ Failed to add investment.");
        }
    } catch (error) {
        console.error("Error adding investment:", error);
    }
});

// Function to fetch and display risk analysis
// async function loadRiskData() {
//     try {
//         console.log("Fetching latest risk data...");
//         const response = await fetch(`${API_BASE_URL}/risk`);
//         const data = await response.json();
//
//         console.log("Risk Data Fetched:", data); // ✅ Debugging
//
//         const tableBody = document.getElementById("riskTable");
//         tableBody.innerHTML = "";  // ✅ Clear previous data
//
//         if (!data || data.length === 0) {
//             console.warn("⚠ No risk data received from backend.");
//             return;
//         }
//
//         data.forEach((item) => {
//             console.log("Adding row:", item); // ✅ Debugging
//             const row = `<tr>
//                 <td>${item.Investment || "N/A"}</td>
//                 <td>${item["Risk (Std Dev)"] ? item["Risk (Std Dev)"].toFixed(4) : "N/A"}</td>
//             </tr>`;
//             tableBody.insertAdjacentHTML("beforeend", row); // ✅ More efficient
//         });
//
//     } catch (error) {
//         console.error("Error fetching risk data:", error);
//     }
// }


async function loadRiskData() {
    // ✅ Manually add 4 fake investments (just for CV screenshots)
    const dummyData = [
        { "Investment": "AAPL", "Risk (Std Dev)": 0.0206 },
        { "Investment": "MSFT", "Risk (Std Dev)": 0.0172 },
        { "Investment": "GOOGL", "Risk (Std Dev)": 0.0221 },
        { "Investment": "TSLA", "Risk (Std Dev)": 0.0254 }
    ];

    console.log("📊 Using Dummy Data for Display:", dummyData);

    const tableBody = document.querySelector("#riskTable");
    tableBody.innerHTML = "";  // Clear previous data

    dummyData.forEach((item) => {
        const row = `<tr>
            <td>${item.Investment}</td>
            <td>${item["Risk (Std Dev)"].toFixed(4)}</td>
        </tr>`;
        tableBody.innerHTML += row;
    });

    console.log("✅ Dummy Data Rendered in Table!");
}

// ✅ Load dummy risk data when page opens
loadRiskData();


// ✅ Load risk data when page opens
// loadRiskData();
