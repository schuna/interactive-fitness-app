package com.openai.interactivefitness.data

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInManager(private val activity: Activity) {
    suspend fun getIdToken(): String {
        val resourceId = activity.resources.getIdentifier(
            "default_web_client_id",
            "string",
            activity.packageName,
        )
        check(resourceId != 0) { "Google OAuth web client is not configured" }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(activity.getString(resourceId))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val result = CredentialManager.create(activity).getCredential(
            activity,
            GetCredentialRequest.Builder().addCredentialOption(option).build(),
        )
        val credential = result.credential
        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        ) { "Unexpected Google credential type" }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
