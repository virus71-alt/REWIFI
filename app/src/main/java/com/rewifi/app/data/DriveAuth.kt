package com.rewifi.app.data

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

/** Thin wrapper over Google Sign-In, scoped to the Drive app-data folder. */
object DriveAuth {

    fun client(context: Context): GoogleSignInClient {
        val opts = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveBackup.SCOPE))
            .build()
        return GoogleSignIn.getClient(context, opts)
    }

    fun signedInEmail(context: Context): String? =
        GoogleSignIn.getLastSignedInAccount(context)?.email

    fun account(context: Context): Account? =
        GoogleSignIn.getLastSignedInAccount(context)?.account
}
