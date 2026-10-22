@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.restroomqueue

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId

// ---------- helpers ----------
private val palette = listOf(Blue, Cyan, Mint)
private val pill = RoundedCornerShape(50)
private val bg = Brush.verticalGradient(listOf(Navy, Slate))
private val XBold = FontWeight.ExtraBold

private fun norm(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()
private fun initials(n: String) = n.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
private fun colorFor(n: String) = palette[(n.hashCode() and 0x7fffffff) % palette.size]
@Composable private fun statusLabel(s: Student) = when {
    s.outSince != null -> t("🚶 Fora", "🚶 Out")
    s.queuedAt != null -> t("⏳ Na fila", "⏳ In line")
    else -> t("😊 Disponível", "😊 Available")
}

@Composable private fun barColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent, titleContentColor = Mint, actionIconContentColor = Mint, navigationIconContentColor = Mint
)

@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Mint, unfocusedBorderColor = Cyan.copy(alpha = .5f),
    focusedContainerColor = Slate, unfocusedContainerColor = Slate
)

@Composable private fun mintButton() = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Navy)

// ---------- root ----------
@Composable fun App(vm: VM = viewModel()) {
    val user by vm.user.collectAsState()
    val lang by vm.lang.collectAsState()
    var open by remember { mutableStateOf<ClassRoom?>(null) }
    val c = open
    CompositionLocalProvider(LocalLang provides lang, LocalContentColor provides Color.White) {
        Box(Modifier.fillMaxSize().background(bg)) {
            when {
                user == null -> AuthScreen(vm)
                c == null -> ClassList(vm) { open = it }
                else -> ClassScreen(vm, c) { open = null }
            }
        }
    }
}

@Composable fun Avatar(name: String, size: Dp = 46.dp) {
    val c = colorFor(name)
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(c, c.copy(alpha = .55f)))),
        contentAlignment = Alignment.Center
    ) { Text(initials(name), color = Navy, fontWeight = XBold, fontSize = (size.value * .36f).sp) }
}

@Composable fun Header(text: String) =
    Text(text, color = Cyan, style = MaterialTheme.typography.titleMedium, fontWeight = XBold, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))

@Composable fun EmptyHint(emoji: String, title: String, sub: String) =
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 56.sp)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = XBold, textAlign = TextAlign.Center)
        Text(sub, color = Color.White.copy(alpha = .7f), textAlign = TextAlign.Center)
    }

// ---------- classes ----------
@Composable fun ClassCard(c: ClassRoom, i: Int, modifier: Modifier, onOpen: () -> Unit, onDelete: () -> Unit) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(palette[i % 3], palette[(i + 1) % 3])))
            .clickable(onClick = onOpen).padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🚻", fontSize = 40.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.headlineMedium, fontWeight = XBold, color = Navy)
                Text(t("Até ${c.maxOut} fora ao mesmo tempo · toque para abrir", "Up to ${c.maxOut} out at once · tap to open"), color = Navy.copy(alpha = .75f))
            }
            IconButton(onDelete) { Icon(Icons.Default.Delete, t("Excluir", "Delete"), tint = Navy) }
        }
    }
}

@Composable fun ClassList(vm: VM, onOpen: (ClassRoom) -> Unit) {
    val classes by vm.classes.collectAsState()
    var dialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var account by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<ClassRoom?>(null) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t("Fila do Banheiro 🚽", "Restroom Queue 🚽"), fontWeight = XBold) },
                actions = {
                    LanguageButton(vm)
                    IconButton({ vm.message.value = null; account = true }) { Icon(Icons.Default.AccountCircle, t("Conta", "Account")) }
                },
                colors = barColors()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text(t("Nova turma", "New class")) },
                containerColor = Mint, contentColor = Navy, shape = pill
            )
        }
    ) { pad ->
        if (classes.isEmpty()) Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyHint("🚽", t("Nenhuma turma ainda", "No classes yet"), t("Toque em “Nova turma” para criar a primeira", "Tap “New class” to create your first one"))
        } else LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(classes, key = { _, c -> c.id }) { i, c -> ClassCard(c, i, Modifier.animateItem(), { onOpen(c) }) { toDelete = c } }
        }
    }
    if (account) AccountDialog(vm) { account = false }
    if (dialog) AlertDialog(
        onDismissRequest = { dialog = false },
        title = { Text(t("Nova turma ✨", "New class ✨")) },
        text = { OutlinedTextField(name, { name = it }, label = { Text(t("Nome da turma", "Class name")) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors()) },
        confirmButton = { TextButton({ vm.addClass(name); name = ""; dialog = false }) { Text(t("Criar", "Create")) } },
        dismissButton = { TextButton({ dialog = false }) { Text(t("Cancelar", "Cancel")) } }
    )
    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(t("Excluir a turma ${c.name}?", "Delete class ${c.name}?")) },
            text = { Text(t("Todos os alunos dela também serão removidos. As contagens anônimas de idas ao banheiro continuam nas estatísticas.", "All its students will be removed too. Anonymous trip counts stay in the stats.")) },
            confirmButton = { TextButton({ vm.deleteClass(c); toDelete = null }) { Text(t("Excluir", "Delete"), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ toDelete = null }) { Text(t("Cancelar", "Cancel")) } }
        )
    }
}

