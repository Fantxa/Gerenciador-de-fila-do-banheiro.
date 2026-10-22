package com.example.restroomqueue

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

class VM(private val app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    val classes = dao.classes().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    fun students(id: Long) = dao.students(id)
    fun tripTimes(id: Long) = dao.tripTimes(id)

    // ---------- Auth + cloud ----------
    private val auth = FirebaseAuth.getInstance()
    private val cloud = Cloud(AppDb.get(app))
    val user = MutableStateFlow(auth.currentUser)
    val busy = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    // ---------- language (default: Brazilian Portuguese) ----------
    val lang = MutableStateFlow(Settings.lang(app))
    fun setLang(l: Lang) { Settings.setLang(app, l); lang.value = l }
    private fun t(pt: String, en: String) = if (lang.value == Lang.PT) pt else en

    private fun friendly(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> t("Senha muito fraca (mínimo de 6 caracteres).", "Password too weak (6+ characters).")
        is FirebaseAuthUserCollisionException -> t("Este e-mail já está cadastrado.", "This email is already registered.")
        is FirebaseAuthInvalidUserException, is FirebaseAuthInvalidCredentialsException ->
            t("E-mail ou senha inválidos.", "Invalid email or password.")
        is FirebaseNetworkException -> t("Sem conexão com a internet.", "No internet connection.")
        else -> e.localizedMessage ?: t("Erro desconhecido", "Unknown error")
    }
    init { auth.addAuthStateListener { user.value = it.currentUser } }

    // Runs a network task with a timeout; returns an error text, or null on success.
    private suspend fun guarded(block: suspend () -> Unit): String? = try {
        if (withTimeoutOrNull(30_000) { block() } == null) t("Tempo esgotado – verifique sua conexão", "Timed out – check your connection") else null
    } catch (e: Exception) { friendly(e) }

    fun authenticate(email: String, pass: String, create: Boolean) = viewModelScope.launch {
        busy.value = true; message.value = null
        message.value = guarded {
            if (create) auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            else auth.signInWithEmailAndPassword(email.trim(), pass).await()
        }
        val uid = auth.currentUser?.uid
        if (uid != null && dao.classCount() == 0) guarded { cloud.restore(uid) } // fresh device: pull saved data
        busy.value = false
    }

    fun backup() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        busy.value = true
        message.value = guarded { cloud.backup(uid) } ?: t("Backup concluído", "Backup complete")
        busy.value = false
    }

    fun restore() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        busy.value = true
        message.value = guarded { cloud.restore(uid) } ?: t("Restauração concluída", "Restore complete")
        busy.value = false
    }

    // Backs up first, then clears this device so the next person who signs in doesn't see your classes.
    fun signOut() = viewModelScope.launch {
        val uid = auth.currentUser?.uid ?: return@launch
        busy.value = true
        val err = guarded { cloud.backup(uid) }
        if (err == null) {
            cloud.wipeLocal()
            WorkManager.getInstance(app).cancelAllWork()
            auth.signOut()
        } else message.value = t(
            "Não foi possível fazer o backup antes de sair ($err). Você continua conectado, então nada foi perdido.",
            "Couldn't back up before signing out ($err). You're still signed in, so nothing is lost."
        )
        busy.value = false
    }

    fun addClass(name: String) = viewModelScope.launch { if (name.isNotBlank()) dao.insertClass(ClassRoom(name = name.trim())) }
    fun setMaxOut(id: Long, n: Int) = viewModelScope.launch { dao.setMaxOut(id, n.coerceIn(1, 20)) }
    fun deleteClass(c: ClassRoom) = viewModelScope.launch { dao.deleteClass(c) }

    fun addStudents(classId: Long, names: List<String>) = viewModelScope.launch {
        dao.insertStudents(names.map { it.trim() }.filter { it.isNotBlank() }.distinct().map { Student(classId = classId, name = it) })
    }

    fun readPdf(uri: Uri, done: (List<String>) -> Unit) = viewModelScope.launch {
        val r = withContext(Dispatchers.IO) { runCatching { PdfNames.extract(app, uri) }.getOrDefault(emptyList()) }
        done(r)
    }

    fun renameStudent(s: Student, newName: String) = viewModelScope.launch {
        if (newName.isNotBlank()) dao.update(s.copy(name = newName.trim()))
    }

    fun deleteStudent(s: Student) {
        viewModelScope.launch { dao.deleteStudent(s) }
        WorkManager.getInstance(app).cancelUniqueWork("out-${s.id}") // no alert for a removed student
    }

    fun enqueue(s: Student) = viewModelScope.launch { dao.update(s.copy(queuedAt = System.currentTimeMillis())) }
    fun leaveLine(s: Student) = viewModelScope.launch { dao.update(s.copy(queuedAt = null)) }

    fun sendOut(s: Student) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            dao.update(s.copy(queuedAt = null, outSince = now))
            dao.insertTrip(Trip(classId = s.classId, timestamp = now))
        }
        val req = OneTimeWorkRequestBuilder<OverdueWorker>()
            .setInitialDelay(10, TimeUnit.MINUTES)
            .setInputData(workDataOf("id" to s.id, "since" to now)).build()
        WorkManager.getInstance(app).enqueueUniqueWork("out-${s.id}", ExistingWorkPolicy.REPLACE, req)
    }

    fun back(s: Student) {
        viewModelScope.launch { dao.update(s.copy(outSince = null)) }
        WorkManager.getInstance(app).cancelUniqueWork("out-${s.id}")
    }
}
