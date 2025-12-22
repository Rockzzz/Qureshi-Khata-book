# Tabela Hisab App - Architecture & Module Documentation

## 📱 App Overview

**Tabela Hisab** is an Android accounting/ledger management application designed for small businesses, particularly for tracking customer transactions, daily cash/bank balances, trading operations, and managing accounts receivable/payable. The app features voice-based transaction entry using AI (Google Gemini) for natural language processing.

---

## 🏗️ Architecture Overview

The app follows **MVVM (Model-View-ViewModel)** architecture with:
- **Jetpack Compose** for UI
- **Room Database** for local data persistence
- **Hilt** for Dependency Injection
- **Kotlin Coroutines & Flow** for asynchronous operations
- **Navigation Compose** for screen navigation

---

## 📂 Project Structure

```
app/src/main/java/com/example/tabelahisabapp/
├── data/                    # Data Layer
│   ├── db/                  # Room Database
│   │   ├── entity/          # Database entities (tables)
│   │   ├── dao/             # Data Access Objects
│   │   ├── models/          # Data models/views
│   │   └── AppDatabase.kt   # Database configuration
│   ├── preferences/         # DataStore preferences
│   └── repository/         # Repository pattern implementation
├── di/                      # Dependency Injection modules
├── ui/                      # UI Layer (Compose Screens)
│   ├── customer/           # Customer management module
│   ├── daily/              # Daily ledger module
│   ├── trading/            # Trading operations module
│   ├── voice/              # Voice recording & AI parsing
│   ├── company/            # Company management
│   ├── settings/           # Settings & configuration
│   ├── home/               # Home dashboard
│   ├── components/         # Reusable UI components
│   └── theme/              # App theming
├── utils/                   # Utility classes
└── MainActivity.kt         # Main entry point & navigation
```

---

## 🗄️ Data Models & Database Schema

### Core Entities

#### 1. **Customer** (`Customer.kt`)
- Stores customer/vendor information
- Fields: `id`, `name`, `phone`, `type` (CUSTOMER/SELLER/BOTH), `createdAt`
- Used for tracking who money is received from or paid to

#### 2. **CustomerTransaction** (`CustomerTransaction.kt`)
- Records individual transactions with customers
- Fields: `id`, `customerId`, `type` (CREDIT/DEBIT), `amount`, `date`, `note`, `voiceNotePath`, `paymentMethod` (CASH/BANK), `createdAt`
- Links to Customer via foreign key
- Used for customer ledger (udhaar/credit tracking)

#### 3. **DailyBalance** (`DailyBalance.kt`)
- Daily cash book summary
- Fields: `id`, `date`, `openingCash`, `openingBank`, `closingCash`, `closingBank`, `note`, `createdAt`
- One record per date (unique constraint)
- Auto-calculates closing balances from transactions

#### 4. **DailyLedgerTransaction** (`DailyLedgerTransaction.kt`)
- Individual transactions in daily ledger (cash book)
- Fields: `id`, `date`, `mode` (CASH_IN/CASH_OUT/BANK_IN/BANK_OUT), `amount`, `party`, `note`, `createdAt`
- Used for detailed daily cash book entries
- Auto-calculates totals and closing balances

#### 5. **DailyExpense** (`DailyExpense.kt`)
- Daily expense entries (legacy, may be deprecated)
- Fields: `id`, `date`, `category`, `amount`, `paymentMethod`, `createdAt`

#### 6. **TradeTransaction** (`TradeTransaction.kt`)
- Trading operations (buy/sell transactions)
- Fields: `id`, `date`, `deonar` (location), `type` (BUY/SELL), `itemName`, `quantity`, `buyRate`, `weight`, `rate`, `extraBonus`, `totalAmount`, `profit`, `pricePerUnit`, `note`, `createdAt`
- Used for tracking trading profits/losses

#### 7. **Company** (`Company.kt`)
- Company/business information
- Fields: `id`, `name`, `code`, `address`, `phone`, `email`, `gstNumber`, `createdAt`