// ---------- line page ----------
@Composable fun StudentMenu(onRename: () -> Unit, onDelete: () -> Unit, onLeaveLine: (() -> Unit)? = null) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }) { Icon(Icons.Default.MoreVert, t("Opções", "Options")) }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text(t("Renomear", "Rename")) }, onClick = { open = false; onRename() })
            onLeaveLine?.let { f -> DropdownMenuItem(text = { Text(t("Tirar da fila", "Remove from line")) }, onClick = { open = false; f() }) }
            DropdownMenuItem(text = { Text(t("Excluir", "Delete")) }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable fun StudentCard(
    s: Student, sub: String?, modifier: Modifier = Modifier, onRename: () -> Unit, onDelete: () -> Unit,
    onLeaveLine: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(s.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (sub != null) Text(sub, color = Cyan, style = MaterialTheme.typography.bodySmall)
            }
            actions()
            StudentMenu(onRename, onDelete, onLeaveLine)
        }
    }
}

@Composable fun OutCard(s: Student, now: Long, modifier: Modifier, onBack: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val sec = ((now - s.outSince!!) / 1000).coerceAtLeast(0)
    val late = sec > 600
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = .25f, targetValue = .6f, animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alpha"
    )
    val accent = if (late) MaterialTheme.colorScheme.error else Mint
    Card(
        modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (late) MaterialTheme.colorScheme.error.copy(alpha = pulse * .6f) else Slate)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (sec / 600f).coerceAtMost(1f) }, modifier = Modifier.size(64.dp),
                    color = accent, trackColor = Color.White.copy(alpha = .15f), strokeWidth = 6.dp
                )
                Text("%d:%02d".format(sec / 60, sec % 60), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(s.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(if (late) t("⚠️ Mais de 10 minutos!", "⚠️ Over 10 minutes!") else t("🚶 Fora da sala", "🚶 Out of the room"), color = accent, style = MaterialTheme.typography.bodySmall)
            }
            Button(onBack, shape = pill) { Text(t("Voltou", "Back")) }
            StudentMenu(onRename, onDelete)
        }
    }
}

@Composable fun LimitStepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(pill).background(Slate).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(t("🚦 Máx. fora ao mesmo tempo", "🚦 Max out at once"), Modifier.padding(start = 12.dp).weight(1f), color = Color.White, fontWeight = FontWeight.SemiBold)
        IconButton({ onChange(value - 1) }, enabled = value > 1) { Text("−", fontSize = 26.sp, color = Cyan) }
        Text("$value", fontSize = 22.sp, fontWeight = XBold, color = Mint)
        IconButton({ onChange(value + 1) }, enabled = value < 20) { Text("+", fontSize = 26.sp, color = Cyan) }
    }
}

