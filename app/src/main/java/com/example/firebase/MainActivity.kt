package com.example.firebase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.firebase.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val personCollectionRef = Firebase.firestore.collection("person")
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        // save data to firestore
        binding.saveBtn.setOnClickListener {
            val person = getOldPersons()
            savePerson(person)
        }
//        subscribeToRealtimeUpdates()

        binding.retrevieBtn.setOnClickListener {
            retrievePersons()
        }
        binding.btnUploadData.setOnClickListener {
            val person = getOldPersons()
            val newMapPerson = getNewPersonMap()
            updatePerson(person, newMapPerson)
        }

        binding.btnDeletedata.setOnClickListener {
            val person = getOldPersons()
            deletePerson(person)
        }

        binding.btnBatchesWrite.setOnClickListener {
            changeName("5LWAMXrDyAsKj7Kg6ogq", "Abu", "Abus")
        }

    }


    private fun changeName(
        personId: String,
        newFirstName: String,
        newLastName: String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runBatch { batch ->
                val personRef = personCollectionRef.document(personId)
                batch.update(personRef, "firstName", newFirstName)
                batch.update(personRef, "lastName", newLastName)
            }.await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Get data from the user
    private fun getOldPersons(): Person {
        val firstName = binding.firstName.text.toString()
        val lastName = binding.lastName.text.toString()
        val age = binding.age.text.toString().toInt()
        return Person(firstName = firstName, lastName = lastName, age = age)
    }

    // Get data from the user
    private fun getNewPersonMap(): Map<String, Any> {
        val firstName = binding.newfirstName.text.toString()
        val lastName = binding.newlastName.text.toString()
        val age = binding.newage.text.toString()
        val map = mutableMapOf<String, Any>()
        if (firstName.isNotEmpty()) {
            map["firstName"] = firstName
        }
        if (lastName.isNotEmpty()) {
            map["lastName"] = lastName
        }
        if (age.isNotEmpty()) {
            map["age"] = age
        }
        return map

    }

    // deleted data from firestore
    private fun deletePerson(person: Person) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = personCollectionRef
                .whereEqualTo("firstName", person.firstName)
                .whereEqualTo("lastName", person.lastName)
                .whereEqualTo("age", person.age)
                .get()
                .await()
            if (personQuery.documents.isNotEmpty()) {
                for (document in personQuery) {
                    try {
                        personCollectionRef.document(document.id).delete().await()
//                        personCollectionRef.document(document.id).update(mapOf("firstName" to FieldValue.delete())).await()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Deleted", Toast.LENGTH_LONG)
                                .show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "No person matched the query",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


    // Update data in firestore
    private fun updatePerson(person: Person, newMapPerson: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = personCollectionRef
                .whereEqualTo("firstName", person.firstName)
                .whereEqualTo("lastName", person.lastName)
                .whereEqualTo("age", person.age)
                .get()
                .await()
            if (personQuery.documents.isNotEmpty()) {
                for (document in personQuery) {
                    try {
                        personCollectionRef.document(document.id)
                            .update("firstName", person.firstName).await()
                        personCollectionRef.document(document.id).set(
                            newMapPerson,
                            SetOptions.merge()
                        ).await()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Updated", Toast.LENGTH_LONG)
                                .show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "No person matched the query",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    // Realtime updates
    private fun subscribeToRealtimeUpdates() {
        personCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val sb = StringBuilder()
                for (document in it) {
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                }
                binding.textView.text = sb.toString()
            }
        }
    }

    // Retrieve data from firestore
    private fun retrievePersons() = CoroutineScope(Dispatchers.IO).launch {
        val fromAge = binding.fromAge.text.toString().toInt()
        val toAge = binding.toAge.text.toString().toInt()
        try {
            val querySnapshot = personCollectionRef
                .whereGreaterThan("age", fromAge)
                .whereLessThan("age", toAge)
                .orderBy("age")
                .get()
                .await()

            val sb = StringBuilder()
            for (document in querySnapshot.documents) {
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }
            withContext(Dispatchers.Main) {
                binding.textView.text = sb.toString()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("MainActivityT", e.message.toString())
            }
        }
    }

    // Save data to firestore
    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

