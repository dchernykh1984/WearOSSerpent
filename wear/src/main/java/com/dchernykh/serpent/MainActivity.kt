package com.dchernykh.serpent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dchernykh.serpent.layout.GRID_CELLS
import com.dchernykh.serpent.store.DataStoreRecordStore
import com.dchernykh.serpent.ui.SerpentApp

/**
 * The one and only activity. A watch game is a single full-screen surface with no
 * navigation to speak of, so there is nothing for a second one to do.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember { DataStoreRecordStore(applicationContext) }
            SerpentApp(viewModel(factory = SerpentViewModel.factory(store, GRID_CELLS)))
        }
    }
}