@Composable fun ClassScreen(vm: VM, c: ClassRoom, onBack: () -> Unit) {
    val students by remember(c.id) { vm.students(c.id) }.collectAsState(emptyList())
    val trips by remember(c.id) { vm.tripTimes(c.id) }.collectAsState(emptyList())
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(1000); value = System.currentTimeMillis() } }
    val allClasses by vm.classes.collectAsState()
    val maxOut = allClasses.firstOrNull { it.id == c.id }?.maxOut ?: c.maxOut
    val focus = LocalFocusManager.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var names by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Student?>(null) }
    var deleting by remember { mutableStateOf<Student?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.readPdf(it) { list -> names = list.joinToString("\n") } }
    }

    val q = norm(query.trim())
    val allOut = students.filter { it.outSince != null }
    val allLine = students.filter { it.queuedAt != null }.sortedBy { it.queuedAt }
    val out = allOut.filter { q.isEmpty() || norm(it.name).contains(q) }
    val line = allLine.filter { q.isEmpty() || norm(it.name).contains(q) }
    val free = students.filter { it.queuedAt == null && it.outSince == null && (q.isEmpty() || norm(it.name).contains(q)) }
    // live suggestions while typing: names starting with the text first, then names containing it
    val suggestions = if (focused && q.isNotEmpty())
        students.filter { norm(it.name).contains(q) && norm(it.name) != q }
            .sortedBy { if (norm(it.name).startsWith(q)) 0 else 1 }.take(5)
    else emptyList()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(t("Turma ${c.name}", "Class ${c.name}"), fontWeight = XBold)
                        Text(t("${allOut.size} fora · ${allLine.size} aguardando", "${allOut.size} out · ${allLine.size} waiting"), style = MaterialTheme.typography.bodySmall, color = Cyan)
                    }
                },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("Voltar", "Back")) } },
                colors = barColors()
            )
        },
        bottomBar = {
            NavigationBar {
                val nav = NavigationBarItemDefaults.colors(selectedTextColor = Mint, unselectedTextColor = Color.White.copy(alpha = .7f), indicatorColor = Blue)
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Text("🚽", fontSize = 22.sp) }, label = { Text(t("Fila", "Line")) }, colors = nav)
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Text("📊", fontSize = 22.sp) }, label = { Text(t("Estatísticas", "Insights")) }, colors = nav)
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            if (tab == 1) Insights(trips) else {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).onFocusChanged { focused = it.isFocused },
                    placeholder = { Text(t("Buscar alunos…", "Search students…")) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Default.Close, t("Limpar", "Clear")) } },
                    singleLine = true, shape = pill, colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() })
                )
                AnimatedVisibility(suggestions.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp)) {
                        suggestions.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().clickable { query = s.name; focus.clearFocus() }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(s.name, 32.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(s.name, Modifier.weight(1f))
                                Text(statusLabel(s), color = Cyan, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                LazyColumn(
                    Modifier.padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button({ picker.launch(arrayOf("application/pdf")) }, shape = pill) { Text(t("📄 Importar PDF", "📄 Import PDF")) }
                            OutlinedButton({ names = "" }, shape = pill) { Text(t("✏️ Adicionar nomes", "✏️ Add names")) }
                        }
                    }
                    item { LimitStepper(maxOut) { vm.setMaxOut(c.id, it) } }
                    if (allOut.size >= maxOut) item {
                        Card(colors = CardDefaults.cardColors(containerColor = Cyan.copy(alpha = .18f)), shape = RoundedCornerShape(18.dp)) {
                            Text(t("🚦 Limite atingido – espere alguém voltar", "🚦 Limit reached – wait for someone to come back"), Modifier.padding(14.dp))
                        }
                    }
                    if (students.isEmpty()) item { EmptyHint("👋", t("Nenhum aluno ainda", "No students yet"), t("Importe um PDF ou toque em “Adicionar nomes” para começar", "Import a PDF or tap “Add names” to get started")) }
                    else if (q.isNotEmpty() && out.isEmpty() && line.isEmpty() && free.isEmpty())
                        item { EmptyHint("🔍", t("Ninguém encontrado para “$query”", "No one matches “$query”"), t("Confira a ortografia ou limpe a busca", "Check the spelling or clear the search")) }

                    if (out.isNotEmpty()) {
                        item { Header(t("🚶  Fora agora (${allOut.size}/$maxOut)", "🚶  Out now (${allOut.size}/$maxOut)")) }
                        items(out, key = { it.id }) { s ->
                            OutCard(s, now, Modifier.animateItem(), { vm.back(s) }, { editing = s }, { deleting = s })
                        }
                    }
                    if (line.isNotEmpty()) {
                        item { Header(t("⏳  Na fila (${allLine.size})", "⏳  In line (${allLine.size})")) }
                        items(line, key = { it.id }) { s ->
                            StudentCard(
                                s, t("Posição ${allLine.indexOf(s) + 1}", "Position ${allLine.indexOf(s) + 1}"), Modifier.animateItem(),
                                onRename = { editing = s }, onDelete = { deleting = s }, onLeaveLine = { vm.leaveLine(s) }
                            ) {
                                Button({ vm.sendOut(s) }, enabled = allOut.size < maxOut, shape = pill, colors = mintButton()) { Text(t("Liberar", "Send out")) }
                            }
                        }
                    }
                    if (free.isNotEmpty()) {
                        item { Header(t("😊  Disponíveis", "😊  Available")) }
                        items(free, key = { it.id }) { s ->
                            StudentCard(s, null, Modifier.animateItem(), onRename = { editing = s }, onDelete = { deleting = s }) {
                                Button({ vm.enqueue(s) }, shape = pill) { Text(t("Entrar na fila", "Join line")) }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { st ->
        var newName by remember(st.id) { mutableStateOf(st.name) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(t("Editar aluno ✏️", "Edit student ✏️")) },
            text = { OutlinedTextField(newName, { newName = it }, singleLine = true, label = { Text(t("Nome", "Name")) }, shape = RoundedCornerShape(16.dp), colors = fieldColors()) },
            confirmButton = { TextButton({ vm.renameStudent(st, newName); editing = null }) { Text(t("Salvar", "Save")) } },
            dismissButton = { TextButton({ editing = null }) { Text(t("Cancelar", "Cancel")) } }
        )
    }
    deleting?.let { st ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(t("Excluir ${st.name}?", "Delete ${st.name}?")) },
            text = { Text(t("O aluno será removido da turma. As contagens anteriores nas Estatísticas continuam (elas são anônimas).", "They will be removed from this class. Past trip counts in Insights are kept (they are anonymous).")) },
            confirmButton = { TextButton({ vm.deleteStudent(st); deleting = null }) { Text(t("Excluir", "Delete"), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ deleting = null }) { Text(t("Cancelar", "Cancel")) } }
        )
    }
    names?.let { text ->
        AlertDialog(
            onDismissRequest = { names = null },
            title = { Text(t("Revisar nomes 📝", "Review names 📝")) },
            text = { OutlinedTextField(text, { names = it }, label = { Text(t("Um nome por linha", "One name per line")) }, modifier = Modifier.height(300.dp), shape = RoundedCornerShape(16.dp), colors = fieldColors()) },
            confirmButton = { TextButton({ vm.addStudents(c.id, text.lines()); names = null }) { Text(t("Salvar", "Save")) } },
            dismissButton = { TextButton({ names = null }) { Text(t("Cancelar", "Cancel")) } }
        )
    }
}

// ---------- insights ----------
@Composable fun Bar(day: String, n: Int, max: Int, top: Boolean) {
    val h by animateDpAsState((170f * n / max).dp + 4.dp, label = "bar")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        if (top) Text("🔥", fontSize = 16.sp)
        Text("$n", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.width(30.dp).height(h).clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(Brush.verticalGradient(if (top) listOf(Mint, Cyan) else listOf(Cyan, Blue)))
        )
        Spacer(Modifier.height(4.dp))
        Text(day, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun Insights(times: List<Long>) {
    val pt = LocalLang.current == Lang.PT
    val days = if (pt) listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom") else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val full = if (pt) listOf("Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira", "Sexta-feira", "Sábado", "Domingo")
               else listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val counts = IntArray(7)
    times.forEach { counts[Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).dayOfWeek.value - 1]++ }
    val max = counts.max().coerceAtLeast(1)
    val best = counts.indexOf(counts.max())
    Column(Modifier.padding(20.dp)) {
        Text(t("Movimento do banheiro 📊", "Restroom traffic 📊"), style = MaterialTheme.typography.headlineSmall, fontWeight = XBold, color = Mint)
        Text(t("${times.size} ${if (times.size == 1) "ida registrada" else "idas registradas"} nesta turma", "${times.size} ${if (times.size == 1) "trip" else "trips"} recorded for this class"), color = Color.White.copy(alpha = .7f))
        Spacer(Modifier.height(16.dp))
        if (times.isNotEmpty()) Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Blue, Cyan))).padding(18.dp)
        ) {
            Column {
                Text(t("🔥 Dia mais movimentado", "🔥 Busiest day"), color = Navy)
                Text(t("${full[best]} · ${counts[best]} ${if (counts[best] == 1) "ida" else "idas"}", "${full[best]} · ${counts[best]} ${if (counts[best] == 1) "trip" else "trips"}"), style = MaterialTheme.typography.headlineSmall, fontWeight = XBold, color = Navy)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            counts.forEachIndexed { i, n -> Bar(days[i], n, max, times.isNotEmpty() && i == best) }
        }
        Spacer(Modifier.height(16.dp))
        Text(t("As contagens são por turma. Nenhum dado individual é guardado. 🔒", "Counts are per class. No individual data is stored. 🔒"), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .6f))
    }
}

