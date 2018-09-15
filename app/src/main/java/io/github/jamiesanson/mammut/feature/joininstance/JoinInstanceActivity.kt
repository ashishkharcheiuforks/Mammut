package io.github.jamiesanson.mammut.feature.joininstance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.dagger.MammutViewModelFactory
import io.github.jamiesanson.mammut.extension.applicationComponent
import io.github.jamiesanson.mammut.extension.provideViewModel
import io.github.jamiesanson.mammut.feature.base.BaseActivity
import io.github.jamiesanson.mammut.feature.joininstance.dagger.JoinInstanceModule
import javax.inject.Inject

class JoinInstanceActivity: BaseActivity() {

    private lateinit var viewModel: JoinInstanceViewModel

    @Inject lateinit var viewModelFactory: MammutViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_instance)
        viewModel = provideViewModel(viewModelFactory)
    }

    override fun injectDependencies() {
        applicationComponent
                .plus(JoinInstanceModule)
                .inject(this)
    }
}