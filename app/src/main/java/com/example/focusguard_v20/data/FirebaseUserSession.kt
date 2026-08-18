package com.example.focusguard_v20.data

import com.google.firebase.auth.FirebaseAuth

class FirebaseUserSession(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    fun uidOrNull(): String? = auth.currentUser?.uid
}

