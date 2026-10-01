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

@Composable fun MyCircleScreen(viewModel:SafetyViewModel){
 val contacts by viewModel.trustedContacts.collectAsState(); var add by remember{mutableStateOf(false)}; var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var rel by remember{mutableStateOf("")};var sms by remember{mutableStateOf(false)}
 LazyColumn(Modifier.fillMaxSize().background(NavyBackground).padding(20.dp),contentPadding=PaddingValues(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Text("Trusted Circle",fontSize=28.sp,fontWeight=FontWeight.Black);Text("Your emergency network. Only people you choose.",color=TextSecondary,fontSize=13.sp)}
  item{Card(colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.VerifiedUser,null,tint=BeaconCyan,modifier=Modifier.size(30.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("${contacts.size}/5 trusted contacts",fontWeight=FontWeight.Bold);Text("Consent-based sharing only",fontSize=11.sp,color=TextSecondary)};Button(onClick={add=true},shape=RoundedCornerShape(12.dp)){Text("Add")}}}}
  item{OutlinedButton(onClick={sms=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Icon(Icons.Default.Sms,null);Spacer(Modifier.width(8.dp));Text("Preview emergency SMS")}}
  items(contacts,key={it.id}){c->Card(colors=CardDefaults.cardColors(containerColor=NavySurface),shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Surface(Modifier.size(44.dp),shape=RoundedCornerShape(14.dp),color=BeaconCyan.copy(.12f)){Box(contentAlignment=Alignment.Center){Text(c.name.take(1).uppercase(),color=BeaconCyan,fontWeight=FontWeight.Bold)}};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Bold);Text(c.relationship,color=TextSecondary,fontSize=12.sp);Text(c.phone,color=TextMuted,fontSize=11.sp)};if(c.isPrimary)Text("PRIMARY",color=SafeGreen,fontSize=9.sp,fontWeight=FontWeight.Bold);else IconButton({viewModel.deleteContact(c.id)}){Icon(Icons.Default.DeleteOutline,null,tint=TextMuted)}}}}
 }
 if(add)AlertDialog(onDismissRequest={add=false},title={Text("Add trusted contact")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(name,{name=it},label={Text("Full name")});OutlinedTextField(phone,{phone=it},label={Text("Phone")});OutlinedTextField(rel,{rel=it},label={Text("Relationship")})}},confirmButton={Button(onClick={if(name.isNotBlank()&&phone.isNotBlank()){viewModel.addContact(name,phone,rel.ifBlank{"Trusted contact"});name="";phone="";rel="";add=false}}){Text("Add")}},dismissButton={TextButton({add=false}){Text("Cancel")}})}
 if(sms)AlertDialog(onDismissRequest={sms=false},title={Text("Emergency message")},text={Text(viewModel.getSmsBroadcastText(),fontSize=13.sp)},confirmButton={Button({sms=false}){Text("Close")}})
}
