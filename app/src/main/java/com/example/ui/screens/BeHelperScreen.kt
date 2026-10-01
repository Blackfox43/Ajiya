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

@Composable fun BeHelperScreen(viewModel:SafetyViewModel){
 val user by viewModel.currentUser.collectAsState();val alerts by viewModel.helperAlerts.collectAsState()
 LazyColumn(Modifier.fillMaxSize().background(NavyBackground).padding(20.dp),contentPadding=PaddingValues(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Text("Safe Circle",fontSize=28.sp,fontWeight=FontWeight.Black);Text("Become a nearby responder without seeing private identities.",color=TextSecondary,fontSize=13.sp)}
  item{Card(colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Be a Helper",fontWeight=FontWeight.Bold,fontSize=17.sp);Text(if(user?.isHelper==true)"You're receiving nearby distress alerts." else "Receive anonymized alerts within your radius.",fontSize=12.sp,color=TextSecondary)};Switch(checked=user?.isHelper==true,onCheckedChange={viewModel.setHelperStatus(it)})}}}
  item{Text("Nearby alerts",fontWeight=FontWeight.Bold,fontSize=17.sp)}
  if(alerts.isEmpty())item{EmptyState("No active distress beacons","New nearby alerts will appear here.")}
  items(alerts,key={it.id}){a->Card(colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(17.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.LocationOn,null,tint=CrimsonLight);Spacer(Modifier.width(8.dp));Text(a.crisisType,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("${a.distanceMeters}m",color=AlertAmber,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(7.dp));Text("Identity protected until help is accepted.",fontSize=11.sp,color=TextSecondary);Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({viewModel.respondToHelperAlert(a.id,true)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp)){Text("Accept Help")};OutlinedButton({viewModel.respondToHelperAlert(a.id,false)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp)){Text("Decline")}}}}}
 }
}
@Composable private fun EmptyState(title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(26.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.RadarScan,null,tint=TextMuted,modifier=Modifier.size(42.dp));Spacer(Modifier.height(10.dp));Text(title,fontWeight=FontWeight.Bold);Text(body,color=TextSecondary,fontSize=12.sp)}}}
