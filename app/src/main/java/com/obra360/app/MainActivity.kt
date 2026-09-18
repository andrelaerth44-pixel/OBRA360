package com.obra360.app

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

val Paper=Color(0xFFF7F7F2)
val Ink=Color(0xFF20231F)
val Green=Color(0xFF355C45)
val Muted=Color(0xFF6F756F)

@Entity(tableName="works")
data class Work(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val client:String="",val location:String="",val progress:Int=0)
@Entity(tableName="tasks")
data class Task(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val title:String,val stage:String="Geral",val done:Boolean=false)
@Entity(tableName="diary")
data class Diary(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val text:String,val time:Long=System.currentTimeMillis())
@Entity(tableName="checks")
data class Check(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val title:String,val done:Boolean=false)
@Entity(tableName="materials")
data class Material(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val name:String,val unit:String,val quantity:Double,val unitCost:Double)
@Entity(tableName="expenses")
data class Expense(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val description:String,val category:String,val amount:Double,val date:Long=System.currentTimeMillis())
@Entity(tableName="measurements")
data class Measurement(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val description:String,val unit:String,val quantity:Double,val unitPrice:Double)
@Entity(tableName="photos")
data class Photo(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val uri:String,val caption:String="",val time:Long=System.currentTimeMillis())
@Entity(tableName="documents")
data class ProjectDocument(@PrimaryKey(autoGenerate=true) val id:Long=0,val workId:Long,val uri:String,val name:String,val time:Long=System.currentTimeMillis())

