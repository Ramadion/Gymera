package com.DeBiaseRamiro.gymera

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Esta anotación le dice a Hilt que arranque su sistema de inyección
@HiltAndroidApp
class GymeraApplication : Application()