// ---------- auth + account ----------
@Composable fun AuthScreen(vm: VM) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(false) }
    val busy by vm.busy.collectAsState()
    val msg by vm.message.collectAsState()
    Box(Modifier.fillMaxSize().statusBarsPadding()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Blue, Mint))), contentAlignment = Alignment.Center) {
            Text("🚽", fontSize = 48.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(t("Fila do Banheiro", "Restroom Queue"), style = MaterialTheme.typography.headlineLarge, fontWeight = XBold, color = Mint)
        Text(t("Filas curtas. Turmas felizes. ✨", "Short lines. Happy classes. ✨"), color = Color.White.copy(alpha = .75f))
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            email, { email = it }, label = { Text(t("E-mail", "Email")) }, singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            pass, { pass = it }, label = { Text(t("Senha (mín. 6 caracteres)", "Password (6+ characters)")) }, singleLine = true, shape = RoundedCornerShape(18.dp), colors = fieldColors(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
        )
        msg?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(18.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint)
        Button(
            { vm.authenticate(email, pass, create) }, enabled = !busy && email.isNotBlank() && pass.length >= 6,
            shape = pill, colors = mintButton(), modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (create) t("Cadastrar", "Sign up") else t("Entrar", "Sign in"), fontWeight = XBold) }
        TextButton({ create = !create; vm.message.value = null }) {
            Text(if (create) t("Já tenho uma conta", "I already have an account") else t("Criar uma conta", "Create an account"), color = Cyan)
        }
    }
    LanguageButton(vm, Modifier.align(Alignment.TopEnd))
    }
}