@Dao interface WorkDao{@Query("SELECT * FROM works ORDER BY id DESC") fun all():Flow<List<Work>>;@Insert suspend fun add(w:Work);@Update suspend fun update(w:Work)}
@Dao interface TaskDao{@Query("SELECT * FROM tasks WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<Task>>;@Insert suspend fun add(t:Task);@Update suspend fun update(t:Task)}
@Dao interface DiaryDao{@Query("SELECT * FROM diary WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<Diary>>;@Insert suspend fun add(d:Diary)}
@Dao interface CheckDao{@Query("SELECT * FROM checks WHERE workId=:id ORDER BY id") fun all(id:Long):Flow<List<Check>>;@Insert suspend fun add(c:Check);@Update suspend fun update(c:Check)}
@Dao interface MaterialDao{@Query("SELECT * FROM materials WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<Material>>;@Insert suspend fun add(x:Material)}
@Dao interface ExpenseDao{@Query("SELECT * FROM expenses WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<Expense>>;@Insert suspend fun add(x:Expense)}
@Dao interface MeasurementDao{@Query("SELECT * FROM measurements WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<Measurement>>;@Insert suspend fun add(x:Measurement)}
@Dao interface PhotoDao{@Query("SELECT * FROM photos WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<Photo>>;@Insert suspend fun add(x:Photo)}
@Dao interface DocumentDao{@Query("SELECT * FROM documents WHERE workId=:id ORDER BY id DESC") fun all(id:Long):Flow<List<ProjectDocument>>;@Insert suspend fun add(x:ProjectDocument)}

@Database(entities=[Work::class,Task::class,Diary::class,Check::class,Material::class,Expense::class,Measurement::class,Photo::class,ProjectDocument::class],version=2,exportSchema=false)
abstract class DB:RoomDatabase(){
 abstract fun works():WorkDao;abstract fun tasks():TaskDao;abstract fun diary():DiaryDao;abstract fun checks():CheckDao
 abstract fun materials():MaterialDao;abstract fun expenses():ExpenseDao;abstract fun measurements():MeasurementDao
 abstract fun photos():PhotoDao;abstract fun documents():DocumentDao
 companion object{private var x:DB?=null;fun get(a:Application)=x?:Room.databaseBuilder(a,DB::class.java,"obra360.db").fallbackToDestructiveMigration().build().also{x=it}}
}

class VM(a:Application):AndroidViewModel(a){
 private val db=DB.get(a)
 val works=db.works().all().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 fun addWork(n:String,c:String,l:String)=viewModelScope.launch{db.works().add(Work(name=n,client=c,location=l))}
 fun tasks(id:Long)=db.tasks().all(id);fun addTask(id:Long,t:String)=viewModelScope.launch{db.tasks().add(Task(workId=id,title=t))}
 fun toggle(t:Task)=viewModelScope.launch{db.tasks().update(t.copy(done=!t.done))}
 fun diary(id:Long)=db.diary().all(id);fun addDiary(id:Long,t:String)=viewModelScope.launch{db.diary().add(Diary(workId=id,text=t))}
 fun checks(id:Long)=db.checks().all(id);fun addCheck(id:Long,t:String)=viewModelScope.launch{db.checks().add(Check(workId=id,title=t))}
 fun toggle(c:Check)=viewModelScope.launch{db.checks().update(c.copy(done=!c.done))}
 fun setProgress(w:Work,p:Int)=viewModelScope.launch{db.works().update(w.copy(progress=p.coerceIn(0,100)))}
 fun materials(id:Long)=db.materials().all(id);fun addMaterial(x:Material)=viewModelScope.launch{db.materials().add(x)}
 fun expenses(id:Long)=db.expenses().all(id);fun addExpense(x:Expense)=viewModelScope.launch{db.expenses().add(x)}
 fun measurements(id:Long)=db.measurements().all(id);fun addMeasurement(x:Measurement)=viewModelScope.launch{db.measurements().add(x)}
 fun photos(id:Long)=db.photos().all(id);fun addPhoto(x:Photo)=viewModelScope.launch{db.photos().add(x)}
 fun documents(id:Long)=db.documents().all(id);fun addDocument(x:ProjectDocument)=viewModelScope.launch{db.documents().add(x)}
}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{Theme{App()}}}}
@Composable fun Theme(content:@Composable()->Unit)=MaterialTheme(colorScheme=lightColorScheme(primary=Green,background=Paper,surface=Color.White,onSurface=Ink),content=content)

@Composable fun App(vm:VM=viewModel()){
 var tab by rememberSaveable{mutableStateOf(0)};var work by rememberSaveable{mutableStateOf<Long?>(null)};val works by vm.works.collectAsState()
 Surface(Modifier.fillMaxSize(),color=Paper){
  if(work!=null)Detail(works.firstOrNull{it.id==work},vm){work=null}
  else Scaffold(containerColor=Paper,bottomBar={NavigationBar{
   Nav("Início",Icons.Default.Home,tab==0){tab=0};Nav("Obras",Icons.Default.Construction,tab==1){tab=1};Nav("Cálculos",Icons.Default.Calculate,tab==2){tab=2};Nav("Diário",Icons.Default.Description,tab==3){tab=3}
  }}){p->Box(Modifier.padding(p)){when(tab){0->Home(works){work=it.id};1->Works(works,vm){work=it.id};2->Calc();3->DiaryScreen(works,vm)}}}}
 }
}
@Composable fun Nav(t:String,i:androidx.compose.ui.graphics.vector.ImageVector,s:Boolean,c:()->Unit)=NavigationBarItem(s,c,icon={Icon(i,null)},label={Text(t)})

@Composable fun Home(ws:List<Work>,open:(Work)->Unit){LazyColumn{item{Title("OBRA360","Gestão prática da obra, mesmo offline.")};item{Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Obras",ws.size,Modifier.weight(1f));Stat("Ativas",ws.count{it.progress<100},Modifier.weight(1f));Stat("Concluídas",ws.count{it.progress==100},Modifier.weight(1f))}};item{Head("Obras recentes")};if(ws.isEmpty())item{Empty("Ainda não há obras","Crie uma obra para começar.")};items(ws){Card(it,open)};item{Spacer(Modifier.height(80.dp))}}}
@Composable fun Title(a:String,b:String){Column(Modifier.padding(20.dp)){Text(a,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text(b,color=Muted)}}
@Composable fun Head(t:String)=Text(t,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,modifier=Modifier.padding(20.dp,22.dp,20.dp,10.dp))
@Composable fun Stat(t:String,n:Int,modifier:Modifier){Surface(modifier,shape=RoundedCornerShape(18.dp),color=Color.White){Column(Modifier.padding(13.dp)){Text(n.toString(),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(t,color=Muted)}}}
@Composable fun Empty(a:String,b:String){Column(Modifier.fillMaxWidth().padding(40.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Construction,null,tint=Muted,modifier=Modifier.size(40.dp));Text(a,fontWeight=FontWeight.Bold);Text(b,color=Muted)}}
@Composable fun Card(w:Work,open:(Work)->Unit){Surface(Modifier.padding(8.dp,5.dp).fillMaxWidth().clickable{open(w)},shape=RoundedCornerShape(20.dp),color=Color.White){Column(Modifier.padding(17.dp)){Row{Column(Modifier.weight(1f)){Text(w.name,fontWeight=FontWeight.Bold);Text(w.client,color=Muted)};Text(w.progress.toString()+"%",color=Green,fontWeight=FontWeight.Bold)};LinearProgressIndicator(progress={w.progress/100f},Modifier.fillMaxWidth().padding(top=12.dp));Text(w.location,color=Muted,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=8.dp))}}}

@Composable fun Works(ws:List<Work>,vm:VM,open:(Work)->Unit){var show by remember{mutableStateOf(false)};LazyColumn{item{Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically){Text("Obras",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));FilledIconButton(onClick={show=true}){Icon(Icons.Default.Add,null)}}};items(ws){Card(it,open)};item{Spacer(Modifier.height(80.dp))}};if(show)WorkDialog({show=false;vm.addWork(it[0],it[1],it[2])}){show=false}}
@Composable fun WorkDialog(save:(List<String>)->Unit,cancel:()->Unit){var n by remember{mutableStateOf("")};var c by remember{mutableStateOf("")};var l by remember{mutableStateOf("")};AlertDialog(onDismissRequest=cancel,title={Text("Nova obra")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(n,{n=it},label={Text("Nome")});OutlinedTextField(c,{c=it},label={Text("Cliente")});OutlinedTextField(l,{l=it},label={Text("Local")})}},confirmButton={Button(enabled=n.isNotBlank(),onClick={save(listOf(n,c,l))}){Text("Criar")}},dismissButton={TextButton(onClick=cancel){Text("Cancelar")}})}

@Composable fun Detail(w:Work?,vm:VM,back:()->Unit){
 if(w==null){back();return}
 var page by rememberSaveable{mutableStateOf(0)};var dialog by remember{mutableStateOf("")}
 val ts by vm.tasks(w.id).collectAsState(initial=emptyList());val ds by vm.diary(w.id).collectAsState(initial=emptyList());val cs by vm.checks(w.id).collectAsState(initial=emptyList())
 val ms by vm.materials(w.id).collectAsState(initial=emptyList());val es by vm.expenses(w.id).collectAsState(initial=emptyList());val mets by vm.measurements(w.id).collectAsState(initial=emptyList())
 val ps by vm.photos(w.id).collectAsState(initial=emptyList());val docs by vm.documents(w.id).collectAsState(initial=emptyList())
 val ctx=LocalContext.current
 val pdfLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->if(uri!=null)writeReport(ctx,uri,w,ts,ds,ms,es,mets)}
 val photoLauncher=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->if(uri!=null){try{ctx.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};vm.addPhoto(Photo(workId=w.id,uri=uri.toString()))}}
 val docLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){try{ctx.contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};vm.addDocument(ProjectDocument(workId=w.id,uri=uri.toString(),name=displayName(ctx.contentResolver,uri))) }}
 Column{
  Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null)};Column(Modifier.weight(1f)){Text(w.name,fontWeight=FontWeight.Bold);Text(w.client,color=Muted)};IconButton(onClick={pdfLauncher.launch(w.name+"-relatorio.pdf")}){Icon(Icons.Default.PictureAsPdf,null)}}
  ScrollableTabRow(selectedTabIndex=page){listOf("Resumo","Tarefas","Diário","Checklist","Materiais","Custos","Medições","Fotos","Docs").forEachIndexed{i,s->Tab(selected=page==i,onClick={page=i},text={Text(s)})}}
  when(page){
   0->Summary(w,vm);1->TaskPage(ts,{dialog="task"}){i->vm.toggle(ts[i])};2->ListPage("Diário",ds.map{date(it.time)+" — "+it.text}){dialog="diary"};3->CheckPage(cs,{dialog="check"}){i->vm.toggle(cs[i])}
   4->MaterialsPage(ms,{dialog="material"});5->CostsPage(ms,es,{dialog="expense"});6->MeasurementsPage(mets,{dialog="measurement"})
   7->PhotosPage(ps){photoLauncher.launch(ActivityResultContracts.PickVisualMedia.ImageOnly)};8->DocumentsPage(docs){docLauncher.launch(arrayOf("*/*"))}
  }
 }
 if(dialog.isNotBlank())EntryDialog(dialog,{v->
  when(dialog){"task"->vm.addTask(w.id,v[0]);"diary"->vm.addDiary(w.id,v[0]);"check"->vm.addCheck(w.id,v[0]);"material"->vm.addMaterial(Material(workId=w.id,name=v[0],unit=v[1],quantity=v[2].d(),unitCost=v[3].d()));"expense"->vm.addExpense(Expense(workId=w.id,description=v[0],category=v[1],amount=v[2].d()));"measurement"->vm.addMeasurement(Measurement(workId=w.id,description=v[0],unit=v[1],quantity=v[2].d(),unitPrice=v[3].d()))};dialog=""},{dialog=""})
 }
}

@Composable fun Summary(w:Work,vm:VM){var slider by remember(w.id,w.progress){mutableFloatStateOf(w.progress.toFloat())};LazyColumn{item{Column(Modifier.padding(20.dp)){Text("Progresso",fontWeight=FontWeight.Bold);Text(slider.toInt().toString()+"%",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);Slider(value=slider,onValueChange={slider=it},onValueChangeFinished={vm.setProgress(w,slider.toInt())},valueRange=0f..100f,steps=99);Text("Local",fontWeight=FontWeight.Bold);Text(w.location.ifBlank{"Não informado"},color=Muted);Text("Cliente",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp));Text(w.client.ifBlank{"Não informado"},color=Muted)}};item{Head("Etapas sugeridas");listOf("Fundação","Estrutura","Alvenaria","Instalações","Revestimentos","Acabamentos").forEach{ListItem(headlineContent={Text(it)},leadingContent={Icon(Icons.Default.Construction,null,tint=Green)})}};item{Spacer(Modifier.height(80.dp))}}}

@Composable fun ListPage(title:String,rows:List<String>,add:()->Unit){LazyColumn{item{Row(Modifier.padding(20.dp)){Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};items(rows){ListItem(headlineContent={Text(it)})};item{Spacer(Modifier.height(80.dp))}}}
@Composable fun TaskPage(rows:List<Task>,add:()->Unit,toggle:(Int)->Unit){LazyColumn{item{Row(Modifier.padding(20.dp)){Text("Tarefas",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};items(rows.size){i->ListItem(headlineContent={Text(rows[i].title)},supportingContent={Text(rows[i].stage)},leadingContent={Checkbox(checked=rows[i].done,onCheckedChange={toggle(i)})})};item{Spacer(Modifier.height(80.dp))}}}
@Composable fun CheckPage(rows:List<Check>,add:()->Unit,toggle:(Int)->Unit){LazyColumn{item{Row(Modifier.padding(20.dp)){Text("Checklist",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};items(rows.size){i->ListItem(headlineContent={Text(rows[i].title)},leadingContent={Checkbox(checked=rows[i].done,onCheckedChange={toggle(i)})})};item{Spacer(Modifier.height(80.dp))}}}

@Composable fun MaterialsPage(rows:List<Material>,add:()->Unit){val total=rows.sumOf{it.quantity*it.unitCost};LazyColumn{item{Row(Modifier.padding(20.dp)){Column(Modifier.weight(1f)){Text("Materiais",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Total estimado: "+money(total),color=Green,fontWeight=FontWeight.Bold)};IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};items(rows){x->ListItem(headlineContent={Text(x.name)},supportingContent={Text("\${x.quantity} \${x.unit} × \${money(x.unitCost)}")},trailingContent={Text(money(x.quantity*x.unitCost),fontWeight=FontWeight.Bold)})};item{Spacer(Modifier.height(80.dp))}}}
@Composable fun CostsPage(ms:List<Material>,es:List<Expense>,add:()->Unit){val materials=ms.sumOf{it.quantity*it.unitCost};val expenses=es.sumOf{it.amount};val total=materials+expenses;LazyColumn{item{Row(Modifier.padding(20.dp)){Column(Modifier.weight(1f)){Text("Custos",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Total: "+money(total),color=Green,fontWeight=FontWeight.Bold)};IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};item{ListItem(headlineContent={Text("Materiais")},trailingContent={Text(money(materials))});ListItem(headlineContent={Text("Despesas")},trailingContent={Text(money(expenses))})};items(es){x->ListItem(headlineContent={Text(x.description)},supportingContent={Text(x.category+" • "+date(x.date))},trailingContent={Text(money(x.amount),fontWeight=FontWeight.Bold)})};item{Spacer(Modifier.height(80.dp))}}}
@Composable fun MeasurementsPage(rows:List<Measurement>,add:()->Unit){val total=rows.sumOf{it.quantity*it.unitPrice};LazyColumn{item{Row(Modifier.padding(20.dp)){Column(Modifier.weight(1f)){Text("Medições",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Valor estimado: "+money(total),color=Green,fontWeight=FontWeight.Bold)};IconButton(onClick=add){Icon(Icons.Default.Add,null)}}};items(rows){x->ListItem(headlineContent={Text(x.description)},supportingContent={Text("\${x.quantity} \${x.unit} × \${money(x.unitPrice)}")},trailingContent={Text(money(x.quantity*x.unitPrice),fontWeight=FontWeight.Bold)})};item{Spacer(Modifier.height(80.dp))}}}

@Composable fun PhotosPage(rows:List<Photo>,pick:()->Unit){Column{Row(Modifier.padding(20.dp)){Text("Fotos",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=pick){Icon(Icons.Default.AddAPhoto,null)}};if(rows.isEmpty())Empty("Sem fotos","Adicione fotos do andamento da obra.");else LazyVerticalGrid(columns=GridCells.Fixed(2),contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(rows){AsyncImage(model=it.uri,contentDescription=it.caption,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxWidth().height(150.dp))}}}}
@Composable fun DocumentsPage(rows:List<ProjectDocument>,pick:()->Unit){LazyColumn{item{Row(Modifier.padding(20.dp)){Text("Documentos",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=pick){Icon(Icons.Default.AttachFile,null)}}};if(rows.isEmpty())item{Empty("Sem documentos","Anexe plantas, orçamentos, contratos ou outros ficheiros.")};items(rows){ListItem(headlineContent={Text(it.name)},supportingContent={Text(date(it.time))},leadingContent={Icon(Icons.Default.Description,null,tint=Green)})};item{Spacer(Modifier.height(80.dp))}}}

@Composable fun EntryDialog(kind:String,save:(List<String>)->Unit,cancel:()->Unit){
 var a by remember{mutableStateOf("")};var b by remember{mutableStateOf("")};var c by remember{mutableStateOf("")};var d by remember{mutableStateOf("")}
 val title=when(kind){"task"->"Nova tarefa";"diary"->"Registo do diário";"check"->"Novo checklist";"material"->"Novo material";"expense"->"Nova despesa";else->"Nova medição"}
 val fields=when(kind){"task","diary","check"->1;"material","measurement"->4;else->3}
 AlertDialog(onDismissRequest=cancel,title={Text(title)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
  OutlinedTextField(a,{a=it},label={Text(if(kind=="material")"Material" else if(kind=="expense")"Descrição" else if(kind=="measurement")"Medição" else "Descrição")},modifier=Modifier.fillMaxWidth(),minLines=if(kind=="diary")3 else 1)
  if(fields>=3)OutlinedTextField(b,{b=it},label={Text(if(kind=="material"||kind=="measurement")"Unidade" else "Categoria")},modifier=Modifier.fillMaxWidth())
  if(fields>=3)OutlinedTextField(c,{c=it},label={Text(if(kind=="material"||kind=="measurement")"Quantidade" else "Valor")},modifier=Modifier.fillMaxWidth())
  if(fields>=4)OutlinedTextField(d,{d=it},label={Text("Preço unitário")},modifier=Modifier.fillMaxWidth())
 }},confirmButton={Button(enabled=a.isNotBlank()&&if(fields>=3)c.isNotBlank() else true,onClick={save(listOf(a,b,c,d))}){Text("Guardar")}},dismissButton={TextButton(onClick=cancel){Text("Cancelar")}})
}

@Composable fun Calc(){
 var type by rememberSaveable{mutableStateOf(0)};var a by remember{mutableStateOf("")};var b by remember{mutableStateOf("")};var c by remember{mutableStateOf("")}
 val r=when(type){0->a.d()*b.d()*c.d();1->a.d()*(b.d()/100);2->ceil(a.d()*b.d());else->if(b.d()>0)ceil(a.d()/b.d()) else 0.0}
 LazyColumn(Modifier.padding(20.dp)){item{Title("Calculadoras","Estimativas rápidas para o canteiro.")};item{SingleChoiceSegmentedButtonRow{listOf("Concreto","Argamassa","Blocos","Pintura").forEachIndexed{i,s->SegmentedButton(selected=type==i,onClick={type=i},shape=SegmentedButtonDefaults.itemShape(i,4)){Text(s)}}}};item{Spacer(Modifier.height(15.dp));OutlinedTextField(a,{a=it},label={Text(if(type==2)"Área (m²)" else "Medida")},modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(8.dp));OutlinedTextField(b,{b=it},label={Text(if(type==0)"Largura" else if(type==1)"Espessura (cm)" else if(type==2)"Peças/m²" else "Rendimento m²")},modifier=Modifier.fillMaxWidth());if(type==0){Spacer(Modifier.height(8.dp));OutlinedTextField(c,{c=it},label={Text("Altura")},modifier=Modifier.fillMaxWidth())};Spacer(Modifier.height(18.dp))};item{Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=Green){Column(Modifier.padding(22.dp)){Text("Resultado",color=Color.White);Text("%.2f".format(r),style=MaterialTheme.typography.headlineMedium,color=Color.White,fontWeight=FontWeight.Bold)}}};item{Text("Use como estimativa e confirme traço, perdas e especificações do projeto.",color=Muted,modifier=Modifier.padding(top=16.dp));Spacer(Modifier.height(80.dp))}}}

fun String.d()=replace(",","." ).toDoubleOrNull()?:0.0
fun money(v:Double)="%.2f".format(Locale.US,v)+" Kz"
fun date(t:Long)=SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(t))

@Composable fun DiaryScreen(ws:List<Work>,vm:VM){
 var id by rememberSaveable{mutableStateOf<Long?>(null)};LaunchedEffect(ws){if(id==null)id=ws.firstOrNull()?.id};var show by remember{mutableStateOf(false)}
 val ds=id?.let{vm.diary(it)}?.collectAsState(initial=emptyList())?.value?:emptyList()
 LazyColumn{item{Row(Modifier.padding(20.dp)){Text("Diário",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(enabled=id!=null,onClick={show=true}){Icon(Icons.Default.Add,null)}}};if(ws.isEmpty())item{Empty("Sem obras","Crie uma obra para começar o diário.")};items(ds){ListItem(headlineContent={Text(it.text)},supportingContent={Text(date(it.time))})};item{Spacer(Modifier.height(80.dp))}}
 if(show&&id!=null)EntryDialog("diary",{show=false;vm.addDiary(id!!,it[0])},{show=false})
}

fun displayName(resolver:ContentResolver,uri:Uri):String{var name="Documento";resolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())name=it.getString(0)};return name}

fun writeReport(context:Context,uri:Uri,w:Work,tasks:List<Task>,diary:List<Diary>,materials:List<Material>,expenses:List<Expense>,measurements:List<Measurement>){
 val doc=PdfDocument();var pageNo=1;var page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNo).create());val paint=Paint().apply{isAntiAlias=true;textSize=12f};var y=45f
 fun line(s:String,bold:Boolean=false){paint.textSize=if(bold)18f else 12f;paint.typeface=if(bold)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;page.canvas.drawText(s.take(82),40f,y,paint);y+=22f;if(y>800){doc.finishPage(page);pageNo++;page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNo).create());y=45f}}
 line("OBRA360 — Relatório da obra",true);line(w.name,true);line("Cliente: "+w.client);line("Local: "+w.location);line("Progresso: "+w.progress+"%");line("")
 line("Tarefas",true);tasks.forEach{line("["+(if(it.done)"x" else " ")+"] "+it.title)}
 line("Diário",true);diary.forEach{line(date(it.time)+" — "+it.text)}
 line("Materiais",true);materials.forEach{line(it.name+": "+it.quantity+" "+it.unit+" × "+money(it.unitCost)+" = "+money(it.quantity*it.unitCost))}
 line("Despesas",true);expenses.forEach{line(it.description+": "+money(it.amount))}
 line("Medições",true);measurements.forEach{line(it.description+": "+it.quantity+" "+it.unit+" × "+money(it.unitPrice)+" = "+money(it.quantity*it.unitPrice))}
 doc.finishPage(page);context.contentResolver.openOutputStream(uri)?.use{doc.writeTo(it)};doc.close()
}
