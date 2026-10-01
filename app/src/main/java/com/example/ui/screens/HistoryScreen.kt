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

@Composable fun HistoryScreen(viewModel:SafetyViewModel){
 val events by viewModel.allEvents.collectAsState()
 LazyColumn(Modifier.fillMaxSize().background(NavyBackground).padding(20.dp),contentPadding=PaddingValues(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Safety History",fontSize=28.sp,fontWeight=FontWeight.Black);Text("Ephemeral records are designed to expire after 24 hours.",color=TextSecondary,fontSize=12.sp)};TextButton({viewModel.purgeHistoryNow()}){Text("Purge",color=AlertAmber)}}}
  if(events.isEmpty())item{Card(colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.History,null,tint=TextMuted,modifier=Modifier.size(44.dp));Spacer(Modifier.height(10.dp));Text("No recent events",fontWeight=FontWeight.Bold);Text("Resolved alerts will appear here temporarily.",color=TextSecondary,fontSize=12.sp)}}}
  items(events,key={it.id}){e->Card(colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(17.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(if(e.mode=="RISKY_TRIP")Icons.Default.Route else Icons.Default.Warning,null,tint=if(e.status=="active")CrimsonLight else SafeGreen);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(e.mode.replace("_"," "),fontWeight=FontWeight.Bold);Text(e.address,color=TextSecondary,fontSize=11.sp)};Text(e.status.uppercase(),fontSize=9.sp,fontWeight=FontWeight.Bold,color=if(e.status=="active")CrimsonLight else SafeGreen)};Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){Text("Battery ${e.battery}%",fontSize=11.sp,color=TextMuted);Text(e.id,fontSize=11.sp,color=TextMuted)}}}}
 }
}