@Composable fun AccountDialog(vm: VM, onClose: () -> Unit) {
    val user by vm.user.collectAsState()
    val busy by vm.busy.collectAsState()
    val msg by vm.message.collectAsState()
    var confirmRestore by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onClose() },
        title = { Text(t("👤 Conta", "👤 Account")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(user?.email ?: "", color = Cyan)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Mint)
                msg?.let { Text(it) }
                Button({ vm.backup() }, enabled = !busy, shape = pill, colors = mintButton(), modifier = Modifier.fillMaxWidth()) { Text(t("☁️ Fazer backup agora", "☁️ Back up now")) }
                OutlinedButton({ confirmRestore = true }, enabled = !busy, shape = pill, modifier = Modifier.fillMaxWidth()) { Text(t("Restaurar da nuvem", "Restore from cloud")) }
                OutlinedButton({ vm.signOut() }, enabled = !busy, shape = pill, modifier = Modifier.fillMaxWidth()) { Text(t("Sair", "Sign out")) }
            }
        },
        confirmButton = { TextButton(onClose, enabled = !busy) { Text(t("Fechar", "Close")) } }
    )
    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text(t("Restaurar da nuvem?", "Restore from cloud?")) },
        text = { Text(t("Isso substitui tudo neste aparelho pelo seu último backup.", "This replaces everything on this device with your last backup.")) },
        confirmButton = { TextButton({ vm.restore(); confirmRestore = false }) { Text(t("Restaurar", "Restore")) } },
        dismissButton = { TextButton({ confirmRestore = false }) { Text(t("Cancelar", "Cancel")) } }
    )
}

// ---------- language switch ----------
@Composable fun LanguageButton(vm: VM, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val lang by vm.lang.collectAsState()
    IconButton({ open = true }, modifier) { Text("🌐", fontSize = 22.sp) }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("🌐 " + t("Idioma", "Language")) },
        text = {
            Column {
                listOf(Lang.PT to "🇧🇷 Português (Brasil)", Lang.EN to "🇺🇸 English").forEach { (l, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.setLang(l); open = false }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = lang == l, onClick = { vm.setLang(l); open = false }, colors = RadioButtonDefaults.colors(selectedColor = Mint))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton({ open = false }) { Text(t("Fechar", "Close")) } }
    )
}
