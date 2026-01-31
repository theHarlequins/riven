package ua.riven.app.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import ua.riven.app.db.RivenDatabase
import ua.riven.app.db.TransactionRecord
import ua.riven.app.db.Wallet
import ua.riven.app.db.Envelope

class FinanceRepository(
    private val db: RivenDatabase
) {
    // 3. The "Crisis Slider" should update a simulationMultiplier.
    // Default multiplier is 1.0 (Real Mode).
    private val _simulationMultiplier = MutableStateFlow(1.0)
    private val _lastUpdated = MutableStateFlow<Long?>(null)

    val lastUpdated: Flow<Long?> = _lastUpdated

    // ==================== FLOW EXPOSURES ====================

    val wallets: Flow<List<Wallet>> = db.rivenQueries.getAllWallets()
        .asFlow()
        .mapToList(Dispatchers.IO)

    val envelopes: Flow<List<Envelope>> = db.rivenQueries.getEnvelopes()
        .asFlow()
        .mapToList(Dispatchers.IO)

    val allTransactions: Flow<List<TransactionRecord>> = db.rivenQueries.getAllTransactions()
        .asFlow()
        .mapToList(Dispatchers.IO)

    fun getLastTransactions(limit: Long): Flow<List<TransactionRecord>> = 
        db.rivenQueries.getLastTransactions(limit)
            .asFlow()
            .mapToList(Dispatchers.IO)

    // Monthly Burn Rate (UAH base)
    val monthlyBurnRate: Flow<Double> = db.rivenQueries.getSetting("monthlyBurnRate")
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { it ?: 30000.0 } // Default to 30k UAH if not set

    // Observe rates from DB. Default to hardcoded base if not present.
    private val storedUsdRate: Flow<Double> = db.rivenQueries.getRate("USD")
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.firstOrNull()?.rateToUah ?: 42.0 }

    private val storedEurRate: Flow<Double> = db.rivenQueries.getRate("EUR")
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.firstOrNull()?.rateToUah ?: 45.0 }

    // Effective rates for UI (combining simulation)
    val effectiveUsdRate: Flow<Double> = combine(storedUsdRate, _simulationMultiplier) { rate, mult ->
        rate * mult
    }

    // 1. The totalNetWorthInUah flow must iterate through ALL wallets.
    val totalNetWorthInUah: Flow<Double> = combine(
        wallets, 
        _simulationMultiplier,
        storedUsdRate,
        storedEurRate
    ) { walletList, multiplier, usdBase, eurBase ->
        walletList.sumOf { wallet ->
            val baseRate = when (wallet.currency) {
                "UAH" -> 1.0
                "USD" -> usdBase
                "EUR" -> eurBase
                "BTC" -> 3804200.0 // Keep BTC hardcoded for now or fetch later
                else -> 1.0
            }
            
            val effectiveRate = if (wallet.currency == "UAH") 1.0 else baseRate * multiplier
            wallet.balance * effectiveRate
        }
    }

    // Calculation: TotalLiquidAssets / BurnRate.
    val financialRunwayMonths: Flow<Double> = combine(totalNetWorthInUah, monthlyBurnRate) { netWorth, burnRate ->
        if (burnRate > 0) netWorth / burnRate else 0.0
    }

    // ==================== CRUD OPERATIONS (All on IO dispatcher) ====================

    suspend fun updateMonthlyBurnRate(uahAmount: Double) = withContext(Dispatchers.IO) {
        db.rivenQueries.setSetting("monthlyBurnRate", uahAmount)
    }

    suspend fun updateRates(usd: Double, eur: Double) = withContext(Dispatchers.IO) {
        db.rivenQueries.transaction {
            db.rivenQueries.updateRate("USD", usd)
            db.rivenQueries.updateRate("EUR", eur)
        }
        _lastUpdated.value = Clock.System.now().toEpochMilliseconds()
    }

    suspend fun addWallet(name: String, currency: String, balance: Double, type: String, colorHex: String) = 
        withContext(Dispatchers.IO) {
        db.rivenQueries.insertWallet(name, currency, balance, type, colorHex)
    }

    suspend fun addEnvelope(name: String, limit: Double, icon: String, colorHex: String) = 
        withContext(Dispatchers.IO) {
        db.rivenQueries.insertEnvelope(name, limit, 0.0, icon, colorHex)
    }

    fun getWalletById(id: Long): Flow<Wallet?> = db.rivenQueries.getWalletById(id)
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)

    fun getTransactionsForWallet(walletId: Long): Flow<List<TransactionRecord>> = 
        db.rivenQueries.getTransactionsForWallet(walletId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    fun getEnvelopeById(id: Long): Flow<Envelope?> = db.rivenQueries.getEnvelopeById(id)
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)

    fun getTransactionsForEnvelope(envelopeId: Long): Flow<List<TransactionRecord>> = 
        db.rivenQueries.getTransactionsForEnvelope(envelopeId)
            .asFlow()
            .mapToList(Dispatchers.IO)

    /**
     * Perform a transaction (income or expense) with proper atomicity:
     * 1. Insert Transaction record
     * 2. Update Wallet balance (using balance + amount for atomicity)
     * 3. If expense with envelope, update envelope spent
     */
    suspend fun addTransaction(walletId: Long, amount: Double, category: String, envelopeId: Long? = null) = 
        withContext(Dispatchers.IO) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        db.transaction {
            // Step 1: Insert Transaction record
            db.rivenQueries.insertTransaction(walletId, amount, category, timestamp, envelopeId)
            
            // Step 2: Update Wallet balance (atomic: balance + amount)
            // For income: amount > 0, balance increases
            // For expense: amount < 0, balance decreases
            db.rivenQueries.updateWalletBalance(amount, walletId)
            
            // Step 3: If it's an expense and linked to an envelope, update envelope spent
            // amount is negative for expense, but currentSpent should increase by -amount
            if (amount < 0 && envelopeId != null) {
                db.rivenQueries.updateEnvelopeSpent(-amount, envelopeId)
            }
        }
    }

    /**
     * Transfer money between wallets atomically.
     */
    suspend fun performTransfer(fromWalletId: Long, toWalletId: Long, amount: Double) = 
        withContext(Dispatchers.IO) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        db.transaction {
            // Decrease from source wallet
            db.rivenQueries.insertTransaction(fromWalletId, -amount, "Transfer Out", timestamp, null)
            db.rivenQueries.updateWalletBalance(-amount, fromWalletId)
            
            // Increase destination wallet
            db.rivenQueries.insertTransaction(toWalletId, amount, "Transfer In", timestamp, null)
            db.rivenQueries.updateWalletBalance(amount, toWalletId)
        }
    }

    /**
     * Exchange currency between wallets atomically.
     */
    suspend fun performExchange(
        fromWalletId: Long,
        toWalletId: Long,
        fromAmount: Double,
        toAmount: Double
    ) = withContext(Dispatchers.IO) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        db.transaction {
            db.rivenQueries.insertTransaction(fromWalletId, -fromAmount, "Exchange Out", timestamp, null)
            db.rivenQueries.updateWalletBalance(-fromAmount, fromWalletId)
            
            db.rivenQueries.insertTransaction(toWalletId, toAmount, "Exchange In", timestamp, null)
            db.rivenQueries.updateWalletBalance(toAmount, toWalletId)
        }
    }

    suspend fun updateEnvelopeLimit(id: Long, newLimit: Double) = withContext(Dispatchers.IO) {
        db.rivenQueries.updateEnvelopeLimit(newLimit, id)
    }

    suspend fun deleteWallet(id: Long) = withContext(Dispatchers.IO) {
        db.rivenQueries.deleteWallet(id)
    }

    suspend fun deleteEnvelope(id: Long) = withContext(Dispatchers.IO) {
        db.rivenQueries.deleteEnvelope(id)
    }

    suspend fun injectDemoData() = withContext(Dispatchers.IO) {
        val random = kotlin.random.Random(Clock.System.now().toEpochMilliseconds())
        
        db.transaction {
            // 1. Wallets
            db.rivenQueries.insertWallet("Monobank (White)", "UAH", 12500.0, "Debit", "#2196F3")
            db.rivenQueries.insertWallet("Cash Stash", "USD", 450.0, "Cash", "#4CAF50")
            db.rivenQueries.insertWallet("Crypto", "BTC", 0.05, "Wallet", "#FF9800")
            
            // 2. Envelopes
            db.rivenQueries.insertEnvelope("Groceries", 8000.0, 0.0, "ShoppingBasket", "#4CAF50")
            db.rivenQueries.insertEnvelope("Fuel", 3000.0, 0.0, "LocalGasStation", "#FF9800")
            db.rivenQueries.insertEnvelope("Fun", 2000.0, 0.0, "Celebration", "#E91E63")
            db.rivenQueries.insertEnvelope("Donations", 5000.0, 0.0, "VolunteerActivism", "#2196F3")
        }

        // Fetch IDs to link transactions
        val wallets = db.rivenQueries.getAllWallets().executeAsList()
        val envelopes = db.rivenQueries.getEnvelopes().executeAsList()

        if (wallets.isEmpty()) return@withContext

        db.transaction {
            val now = Clock.System.now().toEpochMilliseconds()
            val dayMillis = 24 * 60 * 60 * 1000L
            
            repeat(15) { i ->
                val wallet = wallets.random()
                val isExpense = random.nextBoolean()
                val amount = if (isExpense) -(50.0 + random.nextInt(1000)) else (1000.0 + random.nextInt(5000))
                val date = now - (random.nextInt(7) * dayMillis) - (random.nextInt(24) * 3600000L)
                
                val envelope = if (isExpense) envelopes.random() else null
                val category = envelope?.name ?: if (isExpense) "Other" else "Salary"
                
                db.rivenQueries.insertTransaction(wallet.id, amount, category, date, envelope?.id)
                db.rivenQueries.updateWalletBalance(amount, wallet.id)
                if (isExpense && envelope != null) {
                    db.rivenQueries.updateEnvelopeSpent(-amount, envelope.id)
                }
            }
        }
    }

    fun simulateCrisisMode(targetUsdRate: Double) {
        // Use 42.0 as the 'Normal' reference point for the multiplier
        val baseUsd = 42.0
        val newMultiplier = targetUsdRate / baseUsd
        _simulationMultiplier.value = newMultiplier
    }
}
