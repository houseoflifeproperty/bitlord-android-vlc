package org.videolan.vlc

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.videolan.libvlc.FactoryManager
import org.videolan.libvlc.interfaces.ILibVLCFactory
import org.videolan.libvlc.interfaces.IMediaFactory
import org.videolan.libvlc.stubs.StubLibVLCFactory
import org.videolan.libvlc.stubs.StubMediaFactory
import org.videolan.medialibrary.MLServiceLocator

@ExperimentalCoroutinesApi
@ObsoleteCoroutinesApi
class VLCTestApplication : Application() {
    init {
        MLServiceLocator.setLocatorMode(MLServiceLocator.LocatorMode.TESTS)
        FactoryManager.registerFactory(IMediaFactory.factoryId, StubMediaFactory())
        FactoryManager.registerFactory(ILibVLCFactory.factoryId, StubLibVLCFactory())
    }
}