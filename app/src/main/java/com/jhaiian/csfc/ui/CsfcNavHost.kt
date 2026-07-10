package com.jhaiian.csfc.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jhaiian.csfc.crash.CrashLogsScreen
import com.jhaiian.csfc.ui.calculator.CalculatorScreen

private const val ROUTE_CALCULATOR = "calculator"
private const val ROUTE_CRASH_LOGS = "crash_logs"

@Composable
fun CsfcNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_CALCULATOR) {
        composable(ROUTE_CALCULATOR) {
            CalculatorScreen(onOpenCrashLogs = { navController.navigate(ROUTE_CRASH_LOGS) })
        }
        composable(ROUTE_CRASH_LOGS) {
            CrashLogsScreen(onBack = { navController.popBackStack() })
        }
    }
}
