package com.project.businesscardscannerapp.AppUI

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard

@Composable
fun SaveContactButton(
    contactName: String,
    phoneNumbers: List<String>,
    email: String?
) {
    val context = LocalContext.current

    // Permission launcher
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveContact(context, contactName, phoneNumbers, email)
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Save button UI
    Button(onClick = {
        contactPermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
    }) {
        Text(text = "Save to Contacts")
    }
}


fun saveContact(
    context: Context,
    name: String,
    phoneNumbers: List<String>,
    email: String?
) {
    val ops = ArrayList<ContentProviderOperation>()

    ops.add(
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build()
    )

    // Name
    ops.add(
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            .build()
    )

    // Multiple phone numbers
    phoneNumbers.forEachIndexed { index, number ->
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    if (index == 0) ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    else ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                )
                .build()
        )
    }

    // Email (optional)
    email?.let {
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, it)
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                .build()
        )
    }

    try {
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        Toast.makeText(context, "Contact saved successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save contact", Toast.LENGTH_SHORT).show()
    }
}


fun autoSaveToContacts(context: Context, card: BusinessCard) {
    if (!card.phones.isNullOrEmpty() && !card.name.isNullOrBlank()) {
        // Ask for permission first if not already granted
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            saveContact(
                context,
                name = card.name,
                phoneNumbers = card.phones,
                email = card.email
            )
        } else {
            // Handle permission request (optional improvement)
            Toast.makeText(context, "Contact permission required", Toast.LENGTH_SHORT).show()
        }
    }
}
