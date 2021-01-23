package acr.browser.lightning

import acr.browser.lightning.device.BuildInfo
import acr.browser.lightning.di.AppComponent
import acr.browser.lightning.di.AppModule
import acr.browser.lightning.di.DaggerAppComponent
import acr.browser.lightning.di.createBuildType
import com.timecat.extend.arms.BaseApplication

class BrowserApp private constructor() {
    companion object {
        @JvmStatic
        var appComponent: AppComponent = DaggerAppComponent.builder().appModule(AppModule(
                BaseApplication.getInstance(),
                BuildInfo(createBuildType())
        )).build()
    }

}
