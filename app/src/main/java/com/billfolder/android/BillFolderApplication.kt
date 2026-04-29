package com.billfolder.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Entry-point do Hilt. A anotação @HiltAndroidApp gera o
 * componente raiz da árvore de DI e instala o ApplicationComponent.
 */
@HiltAndroidApp
class BillFolderApplication : Application()
