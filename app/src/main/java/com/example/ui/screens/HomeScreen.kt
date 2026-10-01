package com.example.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

@Composable fun HomeScreen(viewModel:SafetyViewModel){
 val user by viewModel.currentUser.collectAsState(); val active by viewModel.activeSos.collectAsState()
 val battery by viewModel.batteryLevel.collectAsState(); val address by viewModel.currentAddress.collectAsState()
 val notice by viewModel.statusNotice.collectAsState(); var mode by remember{mutableStateOf("KIDNAP_SILENT")}
 val modes=listOf("KIDNAP_SILENT" to "Silent", "ACCIDENT_LOUD" to "Accident", "DISASTER_CHECKIN" to "Disaster")
 LazyColumn(modifier=Modifier.fillMaxSize().background(NavyBackground).padding(horizontal=20.dp),contentPadding=PaddingValues(top=22.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  item{
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){ Text("AJIYA",fontSize=28.sp,fontWeight=FontWeight.Black,color=TextPrimary); Text("Safety, when it matters.",color=TextSecondary,fontSize=13.sp)}
    Surface(shape=RoundedCornerShape(50),color=SafeGreen.copy(.10f)){Row(Modifier.padding(horizontal=11.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(SafeGreen));Spacer(Modifier.width(7.dp));Text("READY",fontSize=10.sp,color=SafeGreen,fontWeight=FontWeight.Bold)}}}
  }
  item{
   Card(colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(24.dp),border=ButtonDefaults.outlinedButtonBorder){
    Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
     Text(if(active==null)"YOU'RE PROTECTED" else "EMERGENCY ACTIVE",fontSize=12.sp,fontWeight=FontWeight.Bold,color=if(active==null)SafeGreen else CrimsonLight)
     Spacer(Modifier.height(10.dp))
     com.example.ui.components.SosEmergencyButton(crisisMode=mode,isActive=active!=null,onTriggerSos={viewModel.triggerSos(mode)},modifier=Modifier.padding(vertical=4.dp))
     if(active!=null){Button(onClick={viewModel.resolveActiveSos()},colors=ButtonDefaults.buttonColors(containerColor=SafeGreen),shape=RoundedCornerShape(14.dp)){Icon(Icons.Default.Check, null);Spacer(Modifier.width(7.dp));Text("I'm Safe — Resolve Alert",color=Color.Black,fontWeight=FontWeight.Bold)}}}
   }
  }
  item{
   Column{Text("Crisis mode",fontWeight=FontWeight.Bold,fontSize=16.sp);Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){modes.forEach{(key,label)->FilterChip(selected=mode==key,onClick={mode=key;viewModel.setCrisisMode(key)},label={Text(label)},leadingIcon={Icon(if(key=="KIDNAP_SILENT")Icons.Default.VisibilityOff else if(key=="ACCIDENT_LOUD")Icons.Default.Warning else Icons.Default.Public,null)})}}
   }
  }
  item{
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){
    InfoCard("BATTERY","$battery%","Device status",Modifier.weight(1f))
    InfoCard("LOCATION",if(address.length>24)address.take(24)+"…" else address,"Live location",Modifier.weight(1.5f))
   }
  }
  item{
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
    OutlinedButton(onClick={viewModel.triggerRiskyTrip()},modifier=Modifier.weight(1f),shape=RoundedCornerShape(14.dp)){Icon(Icons.Default.Route,null);Spacer(Modifier.width(6.dp));Text("1h Trip")}
    OutlinedButton(onClick={viewModel.refreshLocationAndAddress()},modifier=Modifier.weight(1f),shape=RoundedCornerShape(14.dp)){Icon(Icons.Default.MyLocation,null);Spacer(Modifier.width(6.dp));Text("Refresh")}
   }
  }
  item{
   Card(colors=CardDefaults.cardColors(containerColor=BeaconCyan.copy(.07f)),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().clickable{viewModel.navigateTo(AppNavDestination.CIRCLE)}){
    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Group,null,tint=BeaconCyan);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("Trusted Circle",fontWeight=FontWeight.Bold);Text("People who can receive your emergency signal",fontSize=12.sp,color=TextSecondary)};Icon(Icons.Default.ChevronRight,null,tint=TextMuted)}
   }
  }
  if(notice!=null)item{Snackbar(modifier=Modifier.fillMaxWidth(),action={TextButton({viewModel.dismissNotice()}){Text("Dismiss")}}){Text(notice!!)}}
 }
}
@Composable private fun InfoCard(title:String,value:String,subtitle:String,modifier:Modifier){Card(modifier=modifier,colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(15.dp)){Text(title,fontSize=9.sp,fontWeight=FontWeight.Bold,color=TextMuted);Spacer(Modifier.height(5.dp));Text(value,fontSize=15.sp,fontWeight=FontWeight.SemiBold);Text(subtitle,fontSize=10.sp,color=TextSecondary)}}}
