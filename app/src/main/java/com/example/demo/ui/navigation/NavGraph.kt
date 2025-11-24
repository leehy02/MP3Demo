package com.example.demo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.demo.ble.BleViewModel
//import com.example.demo.ui.screen.FileListScreen
import com.example.demo.ui.screen.AudioScreen

//@Composable
//fun AppNavHost(
//    vm: BleViewModel,
//    navController: NavHostController = rememberNavController()
//) {
//    NavHost(navController = navController, startDestination = Routes.Home) {
//        composable(Routes.Home)     { AudioScreen(vm, navController) }
//        composable(Routes.FileList) { FileListScreen(vm, navController) }
//    }
//}