### Database Configuration
- **Database Name**: `udhaar_ledger.db`
- **Current Version**: 5
- **Migrations**: Handles schema changes from version 1 to 5

---

## 🎯 Key Modules & Features

### 1. **Customer Management Module** (`ui/customer/`)

**Purpose**: Manage customers/vendors and their transactions (udhaar/credit tracking)

**Screens**:
- `CustomerListScreen`: List all customers with balance summary
- `AddCustomerScreen`: Add/edit customer details
- `CustomerLedgerScreen`: View individual customer's transaction history and balance
- `AddTransactionScreen`: Add/edit transactions (CREDIT/DEBIT) for a customer

**ViewModels**:
- `CustomerListViewModel`: Manages customer list and search
- `CustomerLedgerViewModel`: Manages individual customer transactions
- `AddCustomerViewModel`: Handles customer creation/editing
- `AddTransactionViewModel`: Handles transaction creation/editing

**Key Features**:
- Track money received from customers (CREDIT)
- Track money paid to customers/vendors (DEBIT)
- Calculate running balance for each customer
- Support for CASH and BANK payment methods
- Voice note attachment for transactions

---

### 2. **Daily Ledger Module** (`ui/daily/`)

**Purpose**: Daily cash book management - track daily cash and bank movements

**Screens**:
- `DailySummaryHomeScreen`: List of all daily ledger entries
- `DailyEntryScreen`: Main cash book screen (NEW - refactored)
  - Shows opening balance (auto-loaded from previous day)
  - Lists all transactions for the day
  - Auto-calculates totals (Cash In/Out, Bank In/Out)
  - Auto-calculates closing balances
  - Prevents duplicate entries for same date

**ViewModels**:
- `DailySummaryViewModel`: Manages daily balance operations

**Key Features**:
- **Auto Carry-Forward**: Opening balance automatically loaded from previous day's closing balance
- **Transaction Management**: Add/edit/delete transactions with modes:
  - CASH_IN: Money received in cash
  - CASH_OUT: Money paid in cash
  - BANK_IN: Money received in bank
  - BANK_OUT: Money paid from bank
- **Auto Calculations**:
  - Total Cash In/Out
  - Total Bank In/Out
  - Closing Cash = Opening Cash + Cash In - Cash Out
  - Closing Bank = Opening Bank + Bank In - Bank Out
- **Duplicate Prevention**: Only one ledger per date allowed
- **Real-time Updates**: Balances update instantly as transactions are added/edited/deleted

**Data Flow**:
1. User selects date → System loads previous day's closing balance as opening
2. User adds transactions → System calculates totals in real-time
3. User saves → System saves DailyBalance + all DailyLedgerTransactions
4. Next day → Closing balances become new opening balances

---

### 3. **Trading Module** (`ui/trading/`)

**Purpose**: Track trading operations (buy/sell) and calculate profits

**Screens**:
- `TradingHomeScreen`: List of all trading transactions
- `AddTradeTransactionScreen`: Add/edit buy/sell transactions

**ViewModels**:
- `TradingViewModel`: Manages trading operations

**Key Features**:
- Record BUY transactions (purchases)
- Record SELL transactions (sales)
- Calculate profit automatically for SELL transactions
- Track by location (deonar), item name, quantity, weight, rates
- Monthly and overall profit summaries

---

### 4. **Voice Recording & AI Parsing Module** (`ui/voice/`)

**Purpose**: Voice-based transaction entry using speech recognition and AI

**Screens**:
- `VoiceRecordingScreen`: Record voice input
- `VoiceConfirmationScreen`: Confirm AI-parsed transaction
- `VoiceClarificationScreen`: Handle ambiguous/incorrect parsing

**ViewModels**:
- `VoiceFlowViewModel`: Coordinates voice recording, transcription, and AI parsing

**Repositories**:
- `SpeechRepository`: Handles Android Speech Recognition API
- `GeminiRepository`: Integrates with Google Gemini AI for natural language parsing

