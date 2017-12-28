package com.vortexwolf.chan.common;

import org.acra.*;
import org.acra.annotation.*;

import android.app.Application;
import android.content.res.Resources;
import android.httpimage.BitmapMemoryCache;
import android.httpimage.FileSystemPersistence;
import android.httpimage.HttpImageManager;

import com.vortexwolf.chan.R;
import com.vortexwolf.chan.boards.makaba.MakabaApiReader;
import com.vortexwolf.chan.boards.makaba.MakabaModelsMapper;
import com.vortexwolf.chan.boards.makaba.MakabaUrlBuilder;
import com.vortexwolf.chan.boards.makaba.MakabaUrlParser;
import com.vortexwolf.chan.boards.makaba.MakabaWebsite;
import com.vortexwolf.chan.common.library.ExtendedHttpClient;
import com.vortexwolf.chan.common.library.ExtendedObjectMapper;
import com.vortexwolf.chan.common.utils.CompatibilityUtilsImpl;
import com.vortexwolf.chan.db.DvachSqlHelper;
import com.vortexwolf.chan.db.FavoritesDataSource;
import com.vortexwolf.chan.db.HiddenThreadsDataSource;
import com.vortexwolf.chan.db.HistoryDataSource;
import com.vortexwolf.chan.services.BitmapManager;
import com.vortexwolf.chan.services.CacheDirectoryManager;
import com.vortexwolf.chan.services.DvachCaptchaService;
import com.vortexwolf.chan.services.HtmlCaptchaChecker;
import com.vortexwolf.chan.services.IconsList;
import com.vortexwolf.chan.services.MailruCaptchaService;
import com.vortexwolf.chan.services.NavigationService;
import com.vortexwolf.chan.services.PostSender;
import com.vortexwolf.chan.services.SerializationService;
import com.vortexwolf.chan.services.ThreadImagesService;
import com.vortexwolf.chan.services.http.DownloadFileService;
import com.vortexwolf.chan.services.http.HttpBitmapReader;
import com.vortexwolf.chan.services.http.HttpBytesReader;
import com.vortexwolf.chan.services.http.HttpStreamReader;
import com.vortexwolf.chan.services.http.HttpStringReader;
import com.vortexwolf.chan.services.http.JsonHttpReader;
import com.vortexwolf.chan.services.presentation.DraftPostsStorage;
import com.vortexwolf.chan.services.presentation.OpenTabsManager;
import com.vortexwolf.chan.services.presentation.PagesSerializationService;
import com.vortexwolf.chan.settings.ApplicationSettings;

import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;

@ReportsCrashes(mailTo = "2chbrowsergreatagain@gmail.com",
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.LOGCAT
        },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class MainApplication extends Application {

    public static boolean MULTITOUCH_SUPPORT = false;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Constants.SDK_VERSION >= 7) {
            MULTITOUCH_SUPPORT = CompatibilityUtilsImpl.hasMultitouchSupport(this.getPackageManager());
        }

        if (Constants.SDK_VERSION >= 19) {
            CompatibilityUtilsImpl.setSerialExecutor();
        }
        ACRA.init(this);
        ApplicationSettings settings = new ApplicationSettings(this.getApplicationContext(), this.getResources());
        ExtendedHttpClient httpClient = new ExtendedHttpClient(!settings.isUnsafeSSL());
        HttpStreamReader httpStreamReader = new HttpStreamReader(httpClient, this.getResources());
        HttpBytesReader httpBytesReader = new HttpBytesReader(httpStreamReader, this.getResources());
        HttpStringReader httpStringReader = new HttpStringReader(httpBytesReader);
        HttpBitmapReader httpBitmapReader = new HttpBitmapReader(httpBytesReader);
        JsonHttpReader jsonApiReader = new JsonHttpReader(this.getResources(), new ExtendedObjectMapper(), httpStreamReader);
        DvachSqlHelper dbHelper = new DvachSqlHelper(this);
        HistoryDataSource historyDataSource = new HistoryDataSource(dbHelper);
        FavoritesDataSource favoritesDataSource = new FavoritesDataSource(dbHelper);
        HiddenThreadsDataSource hiddenThreadsDataSource = new HiddenThreadsDataSource(dbHelper);
        CacheDirectoryManager cacheManager = new CacheDirectoryManager(super.getCacheDir(), this.getPackageName(), settings);
        BitmapMemoryCache bitmapMemoryCache = new BitmapMemoryCache();
        HttpImageManager imageManager = new HttpImageManager(bitmapMemoryCache, new FileSystemPersistence(cacheManager), this.getResources(), httpBitmapReader);
        NavigationService navigationService = new NavigationService();
        DownloadFileService downloadFileService = new DownloadFileService(this.getResources(), httpStreamReader);
        MakabaUrlBuilder makabaUriBuilder = new MakabaUrlBuilder(settings);
        MakabaApiReader makabaApiReader = new MakabaApiReader(jsonApiReader, new MakabaModelsMapper(), makabaUriBuilder, this.getResources(), settings);

        Container container = Factory.getContainer();
        container.register(Resources.class, this.getResources());
        container.register(MakabaWebsite.class, new MakabaWebsite());
        container.register(MakabaUrlParser.class, new MakabaUrlParser());
        container.register(MakabaUrlBuilder.class, makabaUriBuilder);
        container.register(MakabaApiReader.class, makabaApiReader);
        container.register(ApplicationSettings.class, settings);
        container.register(DefaultHttpClient.class, httpClient);
        container.register(HttpStreamReader.class, httpStreamReader);
        container.register(HttpBytesReader.class, httpBytesReader);
        container.register(HttpStringReader.class, httpStringReader);
        container.register(HttpBitmapReader.class, httpBitmapReader);
        container.register(JsonHttpReader.class, jsonApiReader);
        container.register(PostSender.class, new PostSender(httpClient, this.getResources(), settings, httpStringReader));
        container.register(DraftPostsStorage.class, new DraftPostsStorage());
        container.register(NavigationService.class, navigationService);
        container.register(OpenTabsManager.class, new OpenTabsManager(historyDataSource, navigationService));
        container.register(CacheDirectoryManager.class, cacheManager);
        container.register(PagesSerializationService.class, new PagesSerializationService(cacheManager, new SerializationService()));
        container.register(BitmapMemoryCache.class, bitmapMemoryCache);
        container.register(HttpImageManager.class, imageManager);
        container.register(BitmapManager.class, new BitmapManager(imageManager));
        container.register(HistoryDataSource.class, historyDataSource);
        container.register(FavoritesDataSource.class, favoritesDataSource);
        container.register(HiddenThreadsDataSource.class, hiddenThreadsDataSource);
        container.register(DownloadFileService.class, downloadFileService);
        container.register(ThreadImagesService.class, new ThreadImagesService());
        container.register(HtmlCaptchaChecker.class, new HtmlCaptchaChecker(httpStringReader, settings));
        container.register(IconsList.class, new IconsList());
        container.register(MailruCaptchaService.class, new MailruCaptchaService(httpStringReader));
        container.register(DvachCaptchaService.class, new DvachCaptchaService());

        historyDataSource.open();
        favoritesDataSource.open();
        hiddenThreadsDataSource.open();


        httpClient.setCookie(settings.getCloudflareClearanceCookie());
        httpClient.setCookie(settings.getPassCodeCookie());
        httpClient.setCookie(settings.getAdultAccessCookie());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public File getCacheDir() {
        // NOTE: this method is used in Android 2.2 and higher
        return Factory.getContainer().resolve(CacheDirectoryManager.class).getCurrentCacheDirectory();
    }
}
