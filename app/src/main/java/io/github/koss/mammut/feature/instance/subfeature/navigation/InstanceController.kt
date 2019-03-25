package io.github.koss.mammut.feature.instance.subfeature.navigation

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.github.ajalt.flexadapter.FlexAdapter
import com.github.ajalt.flexadapter.register
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.github.koss.mammut.R
import io.github.koss.mammut.component.GlideApp
import io.github.koss.mammut.feature.instance.subfeature.FullScreenPhotoHandler
import io.github.koss.mammut.feature.instance.subfeature.feed.FeedController
import io.github.koss.mammut.feature.instance.subfeature.feed.FeedType
import io.github.koss.mammut.feature.instance.subfeature.profile.ProfileController
import io.github.koss.mammut.base.BaseController
import io.github.koss.mammut.base.dagger.MammutViewModelFactory
import io.github.koss.mammut.base.dagger.SubcomponentFactory
import io.github.koss.mammut.component.retention.retained
import io.github.koss.mammut.data.models.Account
import io.github.koss.mammut.extension.*
import io.github.koss.mammut.feature.about.AboutController
import io.github.koss.mammut.feature.instance.MultiInstanceController
import io.github.koss.mammut.feature.instance.bottomnav.BottomNavigationViewModel
import io.github.koss.mammut.feature.instance.bottomnav.BottomNavigationViewState
import io.github.koss.mammut.feature.instance.dagger.InstanceComponent
import io.github.koss.mammut.feature.instance.dagger.InstanceModule
import io.github.koss.mammut.feature.instance.dagger.InstanceScope
import io.github.koss.mammut.feature.joininstance.JoinInstanceActivity
import io.github.koss.mammut.feature.settings.SettingsController
import io.github.koss.mammut.toot.ComposeTootController
import io.github.koss.mammut.toot.dagger.ComposeTootModule
import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.ContainerOptions
import kotlinx.android.synthetic.main.controller_instance.*
import kotlinx.android.synthetic.main.controller_instance.view.*
import kotlinx.android.synthetic.main.navigation_bottom_sheet_content.*
import kotlinx.android.synthetic.main.navigation_bottom_sheet_content.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.colorAttr
import org.jetbrains.anko.dimen
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onHover
import org.jetbrains.anko.sdk27.coroutines.onTouch
import javax.inject.Inject

private const val ROUTER_STATES_KEY = "STATE"

const val ARG_INSTANCE_NAME = "instance_name"
const val ARG_AUTH_CODE = "auth_code"

/**
 * The following is an adaption of a brilliant Gist which did everything I wanted.
 * Check out the gist here: https://gist.github.com/StefMa/5a6d99a8948f0a1b80cfbf5bd4b51c20
 *
 * This is the base implementation of a [Controller] which works hand in hand with the [BottomNavigationView].
 *
 * It is designed to work like that:
 * * [Textual explanation](https://i.imgur.com/EqqQyOY.png)
 * * [Visual explanation](https://i.imgur.com/FDb6EGU.png)
 *
 * In other words. It should be behave exactly like the [iOS TabBar](http://apple.co/2y6XIrL)
 *
 * **How does it work?**
 *
 * If one item in the [BottomNavigationView] we do three things:
 * * Save the current [Router.saveInstanceState] in the [routerStates] with the [BottomNavigationView.getSelectedItemId] as key. See [saveStateFromCurrentTab]
 * * Clear the current [Router] hierachy and backstack and everything (cleanup). See [clearStateFromChildRouter]
 * * Try to restore the [Router.restoreInstanceState] with the saved state contains in [routerStates]. See [tryToRestoreStateFromNewTab] and [onNavigationItemSelected]
 *
 * The main idea came from [this PR](https://github.com/bluelinelabs/Conductor/pull/316).
 */
