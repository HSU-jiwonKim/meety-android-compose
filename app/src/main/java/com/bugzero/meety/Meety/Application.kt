package com.bugzero.meety

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

/**
 * Coil 이미지 로딩 최적화
 *
 * - 메모리 캐시: 앱 메모리의 25% (기본값) → 이미 본 이미지 즉시 표시
 * - 디스크 캐시: 50MB → 앱 재시작해도 다시 안 받음
 * - crossfade: 200ms → 이미지 팍 나타나는 대신 부드럽게 페이드인
 */
class MeetyApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB
                    .build()
            }
            .crossfade(200)
            .build()
    }
}