package com.messledger.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.messledger.app.ui.auth.AuthScreen
import com.messledger.app.ui.home.CreateMessScreen
import com.messledger.app.ui.home.HomeScreen
import com.messledger.app.ui.home.JoinMessScreen
import com.messledger.app.ui.mess.MessDetailScreen
import com.messledger.app.ui.mess.activity.ActivityScreen
import com.messledger.app.ui.mess.contributions.AddEditContributionScreen
import com.messledger.app.ui.mess.contributions.ContributionsScreen
import com.messledger.app.ui.mess.expenses.AddEditExpenseScreen
import com.messledger.app.ui.mess.expenses.ExpensesScreen
import com.messledger.app.ui.mess.guestmeals.AddEditGuestMealScreen
import com.messledger.app.ui.mess.guestmeals.GuestMealsScreen
import com.messledger.app.ui.mess.meals.MealsScreen
import com.messledger.app.ui.mess.members.MembersScreen
import com.messledger.app.ui.mess.settings.MessSettingsScreen
import com.messledger.app.ui.requests.PendingRequestsScreen
import com.messledger.app.ui.requests.RequestStatusScreen

@Composable
fun MessLedgerNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToCreateMess = { navController.navigate(Routes.CREATE_MESS) },
                onNavigateToJoinMess = { navController.navigate(Routes.JOIN_MESS) },
                onNavigateToMess = { messId -> navController.navigate(Routes.messDetail(messId)) },
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.CREATE_MESS) {
            CreateMessScreen(
                onBack = { navController.popBackStack() },
                onMessCreated = { messId -> 
                    navController.navigate(Routes.messDetail(messId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        
        composable(
            route = Routes.JOIN_MESS,
            deepLinks = listOf(navDeepLink { uriPattern = "messledger://invite/{messId}" })
        ) {
            JoinMessScreen(
                messId = null,
                onBack = { navController.popBackStack() },
                onSuccess = { messId -> navController.navigate(Routes.messDetail(messId)) }
            )
        }
        
        composable(
            route = Routes.JOIN_MESS_WITH_ID,
            arguments = listOf(navArgument("messId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "messledger://invite/{messId}" })
        ) { backStackEntry ->
            JoinMessScreen(
                messId = backStackEntry.arguments?.getString("messId"),
                onBack = { navController.popBackStack() },
                onSuccess = { messId -> navController.navigate(Routes.messDetail(messId)) }
            )
        }
        
        composable(
            route = Routes.MESS_DETAIL,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            MessDetailScreen(
                messId = messId,
                onNavigateToSettings = { navController.navigate(Routes.messSettings(messId)) },
                onNavigateToRequests = { navController.navigate(Routes.pendingRequests(messId)) },
                onNavigateToMembers = { navController.navigate(Routes.members(messId)) },
                onNavigateToAddExpense = { navController.navigate(Routes.addExpense(messId)) },
                onNavigateToEditExpense = { expenseId -> navController.navigate(Routes.editExpense(messId, expenseId)) },
                onNavigateToAddContribution = { navController.navigate(Routes.addContribution(messId)) },
                onNavigateToEditContribution = { contribId -> navController.navigate(Routes.editContribution(messId, contribId)) },
                onNavigateToGuestMeals = { navController.navigate(Routes.guestMeals(messId)) },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.MEMBERS,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            MembersScreen(
                messId = messId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.MEALS,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            MealsScreen(
                messId = messId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.EXPENSES,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            ExpensesScreen(
                messId = messId,
                onNavigateBack = { navController.popBackStack() },
                onAddExpense = { navController.navigate(Routes.addExpense(messId)) },
                onEditExpense = { expenseId -> navController.navigate(Routes.editExpense(messId, expenseId)) }
            )
        }
        
        composable(
            route = Routes.ADD_EDIT_EXPENSE,
            arguments = listOf(
                navArgument("messId") { type = NavType.StringType },
                navArgument("expenseId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            AddEditExpenseScreen(
                messId = messId,
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.CONTRIBUTIONS,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            ContributionsScreen(
                messId = messId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddContribution = { id -> navController.navigate(Routes.addContribution(id)) },
                onNavigateToEditContribution = { id, contributionId -> navController.navigate(Routes.editContribution(id, contributionId)) }
            )
        }
        
        composable(
            route = Routes.ADD_EDIT_CONTRIBUTION,
            arguments = listOf(
                navArgument("messId") { type = NavType.StringType },
                navArgument("contributionId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            val contributionId = backStackEntry.arguments?.getString("contributionId")
            AddEditContributionScreen(
                messId = messId,
                contributionId = contributionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.GUEST_MEALS,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            GuestMealsScreen(
                messId = messId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddGuestMeal = { id -> navController.navigate(Routes.addGuestMeal(id)) },
                onNavigateToEditGuestMeal = { id, guestMealId -> navController.navigate(Routes.editGuestMeal(id, guestMealId)) }
            )
        }
        
        composable(
            route = Routes.ADD_EDIT_GUEST_MEAL,
            arguments = listOf(
                navArgument("messId") { type = NavType.StringType },
                navArgument("guestMealId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            val guestMealId = backStackEntry.arguments?.getString("guestMealId")
            AddEditGuestMealScreen(
                messId = messId,
                guestMealId = guestMealId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.ACTIVITY,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) { backStackEntry ->
            val messId = backStackEntry.arguments?.getString("messId") ?: ""
            ActivityScreen(
                messId = messId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.MESS_SETTINGS,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) {
            MessSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.PENDING_REQUESTS,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) {
            PendingRequestsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Routes.REQUEST_STATUS,
            arguments = listOf(navArgument("messId") { type = NavType.StringType })
        ) {
            RequestStatusScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