@ContainerOptions(cache = CacheImplementation.NO_CACHE)
class InstanceController(args: Bundle) : BaseController(args),
        BottomNavigationView.OnNavigationItemSelectedListener,
        FullScreenPhotoHandler,
        SubcomponentFactory {

    /**
     * This will hold all the information about the tabs.
     *
     * This needs to be a var because we have to reassign it in [onRestoreInstanceState]
     */
    private var routerStates = SparseArray<Bundle>()

    /**
     * Field containing the job with the coroutine responsible for peaking.
     */
    private var peekJob: Job? = null

    private lateinit var childRouter: Router

    lateinit var component: InstanceComponent

    private val instanceModule: InstanceModule by retained(key = { args.getString(ARG_AUTH_CODE)!! }) {
        InstanceModule(
                instanceName = args.getString(ARG_INSTANCE_NAME)!!,
                accessToken = args.getString(ARG_AUTH_CODE)!!)
    }

    private var fullScreenImageAnimator: ViewsTransitionAnimator<*>? = null

    @Inject
    @InstanceScope
    lateinit var viewModelFactory: MammutViewModelFactory

    private lateinit var bottomNavigationViewModel: BottomNavigationViewModel

    /**
     * This is the current selected item id from the [BottomNavigationView]
     */
    @IdRes
    private var currentSelectedItemId: Int = -1

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        component = (context as AppCompatActivity).applicationComponent
                .plus(instanceModule)

        component.inject(this)

        bottomNavigationViewModel = ViewModelProviders
                .of(activity as AppCompatActivity, viewModelFactory)
                .get(component.accessToken(), BottomNavigationViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        val view = inflater.inflate(R.layout.controller_instance, container, false)

        childRouter = getChildRouter(view.container)

        view.bottomNavigationView.setOnNavigationItemSelectedListener(this)

        view.bottomNavigationView.setOnNavigationItemReselectedListener {
            (childRouter.backstack.last().controller() as? ReselectListener)?.onTabReselected()
        }

        view.addButton.onClick {
            performComposeTootOpen()
        }

        setupBottomNavigation(view)

        // We have not a single bundle/state saved.
        // Looks like this [HomeController] was created for the first time
        if (routerStates.size() == 0) {
            // Select the first item
            currentSelectedItemId = R.id.homeDestination
            childRouter.setRoot(RouterTransaction.with(FeedController.newInstance(FeedType.Home)))
        } else {
            // We have something in the back stack. Maybe an orientation change happen?
            // We can just rebind the current router
            childRouter.rebindIfNeeded()
        }

        return view
    }

    /**
     * Listener which get called if a item from the [BottomNavigationView] is selected
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Save the state from the current tab so that we can restore it later - if needed
        saveStateFromCurrentTab(currentSelectedItemId)
        currentSelectedItemId = item.itemId
        // Clear all the hierarchy and backstack from the router. We have saved it already in the [routerStates]
        clearStateFromChildRouter()
        // Try to restore the state from the new selected tab.
        val bundleState = tryToRestoreStateFromNewTab(currentSelectedItemId)

        if (bundleState is Bundle) {
            // We have found a state (hierarchy/backstack etc.) and can just restore it to the [childRouter]
            childRouter.restoreInstanceState(bundleState)
            childRouter.rebindIfNeeded()
            return true
        }

        // There is no state (hierarchy/backstack etc.) saved in the [routerBundles].
        // We have to create a new [Controller] and set as root
        return selectTabById(item.itemId)
    }

    private fun selectTabById(menuItemId: Int): Boolean {
        val controller = when (menuItemId) {
            Tab.Home.menuItemId -> FeedController.newInstance(FeedType.Home)
            Tab.Local.menuItemId -> FeedController.newInstance(FeedType.Local)
            Tab.Federated.menuItemId -> FeedController.newInstance(FeedType.Federated)
            Tab.Profile.menuItemId -> ProfileController.newInstance(isMe = true)
            else -> return false
        }

        childRouter.setRoot(RouterTransaction.with(controller))

        return true
    }

    /**
     * Try to restore the state (which was saved via [saveStateFromCurrentTab]) from the [routerStates].
     *
     * @return either a valid [Bundle] state or null if no state is available
     */
    private fun tryToRestoreStateFromNewTab(itemId: Int): Bundle? {
        return routerStates.get(itemId)
    }

    /**
     * This will clear the state (hierarchy/backstack etc.) from the [childRouter] and goes back to root.
     */
    private fun clearStateFromChildRouter() {
        childRouter.setPopsLastView(true) /* Ensure the last view can be removed while we do this */
        childRouter.popToRoot()
        childRouter.popCurrentController()
        childRouter.setPopsLastView(false)
    }

    /**
     * This will save the current state of the tab (hierarchy/backstack etc.) from the [childRouter] in a [Bundle]
     * and put it into the [routerStates] with the tab id as key
     */
    private fun saveStateFromCurrentTab(itemId: Int) {
        val routerBundle = Bundle()
        childRouter.saveInstanceState(routerBundle)
        routerStates.put(itemId, routerBundle)
    }

    /**
     * Save our [routerStates] into the instanceState so we don't loose them on orientation change
     */
    override fun onSaveInstanceState(outState: Bundle) {
        saveStateFromCurrentTab(currentSelectedItemId)
        outState.putSparseParcelableArray(ROUTER_STATES_KEY, routerStates)
        super.onSaveInstanceState(outState)
    }

    /**
     * Restore our [routerStates]
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        routerStates = savedInstanceState.getSparseParcelableArray(ROUTER_STATES_KEY)
                ?: SparseArray()
    }

    override fun handleBack(): Boolean {
        return if (fullScreenImageAnimator?.isLeaving == false) {
            fullScreenImageAnimator?.exit(true)
            true
        } else {
            super.handleBack()
        }
    }

    override fun <Module, Subcomponent> buildSubcomponent(module: Module): Subcomponent {
        @Suppress("UNCHECKED_CAST")
        return when (module) {
            is ComposeTootModule -> component.plus(module)
            else -> throw IllegalArgumentException("Unknown module type")
        } as Subcomponent
    }

    /**
     * Function for rendering an interactive full screen image
     */
    override fun displayFullScreenPhoto(imageView: ImageView, photoUrl: String) {
        // Setup animator
        fullScreenImageAnimator = GestureTransitions.from<Unit>(imageView).into(fullScreenGestureImageView).also {
            it.addPositionUpdateListener { position, isLeaving ->
                containerView ?: return@addPositionUpdateListener
                fullScreenPhotoLayout.alpha = position
                val visibility = when {
                    position == 0F && isLeaving -> View.GONE
                    else -> View.VISIBLE
                }

                fullScreenPhotoLayout.visibility = visibility
                fullScreenGestureImageView.visibility = visibility

                if (position == 0f && isLeaving) {
                    // Invalidate the target to ensure it resizes properly
                    GlideApp.with(imageView)
                            .load(photoUrl)
                            .placeholder(fullScreenGestureImageView.drawable)
                            .transition(withCrossFade())
                            .transform(FitCenter())
                            .into(imageView)
                }
            }
        }

        // Reset controller state
        fullScreenGestureImageView.controller.resetState()

        // Start the animation
        fullScreenImageAnimator?.enterSingle(true)

        GlideApp.with(imageView)
                .load(photoUrl)
                .placeholder(imageView.drawable)
                .transition(withCrossFade())
                .transform(FitCenter())
                .into(fullScreenGestureImageView)
    }

    /**
     * Function called to peek the current user. Called when the page is selected.
     */
    fun peekCurrentUser() {
        // Change the peekheight of the bottom navigation, then change it back
        peekJob?.cancel()

        peekJob = launch {
            delay(200L)
            val startingHeight = container.dimen(R.dimen.default_navigation_peek_height)
            val additionalHeight = container.dimen(R.dimen.profile_cell_height)

            withContext(Dispatchers.Main) {
                // Disable interactions
                bottomNavigationSheet.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    (behavior as BottomSheetBehavior).apply {
                        state = BottomSheetBehavior.STATE_EXPANDED
                        peekHeight = startingHeight + additionalHeight
                        state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            }

            delay(1500L)

            withContext(Dispatchers.Main) {
                bottomNavigationSheet.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    (behavior as BottomSheetBehavior).apply {
                        if (state != BottomSheetBehavior.STATE_COLLAPSED) {
                            peekHeight = startingHeight
                        } else {
                            state = BottomSheetBehavior.STATE_EXPANDED
                            peekHeight = startingHeight
                            state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                }

                // The following delay is such that dimming is now re-enabled until the peek animation concludes
                delay(100)
            }
        }

        peekJob?.invokeOnCompletion {
            when (it) {
                is CancellationException -> {
                    // Reset the peek height of the bottom sheet
                    launch (Dispatchers.Main) {
                        bottomNavigationSheet.behaviour<BottomSheetBehavior<View>>()?.peekHeight = container.dimen(R.dimen.default_navigation_peek_height)
                    }
                }
            }
        }
    }

    private fun collapseBottomSheet() {
        view?.bottomNavigationSheet
                ?.behaviour<BottomSheetBehavior<View>>()
                ?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun resetPeek() {
        // Reset peak height and re-enable dimming
        view?.let {
            it.behaviour<BottomSheetBehavior<View>>()?.peekHeight = it.dimen(R.dimen.default_navigation_peek_height)
        }

        peekJob?.cancel()
        peekJob = null
    }

    private fun setupBottomNavigation(view: View) {
        view.bottomSheetContentLayout.elevation = view.bottomNavigationView.elevation
        bottomNavigationViewModel.viewState.observe(this, ::renderBottomNavigationContent)

        val dimBackground = view.bottomNavigationDim
        val statusBarColor = (view.context as AppCompatActivity).window.statusBarColor
        dimBackground.isVisible = false
        dimBackground.onClick {
            collapseBottomSheet()
        }

        // Setup Button click handlers
        view.settingsCell.onClick {
            collapseBottomSheet()
            parentController?.router?.pushController(
                    RouterTransaction
                            .with(SettingsController())
                            .popChangeHandler(VerticalChangeHandler())
                            .pushChangeHandler(VerticalChangeHandler()))
        }

        view.aboutAppCell.onClick {
            collapseBottomSheet()
            parentController?.router?.pushController(
                    RouterTransaction
                            .with(AboutController())
                            .popChangeHandler(VerticalChangeHandler())
                            .pushChangeHandler(VerticalChangeHandler()))
        }

        // Setup Slide handling
        val dimBackgroundColor = view.colorAttr(R.attr.colorControlNormalTransparent)

        view.bottomNavigationSheet
                .behaviour<BottomSheetBehavior<View>>()
                ?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onSlide(view: View, proportion: Float) {
                        if (peekJob?.isActive == true) {
                            // Check to see that the user hasn't tried to swipe up the bottom sheet
                            val peekHeight = view.behaviour<BottomSheetBehavior<View>>()?.peekHeight
                            val screenHeight = view.context.displayMetrics.heightPixels
                            if (peekHeight != null && (screenHeight - view.y) > (view.dimen(R.dimen.profile_cell_height) + peekHeight)) {
                                // Reset peak height and re-enable dimming
                                resetPeek()
                            } else {
                                return
                            }
                        }

                        when (proportion) {
                            0f -> {
                                dimBackground.isVisible = false
                                (view.context as AppCompatActivity).window.statusBarColor = statusBarColor
                                (parentController as MultiInstanceController).unlockViewPager()
                            }
                            else -> dimBackground.apply {
                                isVisible = true
                                alpha = proportion
                                (parentController as MultiInstanceController).lockViewPager()
                            }
                        }

                        // Mask and modify alpha of background color and apply to statusbar
                        val maxAlpha = ((dimBackgroundColor and "FF000000".toLong(radix = 16).toInt()) shr 24).run {
                            this and "FF".toLong(radix = 16).toInt()
                        }

                        val newAlpha = (maxAlpha * proportion).toInt() shl 24

                        val newStatusColor = (dimBackgroundColor and "00FFFFFF".toLong(radix = 16).toInt()) + newAlpha

                        // Update status bar colour
                        (view.context as AppCompatActivity).window.statusBarColor = newStatusColor
                    }

                    override fun onStateChanged(p0: View, p1: Int) {}

                })
    }

    private fun renderBottomNavigationContent(state: BottomNavigationViewState) {
        // Load Account
        @ColorInt val placeholderColor = container.colorAttr(R.attr.colorPrimaryLight)

        GlideApp.with(container)
                .load(state.currentUser.avatar)
                .thumbnail(
                        GlideApp.with(container)
                                .load(ColorDrawable(placeholderColor))
                                .apply(RequestOptions.circleCropTransform())
                )
                .transition(withCrossFade())
                .apply(RequestOptions.circleCropTransform())
                .into(bottomSheetContentLayout.profileImageView)

        with(bottomSheetContentLayout) {
            displayNameTextView.text = state.currentUser.displayName
            usernameTextView.text = state.currentUser.fullAcct(instanceModule.provideInstanceName())

            instancesRecyclerView.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            instancesRecyclerView.adapter = FlexAdapter<Account>().apply {
                register<Account>(layout = R.layout.card_account) { account, view, _ ->
                    view.findViewById<TextView>(R.id.displayNameTextView).text = account.displayName
                    view.findViewById<TextView>(R.id.usernameTextView).apply {
                        text = account.acct
                        isSelected = true
                    }

                    GlideApp.with(view)
                            .load(account.header)
                            .thumbnail(
                                    GlideApp.with(view)
                                            .load(ColorDrawable(placeholderColor))
                            )
                            .apply(RequestOptions.centerCropTransform())
                            .transition(withCrossFade())
                            .into(view.findViewById(R.id.backgroundImageView))

                    GlideApp.with(view)
                            .load(account.avatar)
                            .thumbnail(
                                    GlideApp.with(view)
                                            .load(ColorDrawable(placeholderColor))
                                            .apply(RequestOptions.circleCropTransform())
                            )
                            .apply(RequestOptions.circleCropTransform())
                            .transition(withCrossFade())
                            .into(view.findViewById(R.id.avatarImageView))
                }

                resetItems(state.otherAccounts)
            }

            addAccountButton.onClick {
                collapseBottomSheet()
                startActivity<JoinInstanceActivity>()
            }

            profileCell.onClick {
                collapseBottomSheet()
                childRouter.pushController(
                        RouterTransaction
                                .with(ProfileController.newInstance(account = state.currentUser, isMe = true))
                                .pushChangeHandler(FadeChangeHandler())
                                .popChangeHandler(FadeChangeHandler()))
            }
        }

    }

    /**
     * Function for coordinating the animations which lead to the reveal of the [ComposeTootController]
     */
    private fun performComposeTootOpen() {
        resetPeek()
        router.pushController(RouterTransaction
                .with(ComposeTootController().apply { targetController = this@InstanceController })
                .popChangeHandler(VerticalChangeHandler())
                .pushChangeHandler(VerticalChangeHandler()))
    }
}
