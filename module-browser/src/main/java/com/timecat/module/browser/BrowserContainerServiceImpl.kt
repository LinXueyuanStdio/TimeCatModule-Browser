package com.timecat.module.browser

import android.content.Context
import com.timecat.identity.readonly.RouterHub
import com.timecat.middle.block.service.ContainerService
import com.timecat.middle.block.service.HomeService
import com.xiaojinzi.component.anno.ServiceAnno

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2021/3/24
 * @description null
 * @usage null
 */
@ServiceAnno(ContainerService::class, name = [RouterHub.GLOBAL_BrowserContainerService])
class BrowserContainerServiceImpl: ContainerService {
    override fun loadForVirtualPath(
        context: Context,
        parentUuid: String,
        homeService: HomeService,
        callback: ContainerService.LoadCallback
    ) {
        TODO("Not yet implemented")
    }

}