package com.example.restroomqueue

import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Cloud copy of the local Room data, stored under users/{uid}/... in Firestore.
 * Room stays the source of truth on the device; this is a backup/restore layer.
 * Queue state (who is waiting / out) is NOT backed up, only classes, students and anonymous trips.
 */
class Cloud(private val db: AppDb) {
    private val fs = FirebaseFirestore.getInstance()
    private fun col(uid: String, name: String) = fs.collection("users").document(uid).collection(name)

    suspend fun backup(uid: String) {
        val dao = db.dao()
        push(uid, "classes", dao.allClasses().associate { it.id.toString() to mapOf("name" to it.name, "maxOut" to it.maxOut) })
        push(uid, "students", dao.allStudents().associate { it.id.toString() to mapOf("classId" to it.classId, "name" to it.name) })
        push(uid, "trips", dao.allTrips().associate { it.id.toString() to mapOf("classId" to it.classId, "timestamp" to it.timestamp) })
    }

    // Writes every local doc, then deletes cloud docs that no longer exist locally.
    private suspend fun push(uid: String, name: String, docs: Map<String, Map<String, Any>>) {
        val c = col(uid, name)
        val stale = c.get().await().documents.map { it.id } - docs.keys
        val ops: List<Pair<String, Map<String, Any>?>> =
            docs.map { it.key to it.value } + stale.map { it to null }   // null = delete
        ops.chunked(400).forEach { chunk ->
            val batch = fs.batch()
            chunk.forEach { (id, data) ->
                if (data != null) batch.set(c.document(id), data) else batch.delete(c.document(id))
            }
            batch.commit().await()
        }
    }

    suspend fun restore(uid: String) {
        val classes = col(uid, "classes").get().await().documents.map {
            ClassRoom(it.id.toLong(), it.getString("name") ?: "", (it.getLong("maxOut") ?: 1).toInt())
        }
        val students = col(uid, "students").get().await().documents.mapNotNull {
            Student(it.id.toLong(), it.getLong("classId") ?: return@mapNotNull null, it.getString("name") ?: "")
        }
        val trips = col(uid, "trips").get().await().documents.mapNotNull {
            Trip(it.id.toLong(), it.getLong("classId") ?: return@mapNotNull null, it.getLong("timestamp") ?: return@mapNotNull null)
        }
        db.withTransaction {
            val dao = db.dao()
            dao.clearTrips(); dao.clearStudents(); dao.clearClasses()
            dao.insertClasses(classes); dao.insertStudents(students); dao.insertTrips(trips)
        }
    }

    suspend fun wipeLocal() = db.withTransaction {
        val dao = db.dao()
        dao.clearTrips(); dao.clearStudents(); dao.clearClasses()
    }
}