**Key Features**:
1. **Voice Recording**: Uses Android Speech Recognition API
   - Real-time transcription
   - Visual feedback (amplitude visualization)
   - Recording duration tracking

2. **AI Parsing**: Uses Google Gemini AI
   - Parses natural language (e.g., "Aijaz se 5000 rupaye mile cash mein")
   - Extracts: customer name, amount, transaction type, payment method
   - Handles ambiguous cases with clarification suggestions

3. **Transaction Creation**: 
   - Automatically creates customer if doesn't exist
   - Creates CustomerTransaction with parsed data
   - Updates daily balance automatically
   - Saves voice note file path

**Flow**:
```
User speaks → Speech Recognition → Transcription → Gemini AI Parsing 
→ Confirmation Screen → Save Transaction → Update Daily Balance
```

---

### 5. **Company Management Module** (`ui/company/`)

**Purpose**: Manage company/business information

**Screens**:
- `CompanyListScreen`: List all companies
- `AddCompanyScreen`: Add/edit company details

**ViewModels**:
- `CompanyViewModel`: Manages company operations

---

### 6. **Settings Module** (`ui/settings/`)

**Purpose**: App configuration and utilities

**Screens**:
- `SettingsScreen`: Main settings menu
- `BackupRestoreScreen`: Backup/restore database
- `ExportPrintScreen`: Export data to CSV/Excel
- `ThemeScreen`: Theme customization
- `AboutScreen`: App information

**ViewModels**:
- `BackupViewModel`: Handles backup/restore operations
- `ExportViewModel`: Handles data export
- `ThemeViewModel`: Manages theme preferences

---

## 🔄 Data Flow & Architecture Patterns

### Repository Pattern
All data access goes through `MainRepository`:
- **Single Source of Truth**: Repository abstracts database access
- **Flow-based**: Returns Kotlin Flow for reactive updates
- **Suspend Functions**: For write operations

### ViewModel Pattern
- ViewModels hold UI state and business logic
- Use `StateFlow`/`Flow` for reactive state management
- Communicate with Repository (never directly with database)
- Survive configuration changes

### Dependency Injection (Hilt)
- `@HiltViewModel`: ViewModels are automatically injected
- `DatabaseModule`: Provides database and DAOs
- `PreferencesModule`: Provides DataStore preferences
- All dependencies injected via constructor

### Navigation Flow
```
Home Dashboard
├── Customers Module
│   ├── Customer List
│   ├── Add/Edit Customer
│   ├── Customer Ledger
│   └── Add Transaction
├── Daily Ledger Module
│   ├── Daily Summary List
│   └── Daily Entry Screen (Cash Book)
├── Trading Module
│   ├── Trading List
│   └── Add Trade Transaction
└── Settings
    ├── Backup/Restore
    ├── Export/Print
    ├── Theme
    ├── Company Management
    └── About

Voice Flow (Accessible from multiple screens)
├── Voice Recording
├── Confirmation
└── Clarification (if needed)
```

---

## 🛠️ Technology Stack

### Core Technologies
- **Kotlin**: Programming language
- **Jetpack Compose**: Modern declarative UI framework
- **Room Database**: Local SQLite database with type-safe queries
- **Hilt**: Dependency injection framework
- **Navigation Compose**: Screen navigation
- **Kotlin Coroutines**: Asynchronous programming
- **Flow**: Reactive streams

### Libraries
- **Material 3**: UI components and theming
- **DataStore**: Preferences storage
- **Google Gemini AI**: Natural language processing for voice transactions
- **Android Speech Recognition**: Voice-to-text conversion

---

## 📊 Key Business Logic

### Customer Balance Calculation
```
Balance = Sum(CREDIT transactions) - Sum(DEBIT transactions)
```
- Positive balance = Customer owes money (udhaar)
- Negative balance = Business owes customer

### Daily Ledger Balance Calculation
```
Closing Cash = Opening Cash + Cash In - Cash Out
Closing Bank = Opening Bank + Bank In - Bank Out
```
- Opening balances auto-loaded from previous day
- Totals calculated from all transactions for the day
- Real-time updates as transactions change

