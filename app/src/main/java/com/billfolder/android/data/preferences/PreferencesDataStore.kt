package com.billfolder.android.data.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * DataStore das escolhas deste aparelho (aparência, lembretes). Separado do de
 * tokens: não é da sessão e sobrevive ao logout. Um único delegate por
 * arquivo — dois DataStores no mesmo arquivo derrubam o app.
 */
internal val Context.preferencesDataStore by preferencesDataStore(name = "billfolder_preferences")
