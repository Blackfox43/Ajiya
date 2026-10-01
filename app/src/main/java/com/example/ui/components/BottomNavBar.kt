package com.example.ui.components
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppNavDestination

@Composable
fun BottomNavBar(currentDestination:AppNavDestination,onNavigate:(AppNavDestination)->Unit,modifier:Modifier=Modifier){
 NavigationBar(modifier=modifier.navigationBarsPadding(),containerColor=NavySurface,tonalElevation=0.dp){
  val items=listOf(
   Triple(AppNavDestination.HOME,Icons.Default.Shield,"Safety"),
   Triple(AppNavDestination.CIRCLE,Icons.Default.Group,"Circle"),
   Triple(AppNavDestination.HELPER,Icons.Default.Handshake,"Helper"),
   Triple(AppNavDestination.HISTORY,Icons.Default.History,"History"),
   Triple(AppNavDestination.SECURITY_LEGAL,Icons.Default.Lock,"Security"))
  items.forEach{(dest,icon,label)->
   NavigationBarItem(selected=currentDestination==dest,onClick={onNavigate(dest)},
    icon={Icon(icon,label)},label={Text(label,fontSize=10.sp)},
    colors=NavigationBarItemDefaults.colors(selectedIconColor=BeaconCyan,selectedTextColor=BeaconCyan,
      indicatorColor=BeaconCyan.copy(.12f),unselectedIconColor=TextMuted,unselectedTextColor=TextMuted))
  }
 }
}
