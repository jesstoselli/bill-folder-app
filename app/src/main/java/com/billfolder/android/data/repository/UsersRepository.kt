package com.billfolder.android.data.repository

import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.dto.UserDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepository @Inject constructor(
    private val api: BillFolderApi,
) {
    /** Erros (rede, HTTP) propagam como exceção — o ViewModel vira estado de UI. */
    suspend fun me(): UserDto = api.getMe()
}
