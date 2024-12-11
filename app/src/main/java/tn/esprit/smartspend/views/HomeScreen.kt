package tn.esprit.smartspend.views

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.esprit.smartspend.R
import tn.esprit.smartspend.model.Category
import tn.esprit.smartspend.model.Expense
import tn.esprit.smartspend.model.Income
import tn.esprit.smartspend.model.Item
import tn.esprit.smartspend.network.RetrofitInstance
import tn.esprit.smartspend.ui.theme.*
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    token: String,
    navController: NavHostController,
    onViewAllExpensesClick: (List<Expense>) -> Unit,
    onViewAllIncomesClick: (List<Income>) -> Unit,
    onAddItemClick: () -> Unit,
) {
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var incomes by remember { mutableStateOf<List<Income>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var totalIncome by remember { mutableStateOf(0.0) }
    var totalExpenses by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                categories = fetchCategories()
                val (fetchedExpenses, fetchedIncomes) = fetchRecentTransactions(token)
                expenses = fetchedExpenses
                incomes = fetchedIncomes
                totalIncome = incomes.sumOf { it.amount }
                totalExpenses = expenses.sumOf { it.amount }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error fetching data: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top Bar Image with Balance Card above it
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_topbar),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )

                // Balance Card slightly down from the top bar
                BalanceCard(totalIncome, totalExpenses, modifier = Modifier.offset(y = 110.dp))
            }

            // Scrollable Content (Expenses, Incomes, etc.)
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.padding(top = 330.dp)) {
                        item {
                            // Spending Progress Bar
                            SpendingProgressBar(totalIncome = totalIncome, totalExpenses = totalExpenses)

                            // Recent Expenses Section
                            SectionWithItems(
                                title = "Recent Expenses",
                                items = expenses.take(3).map {
                                    val categoryName = resolveCategoryName(it.category, categories)
                                    val iconRes = resolveCategoryIcon(it.category, categories)
                                    Item(
                                        description = it.description, // Expense description
                                        iconRes = iconRes,           // Icon for the category
                                        amount = -it.amount,         // Negative for expenses
                                        date = it.date               // Original date string
                                    )
                                },
                                onViewAllClick = { onViewAllExpensesClick(expenses) }
                            )

                        }

                        item {
                            SectionWithItems(
                                title = "Recent Incomes",
                                items = incomes.take(3).map {
                                    val categoryName = resolveCategoryName(it.category, categories)
                                    val iconRes = resolveCategoryIcon(it.category, categories)
                                    Item(
                                        description = it.description, // Income description
                                        iconRes = iconRes,           // Icon for the category
                                        amount = it.amount,          // Positive for incomes
                                        date = it.date               // Original date string
                                    )
                                },
                                onViewAllClick = { onViewAllIncomesClick(incomes) }
                            )

                        }
                    }
                }

                // Floating Action Button
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(
                        onClick = onAddItemClick,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(56.dp),
                        containerColor = Navy
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Sand)
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceCard(totalIncome: Double, totalExpenses: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Navy),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Balance: $${String.format("%.2f", totalIncome - totalExpenses)}",
                color = Sand,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Expenses", color = DarkOrangeRed3, fontSize = 18.sp)
                    Text(
                        text = "$${String.format("%.2f", totalExpenses)}",
                        color = DarkOrangeRed3,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Income", color = Teal, fontSize = 18.sp)
                    Text(
                        text = "$${String.format("%.2f", totalIncome)}",
                        color = Teal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SectionWithItems(
    title: String,
    items: List<Item>, // Use the Item data class here
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Navy,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All",
                color = SupportingColor,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Align the amount to the right
            ) {
                // Icon on the left
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Description and Date
                Column(modifier = Modifier.weight(1f)) { // Takes up remaining space
                    Text(
                        text = item.description,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = formatDate(item.date), // Use the helper function to format the date
                        fontSize = 12.sp, // Smaller font size
                        color = Color.Gray
                    )
                }

                // Amount
                val amountText = if (item.amount >= 0) {
                    "+$${String.format("%.2f", item.amount)}"
                } else {
                    "-$${String.format("%.2f", item.amount.absoluteValue)}"
                }
                val amountColor = if (item.amount >= 0) Teal else DarkOrangeRed3
                Text(
                    text = amountText,
                    fontSize = 16.sp,
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper function for date formatting remains the same
fun formatDate(date: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val parsedDate = inputFormat.parse(date)
        outputFormat.format(parsedDate ?: date)
    } catch (e: Exception) {
        date // Return original date if parsing fails
    }
}







@Composable
fun SpendingProgressBar(totalIncome: Double, totalExpenses: Double) {
    val progress = if (totalIncome > 0) (totalExpenses / totalIncome).toFloat() else 1f
    val cappedProgress = progress.coerceAtMost(1f)
    val progressColor = if (progress > 1f) listOf(Red.copy(alpha = 0.2f),Red,)
    else listOf(Teal, MostImportantColor)

    val gradient = Brush.horizontalGradient(colors = progressColor)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spending Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MostImportantColor
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_trending_up), // Example modern icon
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MostImportantColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Modernized Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(50.dp)) // Fully rounded edges
                .background(Color.Gray.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(cappedProgress)
                    .clip(RoundedCornerShape(50.dp))
                    .background(gradient)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Percentage and Warning
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spent: ${String.format("%.1f", progress * 100)}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (progress > 1f) Red else Teal
            )
            if (progress > 1f) {
                Text(
                    text = "Exceeding Income!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Red
                )
            }
        }
    }
}



fun resolveCategoryName(categoryId: String, categories: List<Category>): String {
    return categories.find { it._id == categoryId }?.name ?: "Unknown"
}

fun resolveCategoryIcon(categoryId: String, categories: List<Category>): Int {
    val categoryName = resolveCategoryName(categoryId, categories)
    return when (categoryName) {
        "Groceries" -> R.drawable.groceriesnav
        "Entertainment" -> R.drawable.movienav
        "Healthcare" -> R.drawable.healthnav
        "Housing" -> R.drawable.housenav
        "Transportation" -> R.drawable.carnav
        "Utilities" -> R.drawable.othernav
        "Salary" -> R.drawable.cashnav
        else -> R.drawable.cash_11761323
    }
}

suspend fun fetchCategories(): List<Category> {
    return withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.api.getCategories().execute()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error fetching categories: ${e.message}")
            emptyList()
        }
    }
}

suspend fun fetchRecentTransactions(token: String): Pair<List<Expense>, List<Income>> {
    return withContext(Dispatchers.IO) {
        try {
            val incomesResponse = RetrofitInstance.api.getIncomes(token).execute()
            val expensesResponse = RetrofitInstance.api.getExpenses(token).execute()

            val incomes = if (incomesResponse.isSuccessful) incomesResponse.body() ?: emptyList() else emptyList()
            val expenses = if (expensesResponse.isSuccessful) expensesResponse.body() ?: emptyList() else emptyList()

            Pair(expenses, incomes)
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error fetching transactions: ${e.message}")
            Pair(emptyList(), emptyList())
        }
    }
}
