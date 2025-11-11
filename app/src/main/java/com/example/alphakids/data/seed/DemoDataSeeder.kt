package com.example.alphakids.data.seed

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoDataSeeder @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        val metadataRef = firestore.collection("metadata").document("demo_seed")
        val seeded = metadataRef.get().await().getLong("version") == SEED_VERSION
        if (seeded) return@withContext
        seedUsers()
        seedDocentes()
        seedTutores()
        seedInstituciones()
        seedStudents()
        seedCategories()
        seedWords()
        seedAssignments()
        seedDictionary()
        seedStore()
        seedInventory()
        seedAchievements()
        metadataRef.set(mapOf("version" to SEED_VERSION), SetOptions.merge()).await()
    }

    private suspend fun seedUsers() {
        val users = listOf(
            "docente_demo" to mapOf(
                "fullName" to "Docente Demo",
                "email" to "docente.demo@alphakids.app",
                "role" to "DOCENTE"
            ),
            "tutor_demo" to mapOf(
                "fullName" to "Tutor Demo",
                "email" to "tutor.demo@alphakids.app",
                "role" to "TUTOR"
            )
        )
        users.forEach { (id, data) ->
            firestore.collection("users").document(id).set(data, SetOptions.merge()).await()
        }
    }

    private suspend fun seedDocentes() {
        val data = mapOf(
            "nombre" to "Docente",
            "apellido" to "Demo",
            "institucionId" to "institucion_demo"
        )
        firestore.collection("docentes").document("docente_demo").set(data, SetOptions.merge()).await()
    }

    private suspend fun seedTutores() {
        val data = mapOf(
            "nombre" to "Tutor",
            "apellido" to "Demo",
            "telefono" to "555000111"
        )
        firestore.collection("tutores").document("tutor_demo").set(data, SetOptions.merge()).await()
    }

    private suspend fun seedInstituciones() {
        val data = mapOf(
            "nombre" to "Colegio Demo",
            "direccion" to "Av. Demo 123"
        )
        firestore.collection("instituciones").document("institucion_demo").set(data, SetOptions.merge()).await()
    }

    private suspend fun seedStudents() {
        val students = listOf(
            Triple("estudiante_a", 150, "Ana Rivera"),
            Triple("estudiante_b", 80, "Bruno Torres"),
            Triple("estudiante_c", 60, "Carla Medina")
        )
        students.forEachIndexed { index, (id, coins, fullName) ->
            val parts = fullName.split(" ")
            val nombre = parts.first()
            val apellido = parts.drop(1).joinToString(" ")
            val data = mapOf(
                "nombre" to nombre,
                "apellido" to apellido,
                "edad" to 7 + index,
                "grado" to "${index + 1}º",
                "seccion" to "A",
                "id_tutor" to "tutor_demo",
                "id_docente" to "docente_demo",
                "id_institucion" to "institucion_demo",
                "coins" to coins
            )
            firestore.collection("estudiantes").document(id).set(data, SetOptions.merge()).await()
        }
    }

    private suspend fun seedCategories() {
        val categories = listOf(
            "animales" to "Animales",
            "colores" to "Colores",
            "objetos" to "Objetos"
        )
        categories.forEach { (id, nombre) ->
            firestore.collection("categorias").document(id).set(mapOf("nombre" to nombre), SetOptions.merge()).await()
        }
    }

    private suspend fun seedWords() {
        val words = listOf(
            Triple("gato", 15, "animales"),
            Triple("perro", 15, "animales"),
            Triple("loro", 20, "animales"),
            Triple("rojo", 10, "colores"),
            Triple("azul", 10, "colores"),
            Triple("verde", 12, "colores"),
            Triple("libro", 18, "objetos"),
            Triple("mesa", 25, "objetos")
        )
        words.forEachIndexed { index, (texto, coins, categoria) ->
            val data = mapOf(
                "texto" to texto,
                "rewardCoins" to coins,
                "categoriaId" to categoria,
                "docenteId" to "docente_demo",
                "orden" to index
            )
            firestore.collection("palabras").document(texto).set(data, SetOptions.merge()).await()
        }
    }

    private suspend fun seedAssignments() {
        val assignments = listOf(
            "estudiante_a" to 4,
            "estudiante_b" to 2,
            "estudiante_c" to 2
        )
        assignments.forEach { (studentId, count) ->
            repeat(count) { index ->
                val wordId = "${studentId}_$index"
                val status = if (index % 2 == 0) "PENDIENTE" else "COMPLETADA"
                val data = mapOf(
                    "estudianteId" to studentId,
                    "palabraId" to listOf("gato", "perro", "loro", "libro", "mesa")[index % 5],
                    "estado" to status,
                    "docenteId" to "docente_demo"
                )
                firestore.collection("asignaciones").document(wordId).set(data, SetOptions.merge()).await()
            }
        }
    }

    private suspend fun seedDictionary() {
        val entries = listOf(
            "gato" to "El gato duerme",
            "perro" to "El perro ladra",
            "loro" to "El loro habla",
            "rojo" to "El color rojo",
            "azul" to "El cielo azul",
            "libro" to "Leo un libro"
        )
        entries.forEach { (word, usage) ->
            firestore.collection("diccionarioDocente").document(word).set(
                mapOf(
                    "palabra" to word,
                    "uso" to usage,
                    "docenteId" to "docente_demo"
                ),
                SetOptions.merge()
            ).await()
        }
    }

    private suspend fun seedStore() {
        val items = listOf(
            "pet_gato" to mapOf(
                "name" to "Gato",
                "price" to 120,
                "type" to "PET",
                "meta" to mapOf("petKind" to "GATO")
            ),
            "pet_perro" to mapOf(
                "name" to "Perro",
                "price" to 120,
                "type" to "PET",
                "meta" to mapOf("petKind" to "PERRO")
            ),
            "acc_sombrero" to mapOf(
                "name" to "Sombrero",
                "price" to 45,
                "type" to "ACCESSORY",
                "meta" to mapOf(
                    "petKind" to "UNIVERSAL",
                    "accessorySlot" to "HEAD"
                )
            ),
            "acc_collar" to mapOf(
                "name" to "Collar",
                "price" to 35,
                "type" to "ACCESSORY",
                "meta" to mapOf(
                    "petKind" to "UNIVERSAL",
                    "accessorySlot" to "NECK"
                )
            ),
            "acc_capa" to mapOf(
                "name" to "Capa",
                "price" to 55,
                "type" to "ACCESSORY",
                "meta" to mapOf(
                    "petKind" to "UNIVERSAL",
                    "accessorySlot" to "BACK"
                )
            ),
            "cons_croqueta" to mapOf(
                "name" to "Croqueta",
                "price" to 15,
                "type" to "CONSUMABLE",
                "meta" to mapOf("consumableKind" to "CROQUETA")
            ),
            "cons_hueso" to mapOf(
                "name" to "Hueso",
                "price" to 25,
                "type" to "CONSUMABLE",
                "meta" to mapOf("consumableKind" to "HUESO")
            )
        )
        items.forEach { (id, data) ->
            firestore.collection("tiendaItems").document(id).set(data, SetOptions.merge()).await()
        }
    }

    private suspend fun seedInventory() {
        val inventories = listOf(
            Triple("estudiante_a", "cons_croqueta", 2),
            Triple("estudiante_a", "cons_hueso", 1),
            Triple("estudiante_b", "cons_croqueta", 2),
            Triple("estudiante_c", "cons_croqueta", 2)
        )
        inventories.forEach { (studentId, itemId, qty) ->
            val docId = "${studentId}_$itemId"
            val data = mapOf(
                "estudianteId" to studentId,
                "itemId" to itemId,
                "qty" to qty
            )
            firestore.collection("estudianteInventario").document(docId).set(data, SetOptions.merge()).await()
        }

        val pets = listOf(
            "estudiante_a" to mapOf(
                "petKind" to "GATO",
                "hunger" to 40,
                "happiness" to 80,
                "equippedAccessories" to mapOf(
                    "HEAD" to "acc_sombrero",
                    "NECK" to "acc_collar",
                    "BACK" to "acc_capa"
                )
            ),
            "estudiante_b" to mapOf(
                "petKind" to "PERRO",
                "hunger" to 55,
                "happiness" to 70,
                "equippedAccessories" to emptyMap<String, String?>()
            )
        )
        pets.forEach { (studentId, data) ->
            firestore.collection("estudianteMascotas").document(studentId).set(data, SetOptions.merge()).await()
        }
    }

    private suspend fun seedAchievements() {
        val achievements = listOf(
            "primer_logro" to mapOf(
                "nombre" to "Primera palabra",
                "descripcion" to "Completa tu primera palabra",
                "coins" to 10
            ),
            "maestro_colores" to mapOf(
                "nombre" to "Colores completos",
                "descripcion" to "Aprende todas las palabras de colores",
                "coins" to 20
            )
        )
        achievements.forEach { (id, data) ->
            firestore.collection("logros").document(id).set(data, SetOptions.merge()).await()
        }
    }

    companion object {
        private const val SEED_VERSION = 1L
    }
}
