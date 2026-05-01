# BillWise - Intelligent Financial Tracking App

BillWise is a native Android application built with Kotlin and Jetpack Compose that automates financial tracking by intelligently extracting transactions from SMS and uploaded bills. It features a modern, glassmorphic UI, robust deduplication, and AI-driven insights to give users complete control and visibility over their spending habits.

## 🌟 Key Features

* **Intelligent SMS Parsing:** Automatically reads transactional SMS messages (e.g., from banks and UPI apps like GPay/PhonePe), accurately filtering out promotional spam, identifying refunds, and tracking internal wallet transfers without double-counting them.
* **Smart Bill OCR & Manual Entry:** Uses Google's ML Kit to scan receipts and bills. The sophisticated parser can intelligently extract the amount and merchant even from unstructured short texts (e.g., "coffee 200").
* **Custom Merchant Tagging:** Click on any transaction to rename the merchant or tag a person. BillWise will automatically apply this alias to all past and future transactions with the same raw merchant name, and the search bar supports querying by these custom aliases.
* **Robust Deduplication Engine:** Merges duplicate transactions across different sources (SMS, Manual, OCR). It intelligently compares amounts, merchant substrings, and timestamps within a 24-hour window to ensure your spending isn't double-counted.
* **Dynamic AI Insights & Dashboard:** 
  * Features a gorgeous, animated glassmorphic dashboard tracking your monthly spending vs. budget.
  * Tracks 7-day daily spending, 6-month historical trends, and category breakdowns.
  * AI Insights automatically detect recurring subscriptions, highlight unusual spending, and provide a daily "safe spend" limit to help you stay within budget.

## 📂 Project Structure & Core Files

* **`MainActivity.kt`:** The entry point. Handles Navigation Compose setup, dependency injection (Room, Repositories, ViewModels), and requests SMS permissions.
* **Domain Layer (`domain/`)**
  * **`Transaction.kt` / `Bill.kt` / `Budget.kt`:** Core data models.
  * **`GenerateInsightsUseCase.kt`:** Contains the business logic for generating smart insights (recurring payments, safe daily limits).
  * **`DeduplicateTransactionUseCase.kt`:** Handles the merging of overlapping transactions from different sources.
  * **`AnalyzeSpendingUseCase.kt`:** Computes mathematics for the dashboard (monthly spent, trends), explicitly ignoring internal wallet transfers to keep data accurate.
* **Data Layer (`data/`)**
  * **`parser/SmsParser.kt` & `parser/BillParser.kt`:** Complex regex-based extractors that parse raw text into structured `Transaction` objects.
  * **`local/AppDatabase.kt`:** Room Database setup, including schema migrations.
* **UI Layer (`ui/`)**
  * **`dashboard/DashboardScreen.kt`:** The main overview screen featuring an animated progress bar and ElevatedCards.
  * **`insights/InsightsScreen.kt`:** Renders dynamic Canvas-based bar charts for daily/monthly trends and AI text insights.
  * **`viewmodel/InsightViewModel.kt`:** Manages the StateFlows powering the dashboard and insights, utilizing highly optimized Kotlin `combine` flows.
  * **`viewmodel/TransactionViewModel.kt`:** Manages the transaction list, search filtering, and the logic for auto-applying merchant aliases.

## 🛠️ Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Architecture:** Clean Architecture + MVVM + UDF (Unidirectional Data Flow)
* **Local Database:** Room (SQLite)
* **Asynchronous Execution:** Kotlin Coroutines & Flows (`StateFlow`)
* **Machine Learning:** Google ML Kit Text Recognition API (Optimized for Android 15 / 16KB memory architectures)