### Trading Profit Calculation
```
Profit (for SELL) = (Sell Amount) - (Buy Amount for quantity)
```
- Calculated automatically when SELL transaction is created
- Monthly and overall summaries available

---

## 🔐 Data Persistence

### Room Database
- **Location**: `/data/data/com.example.tabelahisabapp/databases/udhaar_ledger.db`
- **Backup**: Can be exported via Settings → Backup
- **Migrations**: Automatic schema migration from version 1 to 5

### Preferences (DataStore)
- Theme preferences
- User settings

### Voice Notes
- Stored in app's internal storage: `files/voice_notes/`
- File paths stored in `CustomerTransaction.voiceNotePath`

---

## 🎨 UI/UX Features

### Theming
- Material 3 design system
- Custom color scheme (Purple600, SuccessGreen, DangerRed, etc.)
- Dark/Light theme support
- Customizable via Settings

### Navigation
- Bottom navigation bar for main modules
- Slide animations between screens
- Back button handling with exit confirmation on home

### Components
- Reusable cards, buttons, FABs
- Avatar components for customers
- Custom transaction cards with color coding

---

## 🔍 Important Code Patterns

### State Management
```kotlin
// ViewModel exposes StateFlow
private val _state = MutableStateFlow(State())
val state: StateFlow<State> = _state.asStateFlow()

// UI collects state
val state by viewModel.state.collectAsState()
```

### Database Queries
```kotlin
// DAO returns Flow for reactive updates
@Query("SELECT * FROM customers")
fun getAllCustomers(): Flow<List<Customer>>

// Repository exposes Flow
fun getAllCustomers() = customerDao.getAllCustomers()

// ViewModel collects Flow
val customers = repository.getAllCustomers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

### Dependency Injection
```kotlin
// Repository injected into ViewModel
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel()
```

---

## 🚀 Key Features Summary

1. **Customer Management**: Track udhaar/credit with customers and vendors
2. **Daily Cash Book**: Automated daily ledger with transaction tracking
3. **Voice Transactions**: AI-powered voice entry for quick transaction recording
4. **Trading Operations**: Buy/sell tracking with profit calculation
5. **Company Management**: Store business information
6. **Backup/Restore**: Export and import database
7. **Data Export**: CSV/Excel export functionality
8. **Theme Customization**: Personalize app appearance

---

## 📝 Notes for Developers

### Adding New Features
1. Create entity in `data/db/entity/`
2. Create DAO in `data/db/dao/`
3. Add DAO method to `AppDatabase`
4. Add repository methods in `MainRepository`
5. Create ViewModel in appropriate `ui/` module
6. Create Compose screen
7. Add navigation route in `MainActivity.kt`
8. Update `DatabaseModule` if new DAO needed

### Database Migrations
- Always increment database version
- Create migration in `AppDatabase` companion object
- Add migration to `DatabaseModule.provideAppDatabase()`

### Voice Feature
- Requires Google Gemini API key
- Speech recognition requires microphone permission
- Voice notes stored in app's internal storage

---

## 🔗 Module Dependencies

```
MainActivity (Navigation)
    ├── Home Dashboard
    ├── Customer Module
    │   └── Uses: CustomerDao, CustomerTransactionDao
    ├── Daily Ledger Module
    │   └── Uses: DailyBalanceDao, DailyLedgerTransactionDao
    ├── Trading Module
    │   └── Uses: TradeTransactionDao
    ├── Voice Module
    │   └── Uses: SpeechRepository, GeminiRepository, CustomerDao, TransactionDao
    └── Settings Module
        └── Uses: All DAOs (for backup/export)

All Modules
    └── Use: MainRepository (single source of truth)
        └── Uses: All DAOs
            └── Uses: AppDatabase
```

---

This documentation provides a comprehensive overview of the Tabela Hisab app architecture, modules, and functionality. Each module is designed to be independent yet integrated through the shared repository and database layer.


