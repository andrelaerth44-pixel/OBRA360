package com.obra360.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.ceil

val Paper=Color(0xFFF7F7F2); val Ink=Color(0xFF20231F); val Green=Color(0xFF355C45); val Muted=Color(0xFF6F756F)

@Entity(tableName="works") data class Work(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val client:String="",val location:String="",val progress:Int=0)
@Entity(tableName="tasks") data class Task(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val title:String,val stage:String="Geral",val done:Boolean=false)
@Entity(tableName="diary") data class Diary(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val text:String,val time:Long=System.currentTimeMillis())
@Entity(tableName="checks") data class Check(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val title:String,val done:Boolean=false)

@Dao interface WorkDao{@Query("SELECT * FROM works ORDER BY id DESC") fun all():Flow<List<Work>>;@Insert suspend fun add(w:Work);@Update suspend fun update(w:Work)}
@Dao interface TaskDao{@Query("SELECT * FROM tasks WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<Task>>;@Insert suspend fun add(t:Task);@Update suspend fun update(t:Task)}
@Dao interface DiaryDao{@Query("SELECT * FROM diary WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<Diary>>;@Insert suspend fun add(d:Diary)}
@Dao interface CheckDao{@Query("SELECT * FROM checks WHERE workId=:id ORDER BY id") fun all(id:Long):Flow<List<Check>>;@Insert suspend fun add(c:Check);@Update suspend fun update(c:Check)}
@Database(entities=[Work::class,Task::class,Diary::class,Check::class],version=1,exportSchema=false)
abstract class DB:RoomDatabase(){abstract fun works():WorkDao;abstract fun tasks():TaskDao;abstract fun diary():DiaryDao;abstract fun checks():CheckDao
 companion object{private var x:DB?=null;fun get(a:Application)=x?:Room.databaseBuilder(a,DB::class.java,"obra360.db").build().also{x=it}}}

class VM(a:Application):AndroidViewModel(a){
 private val db=DB.get(a);val works=db.works().all().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 fun addWork(n:String,c:String,l:String)=viewModelScope.launch{db.works().add(Work(name=n,client=c,location=l))}
 fun tasks(id:Long)=db.tasks().all(id);fun addTask(id:Long,t:String)=viewModelScope.launch{db.tasks().add(Task(workId=id,title=t))}
 fun toggle(t:Task)=viewModelScope.launch{db.tasks().update(t.copy(done=!t.done))}
 fun diary(id:Long)=db.diary().all(id);fun addDiary(id:Long,t:String)=viewModelScope.launch{db.diary().add(Diary(workId=id,text=t))}
 fun checks(id:Long)=db.checks().all(id);fun addCheck(id:Long,t:String)=viewModelScope.launch{db.checks().add(Check(workId=id,title=t))}
 fun toggle(c:Check)=viewModelScope.launch{db.checks().update(c.copy(done=!c.done))}
 fun setProgress(w:Work,p:Int)=viewModelScope.launch{db.works().update(w.copy(progress=p.coerceIn(0,100)))}
}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{Theme{App()}}}}
@Composable fun Theme(content:@Composable()->Unit)=MaterialTheme(colorScheme=lightColorScheme(primary=Green,background=Paper,surface=Color.White,onSurface=Ink),content=content)

@Composable fun App(vm:VM=viewModel()){
 var tab by rememberSaveable{mutableStateOf(0)};var work by rememberSaveable{mutableStateOf<Long?>(null)};val works by vm.works.collectAsState()
 Surface(Modifier.fillMaxSize(),color=Paper){if(work!=null){Detail(works.firstOrNull{it.id==work},vm){work=null}}else Scaffold(containerColor=Paper,bottomBar={NavigationBar{Nav("Início",Icons.Default.Home,tab==0){tab=0};Nav("Obras",Icons.Default.Construction,tab==1){tab=1};Nav("Cálculos",Icons.Default.Calculate,tab==2){tab=2};Nav("Diário",Icons.Default.Description,tab==3){tab=3}}}){p->Box(Modifier.padding(p)){when(tab){0->Home(works){work=it.id};1->Works(works,vm){work=it.id};2->Calc();3->DiaryScreen(works,vm)}}}}}
@Composable fun Nav(t:String,i:androidx.compose.ui.graphics.vector.ImageVector,s:Boolean,c:()->Unit)=NavigationBarItem(s,c,icon={Icon(i,null)},label={Text(t)})
@Composable fun Home(ws:List<Work>,open:(Work)->Unit){LazyColumn{item{Title("OBRA360","Controle prático da obra, mesmo offline.")};item{Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Obras",ws.size,Modifier.weight(1f));Stat("Ativas",ws.count{it.progress<100},Modifier.weight(1f));Stat("Concluídas",ws.count{it.progress==100},Modifier.weight(1f))}};item{Head("Obras recentes")};if(ws.isEmpty())item{Empty("Ainda não há obras","Crie uma obra para começar.")};items(ws){Card(it,open)};item{Spacer(Modifier.height(80.dp))}}}
@Composable fun Title(a:String,b:String){Column(Modifier.padding(20.dp)){Text(a,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text(b,color=Muted)}}
@Composable fun Head(t:String)=Text(t,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.padding(20.dp,22.dp,20.dp,10.dp))
@Composable fun Stat(t:String,n:Int,modifier:Modifier){Surface(modifier,shape=RoundedCornerShape(18.dp),color=Color.White){Column(Modifier.padding(13.dp)){Text(n.toString(),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(t,color=Muted)}}}
@Composable fun Empty(a:String,b:String){Column(Modifier.fillMaxWidth().padding(40.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Construction,null,tint=Muted,modifier=Modifier.size(40.dp));Text(a,fontWeight=FontWeight.Bold);Text(b,color=Muted)}}
@Composable fun Card(w:Work,open:(Work)->Unit){Surface(Modifier.padding(8.dp,5.dp).fillMaxWidth().clickable{open(w)},shape=RoundedCornerShape(20.dp),color=Color.White){Column(Modifier.padding(17.dp)){Row{Column(Modifier.weight(1f)){Text(w.name,fontWeight=FontWeight.Bold);Text(w.client,color=Muted)};Text(w.progress.toString()+"%",color=Green,fontWeight=FontWeight.Bold)};LinearProgressIndicator(w.progress/100f,Modifier.fillMaxWidth().padding(top=12.dp));Text(w.location,color=Muted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))}}}
@Composable fun Works(ws:List<Work>,vm:VM,open:(Work)->Unit){var show by remember{mutableStateOf(false)};LazyColumn{item{Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically){Text("Obras",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));FilledIconButton({show=true}){Icon(Icons.Default.Add,null)}}};items(ws){Card(it,open)};item{Spacer(Modifier.height(80.dp))}};if(show)WorkDialog({show=false;vm.addWork(it[0],it[1],it[2])}){show=false}}
@Composable fun WorkDialog(save:(List<String>)->Unit,cancel:()->Unit){var n by remember{mutableStateOf("")};var c by remember{mutableStateOf("")};var l by remember{mutableStateOf("")};AlertDialog(onDismissRequest=cancel,title={Text("Nova obra")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(n,{n=it},label={Text("Nome")});OutlinedTextField(c,{c=it},label={Text("Cliente")});OutlinedTextField(l,{l=it},label={Text("Local")})}},confirmButton={Button(enabled=n.isNotBlank(),onClick={save(listOf(n,c,l))}){Text("Criar")}},dismissButton={TextButton(onClick=cancel){Text("Cancelar")}})}
@Composable fun Detail(w:Work?,vm:VM,back:()->Unit){if(w==null){back();return};var page by rememberSaveable{mutableStateOf(0)};val ts by vm.tasks(w.id).collectAsState(initial=emptyList());val ds by vm.diary(w.id).collectAsState(initial=emptyList());val cs by vm.checks(w.id).collectAsState(initial=emptyList());var dialog by remember{mutableStateOf("")};Column{Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null)};Column{Text(w.name,fontWeight=FontWeight.Bold);Text(w.client,color=Muted)}};ScrollableTabRow(selectedTabIndex=page){listOf("Resumo","Tarefas","Diário","Checklist").forEachIndexed{i,s->Tab(selected=page==i,onClick={page=i},text={Text(s)})}};when(page){0->Summary(w,vm);1->TaskPage(ts,{dialog="task"}){i->vm.toggle(ts[i])};2->ListPage("Diário",ds.map{it.text},{dialog="diary"}){_,_->};else->CheckPage(cs,{dialog="check"}){i->vm.toggle(cs[i])}}};if(dialog.isNotBlank())TextDialog(dialog,{value->dialog="";when(dialog){"task"->vm.addTask(w.id,value);"diary"->vm.addDiary(w.id,value);"check"->vm.addCheck(w.id,value)}},{dialog=""})}}
@Composable fun Summary(w:Work){LazyColumn{item{Column(Modifier.padding(20.dp)){Text("Progresso",fontWeight=FontWeight.Bold);Text(w.progress.toString()+"%",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);LinearProgressIndicator(w.progress/100f,Modifier.fillMaxWidth().padding(vertical=12.dp));Text("Local",fontWeight=FontWeight.Bold);Text(w.location.ifBlank{"Não informado"},color=Muted)}};item{Head("Etapas");listOf("Fundação","Estrutura","Alvenaria","Instalações","Revestimentos","Acabamentos").forEach{ListItem(headlineContent={Text(it)},leadingContent={Icon(Icons.Default.Construction,null,tint=Green)})}}}}
@Composable fun ListPage(title:String,rows:List<String>,add:()->Unit,toggle:(Int,Boolean)->Unit){LazyColumn{item{Row(Modifier.padding(20.dp)){Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};items(rows.size){i->ListItem(headlineContent={Text(rows[i])})};item{Spacer(Modifier.height(80.dp))}}}
@Composable fun TaskPage(rows:List<Task>,add:()->Unit,toggle:(Int)->Unit){LazyColumn{item{Row(Modifier.padding(20.dp)){Text("Tarefas",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};items(rows.size){i->ListItem(headlineContent={Text(rows[i].title)},supportingContent={Text(rows[i].stage)},leadingContent={Checkbox(checked=rows[i].done,onCheckedChange={toggle(i)})})};item{Spacer(Modifier.height(80.dp))}}}
@Composable fun CheckPage(rows:List<Check>,add:()->Unit,toggle:(Int)->Unit){LazyColumn{item{Row(Modifier.padding(20.dp)){Text("Checklist",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};items(rows.size){i->ListItem(headlineContent={Text(rows[i].title)},leadingContent={Checkbox(checked=rows[i].done,onCheckedChange={toggle(i)})})};item{Spacer(Modifier.height(80.dp))}}}

@Composable fun TextDialog(kind:String,save:(String)->Unit,cancel:()->Unit){var x by remember{mutableStateOf("")};val title=when(kind){"task"->"Nova tarefa";"diary"->"Registo do diário";else->"Novo item"};AlertDialog(onDismissRequest=cancel,title={Text(title)},text={OutlinedTextField(x,{x=it},label={Text("Descrição")},modifier=Modifier.fillMaxWidth(),minLines=3)},confirmButton={Button(enabled=x.isNotBlank(),onClick={save(x)}){Text("Guardar")}},dismissButton={TextButton(onClick=cancel){Text("Cancelar")}})}
@Composable fun Calc(){var type by rememberSaveable{mutableStateOf(0)};var a by remember{mutableStateOf("")};var b by remember{mutableStateOf("")};var c by remember{mutableStateOf("")};val r=when(type){0->a.d()*b.d()*c.d();1->a.d()*(b.d()/100);2->ceil(a.d()*b.d());else->ceil(a.d()/b.d())};LazyColumn(Modifier.padding(20.dp)){item{Title("Calculadoras","Estimativas rápidas para o canteiro.")};item{SingleChoiceSegmentedButtonRow{listOf("Concreto","Argamassa","Blocos","Pintura").forEachIndexed{i,s->SegmentedButton(selected=type==i,onClick={type=i},shape=SegmentedButtonDefaults.itemShape(i,4)){Text(s)}}}};item{Spacer(Modifier.height(15.dp));OutlinedTextField(a,{a=it},label={Text(if(type==2)"Área (m²)" else "Medida")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(8.dp));OutlinedTextField(b,{b=it},label={Text(if(type==0)"Largura" else if(type==1)"Espessura (cm)" else if(type==2)"Peças/m²" else "Rendimento m²")},modifier=Modifier.fillMaxWidth());if(type==0){Spacer(Modifier.height(8.dp));OutlinedTextField(c,{c=it},label={Text("Altura")},modifier=Modifier.fillMaxWidth())};Spacer(Modifier.height(18.dp))};item{Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=Green){Column(Modifier.padding(22.dp)){Text("Resultado",color=Color.White);Text("%.2f".format(r),style=MaterialTheme.typography.headlineMedium,color=Color.White,fontWeight=FontWeight.Bold)}}};item{Text("Use como estimativa e confirme traço, perdas e especificações do projeto.",color=Muted,modifier=Modifier.padding(top=16.dp));Spacer(Modifier.height(80.dp))}}}
fun String.d()=replace(",","." ).toDoubleOrNull()?:0.0
@Composable fun DiaryScreen(ws:List<Work>,vm:VM){var id by rememberSaveable{mutableStateOf(ws.firstOrNull()?.id)};var show by remember{mutableStateOf(false)};val ds=id?.let{vm.diary(it)}?.collectAsState(initial=emptyList())?.value?:emptyList();LazyColumn{item{Row(Modifier.padding(20.dp)){Text("Diário",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick={show=true}){Icon(Icons.Default.Add,null)}}};items(ds){ListItem(headlineContent={Text(it.text)})};item{Spacer(Modifier.height(80.dp))}};if(show&&id!=null)TextDialog("diary",{show=false;vm.addDiary(id!!,it)},{show=false})}
