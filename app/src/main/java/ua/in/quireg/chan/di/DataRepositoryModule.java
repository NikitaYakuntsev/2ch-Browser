package ua.in.quireg.chan.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import ua.in.quireg.chan.db.DvachSqlHelper;
import ua.in.quireg.chan.db.FavoritesDataSource;
import ua.in.quireg.chan.db.HiddenThreadsDataSource;
import ua.in.quireg.chan.db.HistoryDataSource;
import ua.in.quireg.chan.domain.ApiReader;
import ua.in.quireg.chan.repositories.BoardsRepository;

/**
 * Created by Arcturus Mengsk on 11/21/2017, 2:04 PM.
 * 2ch-Browser
 */

@Module
public class DataRepositoryModule {

    @Provides
    @AppScope
    BoardsRepository providesBoardsRepository(ApiReader apiReader,
                                              OkHttpClient okHttpClient,
                                              Context context) {
        return new BoardsRepository(apiReader, okHttpClient, context);
    }

    @Provides
    @AppScope
    DvachSqlHelper providesDvachSqlHelper(Context context) {
        return new DvachSqlHelper(context);
    }

    @Provides
    @AppScope
    FavoritesDataSource providesFavoritesDataSource(DvachSqlHelper dvachSqlHelper) {
        FavoritesDataSource favoritesDataSource = new FavoritesDataSource(dvachSqlHelper);
        favoritesDataSource.open();
        return favoritesDataSource;
    }

    @Provides
    @AppScope
    HistoryDataSource providesHistoryDataSource(DvachSqlHelper dvachSqlHelper) {
        HistoryDataSource historyDataSource = new HistoryDataSource(dvachSqlHelper);
        historyDataSource.open();
        return historyDataSource;
    }

    @Provides
    @AppScope
    HiddenThreadsDataSource providesHiddenThreadsDataSource(DvachSqlHelper dvachSqlHelper) {
        HiddenThreadsDataSource hiddenThreadsDataSource = new HiddenThreadsDataSource(dvachSqlHelper);
        hiddenThreadsDataSource.open();
        return hiddenThreadsDataSource;
    }
}
